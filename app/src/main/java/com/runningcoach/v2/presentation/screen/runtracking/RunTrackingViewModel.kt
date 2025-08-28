package com.runningcoach.v2.presentation.screen.runtracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.usecase.StartRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackRunSessionUseCase
import com.runningcoach.v2.domain.usecase.EndRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
/**
 * ViewModel for RunTrackingScreen with real-time GPS tracking and metrics.
 * Integrates with backend LocationService and tracking use cases.
 * [TECH-DEBT] Convert to Hilt injection once compatibility is resolved.
 */
class RunTrackingViewModel(
    private val startRunSessionUseCase: StartRunSessionUseCase,
    private val trackRunSessionUseCase: TrackRunSessionUseCase,
    private val endRunSessionUseCase: EndRunSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunTrackingUiState())
    val uiState: StateFlow<RunTrackingUiState> = _uiState.asStateFlow()

    private var currentSessionId: Long? = null
    private val defaultUserId = 1L // TODO: Get from user session management

    /**
     * Starts a new run session and begins GPS tracking
     */
    fun startRunSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = startRunSessionUseCase(defaultUserId)
                
                if (result.isSuccess) {
                    currentSessionId = result.getOrNull()
                    currentSessionId?.let { sessionId ->
                        // Start tracking real-time metrics
                        trackRealTimeMetrics(sessionId)
                        
                        _uiState.value = _uiState.value.copy(
                            isTracking = true,
                            isLoading = false,
                            trackingState = TrackingState.ACTIVE
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to start run session"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Pauses the current run session
     */
    fun pauseRunSession() {
        _uiState.value = _uiState.value.copy(
            isTracking = false,
            trackingState = TrackingState.PAUSED
        )
    }

    /**
     * Resumes the paused run session
     */
    fun resumeRunSession() {
        _uiState.value = _uiState.value.copy(
            isTracking = true,
            trackingState = TrackingState.ACTIVE
        )
    }

    /**
     * Ends the current run session and saves final metrics
     */
    fun endRunSession() {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                try {
                    val finalMetrics = _uiState.value.currentMetrics
                    val result = endRunSessionUseCase(sessionId, finalMetrics)
                    
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isTracking = false,
                            trackingState = TrackingState.COMPLETED,
                            sessionCompleted = true
                        )
                        currentSessionId = null
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = result.exceptionOrNull()?.message ?: "Failed to end run session"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Error ending run session"
                    )
                }
            }
        }
    }

    /**
     * Resets the run session to initial state
     */
    fun resetSession() {
        currentSessionId = null
        _uiState.value = RunTrackingUiState()
    }

    /**
     * Clears the current error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Requests location permissions
     */
    fun requestLocationPermissions() {
        _uiState.value = _uiState.value.copy(
            permissionRequested = true,
            gpsStatus = GPSStatus.REQUESTING_PERMISSION
        )
    }

    /**
     * Updates location permission status
     */
    fun updateLocationPermissionStatus(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = granted,
            gpsStatus = if (granted) GPSStatus.ACQUIRING else GPSStatus.PERMISSION_DENIED
        )
    }

    /**
     * Tracks real-time metrics from the tracking use case
     */
    private fun trackRealTimeMetrics(sessionId: Long) {
        viewModelScope.launch {
            trackRunSessionUseCase.getTrackingData(sessionId)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Error tracking metrics"
                    )
                }
                .collect { trackingData ->
                    updateUIWithTrackingData(trackingData)
                }
        }
    }

    /**
     * Updates UI state with new tracking data
     */
    private fun updateUIWithTrackingData(trackingData: TrackingData) {
        val gpsStatus = when {
            !trackingData.hasGPSSignal -> GPSStatus.SIGNAL_LOST
            trackingData.metrics.hasActiveGPS() -> {
                when (trackingData.metrics.getGPSAccuracy()) {
                    "Excellent" -> GPSStatus.EXCELLENT
                    "Good" -> GPSStatus.GOOD
                    "Fair" -> GPSStatus.FAIR
                    "Poor" -> GPSStatus.POOR
                    else -> GPSStatus.ACQUIRING
                }
            }
            else -> GPSStatus.ACQUIRING
        }

        _uiState.value = _uiState.value.copy(
            currentMetrics = trackingData.metrics,
            locationPointCount = trackingData.routePointCount,
            gpsStatus = gpsStatus,
            lastLocationUpdate = System.currentTimeMillis()
        )
    }
}

/**
 * UI state for RunTrackingScreen
 */
data class RunTrackingUiState(
    val isTracking: Boolean = false,
    val isLoading: Boolean = false,
    val trackingState: TrackingState = TrackingState.INACTIVE,
    val currentMetrics: RunMetrics = RunMetrics(),
    val gpsStatus: GPSStatus = GPSStatus.INACTIVE,
    val hasLocationPermission: Boolean = false,
    val permissionRequested: Boolean = false,
    val locationPointCount: Int = 0,
    val lastLocationUpdate: Long = 0L,
    val sessionCompleted: Boolean = false,
    val error: String? = null
) {
    val formattedDistance: String get() = currentMetrics.getFormattedDistance()
    val formattedDuration: String get() = currentMetrics.getFormattedDuration()
    val formattedPace: String get() = currentMetrics.getFormattedPace()
    val formattedSpeed: String get() = currentMetrics.getFormattedSpeed()
    val gpsAccuracy: String get() = currentMetrics.getGPSAccuracy()
    
    val showStartButton: Boolean get() = !isTracking && trackingState == TrackingState.INACTIVE
    val showPauseButton: Boolean get() = isTracking && trackingState == TrackingState.ACTIVE
    val showResumeButton: Boolean get() = !isTracking && trackingState == TrackingState.PAUSED
    val showStopButton: Boolean get() = trackingState == TrackingState.ACTIVE || trackingState == TrackingState.PAUSED
}

/**
 * Tracking state enum
 */
enum class TrackingState {
    INACTIVE,
    ACTIVE,
    PAUSED,
    COMPLETED
}

/**
 * GPS status enum for UI display
 */
enum class GPSStatus {
    INACTIVE,
    REQUESTING_PERMISSION,
    PERMISSION_DENIED,
    ACQUIRING,
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    SIGNAL_LOST
}