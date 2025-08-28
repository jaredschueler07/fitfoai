package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.data.service.BackgroundLocationService
import com.runningcoach.v2.data.service.LocationService
import com.runningcoach.v2.data.service.SessionRecoveryManager
import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.repository.RunSession
import com.runningcoach.v2.domain.repository.RunSessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of RunSessionRepository following Clean Architecture principles.
 * Manages run session data persistence and real-time metrics updates using Room database and Flow.
 * Enhanced with crash recovery and background service integration.
 */
class RunSessionRepositoryImpl(
    private val runSessionDao: RunSessionDao,
    private val locationService: LocationService,
    private val sessionRecoveryManager: SessionRecoveryManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : RunSessionRepository {

    // Real-time metrics cache for active sessions
    private val _activeMetrics = MutableStateFlow<Map<Long, RunMetrics>>(emptyMap())
    private val _locationHistory = MutableStateFlow<Map<Long, List<LocationData>>>(emptyMap())
    
    // JSON serializer for location data
    private val gson = Gson()
    private val locationListType = object : TypeToken<List<LocationData>>() {}.type
    
    // Crash recovery state
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    override suspend fun startRunSession(userId: Long): Result<Long> {
        return try {
            // Check for crash recovery first
            if (sessionRecoveryManager.hasRecoverableSession()) {
                val recoveryResult = attemptSessionRecovery()
                if (recoveryResult.isSuccess) {
                    return recoveryResult
                }
            }
            
            val currentTime = System.currentTimeMillis()
            val sessionEntity = RunSessionEntity(
                userId = userId,
                startTime = currentTime,
                isCompleted = false,
                createdAt = currentTime
            )
            
            val sessionId = runSessionDao.insertRunSession(sessionEntity)
            
            // Initialize empty metrics and location history for this session
            val currentMetrics = _activeMetrics.value.toMutableMap()
            currentMetrics[sessionId] = RunMetrics(startTime = currentTime)
            _activeMetrics.value = currentMetrics
            
            val currentHistory = _locationHistory.value.toMutableMap()
            currentHistory[sessionId] = emptyList()
            _locationHistory.value = currentHistory
            
            // Save session for crash recovery
            sessionRecoveryManager.saveActiveSession(sessionId, userId)
            
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endRunSession(sessionId: Long, runMetrics: RunMetrics): Result<Unit> {
        return try {
            val existingSession = runSessionDao.getRunSessionById(sessionId)
                ?: return Result.failure(IllegalArgumentException("Session not found"))
            
            val endTime = System.currentTimeMillis()
            val locationHistory = _locationHistory.value[sessionId] ?: emptyList()
            val routeJson = if (locationHistory.isNotEmpty()) {
                gson.toJson(locationHistory)
            } else null
            
            val updatedSession = existingSession.copy(
                endTime = endTime,
                duration = runMetrics.duration * 1000, // Convert seconds to milliseconds
                distance = runMetrics.distance,
                averagePace = runMetrics.averagePace,
                averageHeartRate = runMetrics.averageHeartRate,
                maxHeartRate = runMetrics.maxHeartRate,
                caloriesBurned = runMetrics.caloriesBurned,
                route = routeJson,
                isCompleted = true
            )
            
            runSessionDao.updateRunSession(updatedSession)
            
            // Clean up active metrics and location history
            val currentMetrics = _activeMetrics.value.toMutableMap()
            currentMetrics.remove(sessionId)
            _activeMetrics.value = currentMetrics
            
            val currentHistory = _locationHistory.value.toMutableMap()
            currentHistory.remove(sessionId)
            _locationHistory.value = currentHistory
            
            // Clear crash recovery data
            sessionRecoveryManager.clearActiveSession()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRunMetrics(sessionId: Long, runMetrics: RunMetrics): Result<Unit> {
        return try {
            val updatedMetrics = runMetrics.copy(lastUpdateTime = System.currentTimeMillis())
            val currentMetrics = _activeMetrics.value.toMutableMap()
            currentMetrics[sessionId] = updatedMetrics
            _activeMetrics.value = currentMetrics
            
            // Save to recovery manager periodically
            sessionRecoveryManager.saveMetrics(sessionId, updatedMetrics)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addLocationData(sessionId: Long, locationData: LocationData): Result<Unit> {
        return try {
            val currentHistory = _locationHistory.value.toMutableMap()
            val existingHistory = currentHistory[sessionId] ?: emptyList()
            currentHistory[sessionId] = existingHistory + locationData
            _locationHistory.value = currentHistory
            
            // Update metrics with current location
            val currentMetrics = _activeMetrics.value[sessionId]
            if (currentMetrics != null) {
                val updatedMetrics = currentMetrics.copy(
                    currentLocation = locationData,
                    totalLocationPoints = existingHistory.size + 1,
                    lastLocationTimestamp = locationData.timestamp,
                    distance = locationService.calculateDistance(currentHistory[sessionId] ?: emptyList()),
                    elevationGain = locationService.calculateElevationGain(currentHistory[sessionId] ?: emptyList())
                )
                updateRunMetrics(sessionId, updatedMetrics)
            }
            
            // Save location history to recovery manager periodically (every 5 points)
            val newHistory = currentHistory[sessionId] ?: emptyList()
            if (newHistory.size % 5 == 0) {
                sessionRecoveryManager.saveLocationHistory(sessionId, newHistory)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveSession(userId: Long): Result<Long?> {
        return try {
            val activeSession = runSessionDao.getCurrentActiveSession(userId)
            Result.success(activeSession?.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRunSessions(userId: Long, limit: Int, offset: Int): Flow<List<RunSession>> {
        return runSessionDao.getRecentCompletedRuns(userId, limit)
            .map { entities ->
                entities.map { entity ->
                    val routePoints = entity.route?.let { routeJson ->
                        try {
                            gson.fromJson<List<LocationData>>(routeJson, locationListType)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    
                    RunSession(
                        id = entity.id,
                        userId = entity.userId,
                        startTime = entity.startTime,
                        endTime = entity.endTime,
                        duration = entity.duration,
                        distance = entity.distance,
                        averagePace = entity.averagePace,
                        averageHeartRate = entity.averageHeartRate,
                        maxHeartRate = entity.maxHeartRate,
                        caloriesBurned = entity.caloriesBurned,
                        routePoints = routePoints,
                        notes = entity.notes,
                        isCompleted = entity.isCompleted,
                        createdAt = entity.createdAt
                    )
                }
            }
    }

    override fun getRealTimeMetrics(sessionId: Long): Flow<RunMetrics> {
        return _activeMetrics
            .map { metricsMap -> metricsMap[sessionId] }
            .filterNotNull()
            .distinctUntilChanged()
    }

    override fun getLocationHistory(sessionId: Long): Flow<List<LocationData>> {
        return _locationHistory
            .map { historyMap -> historyMap[sessionId] ?: emptyList() }
            .distinctUntilChanged()
    }

    override suspend fun getCompletedRunsCount(userId: Long): Result<Int> {
        return try {
            val count = runSessionDao.getTotalCompletedRuns(userId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalDistance(userId: Long): Result<Float> {
        return try {
            val totalDistance = runSessionDao.getTotalDistance(userId) ?: 0f
            Result.success(totalDistance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRunSession(sessionId: Long): Result<Unit> {
        return try {
            val session = runSessionDao.getRunSessionById(sessionId)
            if (session != null) {
                runSessionDao.deleteRunSession(session)
                
                // Clean up any active metrics
                val currentMetrics = _activeMetrics.value.toMutableMap()
                currentMetrics.remove(sessionId)
                _activeMetrics.value = currentMetrics
                
                val currentHistory = _locationHistory.value.toMutableMap()
                currentHistory.remove(sessionId)
                _locationHistory.value = currentHistory
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a Flow that combines location updates with metrics calculations
     * for real-time GPS run tracking
     */
    fun getLocationAndMetricsFlow(sessionId: Long): Flow<Pair<LocationData, RunMetrics>> {
        return locationService.getLocationUpdates()
            .onEach { locationData ->
                addLocationData(sessionId, locationData)
            }
            .combine(getRealTimeMetrics(sessionId)) { location, metrics ->
                location to metrics
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Starts comprehensive GPS tracking for a session
     */
    fun startGPSTracking(sessionId: Long): Flow<RunMetrics> {
        return flow {
            locationService.startLocationTracking()
            
            locationService.getLocationUpdates()
                .collect { locationData ->
                    addLocationData(sessionId, locationData)
                    val currentMetrics = _activeMetrics.value[sessionId]
                    if (currentMetrics != null) {
                        emit(currentMetrics)
                    }
                }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Stops GPS tracking for all sessions
     */
    fun stopGPSTracking() {
        locationService.stopLocationTracking()
    }
    
    /**
     * Attempt to recover session from crash
     */
    private suspend fun attemptSessionRecovery(): Result<Long> {
        return try {
            _isRecovering.value = true
            
            val recoveryData = sessionRecoveryManager.getRecoveryData()
                ?: return Result.failure(Exception("No recovery data available"))
            
            val (sessionId, userId, locationHistory, savedMetrics) = recoveryData
            
            // Verify session exists in database
            val existingSession = runSessionDao.getRunSessionById(sessionId)
            if (existingSession == null) {
                // Session doesn't exist, clear recovery data
                sessionRecoveryManager.clearActiveSession()
                return Result.failure(Exception("Session not found in database"))
            }
            
            // Restore location history
            val currentHistory = _locationHistory.value.toMutableMap()
            currentHistory[sessionId] = locationHistory
            _locationHistory.value = currentHistory
            
            // Restore location service history
            locationService.restoreLocationHistory(locationHistory)
            
            // Restore metrics
            val currentMetrics = _activeMetrics.value.toMutableMap()
            val metrics = savedMetrics ?: RunMetrics(startTime = existingSession.startTime)
            currentMetrics[sessionId] = metrics
            _activeMetrics.value = currentMetrics
            
            _isRecovering.value = false
            Result.success(sessionId)
            
        } catch (e: Exception) {
            _isRecovering.value = false
            sessionRecoveryManager.saveErrorState(-1, "Recovery failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check for and handle session recovery on repository initialization
     */
    suspend fun initializeWithRecovery(): Result<Long?> {
        return try {
            if (sessionRecoveryManager.hasRecoverableSession()) {
                val recoveryResult = attemptSessionRecovery()
                if (recoveryResult.isSuccess) {
                    Result.success(recoveryResult.getOrNull())
                } else {
                    // Recovery failed, clean up
                    sessionRecoveryManager.forceCleanup()
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Force clean up recovery data (for manual intervention)
     */
    fun forceCleanupRecovery() {
        sessionRecoveryManager.forceCleanup()
        _isRecovering.value = false
    }
    
    /**
     * Get recovery diagnostics for debugging
     */
    fun getRecoveryDiagnostics(): Map<String, Any> {
        val diagnostics = sessionRecoveryManager.getDiagnostics().toMutableMap()
        diagnostics["repositoryState"] = mapOf(
            "activeMetricsCount" to _activeMetrics.value.size,
            "locationHistoryCount" to _locationHistory.value.size,
            "isRecovering" to _isRecovering.value
        )
        return diagnostics
    }
    
    /**
     * Background service integration - start session with background location service
     */
    suspend fun startSessionWithBackgroundService(
        userId: Long,
        context: android.content.Context
    ): Result<Long> {
        val startResult = startRunSession(userId)
        
        if (startResult.isSuccess) {
            val sessionId = startResult.getOrNull()!!
            
            // Start background location service
            BackgroundLocationService.startService(context, sessionId, userId)
            
            return Result.success(sessionId)
        }
        
        return startResult
    }
    
    /**
     * Background service integration - stop session and service
     */
    suspend fun endSessionWithBackgroundService(
        sessionId: Long,
        runMetrics: RunMetrics,
        context: android.content.Context
    ): Result<Unit> {
        val endResult = endRunSession(sessionId, runMetrics)
        
        // Stop background service regardless of end result
        BackgroundLocationService.stopService(context)
        
        return endResult
    }
    
    /**
     * Enhanced GPS tracking with crash recovery for background service
     */
    fun startCrashResistantGPSTracking(sessionId: Long): Flow<RunMetrics> {
        return flow {
            try {
                // Enable service-optimized tracking
                locationService.startServiceOptimizedTracking()
                
                locationService.getServiceOptimizedLocationUpdates()
                    .collect { locationData ->
                        // Add location with crash recovery
                        addLocationData(sessionId, locationData)
                        
                        val currentMetrics = _activeMetrics.value[sessionId]
                        if (currentMetrics != null) {
                            emit(currentMetrics)
                        }
                    }
            } catch (e: Exception) {
                // Save error state for recovery
                sessionRecoveryManager.saveErrorState(sessionId, "GPS tracking error: ${e.message}")
                throw e
            }
        }.flowOn(Dispatchers.IO)
            .retry(3) { exception ->
                // Retry GPS tracking up to 3 times with delay
                kotlinx.coroutines.delay(5000)
                true
            }
    }
}