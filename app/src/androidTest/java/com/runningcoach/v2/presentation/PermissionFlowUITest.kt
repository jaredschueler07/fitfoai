package com.runningcoach.v2.presentation

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.runningcoach.v2.MainActivity
import com.runningcoach.v2.data.service.PermissionManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 CRITICAL UI TEST: PermissionFlowUITest
 * 
 * Tests for permission request flow UI ensuring proper user guidance and navigation.
 * Critical for P0 release - permission flow must work correctly for GPS tracking.
 * 
 * Test Requirements:
 * ✅ Permission request flow UI navigation
 * ✅ Educational dialogs display correctly
 * ✅ Settings navigation from denied permissions
 * ✅ Progress indicators work properly
 * ✅ Error states display appropriately
 */
@RunWith(AndroidJUnit4::class)
class PermissionFlowUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun setUp() {
        // Navigate to permissions screen if needed
    }

    // ========== P0 PERMISSION FLOW UI TESTS ==========

    @Test
    fun permissionFlow_displaysLocationPermissionEducation() {
        // Look for permission-related UI elements
        composeTestRule.apply {
            // Check if permission education content is displayed
            onNodeWithText("Location Permission Required", substring = true)
                .assertExists()
                
            // Or check for permission request button
            onNodeWithText("Grant Permission", substring = true)
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun permissionFlow_showsPermissionRationale() {
        composeTestRule.apply {
            // Look for rationale text explaining why permissions are needed
            onNode(hasText("FITFOAI needs location access", substring = true))
                .assertExists()
                
            // Check for specific permission benefits
            onNodeWithText("Track your running route", substring = true)
                .assertExists()
                
            onNodeWithText("Calculate distance and pace", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_displaysProgressIndicators() {
        composeTestRule.apply {
            // Check for progress indicators during permission flow
            onNode(hasContentDescription("Permission progress"))
                .assertExists()
                
            // Or check for step indicators
            onNodeWithText("Step 1", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_navigatesToSettingsWhenDenied() {
        composeTestRule.apply {
            // Look for settings navigation button
            onNodeWithText("Open Settings", substring = true)
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun permissionFlow_showsBackgroundLocationEducation() {
        composeTestRule.apply {
            // Check for background location education
            onNodeWithText("Background Location Access", substring = true)
                .assertExists()
                
            // Look for explanation of background location benefits
            onNodeWithText("Continue GPS tracking", substring = true)
                .assertExists()
                
            onNodeWithText("Allow all the time", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_displaysBatteryOptimizationGuidance() {
        composeTestRule.apply {
            // Check for battery optimization guidance
            onNodeWithText("Optimize Battery Settings", substring = true)
                .assertExists()
                
            onNodeWithText("Disable battery optimization", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_showsPermissionDeniedState() {
        composeTestRule.apply {
            // Check for permission denied state
            onNodeWithText("Permission Required", substring = true)
                .assertExists()
                
            onNodeWithText("enable it in app settings", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_displaysCorrectButtonStates() {
        composeTestRule.apply {
            // Check that buttons are properly enabled/disabled
            onNodeWithText("Grant Permission")
                .assertExists()
                .assertIsEnabled()
                
            // Cancel button should also be available
            onNodeWithText("Cancel", substring = true)
                .assertExists()
                .assertIsEnabled()
        }
    }

    @Test
    fun permissionFlow_handlesMultiplePermissionTypes() {
        composeTestRule.apply {
            // Should show different permission types
            onNodeWithText("Location", substring = true)
                .assertExists()
                
            // Activity recognition permission (Android 10+)
            onNodeWithText("Physical Activity", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_showsPermissionStatusFeedback() {
        composeTestRule.apply {
            // Check for permission status indicators
            onNode(hasContentDescription("Permission granted"))
                .assertExists()
                
            // Or text-based status
            onNodeWithText("Granted", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_navigatesCorrectlyAfterGranted() {
        composeTestRule.apply {
            // After permissions are granted, should show continue/next button
            onNodeWithText("Continue", substring = true)
                .assertExists()
                .assertIsEnabled()
                .assertHasClickAction()
        }
    }

    @Test
    fun permissionFlow_displaysContextualIcons() {
        composeTestRule.apply {
            // Check for GPS/location icons
            onNode(hasContentDescription("GPS icon"))
                .assertExists()
                
            // Security/privacy icon
            onNode(hasContentDescription("Security icon"))
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_showsErrorStatesCorrectly() {
        composeTestRule.apply {
            // Error state when permissions permanently denied
            onNodeWithText("Permanently denied", substring = true)
                .assertExists()
                
            // Helpful error message
            onNodeWithText("Please enable in Settings", substring = true)
                .assertExists()
        }
    }

    @Test
    fun permissionFlow_supportsAccessibility() {
        composeTestRule.apply {
            // Check for proper accessibility labels
            onNode(hasContentDescription("Location permission request"))
                .assertExists()
                
            // Buttons should have clear descriptions
            onNodeWithText("Grant Permission")
                .assertHasClickAction()
                .assertIsDisplayed()
        }
    }

    @Test
    fun permissionFlow_displaysVersionSpecificContent() {
        composeTestRule.apply {
            // Should show Android version-specific instructions
            onNodeWithText("Android", substring = true)
                .assertExists()
                
            // Version-specific permission flow
            onNodeWithText("Allow all the time", substring = true)
                .assertExists()
        }
    }
}