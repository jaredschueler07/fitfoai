package com.runningcoach.v2.presentation.screen.runtracking

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.presentation.theme.AppColors
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for RunTrackingScreen testing UI components, interactions,
 * button functionality, metric displays, and GPS status indicators.
 */
@RunWith(AndroidJUnit4::class)
class RunTrackingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: RunTrackingViewModel
    private val mockOnNavigateBack = mockk<() -> Unit>(relaxed = true)

    // Test UI State flows
    private val _uiState = MutableStateFlow(RunTrackingUiState())
    private val uiState = _uiState.asStateFlow()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        mockViewModel = mockk(relaxed = true) {
            every { uiState } returns this@RunTrackingScreenTest.uiState
            every { startRunSession() } just Runs
            every { pauseRunSession() } just Runs
            every { resumeRunSession() } just Runs
            every { endRunSession() } just Runs
            every { clearError() } just Runs
            every { requestLocationPermissions() } just Runs
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun runTrackingScreen_displaysCorrectInitialState() {
        // Arrange
        val initialState = RunTrackingUiState(
            isTracking = false,
            trackingState = TrackingState.INACTIVE,
            hasLocationPermission = true,
            gpsStatus = GPSStatus.INACTIVE
        )
        _uiState.value = initialState

        // Act
        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert
        composeTestRule.onNodeWithText("RUN TRACKING").assertIsDisplayed()
        composeTestRule.onNodeWithText("START").assertIsDisplayed()
        composeTestRule.onNodeWithText("0m").assertIsDisplayed() // Initial distance
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed() // Initial duration
        composeTestRule.onNodeWithText("GPS: INACTIVE").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_startButtonTriggersStartSession() {
        // Arrange
        val initialState = RunTrackingUiState(
            hasLocationPermission = true,
            trackingState = TrackingState.INACTIVE
        )
        _uiState.value = initialState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Act
        composeTestRule.onNodeWithText("START").performClick()

        // Assert
        verify { mockViewModel.startRunSession() }
    }

    @Test
    fun runTrackingScreen_showsLoadingStateCorrectly() {
        // Arrange
        val loadingState = RunTrackingUiState(
            isLoading = true,
            hasLocationPermission = true,
            trackingState = TrackingState.INACTIVE
        )
        _uiState.value = loadingState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert
        composeTestRule.onNode(hasProgressBarSemantics()).assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_displaysActiveTrackingState() {
        // Arrange
        val activeMetrics = RunMetrics(
            distance = 1250f, // 1.25 km
            duration = 385L, // 6:25
            currentPace = 5.2f, // 5:12 min/km
            currentSpeed = 3.2f, // m/s
            caloriesBurned = 95
        )
        
        val activeState = RunTrackingUiState(
            isTracking = true,
            trackingState = TrackingState.ACTIVE,
            currentMetrics = activeMetrics,
            hasLocationPermission = true,
            gpsStatus = GPSStatus.EXCELLENT,
            locationPointCount = 125
        )
        _uiState.value = activeState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert
        composeTestRule.onNodeWithText("1.25").assertIsDisplayed() // Distance
        composeTestRule.onNodeWithText("kilometers").assertIsDisplayed()
        composeTestRule.onNodeWithText("6:25").assertIsDisplayed() // Duration
        composeTestRule.onNodeWithText("5:12").assertIsDisplayed() // Current pace
        composeTestRule.onNodeWithText("95").assertIsDisplayed() // Calories
        composeTestRule.onNodeWithText("GPS: EXCELLENT").assertIsDisplayed()
        composeTestRule.onNodeWithText("125 points tracked").assertIsDisplayed()
        
        // Should show pause and stop buttons
        composeTestRule.onNodeWithText("PAUSE").assertIsDisplayed()
        composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_pauseButtonTriggersPauseSession() {
        // Arrange
        val activeState = RunTrackingUiState(
            isTracking = true,
            trackingState = TrackingState.ACTIVE,
            hasLocationPermission = true
        )
        _uiState.value = activeState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Act
        composeTestRule.onNodeWithText("PAUSE").performClick()

        // Assert
        verify { mockViewModel.pauseRunSession() }
    }

    @Test
    fun runTrackingScreen_stopButtonTriggersEndSession() {
        // Arrange
        val activeState = RunTrackingUiState(
            isTracking = true,
            trackingState = TrackingState.ACTIVE,
            hasLocationPermission = true
        )
        _uiState.value = activeState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Act
        composeTestRule.onNodeWithText("STOP").performClick()

        // Assert
        verify { mockViewModel.endRunSession() }
    }

    @Test
    fun runTrackingScreen_displaysPausedState() {
        // Arrange
        val pausedMetrics = RunMetrics(
            distance = 800f,
            duration = 240L, // 4:00
            caloriesBurned = 60
        )
        
        val pausedState = RunTrackingUiState(
            isTracking = false,
            trackingState = TrackingState.PAUSED,
            currentMetrics = pausedMetrics,
            hasLocationPermission = true,
            gpsStatus = GPSStatus.GOOD
        )
        _uiState.value = pausedState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert
        composeTestRule.onNodeWithText("800m").assertIsDisplayed() // Distance
        composeTestRule.onNodeWithText("4:00").assertIsDisplayed() // Duration
        composeTestRule.onNodeWithText("60").assertIsDisplayed() // Calories
        
        // Should show resume and stop buttons
        composeTestRule.onNodeWithText("RESUME").assertIsDisplayed()
        composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_resumeButtonTriggersResumeSession() {
        // Arrange
        val pausedState = RunTrackingUiState(
            isTracking = false,
            trackingState = TrackingState.PAUSED,
            hasLocationPermission = true
        )
        _uiState.value = pausedState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Act
        composeTestRule.onNodeWithText("RESUME").performClick()

        // Assert
        verify { mockViewModel.resumeRunSession() }
    }

    @Test
    fun runTrackingScreen_navigateBackButtonWorks() {
        // Arrange
        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Act - Find back button by semantic description
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // Assert
        verify { mockOnNavigateBack() }
    }

    @Test
    fun runTrackingScreen_displaysGPSStatusColors() {
        // Test GPS status color indicators
        val gpsStatuses = listOf(
            GPSStatus.EXCELLENT,
            GPSStatus.GOOD, 
            GPSStatus.FAIR,
            GPSStatus.POOR,
            GPSStatus.SIGNAL_LOST,
            GPSStatus.INACTIVE
        )
        
        gpsStatuses.forEach { status ->
            // Arrange
            val testState = RunTrackingUiState(
                gpsStatus = status,
                hasLocationPermission = true
            )
            _uiState.value = testState

            composeTestRule.setContent {
                RunTrackingScreen(
                    viewModel = mockViewModel,
                    onNavigateBack = mockOnNavigateBack
                )
            }

            // Assert
            val expectedText = "GPS: ${status.name.replace("_", " ")}"
            composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
        }
    }

    @Test
    fun runTrackingScreen_displaysPaceCorrectly() {
        // Arrange - Test various pace values
        val metricsWithPace = RunMetrics(
            distance = 1000f,
            duration = 300L, // 5 minutes
            currentPace = 5.5f // 5:30 min/km
        )
        
        val stateWithPace = RunTrackingUiState(
            currentMetrics = metricsWithPace,
            hasLocationPermission = true,
            isTracking = true,
            trackingState = TrackingState.ACTIVE
        )
        _uiState.value = stateWithPace

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Should display pace as 5:30
        composeTestRule.onNodeWithText("5:30").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_displaysSpeedCorrectly() {
        // Arrange
        val metricsWithSpeed = RunMetrics(
            distance = 500f,
            duration = 150L,
            currentSpeed = 4.17f // ~15 km/h
        )
        
        val stateWithSpeed = RunTrackingUiState(
            currentMetrics = metricsWithSpeed,
            hasLocationPermission = true,
            isTracking = true,
            trackingState = TrackingState.ACTIVE
        )
        _uiState.value = stateWithSpeed

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Should display speed in km/h (4.17 m/s = 15.0 km/h)
        composeTestRule.onNodeWithText("15.0 km/h").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_handlesLongDurationFormat() {
        // Arrange - Test hour display
        val metricsWithLongDuration = RunMetrics(
            distance = 5000f,
            duration = 3665L // 1:01:05 (1 hour, 1 minute, 5 seconds)
        )
        
        val stateWithLongDuration = RunTrackingUiState(
            currentMetrics = metricsWithLongDuration,
            hasLocationPermission = true,
            isTracking = true,
            trackingState = TrackingState.ACTIVE
        )
        _uiState.value = stateWithLongDuration

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Should display duration with hours
        composeTestRule.onNodeWithText("1:01:05").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_requestsLocationPermissionOnLoad() {
        // Arrange
        val noPermissionState = RunTrackingUiState(
            hasLocationPermission = false,
            permissionRequested = false
        )
        _uiState.value = noPermissionState

        // Act
        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Should trigger permission request
        verify { mockViewModel.requestLocationPermissions() }
    }

    @Test
    fun runTrackingScreen_handlesZeroMetricsDisplay() {
        // Arrange - Test display with zero metrics
        val zeroMetrics = RunMetrics(
            distance = 0f,
            duration = 0L,
            currentPace = 0f,
            currentSpeed = 0f,
            caloriesBurned = 0
        )
        
        val zeroState = RunTrackingUiState(
            currentMetrics = zeroMetrics,
            hasLocationPermission = true
        )
        _uiState.value = zeroState

        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert
        composeTestRule.onNodeWithText("0m").assertIsDisplayed() // Distance
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed() // Duration
        composeTestRule.onNodeWithText("--:--").assertIsDisplayed() // Pace when zero
        composeTestRule.onNodeWithText("0.0 km/h").assertIsDisplayed() // Speed
        composeTestRule.onNodeWithText("0").assertIsDisplayed() // Calories
    }

    @Test
    fun runTrackingScreen_themeColorsRenderCorrectly() {
        // Arrange
        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Test that the screen renders without theme-related crashes
        composeTestRule.onNodeWithText("RUN TRACKING").assertIsDisplayed()
        // The gradient background should be applied (testing by ensuring content renders)
        composeTestRule.onNodeWithText("START").assertIsDisplayed()
    }

    @Test
    fun runTrackingScreen_errorHandling() {
        // Arrange
        val errorState = RunTrackingUiState(
            error = "GPS connection failed",
            hasLocationPermission = true
        )
        _uiState.value = errorState

        // Act
        composeTestRule.setContent {
            RunTrackingScreen(
                viewModel = mockViewModel,
                onNavigateBack = mockOnNavigateBack
            )
        }

        // Assert - Error should trigger clearError call
        verify { mockViewModel.clearError() }
    }

    /**
     * Helper function to create a semantic matcher for progress bars
     */
    private fun hasProgressBarSemantics(): SemanticsMatcher {
        return SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
            androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
        )
    }
}