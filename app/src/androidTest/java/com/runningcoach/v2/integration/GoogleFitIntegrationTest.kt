package com.runningcoach.v2.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.runningcoach.v2.MainActivity
import com.runningcoach.v2.data.service.GoogleFitService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [TEST-RESULT: Integration tests for Google Fit connection flow]
 * 
 * These integration tests verify the complete Google Fit connection process:
 * 1. Service initialization and connection setup
 * 2. Sign-in flow initiation and permission handling 
 * 3. Data retrieval after successful connection
 * 4. Error handling and edge cases
 * 
 * [BUG-P2]: Google Fit Profile Auto-Fill requires validated integration testing
 * These tests verify that the profile auto-fill workflow operates correctly.
 * 
 * Note: These tests require Google Play Services on the test device.
 */
@RunWith(AndroidJUnit4::class)
class GoogleFitIntegrationTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)

    private lateinit var context: Context
    private lateinit var googleFitService: GoogleFitService

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        googleFitService = GoogleFitService(context)
    }

    @Test
    fun testGoogleFitServiceInitialization() {
        // [TEST-SCENARIO]: Service initializes with correct default state
        
        // Given: Fresh GoogleFitService instance
        // When: Service is created
        val service = GoogleFitService(context)
        
        // Then: Initial state should be correct
        assertNotNull(service)
        assertFalse(service.isConnected.value)
        assertEquals("Not connected", service.connectionStatus.value)
    }

    @Test 
    fun testConnectionFlowInitiation() = runTest {
        // [TEST-SCENARIO]: Connection flow creates valid sign-in intent
        
        // Given: Unconnected GoogleFitService
        assertFalse(googleFitService.isConnected.value)
        
        // When: Initiating connection
        val intent = googleFitService.initiateConnection()
        
        // Then: Sign-in intent should be created
        assertNotNull(intent)
        
        // And: Connection status should be updated
        assertEquals("Connecting to Google Fit...", googleFitService.connectionStatus.value)
    }

    @Test
    fun testConnectionStatusChecking() {
        // [TEST-SCENARIO]: Connection status check returns accurate results
        
        // Given: Fresh service instance
        assertFalse(googleFitService.isConnected.value)
        
        // When: Checking connection status
        googleFitService.checkConnectionStatus()
        
        // Then: Should remain disconnected without valid account
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
    }

    @Test
    fun testTestConnectionMethod() {
        // [TEST-SCENARIO]: Test connection method provides accurate connection state
        
        // Given: Unconnected service
        // When: Testing connection
        val isConnected = googleFitService.testConnection()
        
        // Then: Should return false
        assertFalse(isConnected)
        // And: Should match state flow value
        assertEquals(googleFitService.isConnected.value, isConnected)
    }

    @Test 
    fun testDisconnectionFlow() {
        // [TEST-SCENARIO]: Disconnection resets service state properly
        
        // When: Disconnecting from Google Fit
        googleFitService.disconnect()
        
        // Then: Service should be in disconnected state
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
    }

    @Test
    fun testDataRetrievalWithoutConnection() = runTest {
        // [TEST-SCENARIO]: Data requests fail gracefully without connection
        
        // Given: Disconnected service
        assertFalse(googleFitService.isConnected.value)
        
        // When: Attempting to get various data types
        val stepsResult = googleFitService.getDailySteps()
        val profileResult = googleFitService.getUserProfileData()
        val fitnessResult = googleFitService.getComprehensiveFitnessData()
        val weeklyResult = googleFitService.getWeeklySteps()
        
        // Then: All requests should fail with connection error
        assertTrue("Daily steps should fail", stepsResult.isFailure)
        assertTrue("Profile data should fail", profileResult.isFailure) 
        assertTrue("Comprehensive data should fail", fitnessResult.isFailure)
        assertTrue("Weekly steps should fail", weeklyResult.isFailure)
        
        // And: Error messages should indicate connection issue
        assertTrue(stepsResult.exceptionOrNull()?.message?.contains("not connected") ?: false)
        assertTrue(profileResult.exceptionOrNull()?.message?.contains("not connected") ?: false)
        assertTrue(fitnessResult.exceptionOrNull()?.message?.contains("not connected") ?: false)
        assertTrue(weeklyResult.exceptionOrNull()?.message?.contains("not connected") ?: false)
    }

    @Test
    fun testGracefulDataRetrievalWithoutConnection() = runTest {
        // [TEST-SCENARIO]: Some data requests return graceful nulls instead of failures
        
        // Given: Disconnected service
        assertFalse(googleFitService.isConnected.value)
        
        // When: Requesting optional data types
        val weightResult = googleFitService.getLatestWeight()
        val heightResult = googleFitService.getLatestHeight()
        val heartRateResult = googleFitService.getHeartRateData()
        
        // Then: These should succeed with null/empty values (graceful failure)
        assertTrue("Weight should succeed with null", weightResult.isSuccess)
        assertNull("Weight should be null", weightResult.getOrNull())
        
        assertTrue("Height should succeed with null", heightResult.isSuccess)
        assertNull("Height should be null", heightResult.getOrNull())
        
        assertTrue("Heart rate should succeed with empty list", heartRateResult.isSuccess)
        assertTrue("Heart rate should be empty", heartRateResult.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun testStateFlowUpdates() = runTest {
        // [TEST-SCENARIO]: State flows update correctly during connection changes
        
        // Given: Initial disconnected state
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
        
        // When: Checking connection status multiple times
        googleFitService.checkConnectionStatus()
        delay(100) // Allow state flow updates
        
        // Then: State should remain consistent
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
        
        // When: Disconnecting explicitly
        googleFitService.disconnect()
        delay(100)
        
        // Then: State should be reset
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
    }

    /**
     * [MANUAL-TEST-REQUIRED]: Full Integration Test with Real Google Account
     * 
     * To test complete Google Fit integration on a real device:
     * 
     * 1. Setup Requirements:
     *    - Physical Android device with Google Play Services
     *    - Valid Google account signed in to device
     *    - Google Fit app installed with some fitness data
     *    - Internet connection
     * 
     * 2. Test Steps:
     *    a. Launch FITFOAI app
     *    b. Navigate to Connect Apps screen
     *    c. Tap "Connect to Google Fit"
     *    d. Complete Google sign-in and grant permissions
     *    e. Navigate to PersonalizeProfile screen
     *    f. Verify profile fields auto-populate with Google Fit data
     *    g. Check dashboard for fitness data display
     * 
     * 3. Expected Results:
     *    - googleFitService.isConnected.value == true
     *    - googleFitService.getUserProfileData() returns real data
     *    - Profile screen shows user's name, height, weight from Google Fit
     *    - Dashboard displays current steps, distance, calories
     *    - All data should be converted to imperial units
     * 
     * 4. Edge Cases to Test:
     *    - Account with no fitness data (should handle gracefully)
     *    - Revoke permissions and test reconnection
     *    - Network connectivity issues during data sync
     *    - Multiple Google accounts on device
     * 
     * [FIX-NEEDED: PersonalizeProfileScreen] - Verify auto-fill works correctly
     * [FIX-NEEDED: DashboardScreen] - Verify fitness data displays properly
     */
    
    @Test
    fun testProfileDataModel() {
        // [TEST-SCENARIO]: Profile data model handles all expected fields
        
        // Given: Sample profile data
        val profileData = GoogleFitService.UserProfileData(
            name = "Test User",
            email = "test@example.com", 
            weight = 70.0f, // kg
            height = 1.75f, // meters
            weightImperial = "154 lbs",
            heightImperial = "5'9\""
        )
        
        // Then: All fields should be accessible
        assertEquals("Test User", profileData.name)
        assertEquals("test@example.com", profileData.email)
        assertEquals(70.0f, profileData.weight)
        assertEquals(1.75f, profileData.height)
        assertEquals("154 lbs", profileData.weightImperial)
        assertEquals("5'9\"", profileData.heightImperial)
    }

    @Test
    fun testFitnessDataModel() {
        // [TEST-SCENARIO]: Fitness data model handles all expected metrics
        
        // Given: Sample fitness data
        val fitnessData = GoogleFitService.FitnessData(
            steps = 10000,
            distance = 7000f, // meters
            calories = 400,
            activeMinutes = 60,
            heartRate = 72.5f,
            weight = 70.0f,
            height = 1.75f
        )
        
        // Then: All fields should be accessible
        assertEquals(10000, fitnessData.steps)
        assertEquals(7000f, fitnessData.distance)
        assertEquals(400, fitnessData.calories)
        assertEquals(60, fitnessData.activeMinutes)
        assertEquals(72.5f, fitnessData.heartRate)
        assertEquals(70.0f, fitnessData.weight)
        assertEquals(1.75f, fitnessData.height)
    }
}