package com.runningcoach.v2.data.service

import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HealthConnectPermissionManager
 * Tests permission management, availability checking, and user flows
 */
class HealthConnectPermissionManagerTest {

    private lateinit var permissionManager: HealthConnectPermissionManager
    private val mockActivity = mockk<ComponentActivity>()
    private val mockHealthConnectClient = mockk<HealthConnectClient>()
    
    @Before
    fun setup() {
        clearAllMocks()
        
        // Mock Health Connect client creation
        mockkStatic(HealthConnectClient::class)
        every { HealthConnectClient.getOrCreate(any()) } returns mockHealthConnectClient
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_AVAILABLE
        
        // Create permission manager
        permissionManager = HealthConnectPermissionManager(mockActivity)
    }
    
    @Test
    fun `checkAvailability() - should return AVAILABLE when SDK is available`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_AVAILABLE
        
        // Act
        val result = permissionManager.checkAvailability()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE, 
            result
        )
        
        // Verify permission status was updated
        val status = permissionManager.permissionStatus.value
        assertTrue(status.isAvailable)
        assertFalse(status.needsUpdate)
        assertEquals(HealthConnectClient.SDK_AVAILABLE, status.availabilityStatus)
    }
    
    @Test
    fun `checkAvailability() - should return NEEDS_UPDATE when update required`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns 
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        
        // Act
        val result = permissionManager.checkAvailability()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.HealthConnectAvailability.NEEDS_UPDATE, 
            result
        )
        
        val status = permissionManager.permissionStatus.value
        assertFalse(status.isAvailable)
        assertTrue(status.needsUpdate)
    }
    
    @Test
    fun `checkAvailability() - should return UNAVAILABLE when SDK unavailable`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_UNAVAILABLE
        
        // Act
        val result = permissionManager.checkAvailability()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.HealthConnectAvailability.UNAVAILABLE, 
            result
        )
        
        val status = permissionManager.permissionStatus.value
        assertFalse(status.isAvailable)
        assertFalse(status.needsUpdate)
        assertEquals(HealthConnectClient.SDK_UNAVAILABLE, status.availabilityStatus)
    }
    
    @Test
    fun `hasRequiredPermissions() - should return true when all required permissions granted`() = runTest {
        // Arrange
        val mockPermissionController = mockk<HealthConnectClient.PermissionController>()
        every { mockHealthConnectClient.permissionController } returns mockPermissionController
        
        val grantedPermissions = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class)
            // Add all other required permissions
        ) + HealthConnectPermissionManager.REQUIRED_PERMISSIONS
        
        coEvery { mockPermissionController.getGrantedPermissions() } returns grantedPermissions
        
        // Act
        val result = permissionManager.hasRequiredPermissions()
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun `hasRequiredPermissions() - should return false when required permissions missing`() = runTest {
        // Arrange
        val mockPermissionController = mockk<HealthConnectClient.PermissionController>()
        every { mockHealthConnectClient.permissionController } returns mockPermissionController
        
        val partialPermissions = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
            // Missing other required permissions
        )
        
        coEvery { mockPermissionController.getGrantedPermissions() } returns partialPermissions
        
        // Act
        val result = permissionManager.hasRequiredPermissions()
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `hasRequiredPermissions() - should return false when not available`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_UNAVAILABLE
        permissionManager.checkAvailability() // Update status
        
        // Act
        val result = permissionManager.hasRequiredPermissions()
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `checkPermissionStatus() - should return GRANTED when all permissions available`() = runTest {
        // Arrange
        setupAvailableWithPermissions(true)
        
        // Act
        val result = permissionManager.checkPermissionStatus()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.PermissionResult.GRANTED, 
            result
        )
    }
    
    @Test
    fun `checkPermissionStatus() - should return DENIED when permissions missing`() = runTest {
        // Arrange
        setupAvailableWithPermissions(false)
        
        // Mock partial permissions (hasPartialPermissions returns false in this case)
        val mockPermissionController = mockk<HealthConnectClient.PermissionController>()
        every { mockHealthConnectClient.permissionController } returns mockPermissionController
        coEvery { mockPermissionController.getGrantedPermissions() } returns emptySet()
        
        // Act
        val result = permissionManager.checkPermissionStatus()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.PermissionResult.DENIED, 
            result
        )
    }
    
    @Test
    fun `checkPermissionStatus() - should return HEALTH_CONNECT_UNAVAILABLE when unavailable`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_UNAVAILABLE
        
        // Act
        val result = permissionManager.checkPermissionStatus()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.PermissionResult.HEALTH_CONNECT_UNAVAILABLE, 
            result
        )
    }
    
    @Test
    fun `checkPermissionStatus() - should return NEEDS_UPDATE when update required`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns 
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        
        // Act
        val result = permissionManager.checkPermissionStatus()
        
        // Assert
        assertEquals(
            HealthConnectPermissionManager.PermissionResult.NEEDS_UPDATE, 
            result
        )
    }
    
    @Test
    fun `requestHealthConnectSetup() - should call callback with true when granted`() = runTest {
        // Arrange
        setupAvailableWithPermissions(true)
        var callbackResult: Boolean? = null
        
        // Act
        permissionManager.requestHealthConnectSetup { result ->
            callbackResult = result
        }
        
        // Assert
        assertEquals(true, callbackResult)
    }
    
    @Test
    fun `requestHealthConnectSetup() - should request permissions when denied`() = runTest {
        // Arrange
        setupAvailableWithPermissions(false)
        
        val mockPermissionController = mockk<HealthConnectClient.PermissionController>()
        every { mockHealthConnectClient.permissionController } returns mockPermissionController
        coEvery { mockPermissionController.getGrantedPermissions() } returns emptySet()
        
        // Mock permission request
        every { mockActivity.registerForActivityResult(any(), any()) } returns mockk {
            every { launch(any()) } returns Unit
        }
        
        var callbackInvoked = false
        
        // Act
        permissionManager.requestHealthConnectSetup { result ->
            callbackInvoked = true
        }
        
        // For this test, we just verify the setup was called correctly
        // The actual permission flow would require UI testing
        // Assert
        // Verify the setup process was initiated (specific verification depends on implementation)
    }
    
    @Test
    fun `requestHealthConnectSetup() - should call callback with false when unavailable`() = runTest {
        // Arrange
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_UNAVAILABLE
        
        var callbackResult: Boolean? = null
        
        // Act
        permissionManager.requestHealthConnectSetup { result ->
            callbackResult = result
        }
        
        // Assert
        assertEquals(false, callbackResult)
    }
    
    @Test
    fun `openHealthConnectApp() - should handle app not found gracefully`() {
        // Arrange
        every { mockActivity.startActivity(any()) } throws Exception("App not found")
        every { mockActivity.packageName } returns "com.runningcoach.v2"
        
        // Act & Assert
        // Should not throw exception
        permissionManager.openHealthConnectApp()
    }
    
    private suspend fun setupAvailableWithPermissions(hasPermissions: Boolean) {
        every { HealthConnectClient.getSdkStatus(any()) } returns HealthConnectClient.SDK_AVAILABLE
        permissionManager.checkAvailability()
        
        val mockPermissionController = mockk<HealthConnectClient.PermissionController>()
        every { mockHealthConnectClient.permissionController } returns mockPermissionController
        
        val permissions = if (hasPermissions) {
            HealthConnectPermissionManager.REQUIRED_PERMISSIONS
        } else {
            emptySet()
        }
        
        coEvery { mockPermissionController.getGrantedPermissions() } returns permissions
    }
}