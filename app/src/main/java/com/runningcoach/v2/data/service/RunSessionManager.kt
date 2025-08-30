package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class RunSessionManager(
    private val context: Context,
    private val locationService: LocationService,
    private val database: FITFOAIDatabase,
    private val voiceCoachingManager: VoiceCoachingManager? = null
) {
    
    private val runSessionDao: RunSessionDao = database.runSessionDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _currentSession = MutableStateFlow<RunSessionEntity?>(null)
    val currentSession: StateFlow<RunSessionEntity?> = _currentSession.asStateFlow()
    
    private val _currentMetrics = MutableStateFlow(RunMetrics())
    val currentMetrics: StateFlow<RunMetrics> = _currentMetrics.asStateFlow()
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    
    private var sessionStartTime: Long = 0L
    private var lastUpdateTime: Long = 0L
    private var currentUserId: Long = 1L // Default user ID
    
    // Voice coaching settings
    private var targetPace: String? = null
    private var targetDistance: String? = null
    
    fun setCurrentUser(userId: Long) {
        currentUserId = userId
    }
    
    fun startRunSession(
        targetPace: String? = null,
        targetDistance: String? = null,
        enableVoiceCoaching: Boolean = true
    ): Boolean {
        if (_isSessionActive.value) {
            return false
        }
        
        if (!locationService.hasLocationPermission()) {
            return false
        }
        
        sessionStartTime = System.currentTimeMillis()
        lastUpdateTime = sessionStartTime
        
        // Set coaching parameters
        this.targetPace = targetPace
        this.targetDistance = targetDistance
        
        // Create new run session
        val newSession = RunSessionEntity(
            userId = currentUserId,
            startTime = sessionStartTime,
            duration = 0L,
            distance = 0f
        )
        
        scope.launch {
            val sessionId = runSessionDao.insertRunSession(newSession)
            val sessionWithId = newSession.copy(id = sessionId)
            _currentSession.value = sessionWithId
        }
        
        _isSessionActive.value = true
        _currentMetrics.value = RunMetrics(startTime = sessionStartTime)
        
        // Start location tracking
        locationService.startLocationTracking()
        
        // Start metrics calculation
        startMetricsCalculation()
        
        // Start voice coaching if enabled
        if (enableVoiceCoaching) {
            scope.launch {
                voiceCoachingManager?.startVoiceCoaching(
                    currentMetrics,
                    targetPace,
                    targetDistance
                )
            }
        }
        
        return true
    }
    
    fun pauseRunSession() {
        if (!_isSessionActive.value) return
        
        locationService.stopLocationTracking()
        voiceCoachingManager?.pauseVoiceCoaching()
        _isSessionActive.value = false
    }
    
    fun resumeRunSession() {
        if (_isSessionActive.value) return
        
        locationService.startLocationTracking()
        voiceCoachingManager?.resumeVoiceCoaching(currentMetrics)
        _isSessionActive.value = true
        startMetricsCalculation()
    }
    
    fun stopRunSession(): Boolean {
        if (!_isSessionActive.value) {
            return false
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - sessionStartTime
        
        locationService.stopLocationTracking()
        voiceCoachingManager?.stopVoiceCoaching()
        _isSessionActive.value = false
        
        // Calculate final metrics
        val finalMetrics = calculateFinalMetrics()
        
        // Update session in database
        scope.launch {
            _currentSession.value?.let { session ->
                val updatedSession = session.copy(
                    endTime = endTime,
                    duration = duration,
                    distance = finalMetrics.distance,
                    avgPace = finalMetrics.averagePace,
                    avgHeartRate = finalMetrics.averageHeartRate,
                    maxHeartRate = finalMetrics.maxHeartRate,
                    calories = finalMetrics.caloriesBurned,
                    route = locationService.locationHistory.value.joinToString(";") { it.toJson() }
                )
                
                runSessionDao.updateRunSession(updatedSession)
                _currentSession.value = updatedSession
            }
        }
        
        return true
    }
    
    private fun startMetricsCalculation() {
        scope.launch {
            // Combine location updates with metrics calculation
            combine(
                locationService.locationHistory,
                locationService.currentLocation
            ) { history, currentLocation ->
                updateMetrics(history, currentLocation)
            }.collect()
        }
    }
    
    private fun updateMetrics(locationHistory: List<LocationData>, currentLocation: LocationData?) {
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - sessionStartTime
        
        // Calculate distance from location history
        val distance = locationService.calculateDistance(locationHistory)
        
        // Calculate elevation gain
        val elevationGain = locationService.calculateElevationGain(locationHistory)
        
        // Calculate speeds and paces
        val averageSpeed = if (duration > 0) distance / (duration / 1000f) else 0f
        val averagePace = if (averageSpeed > 0) 1000f / averageSpeed / 60f else 0f // min/km
        
        val currentSpeed = currentLocation?.speed ?: 0f
        val currentPace = if (currentSpeed > 0) 1000f / currentSpeed / 60f else 0f
        
        // Estimate calories (simple formula - can be improved)
        val caloriesBurned = calculateCalories(distance, duration, currentUserId)
        
        val updatedMetrics = RunMetrics(
            distance = distance,
            duration = duration,
            averagePace = averagePace,
            currentPace = currentPace,
            averageSpeed = averageSpeed,
            currentSpeed = currentSpeed,
            caloriesBurned = caloriesBurned,
            elevationGain = elevationGain,
            startTime = sessionStartTime,
            lastUpdateTime = currentTime
        )
        
        _currentMetrics.value = updatedMetrics
        
        // Update session in database periodically
        if (currentTime - lastUpdateTime > 5000) { // Every 5 seconds
            lastUpdateTime = currentTime
            scope.launch {
                _currentSession.value?.let { session ->
                    val updatedSession = session.copy(
                        distance = distance,
                        avgPace = averagePace,
                        calories = caloriesBurned
                    )
                    runSessionDao.updateRunSession(updatedSession)
                    _currentSession.value = updatedSession
                }
            }
        }
    }
    
    private fun calculateFinalMetrics(): RunMetrics {
        val locationHistory = locationService.locationHistory.value
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - sessionStartTime
        
        val distance = locationService.calculateDistance(locationHistory)
        val elevationGain = locationService.calculateElevationGain(locationHistory)
        val averageSpeed = if (duration > 0) distance / (duration / 1000f) else 0f
        val averagePace = if (averageSpeed > 0) 1000f / averageSpeed / 60f else 0f
        val caloriesBurned = calculateCalories(distance, duration, currentUserId)
        
        return RunMetrics(
            distance = distance,
            duration = duration,
            averagePace = averagePace,
            averageSpeed = averageSpeed,
            caloriesBurned = caloriesBurned,
            elevationGain = elevationGain,
            startTime = sessionStartTime,
            lastUpdateTime = currentTime
        )
    }
    
    private fun calculateCalories(distance: Float, duration: Long, userId: Long): Int {
        // Simple calorie calculation based on distance and time
        // In a real app, this would use user's weight, age, and more accurate formulas
        val hours = duration / 3600000f // Convert to hours
        val km = distance / 1000f
        
        // Rough estimate: 100 calories per km for running
        return (km * 100).toInt()
    }
    
    fun getRecentRuns(limit: Int = 10) = runSessionDao.getRecentCompletedRuns(currentUserId, limit)
    
    suspend fun getTotalDistance() = runSessionDao.getTotalDistance(currentUserId)
    
    suspend fun getTotalRuns() = runSessionDao.getTotalCompletedRuns(currentUserId)
    
    fun clearCurrentSession() {
        _currentSession.value = null
        _currentMetrics.value = RunMetrics()
        _isSessionActive.value = false
        locationService.clearLocationHistory()
    }
    
    // Voice coaching controls
    fun toggleVoiceCoaching(enabled: Boolean) {
        voiceCoachingManager?.setVoiceCoachingEnabled(enabled)
    }
    
    fun provideManualCoaching(scenario: FitnessCoachAgent.CoachingScenario) {
        voiceCoachingManager?.provideManualCoaching(scenario)
    }
    
    fun getVoiceCoachingStatus(): VoiceCoachingManager.CoachingStatus? {
        return voiceCoachingManager?.getCurrentCoachingStatus()
    }
    
    fun stopVoiceCoaching() {
        voiceCoachingManager?.stopVoiceCoaching()
    }
}
