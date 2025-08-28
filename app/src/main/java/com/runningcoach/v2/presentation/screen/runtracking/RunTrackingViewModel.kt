package com.runningcoach.v2.presentation.screen.runtracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.usecase.StartRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackRunSessionUseCase
import com.runningcoach.v2.domain.usecase.EndRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackingData
import com.runningcoach.v2.data.service.VoiceCoachingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
/**
 * [BACKEND-UPDATE] Enhanced RunTrackingViewModel with Voice Coaching Integration
 * 
 * Integrates with complete voice coaching system including SmartTriggerEngine,
 * VoiceCacheManager, and AudioFocusManager for intelligent real-time coaching.
 * [TECH-DEBT] Convert to Hilt injection once compatibility is resolved.
 */
class RunTrackingViewModel(
    private val startRunSessionUseCase: StartRunSessionUseCase,
    private val trackRunSessionUseCase: TrackRunSessionUseCase,
    private val endRunSessionUseCase: EndRunSessionUseCase,
    private val voiceCoachingManager: VoiceCoachingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunTrackingUiState())
    val uiState: StateFlow<RunTrackingUiState> = _uiState.asStateFlow()

    private var currentSessionId: Long? = null
    private val defaultUserId = 1L // TODO: Get from user session management
    
    // Voice coaching integration
    val selectedCoach = voiceCoachingManager.observeSelectedCoach()
    val coachingStats = voiceCoachingManager.coachingStats
    val isVoiceCoachingEnabled = voiceCoachingManager.isVoiceCoachingEnabled
    val currentCoachingPhase = voiceCoachingManager.currentCoachingPhase
    
    // Track metrics flow for voice coaching
    private val metricsFlow = MutableSharedFlow<RunMetrics>()
    
    init {
        // Start voice coaching when tracking begins
        viewModelScope.launch {
            combine(
                uiState.map { it.isTracking },
                metricsFlow
            ) { isTracking, metrics ->
                if (isTracking) metrics else null
            }.collect { metrics ->
                metrics?.let {
                    // Voice coaching will process metrics in background
                }
            }
        }
    }

    /**
     * Starts a new run session with voice coaching integration
     */
    fun startRunSession(
        targetPace: String? = null,
        targetDistance: Float? = null // in meters
    ) {
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
                        
                        // Start voice coaching with parameters
                        voiceCoachingManager.startVoiceCoaching(
                            runMetrics = metricsFlow.asSharedFlow(),
                            targetPace = targetPace,
                            targetDistance = targetDistance?.toString(),
                            targetDistanceMeters = targetDistance
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            isTracking = true,
                            isLoading = false,
                            trackingState = TrackingState.ACTIVE,
                            voiceCoachingActive = true,
                            targetPace = targetPace,
                            targetDistance = targetDistance
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
     * Pauses the current run session and voice coaching
     */
    fun pauseRunSession() {
        viewModelScope.launch {
            voiceCoachingManager.pauseVoiceCoaching()
            
            _uiState.value = _uiState.value.copy(
                isTracking = false,
                trackingState = TrackingState.PAUSED
            )
        }
    }

    /**
     * Resumes the paused run session and voice coaching
     */
    fun resumeRunSession() {
        viewModelScope.launch {
            voiceCoachingManager.resumeVoiceCoaching(metricsFlow.asSharedFlow())
            
            _uiState.value = _uiState.value.copy(
                isTracking = true,
                trackingState = TrackingState.ACTIVE
            )
        }
    }

    /**
     * Ends the current run session, stops voice coaching, and saves final metrics
     */
    fun endRunSession() {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                try {
                    // Stop voice coaching first (will provide completion message)
                    voiceCoachingManager.stopVoiceCoaching()
                    
                    val finalMetrics = _uiState.value.currentMetrics
                    val result = endRunSessionUseCase(sessionId, finalMetrics)
                    
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isTracking = false,
                            trackingState = TrackingState.COMPLETED,
                            sessionCompleted = true,
                            voiceCoachingActive = false
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
     * Updates UI state with new tracking data and feeds metrics to voice coaching
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
            locationHistory = trackingData.locationHistory,
            gpsStatus = gpsStatus,
            lastLocationUpdate = System.currentTimeMillis()
        )
        
        // Feed metrics to voice coaching system
        viewModelScope.launch {
            metricsFlow.emit(trackingData.metrics)
        }
    }
    
    // Voice coaching control methods
    
    /**
     * Toggle voice coaching on/off
     */
    fun toggleVoiceCoaching() {
        val newState = !voiceCoachingManager.isVoiceCoachingEnabled.value
        voiceCoachingManager.setVoiceCoachingEnabled(newState)
        
        _uiState.value = _uiState.value.copy(
            voiceCoachingActive = newState && _uiState.value.isTracking
        )
    }
    
    /**
     * Select a different coach personality
     */
    fun selectCoach(coachId: String) {
        viewModelScope.launch {
            val result = voiceCoachingManager.selectCoach(coachId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to select coach: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    /**
     * Test the selected coach voice
     */
    fun testCoachVoice(coachId: String) {
        viewModelScope.launch {
            voiceCoachingManager.testCoachVoice(coachId)
        }
    }
    
    /**
     * Get audio focus status for UI display
     */
    fun getAudioFocusStatus() = voiceCoachingManager.getAudioFocusStatus()
    
    /**
     * Get voice coaching cache statistics
     */
    suspend fun getCacheStats() = voiceCoachingManager.getCacheStats()
    
    /**
     * Get smart trigger statistics
     */
    fun getTriggerStats() = voiceCoachingManager.getTriggerStats()
    
    /**
     * Preload coaching phrases for better performance
     */
    fun preloadCoachingPhrases(coachId: String? = null) {
        viewModelScope.launch {
            voiceCoachingManager.preloadCoachingPhrases(coachId)
        }
    }
    
    /**
     * Set target pace for coaching
     */
    fun setTargetPace(pace: String) {
        _uiState.value = _uiState.value.copy(targetPace = pace)
    }
    
    /**
     * Set target distance for coaching
     */
    fun setTargetDistance(distance: Float) {
        _uiState.value = _uiState.value.copy(targetDistance = distance)
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceCoachingManager.cleanup()
    }
}

/**
 * Enhanced UI state for RunTrackingScreen with voice coaching integration
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
    val locationHistory: List<com.runningcoach.v2.domain.model.LocationData> = emptyList(),
    val lastLocationUpdate: Long = 0L,
    val sessionCompleted: Boolean = false,
    val error: String? = null,
    
    // Voice coaching state
    val voiceCoachingActive: Boolean = false,
    val targetPace: String? = null,
    val targetDistance: Float? = null
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