package com.runningcoach.v2.data.service

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GoogleFitService
 * 
 * Note: These tests primarily verify the service structure and error handling.
 * Real Google Fit API integration tests require actual device testing with 
 * valid Google accounts and permissions.
 */
class GoogleFitServiceTest {

    private lateinit var context: Context
    private lateinit var googleFitService: GoogleFitService

    @Before
    fun setUp() {
        context = mockk<Context>(relaxed = true)
        
        // Mock context responses
        every { context.packageName } returns "com.runningcoach.v2"
        
        googleFitService = GoogleFitService(context)
    }

    @Test
    fun `service initialization should set up proper configuration`() {
        // Verify that the service is properly initialized
        assertNotNull(googleFitService)
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
    }

    @Test
    fun `getDailySteps should return failure when not connected`() = runTest {
        // Given: No Google account connected
        
        // When: Requesting daily steps
        val result = googleFitService.getDailySteps()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `getLatestWeight should return null when not connected`() = runTest {
        // When: Requesting weight data without connection
        val result = googleFitService.getLatestWeight()
        
        // Then: Should return success with null value (graceful failure)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getLatestHeight should return null when not connected`() = runTest {
        // When: Requesting height data without connection
        val result = googleFitService.getLatestHeight()
        
        // Then: Should return success with null value (graceful failure)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getHeartRateData should return empty list when not connected`() = runTest {
        // When: Requesting heart rate data without connection
        val result = googleFitService.getHeartRateData()
        
        // Then: Should return success with empty list (graceful failure)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun `getUserProfileData should fail when not connected`() = runTest {
        // When: Requesting user profile data without connection
        val result = googleFitService.getUserProfileData()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `getComprehensiveFitnessData should fail when not connected`() = runTest {
        // When: Requesting comprehensive fitness data without connection
        val result = googleFitService.getComprehensiveFitnessData()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `getWeeklySteps should fail when not connected`() = runTest {
        // When: Requesting weekly steps without connection
        val result = googleFitService.getWeeklySteps()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `getDailyDistance should fail when not connected`() = runTest {
        // When: Requesting daily distance without connection
        val result = googleFitService.getDailyDistance()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `getDailyCalories should fail when not connected`() = runTest {
        // When: Requesting daily calories without connection
        val result = googleFitService.getDailyCalories()
        
        // Then: Should fail with proper error message
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Google Fit not connected") ?: false)
    }

    @Test
    fun `initiateConnection should return intent for sign in`() {
        // When: Initiating connection
        val intent = googleFitService.initiateConnection()
        
        // Then: Should return an intent (even if empty when mocked)
        assertNotNull(intent)
    }

    @Test
    fun `testConnection should return current connection status`() {
        // When: Testing connection
        val isConnected = googleFitService.testConnection()
        
        // Then: Should return false when not connected
        assertFalse(isConnected)
    }

    @Test
    fun `disconnect should reset connection state`() {
        // When: Disconnecting from Google Fit
        googleFitService.disconnect()
        
        // Then: Should update connection status
        assertFalse(googleFitService.isConnected.value)
        assertEquals("Not connected", googleFitService.connectionStatus.value)
    }

    /**
     * Integration test instructions:
     * 
     * To test the actual Google Fit integration:
     * 1. Run the app on a physical device
     * 2. Grant Google Fit permissions
     * 3. Ensure the device has Google Fit app installed with some fitness data
     * 4. Use the app's connect functionality to link Google Fit
     * 5. Navigate to the profile screen and verify real data is displayed
     * 
     * Expected behavior with real connection:
     * - getDailySteps() should return actual step count from Google Fit
     * - getLatestWeight() should return last recorded weight or null
     * - getLatestHeight() should return last recorded height or null
     * - getUserProfileData() should return real profile data with conversions
     * - getComprehensiveFitnessData() should return combined real data
     * 
     * Data sources Google Fit can provide:
     * - Steps from phone's pedometer or fitness trackers
     * - Heart rate from connected wearables
     * - Weight/height from manual entries or smart scales
     * - Distance from GPS activities
     * - Calories from activity tracking
     */
}