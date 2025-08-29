package com.runningcoach.v2.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.MainActivity
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [TEST-RESULT: Integration tests for complete onboarding user journey]
 * 
 * These integration tests verify the full onboarding flow from welcome to dashboard:
 * 1. Welcome Screen → Connect Apps Screen
 * 2. Connect Apps Screen → Personalize Profile Screen  
 * 3. Personalize Profile Screen → Set Event Goal Screen
 * 4. Set Event Goal Screen → Dashboard Screen
 * 
 * [BUG-P1]: No Persistent User Profiles - Testing workflow verification
 * These tests ensure proper navigation flow and data persistence between screens.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        // Get database instance and clear any existing data
        val database = FITFOAIDatabase.getDatabase(composeTestRule.activity)
        userRepository = UserRepository(database)
        
        // Clear onboarding status to force fresh onboarding flow
        runBlocking {
            userRepository.clearUserData()
        }
    }

    @After
    fun tearDown() {
        // Clean up database after tests
        runBlocking {
            userRepository.clearUserData()
        }
    }

    @Test
    fun testCompleteOnboardingFlowNavigation() {
        // [TEST-SCENARIO]: Complete onboarding flow navigation from welcome to dashboard
        
        // Given: App starts with fresh onboarding state
        composeTestRule.waitForIdle()
        
        // Then: Should display Welcome Screen
        composeTestRule
            .onNodeWithText("Welcome to FITFO AI")
            .assertIsDisplayed()
        
        // When: Tapping Get Started button
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        
        // Then: Should navigate to Connect Apps Screen
        composeTestRule
            .onNodeWithText("Connect Your Apps")
            .assertIsDisplayed()
        
        // When: Completing connect apps (skip or connect)
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        
        // Then: Should navigate to Personalize Profile Screen
        composeTestRule
            .onNodeWithText("Let's personalize your profile")
            .assertIsDisplayed()
        
        // When: Filling out profile information
        fillProfileInformation()
        
        // And: Tapping Continue button
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        
        // Then: Should navigate to Set Event Goal Screen
        composeTestRule
            .onNodeWithText("What's your running goal?")
            .assertIsDisplayed()
        
        // When: Selecting a race goal
        composeTestRule
            .onNodeWithText("5K")
            .performClick()
        
        // And: Completing goal setup
        composeTestRule
            .onNodeWithText("Complete Setup")
            .performClick()
        
        // Then: Should navigate to Dashboard Screen
        composeTestRule
            .onNodeWithText("Welcome back")
            .assertIsDisplayed()
        
        // And: Onboarding should be marked as completed
        runBlocking {
            assert(userRepository.isOnboardingCompleted())
        }
    }

    @Test
    fun testWelcomeScreenUI() {
        // [TEST-SCENARIO]: Welcome screen displays all expected UI elements
        
        composeTestRule.waitForIdle()
        
        // Verify all expected elements are present
        composeTestRule
            .onNodeWithText("Welcome to FITFO AI")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Get Started")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test 
    fun testConnectAppsScreenUI() {
        // [TEST-SCENARIO]: Connect Apps screen displays connection options
        
        navigateToConnectApps()
        
        // Verify connection options are displayed
        composeTestRule
            .onNodeWithText("Connect Your Apps")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Google Fit")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Fitbit")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Spotify")
            .assertIsDisplayed()
        
        // Continue button should be available to skip connections
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun testPersonalizeProfileScreenUI() {
        // [TEST-SCENARIO]: Profile screen displays all input fields
        
        navigateToPersonalizeProfile()
        
        // Verify profile form elements are present
        composeTestRule
            .onNodeWithText("Let's personalize your profile")
            .assertIsDisplayed()
        
        // Check for name input field
        composeTestRule
            .onNodeWithText("Name")
            .assertIsDisplayed()
        
        // Check for height input field
        composeTestRule
            .onNodeWithText("Height")
            .assertIsDisplayed()
        
        // Check for weight input field  
        composeTestRule
            .onNodeWithText("Weight")
            .assertIsDisplayed()
        
        // Check for fitness level selection
        composeTestRule
            .onNodeWithText("Fitness Level")
            .assertIsDisplayed()
    }

    @Test
    fun testSetEventGoalScreenUI() {
        // [TEST-SCENARIO]: Event goal screen displays race options
        
        navigateToSetEventGoal()
        
        // Verify goal selection elements
        composeTestRule
            .onNodeWithText("What's your running goal?")
            .assertIsDisplayed()
        
        // Check for race distance options
        composeTestRule
            .onNodeWithText("5K")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("10K")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Half Marathon")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Marathon")
            .assertIsDisplayed()
    }

    @Test
    fun testBackNavigationDuringOnboarding() {
        // [TEST-SCENARIO]: Back navigation works correctly during onboarding
        
        // Navigate forward through screens
        navigateToPersonalizeProfile()
        
        // When: Using back navigation (if supported)
        composeTestRule.activity.onBackPressed()
        
        // Then: Should navigate back to Connect Apps screen
        composeTestRule
            .onNodeWithText("Connect Your Apps")
            .assertIsDisplayed()
    }

    @Test
    fun testDataPersistenceAcrossScreens() {
        // [TEST-SCENARIO]: User data persists as user navigates through onboarding
        
        navigateToPersonalizeProfile()
        
        // When: Entering profile information
        val testName = "Test Runner"
        composeTestRule
            .onNodeWithText("Name")
            .performTextClearance()
        composeTestRule
            .onNodeWithText("Name")
            .performTextInput(testName)
        
        // Navigate away and back (simulating interruption)
        composeTestRule.activity.onBackPressed()
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        
        // Then: Previously entered data should still be present
        composeTestRule
            .onNodeWithText(testName)
            .assertIsDisplayed()
    }

    @Test
    fun testSkipOnboardingBehavior() {
        // [TEST-SCENARIO]: User can skip optional onboarding steps
        
        composeTestRule.waitForIdle()
        
        // Start onboarding flow
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        
        // Skip connect apps
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        
        // Skip to minimal profile setup
        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("Minimal User")
        
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        
        // Complete with minimal goal
        composeTestRule
            .onNodeWithText("5K")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Complete Setup")
            .performClick()
        
        // Should still reach dashboard successfully
        composeTestRule
            .onNodeWithText("Welcome back")
            .assertIsDisplayed()
    }

    @Test
    fun testOnboardingCompletionPersistence() {
        // [TEST-SCENARIO]: Onboarding completion status persists across app restarts
        
        // Complete onboarding flow
        testCompleteOnboardingFlowNavigation()
        
        // Simulate app restart by clearing and recreating activity
        composeTestRule.activity.recreate()
        composeTestRule.waitForIdle()
        
        // Should start directly at dashboard, not welcome screen
        composeTestRule
            .onNodeWithText("Welcome back")
            .assertIsDisplayed()
        
        // Welcome screen should not be shown
        composeTestRule
            .onNodeWithText("Welcome to FITFO AI")
            .assertDoesNotExist()
    }

    // Helper methods for navigation
    private fun navigateToConnectApps() {
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
    }

    private fun navigateToPersonalizeProfile() {
        navigateToConnectApps()
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
    }

    private fun navigateToSetEventGoal() {
        navigateToPersonalizeProfile()
        fillProfileInformation()
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
    }

    private fun fillProfileInformation() {
        // Fill minimum required profile information
        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("Test Runner")
        
        composeTestRule
            .onNodeWithText("Height")
            .performTextInput("5'8\"")
        
        composeTestRule
            .onNodeWithText("Weight")
            .performTextInput("150")
        
        // Select fitness level (tap on beginner option)
        composeTestRule
            .onNodeWithText("Beginner")
            .performClick()
    }

    /**
     * [MANUAL-TEST-REQUIRED]: Real User Onboarding Flow Verification
     * 
     * To test complete onboarding experience on real device:
     * 
     * 1. Fresh App Install:
     *    - Uninstall and reinstall app
     *    - Launch app for first time
     *    - Should start at Welcome Screen
     * 
     * 2. Complete Onboarding:
     *    a. Welcome → Tap "Get Started"
     *    b. Connect Apps → Try connecting real Google Fit account
     *    c. Profile → Verify auto-fill from Google Fit data
     *    d. Goal → Select realistic race goal
     *    e. Dashboard → Verify onboarding completion
     * 
     * 3. Persistence Test:
     *    - Force close app completely
     *    - Relaunch app
     *    - Should start at Dashboard, not Welcome
     *    - Profile data should be preserved
     * 
     * 4. Reset Test:
     *    - Clear app data in device settings
     *    - Launch app again
     *    - Should start fresh onboarding flow
     * 
     * [FIX-NEEDED: UserRepository] - Implement onboarding completion persistence
     * [FIX-NEEDED: PersonalizeProfileScreen] - Implement Google Fit auto-fill
     * [FIX-NEEDED: MainActivity] - Verify start destination logic works correctly
     */
}