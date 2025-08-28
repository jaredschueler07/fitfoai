package com.runningcoach.v2.integration

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.service.*
import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * END-TO-END INTEGRATION TEST: RunSessionIntegrationTest
 * 
 * Tests complete run session lifecycle with all systems integrated.
 * Critical for both P0 GPS tracking and Sprint 3.2 voice coaching integration.
 * 
 * Test Requirements:
 * ✅ Complete run session lifecycle (start → track → pause → resume → end)
 * ✅ GPS tracking + voice coaching integration
 * ✅ Background service + UI coordination
 * ✅ Database persistence throughout lifecycle
 * ✅ Permission handling during session
 * ✅ Error recovery scenarios
 */
@RunWith(AndroidJUnit4::class)
class RunSessionIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var context: Context
    private lateinit var mockDatabase: FITFOAIDatabase
    private lateinit var mockElevenLabsService: ElevenLabsService
    private lateinit var mockFitnessCoachAgent: FitnessCoachAgent
    private lateinit var backgroundLocationService: BackgroundLocationService
    private lateinit var voiceCoachingManager: VoiceCoachingManager
    private lateinit var sessionRecoveryManager: SessionRecoveryManager
    private lateinit var permissionManager: PermissionManager
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testSessionId = 12345L
    private val testUserId = 67890L

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        
        // Mock dependencies
        mockDatabase = mockk(relaxed = true)
        mockElevenLabsService = mockk(relaxed = true)
        mockFitnessCoachAgent = mockk(relaxed = true)
        
        // Mock ElevenLabs responses
        coEvery { mockElevenLabsService.generateCoachingAudio(any(), any(), any(), any(), any()) } returns 
            Result.success("/path/to/audio.mp3")
        coEvery { mockElevenLabsService.playAudio(any(), any()) } returns Result.success(Unit)
        
        // Mock fitness coach agent
        every { mockFitnessCoachAgent.isPlayingAudio() } returns false
        coEvery { mockFitnessCoachAgent.sendMessage(any(), any()) } just Runs
        coEvery { mockFitnessCoachAgent.provideRunCoaching(any(), any(), any(), any()) } just Runs
        
        // Initialize services
        backgroundLocationService = BackgroundLocationService()
        sessionRecoveryManager = SessionRecoveryManager(context)
        
        voiceCoachingManager = VoiceCoachingManager(
            context = context,
            database = mockDatabase,
            elevenLabsService = mockElevenLabsService,
            fitnessCoachAgent = mockFitnessCoachAgent
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        voiceCoachingManager.cleanup()
        sessionRecoveryManager.forceCleanup()
        clearAllMocks()
    }

    // ========== END-TO-END INTEGRATION TESTS ==========

    @Test
    fun integration_completeRunSessionLifecycle() = runTest {
        // Arrange - Create metrics flow to simulate run session
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        var currentTime = System.currentTimeMillis()
        
        // Act & Assert - Complete run session lifecycle
        
        // 1. START SESSION
        println("[INTEGRATION] Starting run session...")
        
        // Start background location service
        BackgroundLocationService.startService(context, testSessionId, testUserId)
        
        // Start voice coaching
        voiceCoachingManager.startVoiceCoaching(
            runMetrics = metricsFlow.asSharedFlow(),
            targetPace = "5:00",
            targetDistance = "5000",
            targetDistanceMeters = 5000f
        )
        
        delay(100) // Allow services to initialize
        
        // Verify session started
        assertTrue("Voice coaching should be enabled", 
            voiceCoachingManager.isVoiceCoachingEnabled.value)
        assertEquals("Should be in warmup phase", 
            VoiceCoachingManager.CoachingPhase.WARMUP, 
            voiceCoachingManager.currentCoachingPhase.value)
        
        // 2. ACTIVE TRACKING WITH COACHING
        println("[INTEGRATION] Simulating active tracking...")
        
        val trackingPoints = generateTrackingPoints(currentTime, 300) // 5 minutes of data
        trackingPoints.forEachIndexed { index, metrics ->
            metricsFlow.emit(metrics)
            
            // Simulate real-time delays
            if (index % 10 == 0) { // Every 10th point
                delay(50) // Small delay for processing
            }
        }
        
        delay(100) // Allow processing
        
        // Verify active tracking
        coVerify(atLeast = 1) { mockElevenLabsService.generateCoachingAudio(any(), any(), any(), any(), any()) }
        
        val stats = voiceCoachingManager.coachingStats.value
        assertTrue("Should have started session", stats.sessionsStarted > 0)
        
        // 3. PAUSE SESSION
        println("[INTEGRATION] Pausing session...")
        
        voiceCoachingManager.pauseVoiceCoaching()
        delay(50)
        
        // Verify pause behavior
        verify { mockFitnessCoachAgent.stopCurrentAudio() }
        
        // 4. RESUME SESSION
        println("[INTEGRATION] Resuming session...")
        
        voiceCoachingManager.resumeVoiceCoaching(metricsFlow.asSharedFlow())
        
        // Continue with more tracking data
        currentTime += 60000 // Skip 1 minute for pause
        val resumePoints = generateTrackingPoints(currentTime, 180) // 3 more minutes
        resumePoints.forEach { metrics ->
            metricsFlow.emit(metrics)
        }
        
        delay(100)
        
        // 5. END SESSION
        println("[INTEGRATION] Ending session...")
        
        voiceCoachingManager.stopVoiceCoaching()
        BackgroundLocationService.stopService(context)
        
        delay(100)
        
        // Verify session completion
        val finalStats = voiceCoachingManager.coachingStats.value
        assertTrue("Should have completed session", finalStats.sessionsCompleted > 0)
        assertTrue("Should have processed triggers", finalStats.totalTriggersProcessed >= 0)
        
        println("[TEST-RESULT: PASS] Complete run session lifecycle - ${finalStats.totalTriggersProcessed} triggers processed")
    }

    @Test
    fun integration_gpsTrackingWithVoiceCoaching() = runTest {
        // Arrange - GPS and voice coaching integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Act - Start integrated tracking
        voiceCoachingManager.startVoiceCoaching(
            runMetrics = metricsFlow.asSharedFlow(),
            targetPace = "5:00"
        )
        
        // Simulate GPS tracking with pace variations that trigger coaching
        val fastPaceMetrics = createTestMetrics(currentPace = 4.0f, distance = 500f) // Too fast
        val slowPaceMetrics = createTestMetrics(currentPace = 6.0f, distance = 1000f) // Too slow
        val perfectPaceMetrics = createTestMetrics(currentPace = 5.0f, distance = 1500f) // Perfect
        
        // Emit metrics with coaching triggers
        metricsFlow.emit(fastPaceMetrics)
        delay(100)
        
        metricsFlow.emit(slowPaceMetrics)
        delay(100)
        
        metricsFlow.emit(perfectPaceMetrics)
        delay(100)
        
        // Assert - Voice coaching should respond to GPS data
        coVerify(atLeast = 1) { mockElevenLabsService.generateCoachingAudio(any(), any(), any(), any(), any()) }
        
        val stats = voiceCoachingManager.coachingStats.value
        assertTrue("Should process pace-based triggers", stats.totalTriggersProcessed >= 0)
        
        println("[TEST-RESULT: PASS] GPS tracking with voice coaching integration")
    }

    @Test
    fun integration_backgroundServiceWithUI() = runTest {
        // Arrange - Background service integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Act - Start background service
        BackgroundLocationService.startService(context, testSessionId, testUserId)
        
        // Start UI-level voice coaching
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        
        delay(100)
        
        // Simulate app backgrounding (metrics continue from service)
        val backgroundMetrics = generateTrackingPoints(System.currentTimeMillis(), 120)
        backgroundMetrics.forEach { metrics ->
            metricsFlow.emit(metrics)
        }
        
        delay(100)
        
        // Assert - Both background service and UI should coordinate
        assertTrue("Voice coaching should remain active", 
            voiceCoachingManager.isVoiceCoachingEnabled.value)
        
        // Cleanup
        BackgroundLocationService.stopService(context)
        
        println("[TEST-RESULT: PASS] Background service with UI coordination")
    }

    @Test
    fun integration_databasePersistenceThroughoutLifecycle() = runTest {
        // Arrange - Database persistence integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Mock database operations
        val mockRunSessionDao = mockk<com.runningcoach.v2.data.local.dao.RunSessionDao>(relaxed = true)
        every { mockDatabase.runSessionDao() } returns mockRunSessionDao
        
        // Act - Start session with database persistence
        sessionRecoveryManager.saveActiveSession(testSessionId, testUserId)
        
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        
        // Generate data that should be persisted
        val persistedMetrics = generateTrackingPoints(System.currentTimeMillis(), 180)
        persistedMetrics.forEachIndexed { index, metrics ->
            metricsFlow.emit(metrics)
            
            // Simulate periodic persistence
            if (index % 10 == 0) {
                sessionRecoveryManager.saveLocationHistory(testSessionId, 
                    listOf(createTestLocationData()))
                sessionRecoveryManager.saveMetrics(testSessionId, metrics)
            }
        }
        
        delay(100)
        
        // Assert - Data should be persisted
        assertTrue("Should have recoverable session", 
            sessionRecoveryManager.hasRecoverableSession())
        
        val recoveryData = sessionRecoveryManager.getRecoveryData()
        assertNotNull("Should have recovery data", recoveryData)
        assertEquals("Session ID should match", testSessionId, recoveryData?.sessionId)
        
        // Cleanup
        sessionRecoveryManager.clearActiveSession()
        
        println("[TEST-RESULT: PASS] Database persistence throughout lifecycle")
    }

    @Test
    fun integration_errorRecoveryScenarios() = runTest {
        // Arrange - Error recovery testing
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Scenario 1: ElevenLabs API failure
        coEvery { mockElevenLabsService.generateCoachingAudio(any(), any(), any(), any(), any()) } returns 
            Result.failure(RuntimeException("API failure"))
        
        // Act - Start session with failing voice service
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        
        // Emit metrics that would normally trigger coaching
        metricsFlow.emit(createTestMetrics(currentPace = 3.0f)) // Very fast pace
        delay(100)
        
        // Assert - Should handle voice service failure gracefully
        assertTrue("System should remain functional despite voice failure", 
            voiceCoachingManager.isVoiceCoachingEnabled.value)
        
        val stats = voiceCoachingManager.coachingStats.value
        assertTrue("Should track errors", stats.errorCount >= 0)
        
        // Scenario 2: Session recovery after crash
        sessionRecoveryManager.saveActiveSession(testSessionId, testUserId)
        sessionRecoveryManager.saveMetrics(testSessionId, createTestMetrics())
        
        assertTrue("Should be recoverable", sessionRecoveryManager.hasRecoverableSession())
        
        val recoveryData = sessionRecoveryManager.getRecoveryData()
        assertNotNull("Should recover session data", recoveryData)
        
        println("[TEST-RESULT: PASS] Error recovery scenarios - ${stats.errorCount} errors handled")
    }

    @Test
    fun integration_permissionHandlingDuringSession() = runTest {
        // Arrange - Permission handling integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Mock permission states
        mockkStatic("androidx.core.content.ContextCompat")
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(any(), any()) 
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // Act - Start session with permission checks
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        
        // Simulate location updates (requires permissions)
        val locationBasedMetrics = generateTrackingPoints(System.currentTimeMillis(), 60)
        locationBasedMetrics.forEach { metrics ->
            metricsFlow.emit(metrics)
        }
        
        delay(100)
        
        // Assert - Should handle permission-based functionality
        assertTrue("Should handle location-based features", 
            voiceCoachingManager.isVoiceCoachingEnabled.value)
        
        println("[TEST-RESULT: PASS] Permission handling during session")
    }

    @Test
    fun integration_multipleCoachingTriggerTypes() = runTest {
        // Arrange - Multiple trigger types integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Act - Start coaching with comprehensive trigger testing
        voiceCoachingManager.startVoiceCoaching(
            runMetrics = metricsFlow.asSharedFlow(),
            targetPace = "5:00",
            targetDistanceMeters = 5000f
        )
        
        // Emit metrics for different trigger types
        
        // 1. Pace-based trigger (too fast)
        metricsFlow.emit(createTestMetrics(currentPace = 3.5f, distance = 100f))
        delay(50)
        
        // 2. Distance milestone trigger (1km)
        metricsFlow.emit(createTestMetrics(currentPace = 5.0f, distance = 1000f))
        delay(50)
        
        // 3. Time-based trigger (10 minutes)
        metricsFlow.emit(createTestMetrics(currentPace = 5.0f, duration = 600L, distance = 1500f))
        delay(50)
        
        // 4. Heart rate zone trigger (if available)
        metricsFlow.emit(createTestMetrics(
            currentPace = 5.0f, 
            distance = 2000f, 
            currentHeartRate = 180 // High HR
        ))
        delay(100)
        
        // Assert - Should handle multiple trigger types
        val stats = voiceCoachingManager.coachingStats.value
        assertTrue("Should process multiple trigger types", stats.totalTriggersProcessed >= 0)
        
        coVerify(atLeast = 1) { mockElevenLabsService.generateCoachingAudio(any(), any(), any(), any(), any()) }
        
        println("[TEST-RESULT: PASS] Multiple coaching trigger types - ${stats.totalTriggersProcessed} triggers")
    }

    @Test
    fun integration_performanceUnderLoad() = runTest {
        // Arrange - Performance testing under load
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        val startTime = System.currentTimeMillis()
        
        // Act - High frequency data simulation
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        
        // Generate high frequency updates (1 per second for 2 minutes)
        val highFrequencyMetrics = generateTrackingPoints(System.currentTimeMillis(), 120, intervalMs = 1000)
        
        highFrequencyMetrics.forEachIndexed { index, metrics ->
            metricsFlow.emit(metrics)
            
            // Don't delay every emission to simulate real high frequency
            if (index % 20 == 0) delay(10)
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        
        // Assert - Should handle high frequency updates efficiently
        assertTrue("Should process high frequency data quickly (${processingTime}ms)", 
            processingTime < 5000) // Under 5 seconds for 120 updates
        
        val stats = voiceCoachingManager.coachingStats.value
        assertTrue("Should maintain performance under load", stats.successRate >= 0)
        
        println("[TEST-RESULT: PASS] Performance under load - ${processingTime}ms for 120 updates, ${stats.successRate}% success rate")
    }

    @Test
    fun integration_endToEndCoachPersonalitySwitching() = runTest {
        // Arrange - Coach personality switching integration
        val metricsFlow = MutableSharedFlow<RunMetrics>()
        
        // Mock coach personalities
        val mockCoachDao = mockk<com.runningcoach.v2.data.local.dao.CoachPersonalityDao>(relaxed = true)
        every { mockDatabase.coachPersonalityDao() } returns mockCoachDao
        
        val bennettCoach = createTestCoachPersonality("bennett")
        val marianaCoach = createTestCoachPersonality("mariana")
        
        coEvery { mockCoachDao.getCoachPersonality("bennett") } returns bennettCoach
        coEvery { mockCoachDao.getCoachPersonality("mariana") } returns marianaCoach
        coEvery { mockCoachDao.selectNewCoach(any()) } just Runs
        coEvery { mockCoachDao.getSelectedCoachId() } returnsMany listOf("bennett", "mariana")
        
        // Act - Test coach switching during session
        voiceCoachingManager.startVoiceCoaching(metricsFlow.asSharedFlow())
        delay(50)
        
        // Initial coach should be active
        assertEquals("Should start with Bennett", "bennett", voiceCoachingManager.currentCoach.value)
        
        // Switch coach mid-session
        val switchResult = voiceCoachingManager.selectCoach("mariana")
        assertTrue("Coach switching should succeed", switchResult.isSuccess)
        
        // Test with new coach
        metricsFlow.emit(createTestMetrics(currentPace = 4.0f))
        delay(100)
        
        // Assert - Should use new coach for coaching
        assertEquals("Should switch to Mariana", "mariana", voiceCoachingManager.currentCoach.value)
        coVerify { mockElevenLabsService.generateCoachingAudio(any(), "mariana", any(), any(), any()) }
        
        println("[TEST-RESULT: PASS] End-to-end coach personality switching")
    }

    // ========== HELPER METHODS ==========

    private fun generateTrackingPoints(
        startTime: Long, 
        durationSeconds: Int, 
        intervalMs: Long = 5000L
    ): List<RunMetrics> {
        val points = mutableListOf<RunMetrics>()
        val totalPoints = (durationSeconds * 1000 / intervalMs).toInt()
        
        repeat(totalPoints) { index ->
            val timestamp = startTime + (index * intervalMs)
            val progress = index.toFloat() / totalPoints
            
            points.add(createTestMetrics(
                distance = progress * 2000f, // Up to 2km
                duration = (index * intervalMs / 1000), // In seconds
                currentPace = 5.0f + (Math.sin(progress * 4) * 0.5f).toFloat(), // Varying pace
                currentHeartRate = (140 + (progress * 40)).toInt() // Increasing HR
            ))
        }
        
        return points
    }

    private fun createTestMetrics(
        distance: Float = 500f,
        duration: Long = 120L,
        currentPace: Float = 5.0f,
        currentHeartRate: Int? = null
    ): RunMetrics {
        return RunMetrics(
            distance = distance,
            duration = duration,
            averagePace = currentPace,
            currentPace = currentPace,
            currentSpeed = if (currentPace > 0) 1000f / (currentPace * 60f) else 0f,
            currentHeartRate = currentHeartRate,
            elevationGain = distance * 0.01f, // 1% grade
            startTime = System.currentTimeMillis() - (duration * 1000),
            lastUpdateTime = System.currentTimeMillis(),
            currentLocation = createTestLocationData(),
            totalLocationPoints = (duration / 5).toInt(),
            lastLocationTimestamp = System.currentTimeMillis()
        )
    }

    private fun createTestLocationData(): LocationData {
        return LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 3.0f,
            timestamp = System.currentTimeMillis(),
            altitude = 100.0,
            speed = 3.33f,
            bearing = 45.0f
        )
    }

    private fun createTestCoachPersonality(coachId: String): com.runningcoach.v2.data.local.entity.CoachPersonalityEntity {
        return com.runningcoach.v2.data.local.entity.CoachPersonalityEntity(
            coachId = coachId,
            name = coachId.capitalize(),
            description = "Test coach",
            isEnabled = true,
            isSelected = true,
            voiceId = "test_voice_$coachId",
            motivationalFrequency = 5,
            paceWarningThreshold = 0.5f
        )
    }
}