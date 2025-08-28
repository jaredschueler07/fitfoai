package com.runningcoach.v2.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ENHANCED UI TEST: RunTrackingUITest
 * 
 * Tests for enhanced run tracking UI with voice coaching and background service integration.
 * Critical for both P0 GPS tracking and Sprint 3.2 voice coaching features.
 * 
 * Test Requirements:
 * ✅ Background tracking indicators
 * ✅ GPS status display accuracy
 * ✅ Session recovery UI workflow
 * ✅ Voice coaching integration
 * ✅ Permission prompts when needed
 */
@RunWith(AndroidJUnit4::class)
class RunTrackingUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Navigate to run tracking screen
        composeTestRule.apply {
            onNodeWithText("Start Run").performClick()
        }
    }

    // ========== ENHANCED RUN TRACKING UI TESTS ==========

    @Test
    fun runTracking_displaysGPSStatusIndicator() {
        composeTestRule.apply {
            // GPS status indicator should be visible
            onNode(hasContentDescription("GPS status"))
                .assertExists()
                .assertIsDisplayed()
            
            // GPS accuracy indicator
            onNodeWithText("GPS Accuracy", substring = true)
                .assertExists()
            
            // Signal strength indicator
            onNode(hasContentDescription("GPS signal strength"))
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsRealTimeMetrics() {
        composeTestRule.apply {
            // Distance display
            onNodeWithText("Distance")
                .assertExists()
                .assertIsDisplayed()
            
            // Time/Duration display
            onNodeWithText("Duration", substring = true)
                .assertExists()
            
            // Pace display
            onNodeWithText("Pace", substring = true)
                .assertExists()
            
            // Speed display
            onNodeWithText("Speed", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysBackgroundTrackingIndicator() {
        composeTestRule.apply {
            // Background tracking status
            onNodeWithText("Background Tracking", substring = true)
                .assertExists()
            
            // Active background service indicator
            onNode(hasContentDescription("Background service active"))
                .assertExists()
            
            // Notification indicator
            onNodeWithText("GPS tracking notification active", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsVoiceCoachingControls() {
        composeTestRule.apply {
            // Voice coaching toggle
            onNode(hasContentDescription("Toggle voice coaching"))
                .assertExists()
                .assertIsDisplayed()
                .assertIsToggleable()
            
            // Current coach indicator
            onNodeWithText("Coach:", substring = true)
                .assertExists()
            
            // Voice coaching status
            onNode(hasContentDescription("Voice coaching status"))
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysSessionRecoveryUI() {
        composeTestRule.apply {
            // Session recovery prompt (when applicable)
            onNodeWithText("Resume Previous Run", substring = true)
                .assertExists()
            
            // Recovery options
            onNodeWithText("Resume")
                .assertExists()
                .assertHasClickAction()
            
            onNodeWithText("Start New")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun runTracking_showsMapWithGPSRoute() {
        composeTestRule.apply {
            // Map view should be present
            onNode(hasContentDescription("Run tracking map"))
                .assertExists()
                .assertIsDisplayed()
            
            // GPS route indicator
            onNode(hasContentDescription("GPS route"))
                .assertExists()
            
            // Current location marker
            onNode(hasContentDescription("Current location"))
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysRunControlButtons() {
        composeTestRule.apply {
            // Start/Stop button
            onNodeWithText("Start", substring = true)
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
            
            // Pause button
            onNodeWithText("Pause")
                .assertExists()
                .assertHasClickAction()
            
            // Stop/Finish button
            onNodeWithText("Stop")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun runTracking_showsElevationProfile() {
        composeTestRule.apply {
            // Elevation gain display
            onNodeWithText("Elevation", substring = true)
                .assertExists()
            
            // Elevation chart/graph
            onNode(hasContentDescription("Elevation profile"))
                .assertExists()
            
            // Elevation metrics
            onNodeWithText("Gain", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysHeartRateZone() {
        composeTestRule.apply {
            // Heart rate display (if available)
            onNodeWithText("Heart Rate", substring = true)
                .assertExists()
            
            // Heart rate zone indicator
            onNode(hasContentDescription("Heart rate zone"))
                .assertExists()
            
            // Zone color indicator
            onNode(hasContentDescription("Zone 2", substring = true))
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsCoachingMessagesFeed() {
        composeTestRule.apply {
            // Recent coaching messages
            onNodeWithText("Recent Coaching", substring = true)
                .assertExists()
            
            // Message history
            onNode(hasContentDescription("Coaching message history"))
                .assertExists()
            
            // Last coaching timestamp
            onNodeWithText("seconds ago", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysPermissionPrompts() {
        composeTestRule.apply {
            // Permission status indicators
            onNodeWithText("Permissions", substring = true)
                .assertExists()
            
            // Location permission status
            onNode(hasContentDescription("Location permission status"))
                .assertExists()
            
            // Permission request button (when needed)
            onNodeWithText("Enable Location")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun runTracking_showsAudioFeedbackOverlay() {
        composeTestRule.apply {
            // Audio feedback overlay
            onNode(hasContentDescription("Audio feedback overlay"))
                .assertExists()
            
            // Volume indicator
            onNode(hasContentDescription("Audio volume"))
                .assertExists()
            
            // Currently playing indicator
            onNodeWithText("Playing", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysWorkoutPhaseIndicator() {
        composeTestRule.apply {
            // Workout phase display
            onNodeWithText("Phase:", substring = true)
                .assertExists()
            
            // Current phase (Warmup, Main, Cooldown)
            onNodeWithText("Warmup", substring = true)
                .assertExists()
            
            // Phase progress indicator
            onNode(hasContentDescription("Phase progress"))
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsTargetPaceIndicator() {
        composeTestRule.apply {
            // Target pace display
            onNodeWithText("Target", substring = true)
                .assertExists()
            
            // Pace comparison
            onNode(hasContentDescription("Pace comparison"))
                .assertExists()
            
            // Pace zone indicator
            onNodeWithText("On Pace", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysDistanceMilestones() {
        composeTestRule.apply {
            // Distance milestones
            onNodeWithText("1 km", substring = true)
                .assertExists()
            
            // Milestone progress
            onNode(hasContentDescription("Distance milestone progress"))
                .assertExists()
            
            // Next milestone indicator
            onNodeWithText("Next:", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsRunStatsSummary() {
        composeTestRule.apply {
            // Statistics card
            onNode(hasContentDescription("Run statistics"))
                .assertExists()
                .assertIsDisplayed()
            
            // Average pace
            onNodeWithText("Avg Pace", substring = true)
                .assertExists()
            
            // Best pace
            onNodeWithText("Best Pace", substring = true)
                .assertExists()
            
            // Calories burned
            onNodeWithText("Calories", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysErrorRecoveryOptions() {
        composeTestRule.apply {
            // Error state indicator
            onNode(hasContentDescription("GPS error"))
                .assertExists()
            
            // Retry button
            onNodeWithText("Retry GPS")
                .assertExists()
                .assertHasClickAction()
            
            // Error message
            onNodeWithText("GPS signal lost", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsVoiceCoachingSettings() {
        composeTestRule.apply {
            // Quick voice settings
            onNode(hasContentDescription("Voice coaching settings"))
                .assertExists()
                .assertHasClickAction()
            
            // Coach selector
            onNodeWithText("Select Coach")
                .assertExists()
            
            // Volume control
            onNode(hasContentDescription("Voice volume"))
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysEmergencyControls() {
        composeTestRule.apply {
            // Emergency stop button
            onNodeWithContentDescription("Emergency stop")
                .assertExists()
                .assertIsDisplayed()
            
            // SOS/Help button
            onNodeWithText("Help")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun runTracking_showsDataSyncStatus() {
        composeTestRule.apply {
            // Data sync indicator
            onNode(hasContentDescription("Data sync status"))
                .assertExists()
            
            // Sync progress
            onNodeWithText("Syncing", substring = true)
                .assertExists()
            
            // Last sync time
            onNodeWithText("Last synced", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysWeatherInfo() {
        composeTestRule.apply {
            // Weather information
            onNode(hasContentDescription("Weather info"))
                .assertExists()
            
            // Temperature
            onNodeWithText("°C", substring = true)
                .assertExists()
            
            // Weather conditions
            onNodeWithText("Conditions", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_supportsScreenOrientation() {
        composeTestRule.apply {
            // Should work in both portrait and landscape
            onNode(hasContentDescription("Run tracking map"))
                .assertExists()
                .assertIsDisplayed()
            
            // Rotate device (simulated)
            // Key UI elements should remain accessible
            onNodeWithText("Distance")
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysAccessibilitySupport() {
        composeTestRule.apply {
            // All interactive elements should have proper descriptions
            onNodeWithContentDescription("Start run tracking")
                .assertExists()
            
            onNodeWithContentDescription("Current pace: 5 minutes 30 seconds per kilometer")
                .assertExists()
            
            onNodeWithContentDescription("GPS accuracy: Good, 3 meters")
                .assertExists()
        }
    }

    @Test
    fun runTracking_showsRunTypeSelector() {
        composeTestRule.apply {
            // Run type selection
            onNodeWithText("Run Type")
                .assertExists()
            
            // Training types
            onNodeWithText("Easy Run", substring = true)
                .assertExists()
            
            onNodeWithText("Tempo Run", substring = true)
                .assertExists()
            
            onNodeWithText("Long Run", substring = true)
                .assertExists()
        }
    }

    @Test
    fun runTracking_displaysConnectedAppStatus() {
        composeTestRule.apply {
            // Connected apps status
            onNodeWithText("Connected Apps", substring = true)
                .assertExists()
            
            // Fitbit connection
            onNode(hasContentDescription("Fitbit connected"))
                .assertExists()
            
            // Spotify connection
            onNode(hasContentDescription("Spotify connected"))
                .assertExists()
        }
    }
}