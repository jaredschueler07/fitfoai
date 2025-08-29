package com.runningcoach.v2.integration

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.*
import com.runningcoach.v2.data.manager.HealthConnectManager
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Integration tests for Health Connect functionality
 * Tests the complete workflow from permissions to data sync
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectIntegrationTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: FITFOAIDatabase
    private lateinit var context: Context
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var permissionManager: HealthConnectPermissionManager
    
    private val testUser = UserEntity(
        id = 1L,
        name = "Integration Test User",
        email = "integration@test.com",
        height = 70,
        weight = 160.0f
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            FITFOAIDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Create test user
        runTest {
            database.userDao().insertUser(testUser)
        }
        
        // Note: For real integration tests, you would need a proper activity context
        // Here we're testing the database integration primarily
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun databaseMigrationIncludesHealthConnectTables() = runTest {
        // Arrange & Act
        val healthConnectDao = database.healthConnectDailySummaryDao()
        
        // Test that we can insert Health Connect data
        val today = LocalDate.now()
        val dateMillis = today.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val summary = HealthConnectDailySummaryEntity(
            userId = testUser.id,
            date = dateMillis,
            steps = 10000,
            distance = 8000f,
            calories = 500,
            activeMinutes = 60,
            avgHeartRate = 75f,
            lastSynced = System.currentTimeMillis()
        )

        // Act
        val summaryId = healthConnectDao.insertOrUpdateDailySummary(summary)

        // Assert
        assertTrue(summaryId > 0)
        
        val retrieved = healthConnectDao.getDailySummaryForDate(testUser.id, dateMillis)
        assertNotNull(retrieved)
        assertEquals(summary.steps, retrieved?.steps)
        assertEquals(summary.distance, retrieved?.distance, 0.1f)
        assertEquals(summary.calories, retrieved?.calories)
    }

    @Test
    fun runSessionEntitySupportsHealthConnectFields() = runTest {
        // Arrange
        val runSessionDao = database.runSessionDao()
        
        val runSession = RunSessionEntity(
            userId = testUser.id,
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            duration = 3600000,
            distance = 5000f,
            avgSpeed = 2.5f,
            source = DataSource.HEALTH_CONNECT,
            healthConnectSessionId = "test-hc-session-id",
            syncedWithHealthConnect = true,
            healthConnectLastSyncTime = System.currentTimeMillis(),
            migratedToHealthConnect = false
        )

        // Act
        val sessionId = runSessionDao.insertRunSession(runSession)

        // Assert
        assertTrue(sessionId > 0)
        
        val retrieved = runSessionDao.getSessionById(sessionId)
        assertNotNull(retrieved)
        assertEquals(DataSource.HEALTH_CONNECT, retrieved?.source)
        assertEquals("test-hc-session-id", retrieved?.healthConnectSessionId)
        assertTrue(retrieved?.syncedWithHealthConnect ?: false)
        assertFalse(retrieved?.migratedToHealthConnect ?: true)
    }

    @Test
    fun healthConnectRunSessionQueries() = runTest {
        // Arrange
        val runSessionDao = database.runSessionDao()
        
        val healthConnectSession = RunSessionEntity(
            userId = testUser.id,
            startTime = System.currentTimeMillis() - 7200000,
            endTime = System.currentTimeMillis() - 3600000,
            duration = 3600000,
            distance = 5000f,
            source = DataSource.HEALTH_CONNECT,
            healthConnectSessionId = "hc-session-1",
            syncedWithHealthConnect = true
        )
        
        val googleFitSession = RunSessionEntity(
            userId = testUser.id,
            startTime = System.currentTimeMillis() - 10800000,
            endTime = System.currentTimeMillis() - 7200000,
            duration = 3600000,
            distance = 3000f,
            source = DataSource.GOOGLE_FIT,
            googleFitSessionId = "gf-session-1",
            syncedWithGoogleFit = true,
            migratedToHealthConnect = false
        )

        // Act
        val hcSessionId = runSessionDao.insertRunSession(healthConnectSession)
        val gfSessionId = runSessionDao.insertRunSession(googleFitSession)

        // Test Health Connect session retrieval
        val retrievedHcSession = runSessionDao.getSessionByHealthConnectId("hc-session-1")
        assertNotNull(retrievedHcSession)
        assertEquals(hcSessionId, retrievedHcSession?.id)

        // Test Google Fit migration query
        val unmigratedSessions = runSessionDao.getUnmigratedGoogleFitSessions()
        assertEquals(1, unmigratedSessions.size)
        assertEquals(gfSessionId, unmigratedSessions[0].id)
        
        // Test migration marking
        runSessionDao.markAsMigrated(gfSessionId)
        val afterMigration = runSessionDao.getUnmigratedGoogleFitSessions()
        assertEquals(0, afterMigration.size)
    }

    @Test
    fun healthConnectDailySummaryAnalytics() = runTest {
        // Arrange
        val healthConnectDao = database.healthConnectDailySummaryDao()
        val today = LocalDate.now()
        
        val summaries = listOf(
            HealthConnectDailySummaryEntity(
                userId = testUser.id,
                date = today.minusDays(6).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                distance = 6000f,
                calories = 400,
                activeMinutes = 45,
                avgHeartRate = 75f,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = testUser.id,
                date = today.minusDays(4).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 12000,
                distance = 9000f,
                calories = 600,
                activeMinutes = 70,
                avgHeartRate = 80f,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = testUser.id,
                date = today.minusDays(2).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 6000,
                distance = 4500f,
                calories = 300,
                activeMinutes = 35,
                avgHeartRate = 70f,
                lastSynced = System.currentTimeMillis()
            )
        )

        // Insert test data
        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }

        // Test weekly analytics
        val startDate = today.minusDays(7).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val endDate = today.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val weeklySummary = healthConnectDao.getWeeklyActivitySummary(testUser.id, startDate, endDate)
        
        // Assert
        assertEquals(26000, weeklySummary.totalSteps) // 8000 + 12000 + 6000
        assertEquals(19500f, weeklySummary.totalDistance, 0.1f)
        assertEquals(1300, weeklySummary.totalCalories)
        assertEquals(150, weeklySummary.totalActiveMinutes)
        assertEquals(75f, weeklySummary.avgHeartRate, 1f)
        assertEquals(3, weeklySummary.daysWithData)

        // Test activity level calculations
        val activeDay = summaries[1] // 12000 steps, 70 active minutes
        assertEquals(
            HealthConnectDailySummaryEntity.ActivityLevel.VERY_ACTIVE,
            activeDay.getActivityLevel()
        )
        
        val moderateDay = summaries[0] // 8000 steps, 45 active minutes
        assertEquals(
            HealthConnectDailySummaryEntity.ActivityLevel.ACTIVE,
            moderateDay.getActivityLevel()
        )
    }

    @Test
    fun dataSourceEnumIncludesHealthConnectValues() {
        // Test that new enum values are available
        val healthConnectSource = DataSource.HEALTH_CONNECT
        val migrationSource = DataSource.GOOGLE_FIT_MIGRATION
        
        assertNotNull(healthConnectSource)
        assertNotNull(migrationSource)
        
        // Test enum ordering doesn't break existing values
        assertEquals("FITFOAI", DataSource.FITFOAI.name)
        assertEquals("GOOGLE_FIT", DataSource.GOOGLE_FIT.name)
        assertEquals("HEALTH_CONNECT", DataSource.HEALTH_CONNECT.name)
        assertEquals("GOOGLE_FIT_MIGRATION", DataSource.GOOGLE_FIT_MIGRATION.name)
    }

    @Test
    fun healthConnectSummaryEntityValidation() {
        // Test entity business logic methods
        val summary = HealthConnectDailySummaryEntity(
            userId = testUser.id,
            date = System.currentTimeMillis(),
            steps = 10000,
            distance = 8000f,
            calories = 500,
            activeMinutes = 60,
            avgHeartRate = 75f,
            maxHeartRate = 150f,
            minHeartRate = 60f,
            lastSynced = System.currentTimeMillis()
        )

        // Test significant data detection
        assertTrue(summary.hasSignificantData())
        
        // Test heart rate data availability
        assertTrue(summary.hasHeartRateData())
        
        // Test heart rate range
        val heartRateRange = summary.getHeartRateRange()
        assertNotNull(heartRateRange)
        assertEquals(60f, heartRateRange?.first)
        assertEquals(150f, heartRateRange?.second)
        
        // Test activity level calculation
        assertEquals(
            HealthConnectDailySummaryEntity.ActivityLevel.VERY_ACTIVE,
            summary.getActivityLevel()
        )
        
        // Test active calories calculation
        val activeCalories = summary.getActiveCalories()
        assertTrue(activeCalories > 0)
        assertTrue(activeCalories < summary.calories) // Should be less than total
    }

    @Test
    fun healthConnectConnectedAppIntegration() = runTest {
        // Arrange
        val connectedAppDao = database.connectedAppDao()
        
        val healthConnectApp = ConnectedAppEntity(
            userId = testUser.id,
            appType = "HEALTH_CONNECT",
            appName = "Health Connect",
            isConnected = true,
            lastSyncTime = System.currentTimeMillis()
        )

        // Act
        connectedAppDao.insertConnectedApp(healthConnectApp)

        // Assert
        val retrievedApp = connectedAppDao.getConnectedAppByType(testUser.id, "HEALTH_CONNECT")
        assertNotNull(retrievedApp)
        assertEquals("Health Connect", retrievedApp?.appName)
        assertTrue(retrievedApp?.isConnected ?: false)
        
        // Test update connection status
        connectedAppDao.updateConnectionStatus(
            userId = testUser.id,
            appType = "HEALTH_CONNECT",
            isConnected = false,
            lastSyncTime = null
        )
        
        val updatedApp = connectedAppDao.getConnectedAppByType(testUser.id, "HEALTH_CONNECT")
        assertNotNull(updatedApp)
        assertFalse(updatedApp?.isConnected ?: true)
        assertNull(updatedApp?.lastSyncTime)
    }
}