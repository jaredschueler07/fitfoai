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
 * SPRINT 3.2 UI TEST: VoiceCoachingUITest
 * 
 * Tests for voice coaching UI components ensuring proper coach selection and audio controls.
 * Critical for Sprint 3.2 feature completeness - voice coaching UI must be intuitive.
 * 
 * Test Requirements:
 * ✅ Coach selection flow (4 personalities)
 * ✅ Audio controls (play, pause, volume)
 * ✅ Visual feedback during coaching
 * ✅ Settings persistence across sessions
 * ✅ Voice preview functionality
 * ✅ Real-time status indicators
 */
@RunWith(AndroidJUnit4::class)
class VoiceCoachingUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Navigate to voice coaching screen or settings
        composeTestRule.apply {
            // Navigate to voice coaching settings if not already there
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText("Voice Coaching").performClick()
        }
    }

    // ========== SPRINT 3.2 VOICE COACHING UI TESTS ==========

    @Test
    fun voiceCoaching_displaysAllCoachPersonalities() {
        composeTestRule.apply {
            // Check for all 4 coach personalities
            onNodeWithText("Bennett").assertExists().assertIsDisplayed()
            onNodeWithText("Mariana").assertExists().assertIsDisplayed()
            onNodeWithText("Becs").assertExists().assertIsDisplayed()
            onNodeWithText("Goggins").assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun voiceCoaching_showsCoachDescriptions() {
        composeTestRule.apply {
            // Bennett - Professional, data-driven
            onNodeWithText("Professional", substring = true).assertExists()
            onNodeWithText("data-driven", substring = true).assertExists()
            
            // Mariana - Energetic, motivational
            onNodeWithText("Energetic", substring = true).assertExists()
            onNodeWithText("motivational", substring = true).assertExists()
            
            // Becs - Calm, supportive
            onNodeWithText("Calm", substring = true).assertExists()
            onNodeWithText("supportive", substring = true).assertExists()
            
            // Goggins - Intense, challenging
            onNodeWithText("Intense", substring = true).assertExists()
            onNodeWithText("challenging", substring = true).assertExists()
        }
    }

    @Test
    fun voiceCoaching_allowsCoachSelection() {
        composeTestRule.apply {
            // Select Bennett coach
            onNodeWithText("Bennett")
                .assertHasClickAction()
                .performClick()
            
            // Should show as selected
            onNode(hasContentDescription("Bennett selected"))
                .assertExists()
            
            // Try selecting different coach
            onNodeWithText("Mariana")
                .performClick()
            
            onNode(hasContentDescription("Mariana selected"))
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysVoicePreviewButton() {
        composeTestRule.apply {
            // Each coach should have a preview button
            onNodeWithContentDescription("Preview Bennett voice")
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
            
            onNodeWithContentDescription("Preview Mariana voice")
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun voiceCoaching_showsAudioControlPanel() {
        composeTestRule.apply {
            // Audio control buttons
            onNodeWithContentDescription("Play voice coaching")
                .assertExists()
                .assertIsDisplayed()
            
            onNodeWithContentDescription("Pause voice coaching")
                .assertExists()
            
            // Volume control
            onNodeWithContentDescription("Volume control")
                .assertExists()
            
            // Mute button
            onNodeWithContentDescription("Mute voice coaching")
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysVolumeSlider() {
        composeTestRule.apply {
            // Volume slider should be present
            onNode(hasContentDescription("Voice coaching volume"))
                .assertExists()
                .assertIsDisplayed()
            
            // Should be interactive
            onNode(hasContentDescription("Voice coaching volume"))
                .assertHasClickAction()
        }
    }

    @Test
    fun voiceCoaching_showsRealTimeStatus() {
        composeTestRule.apply {
            // Voice coaching status indicator
            onNodeWithText("Voice Coaching: ON", substring = true)
                .assertExists()
            
            // Current coach indicator
            onNodeWithText("Current Coach:", substring = true)
                .assertExists()
            
            // Audio status
            onNode(hasContentDescription("Audio status"))
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysCoachingSettings() {
        composeTestRule.apply {
            // Coaching frequency settings
            onNodeWithText("Coaching Frequency")
                .assertExists()
            
            // Motivation level setting
            onNodeWithText("Motivation Level")
                .assertExists()
            
            // Milestone announcements toggle
            onNodeWithText("Milestone Announcements")
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_showsToggleSwitch() {
        composeTestRule.apply {
            // Voice coaching enable/disable toggle
            onNode(hasContentDescription("Enable voice coaching"))
                .assertExists()
                .assertIsDisplayed()
                .assertIsToggleable()
        }
    }

    @Test
    fun voiceCoaching_displaysVisualFeedback() {
        composeTestRule.apply {
            // Visual feedback when coaching is active
            onNode(hasContentDescription("Voice coaching active"))
                .assertExists()
            
            // Audio waveform or visualizer
            onNode(hasContentDescription("Audio visualizer"))
                .assertExists()
            
            // Speaking indicator
            onNodeWithText("Speaking", substring = true)
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_showsCoachingHistory() {
        composeTestRule.apply {
            // Recent coaching messages
            onNodeWithText("Recent Messages")
                .assertExists()
            
            // Coaching statistics
            onNodeWithText("Messages Played", substring = true)
                .assertExists()
            
            onNodeWithText("Success Rate", substring = true)
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysConnectionStatus() {
        composeTestRule.apply {
            // ElevenLabs API connection status
            onNodeWithText("Voice API Status", substring = true)
                .assertExists()
            
            // Connection indicator
            onNode(hasContentDescription("API connected"))
                .assertExists()
            
            // Latency information
            onNodeWithText("Response Time", substring = true)
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_showsAdvancedSettings() {
        composeTestRule.apply {
            // Advanced settings section
            onNodeWithText("Advanced Settings")
                .assertExists()
                .performClick()
            
            // Speech rate setting
            onNodeWithText("Speech Rate")
                .assertExists()
            
            // Voice stability setting
            onNodeWithText("Voice Stability")
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysCoachPersonalityCards() {
        composeTestRule.apply {
            // Coach cards should be in a grid or list
            onNode(hasContentDescription("Bennett coach card"))
                .assertExists()
                .assertIsDisplayed()
            
            onNode(hasContentDescription("Mariana coach card"))
                .assertExists()
                .assertIsDisplayed()
            
            // Cards should show coach images/avatars
            onNode(hasContentDescription("Bennett avatar"))
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_showsTestVoiceButton() {
        composeTestRule.apply {
            // Test voice button for each coach
            onNodeWithText("Test Voice", substring = true)
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun voiceCoaching_displaysCoachingTriggerSettings() {
        composeTestRule.apply {
            // Trigger settings section
            onNodeWithText("Coaching Triggers")
                .assertExists()
            
            // Pace warnings toggle
            onNodeWithText("Pace Warnings")
                .assertExists()
            
            // Milestone celebrations toggle
            onNodeWithText("Milestone Celebrations")
                .assertExists()
            
            // Motivational messages toggle
            onNodeWithText("Motivational Messages")
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_showsAudioQualitySettings() {
        composeTestRule.apply {
            // Audio quality selection
            onNodeWithText("Audio Quality")
                .assertExists()
            
            // Bit rate options
            onNodeWithText("High Quality", substring = true)
                .assertExists()
            
            onNodeWithText("Standard Quality", substring = true)
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysOfflineModeToggle() {
        composeTestRule.apply {
            // Offline mode setting
            onNodeWithText("Offline Mode")
                .assertExists()
            
            onNode(hasContentDescription("Enable offline mode"))
                .assertExists()
                .assertIsToggleable()
        }
    }

    @Test
    fun voiceCoaching_showsCacheManagement() {
        composeTestRule.apply {
            // Cache management section
            onNodeWithText("Voice Cache")
                .assertExists()
            
            // Clear cache button
            onNodeWithText("Clear Cache")
                .assertExists()
                .assertHasClickAction()
            
            // Cache size display
            onNodeWithText("Cache Size", substring = true)
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_displaysErrorStates() {
        composeTestRule.apply {
            // Error message display
            onNode(hasContentDescription("Error message"))
                .assertExists()
            
            // Retry button when errors occur
            onNodeWithText("Retry")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun voiceCoaching_supportsAccessibility() {
        composeTestRule.apply {
            // All controls should have proper accessibility labels
            onNodeWithContentDescription("Select Bennett as your voice coach")
                .assertExists()
            
            onNodeWithContentDescription("Adjust voice coaching volume")
                .assertExists()
            
            onNodeWithContentDescription("Preview Mariana voice sample")
                .assertExists()
        }
    }

    @Test
    fun voiceCoaching_persistsSettingsAcrossSessions() {
        composeTestRule.apply {
            // Select a coach
            onNodeWithText("Goggins").performClick()
            
            // Change volume
            onNode(hasContentDescription("Voice coaching volume"))
                .performClick()
            
            // Navigate away and back
            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Voice Coaching").performClick()
            
            // Settings should be preserved
            onNode(hasContentDescription("Goggins selected"))
                .assertExists()
        }
    }
}