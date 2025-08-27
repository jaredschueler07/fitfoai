package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.data.service.LocationService
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
 */
class RunSessionRepositoryImpl(
    private val runSessionDao: RunSessionDao,
    private val locationService: LocationService,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : RunSessionRepository {

    // Real-time metrics cache for active sessions
    private val _activeMetrics = MutableStateFlow<Map<Long, RunMetrics>>(emptyMap())
    private val _locationHistory = MutableStateFlow<Map<Long, List<LocationData>>>(emptyMap())
    
    // JSON serializer for location data
    private val gson = Gson()
    private val locationListType = object : TypeToken<List<LocationData>>() {}.type

    override suspend fun startRunSession(userId: Long): Result<Long> {
        return try {
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
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRunMetrics(sessionId: Long, runMetrics: RunMetrics): Result<Unit> {
        return try {
            val currentMetrics = _activeMetrics.value.toMutableMap()
            currentMetrics[sessionId] = runMetrics.copy(lastUpdateTime = System.currentTimeMillis())
            _activeMetrics.value = currentMetrics
            
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
}