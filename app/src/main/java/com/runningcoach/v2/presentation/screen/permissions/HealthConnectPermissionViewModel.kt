package com.runningcoach.v2.presentation.screen.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Health Connect permission screen.
 * Manages Health Connect availability checking and permission requests.
 */
class HealthConnectPermissionViewModel(
    private val healthConnectPermissionManager: HealthConnectPermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthConnectPermissionUiState())
    val uiState: StateFlow<HealthConnectPermissionUiState> = _uiState.asStateFlow()
    
    /**
     * Check Health Connect availability on device
     */
    fun checkAvailability() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                availability = HealthConnectAvailability.CHECKING,
                isLoading = true
            )
            
            try {
                val availability = healthConnectPermissionManager.checkAvailability()
                val hasPermissions = if (availability == com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE) {
                    healthConnectPermissionManager.hasRequiredPermissions()
                } else {
                    false
                }
                
                _uiState.value = _uiState.value.copy(
                    availability = mapAvailability(availability),
                    hasRequiredPermissions = hasPermissions,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    availability = HealthConnectAvailability.UNAVAILABLE,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Request Health Connect permissions
     */
    fun requestPermissions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                healthConnectPermissionManager.requestHealthConnectSetup { granted ->
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            hasRequiredPermissions = granted,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Open Play Store for Health Connect update
     */
    fun openPlayStoreForUpdate() {
        healthConnectPermissionManager.openPlayStoreForHealthConnect()
    }
    
    /**
     * Open Health Connect app for manual permission management
     */
    fun openHealthConnectApp() {
        healthConnectPermissionManager.openHealthConnectApp()
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun mapAvailability(
        availability: com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability
    ): HealthConnectAvailability {
        return when (availability) {
            com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE -> 
                HealthConnectAvailability.AVAILABLE
            com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability.NEEDS_UPDATE -> 
                HealthConnectAvailability.NEEDS_UPDATE
            com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability.UNAVAILABLE -> 
                HealthConnectAvailability.UNAVAILABLE
            com.runningcoach.v2.data.service.HealthConnectPermissionManager.HealthConnectAvailability.CHECKING -> 
                HealthConnectAvailability.CHECKING
        }
    }
}