package com.runningcoach.v2.data.manager

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.HealthConnectDailySummaryDao
import com.runningcoach.v2.data.local.dao.UserDao
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.local.dao.ConnectedAppDao
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.data.local.entity.DataSource
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HealthConnectManager
 * Tests core functionality including connection management, data sync, and error handling
 */
class HealthConnectManagerTest {

    private lateinit var healthConnectManager: HealthConnectManager
    private val mockContext = mockk<Context>()
    private val mockDatabase = mockk<FITFOAIDatabase>()
    private val mockPermissionManager = mockk<HealthConnectPermissionManager>()
    private val mockHealthConnectClient = mockk<HealthConnectClient>()
    
    // DAOs
    private val mockUserDao = mockk<UserDao>()
    private val mockRunSessionDao = mockk<RunSessionDao>()
    private val mockHealthConnectDao = mockk<HealthConnectDailySummaryDao>()
    private val mockConnectedAppDao = mockk<ConnectedAppDao>()
    
    // Test data
    private val testUser = UserEntity(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        height = 70,
        weight = 150.0f
    )
    
    private val testRunSession = RunSessionEntity(
        id = 1L,
        userId = 1L,
        startTime = System.currentTimeMillis() - 3600000,
        endTime = System.currentTimeMillis(),
        duration = 3600000,
        distance = 5000f,
        avgSpeed = 2.5f,
        source = DataSource.FITFOAI
    )
    
    @Before
    fun setup() {
        // Clear all mocks
        clearAllMocks()
        
        // Setup database mocks
        every { mockDatabase.userDao() } returns mockUserDao
        every { mockDatabase.runSessionDao() } returns mockRunSessionDao
        every { mockDatabase.healthConnectDailySummaryDao() } returns mockHealthConnectDao
        every { mockDatabase.connectedAppDao() } returns mockConnectedAppDao
        
        // Mock static database creation
        mockkObject(FITFOAIDatabase.Companion)
        every { FITFOAIDatabase.getDatabase(any()) } returns mockDatabase
        
        // Mock Health Connect client creation
        mockkStatic(HealthConnectClient::class)
        every { HealthConnectClient.getOrCreate(any()) } returns mockHealthConnectClient
        
        // Default permission manager behavior
        coEvery { mockPermissionManager.checkAvailability() } returns 
            HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        
        // Default user DAO behavior
        coEvery { mockUserDao.getCurrentUser() } returns flowOf(testUser)
        
        // Default connection app DAO behavior
        coEvery { mockConnectedAppDao.getConnectedAppByType(any(), any()) } returns null
        coEvery { mockConnectedAppDao.insertConnectedApp(any()) } returns Unit
        
        // Create manager instance
        healthConnectManager = HealthConnectManager.getInstance(mockContext, mockPermissionManager)
    }
    
    @Test
    fun `connect() - should succeed when Health Connect is available and permissions granted`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.checkAvailability() } returns 
            HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        
        // Act
        val result = healthConnectManager.connect()
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(
            HealthConnectManager.ConnectionState.CONNECTED,
            healthConnectManager.connectionState.first()
        )
        
        // Verify interactions
        coVerify { mockPermissionManager.checkAvailability() }
        coVerify { mockPermissionManager.hasRequiredPermissions() }
    }
    
    @Test
    fun `connect() - should fail when Health Connect is unavailable`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.checkAvailability() } returns 
            HealthConnectPermissionManager.HealthConnectAvailability.UNAVAILABLE
        
        // Act
        val result = healthConnectManager.connect()
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals(
            HealthConnectManager.ConnectionState.UNAVAILABLE,
            healthConnectManager.connectionState.first()
        )
    }
    
    @Test
    fun `connect() - should require permissions when not granted`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.checkAvailability() } returns 
            HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns false
        
        // Act
        val result = healthConnectManager.connect()
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals(
            HealthConnectManager.ConnectionState.AWAITING_PERMISSIONS,
            healthConnectManager.connectionState.first()
        )
    }
    
    @Test
    fun `onPermissionsGranted() - should connect when permissions are granted`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        
        // Act
        healthConnectManager.onPermissionsGranted(true)
        
        // Assert
        assertEquals(
            HealthConnectManager.ConnectionState.CONNECTED,
            healthConnectManager.connectionState.first()
        )
    }
    
    @Test
    fun `onPermissionsGranted() - should remain disconnected when permissions are denied`() = runTest {
        // Act
        healthConnectManager.onPermissionsGranted(false)
        
        // Assert
        assertEquals(
            HealthConnectManager.ConnectionState.DISCONNECTED,
            healthConnectManager.connectionState.first()
        )
        
        // Verify error is set
        val error = healthConnectManager.lastError.first()
        assertNotNull(error)
        assertEquals(
            HealthConnectManager.HealthConnectError.ErrorCode.PERMISSION_DENIED,
            error?.code
        )
    }
    
    @Test
    fun `performFullSync() - should fail when not connected`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns false
        
        // Act
        val result = healthConnectManager.performFullSync()
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals(
            HealthConnectManager.SyncState.ERROR,
            healthConnectManager.syncState.first()
        )
    }
    
    @Test
    fun `performFullSync() - should succeed when connected and sync data`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        coEvery { mockHealthConnectClient.readRecords(any()) } returns mockk {
            every { records } returns emptyList()
        }
        
        // Act
        val result = healthConnectManager.performFullSync()
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(
            HealthConnectManager.SyncState.SUCCESS,
            healthConnectManager.syncState.first()
        )
        
        // Verify last sync time was updated
        assertNotNull(healthConnectManager.lastSyncTime.first())
    }
    
    @Test
    fun `writeRunSession() - should succeed when connected`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        coEvery { mockHealthConnectClient.insertRecords(any()) } returns mockk {
            every { recordIdsList } returns listOf("test-record-id")
        }
        coEvery { mockRunSessionDao.updateHealthConnectSync(any(), any(), any(), any()) } returns Unit
        
        // Act
        val result = healthConnectManager.writeRunSession(testRunSession)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify Health Connect sync was updated
        coVerify { 
            mockRunSessionDao.updateHealthConnectSync(
                testRunSession.id, 
                true, 
                "test-record-id"
            ) 
        }
    }
    
    @Test
    fun `writeRunSession() - should fail when not connected`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns false
        
        // Act
        val result = healthConnectManager.writeRunSession(testRunSession)
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Not connected") ?: false)
    }
    
    @Test
    fun `disconnect() - should update connection state and cancel sync`() = runTest {
        // Arrange
        coEvery { mockConnectedAppDao.updateConnectionStatus(any(), any(), any(), any()) } returns Unit
        
        // Act
        healthConnectManager.disconnect()
        
        // Assert
        assertEquals(
            HealthConnectManager.ConnectionState.DISCONNECTED,
            healthConnectManager.connectionState.first()
        )
        
        // Verify connection status was updated in database
        coVerify { mockConnectedAppDao.updateConnectionStatus(1L, "HEALTH_CONNECT", false, null) }
    }
    
    @Test
    fun `migrateFromGoogleFit() - should migrate unmigrated sessions`() = runTest {
        // Arrange
        val unmigratedSessions = listOf(
            testRunSession.copy(source = DataSource.GOOGLE_FIT, migratedToHealthConnect = false)
        )
        
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        coEvery { mockRunSessionDao.getUnmigratedGoogleFitSessions() } returns unmigratedSessions
        coEvery { mockHealthConnectClient.insertRecords(any()) } returns mockk {
            every { recordIdsList } returns listOf("migrated-record-id")
        }
        coEvery { mockRunSessionDao.updateHealthConnectSync(any(), any(), any(), any()) } returns Unit
        coEvery { mockRunSessionDao.markAsMigrated(any()) } returns Unit
        
        // Act
        val result = healthConnectManager.migrateFromGoogleFit()
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify session was marked as migrated
        coVerify { mockRunSessionDao.markAsMigrated(testRunSession.id) }
    }
    
    @Test
    fun `error handling - should set appropriate error states`() = runTest {
        // Arrange
        coEvery { mockPermissionManager.hasRequiredPermissions() } returns true
        coEvery { mockHealthConnectClient.readRecords(any()) } throws Exception("Network error")
        
        // Act
        val result = healthConnectManager.performFullSync()
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals(
            HealthConnectManager.SyncState.ERROR,
            healthConnectManager.syncState.first()
        )
        
        val error = healthConnectManager.lastError.first()
        assertNotNull(error)
        assertEquals(
            HealthConnectManager.HealthConnectError.ErrorCode.API_ERROR,
            error?.code
        )
        assertTrue(error?.message?.contains("Network error") ?: false)
    }
}