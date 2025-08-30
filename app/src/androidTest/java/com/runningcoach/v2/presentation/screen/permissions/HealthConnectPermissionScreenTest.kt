package com.runningcoach.v2.presentation.screen.permissions

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.presentation.theme.FITFOAITheme
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for HealthConnectPermissionScreen
 * Tests user interactions and state handling
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectPermissionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var permissionsGrantedCalled = false
    private var skipCalled = false
    private val mockUiState = MutableStateFlow(HealthConnectPermissionUiState())

    @Before
    fun setup() {
        permissionsGrantedCalled = false
        skipCalled = false
    }

    @Test
    fun screenDisplaysCorrectHeaderWhenChecking() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.CHECKING,
            isLoading = true
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Health Connect")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Connect with your health ecosystem")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Checking Health Connect")
            .assertIsDisplayed()
    }

    @Test
    fun screenDisplaysPermissionRequestWhenAvailable() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.AVAILABLE,
            hasRequiredPermissions = false,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Connect to Health Connect")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Connect to Health Connect", useUnmergedTree = true)
            .assertIsDisplayed() // Button text
            
        composeTestRule
            .onNodeWithText("Skip for now")
            .assertIsDisplayed()
    }

    @Test
    fun connectButtonTriggersPermissionRequest() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.AVAILABLE,
            hasRequiredPermissions = false
        )

        var requestPermissionsCalled = false
        val mockViewModel = MockHealthConnectPermissionViewModel(mockUiState)
        mockViewModel.onRequestPermissions = { requestPermissionsCalled = true }

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = mockViewModel
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Connect to Health Connect", useUnmergedTree = true)
            .performClick()

        // Assert
        assert(requestPermissionsCalled)
    }

    @Test
    fun skipButtonTriggersCallback() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.AVAILABLE,
            hasRequiredPermissions = false
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        composeTestRule
            .onNodeWithText("Skip for now")
            .performClick()

        // Assert
        assert(skipCalled)
        assert(!permissionsGrantedCalled)
    }

    @Test
    fun screenDisplaysUpdateRequiredWhenNeeded() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.NEEDS_UPDATE,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Update Health Connect")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Health Connect needs to be updated to work with FITFOAI. Please update from the Play Store.")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Update Health Connect", useUnmergedTree = true)
            .assertIsDisplayed() // Button
    }

    @Test
    fun updateButtonTriggersPlayStoreOpen() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.NEEDS_UPDATE
        )

        var openPlayStoreCalled = false
        val mockViewModel = MockHealthConnectPermissionViewModel(mockUiState)
        mockViewModel.onOpenPlayStore = { openPlayStoreCalled = true }

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Update Health Connect", useUnmergedTree = true)
            .performClick()

        // Assert
        assert(openPlayStoreCalled)
    }

    @Test
    fun screenDisplaysUnavailableMessage() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.UNAVAILABLE,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Health Connect Not Available")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Health Connect isn't available on this device. FITFOAI will work with local storage instead.")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Continue Without Health Connect")
            .assertIsDisplayed()
    }

    @Test
    fun permissionsGrantedNavigatesToNextScreen() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.AVAILABLE,
            hasRequiredPermissions = true // Permissions already granted
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Wait for navigation effect
        composeTestRule.waitForIdle()

        // Assert
        assert(permissionsGrantedCalled)
    }

    @Test
    fun screenShowsBenefitsList() {
        // Arrange
        mockUiState.value = HealthConnectPermissionUiState(
            availability = HealthConnectAvailability.AVAILABLE,
            hasRequiredPermissions = false
        )

        // Act
        composeTestRule.setContent {
            FITFOAITheme {
                HealthConnectPermissionScreen(
                    onPermissionsGranted = { permissionsGrantedCalled = true },
                    onSkip = { skipCalled = true },
                    viewModel = MockHealthConnectPermissionViewModel(mockUiState)
                )
            }
        }

        // Assert - Check that benefit items are displayed
        composeTestRule
            .onNodeWithText("Sync fitness data across all health apps")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Automatically backup your running history")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Enhanced AI coaching with health insights")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Better battery life and performance")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Advanced privacy controls")
            .assertIsDisplayed()
    }
}

/**
 * Mock ViewModel for testing
 */
class MockHealthConnectPermissionViewModel(
    private val uiStateFlow: MutableStateFlow<HealthConnectPermissionUiState>
) : HealthConnectPermissionViewModel(mockk()) {
    
    var onRequestPermissions: (() -> Unit)? = null
    var onOpenPlayStore: (() -> Unit)? = null
    
    override val uiState = uiStateFlow.asStateFlow()
    
    override fun requestPermissions() {
        onRequestPermissions?.invoke()
    }
    
    override fun openPlayStoreForUpdate() {
        onOpenPlayStore?.invoke()
    }
    
    override fun checkAvailability() {
        // Mock implementation - no-op for testing
    }
}