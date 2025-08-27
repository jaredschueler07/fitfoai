package com.runningcoach.v2.data.repository

import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.repository.RunSession
import com.runningcoach.v2.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock implementation of RunSessionRepository for testing the UI without backend dependencies.
 * Provides simulated data for run tracking functionality.
 */
class MockRunSessionRepository : RunSessionRepository {
    
    private val _realTimeMetrics = MutableStateFlow(RunMetrics())
    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    
    private val activeSessions = mutableMapOf<Long, Long>() // userId to sessionId
    private var nextSessionId = 1L
    
    override suspend fun startRunSession(userId: Long): Result<Long> {
        return try {
            // Check if user already has an active session
            if (activeSessions.containsKey(userId)) {
                Result.failure(IllegalStateException("User already has an active run session"))
            } else {
                val sessionId = nextSessionId++
                activeSessions[userId] = sessionId
                
                // Start simulated metrics updates
                startSimulatedMetricsUpdates(sessionId)
                
                Result.success(sessionId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun endRunSession(sessionId: Long, runMetrics: RunMetrics): Result<Unit> {
        return try {
            // Find and remove the session
            val userEntry = activeSessions.entries.find { it.value == sessionId }
            userEntry?.let { activeSessions.remove(it.key) }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRunMetrics(sessionId: Long, runMetrics: RunMetrics): Result<Unit> {
        return try {
            _realTimeMetrics.value = runMetrics
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addLocationData(sessionId: Long, locationData: LocationData): Result<Unit> {
        return try {
            val currentHistory = _locationHistory.value.toMutableList()
            currentHistory.add(locationData)
            _locationHistory.value = currentHistory
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveSession(userId: Long): Result<Long?> {
        return try {
            val sessionId = activeSessions[userId]
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getRealTimeMetrics(sessionId: Long): Flow<RunMetrics> {
        return _realTimeMetrics.asStateFlow()
    }
    
    override fun getLocationHistory(sessionId: Long): Flow<List<LocationData>> {
        return _locationHistory.asStateFlow()
    }
    
    override fun getRunSessions(userId: Long, limit: Int, offset: Int): Flow<List<RunSession>> {
        // Return empty list for mock implementation
        return flowOf(emptyList())
    }
    
    override suspend fun getCompletedRunsCount(userId: Long): Result<Int> {
        return Result.success(0)
    }
    
    override suspend fun getTotalDistance(userId: Long): Result<Float> {
        return Result.success(0f)
    }
    
    override suspend fun deleteRunSession(sessionId: Long): Result<Unit> {
        return Result.success(Unit)
    }
    
    /**
     * Simulates real-time metrics updates for demo purposes
     */
    private fun startSimulatedMetricsUpdates(sessionId: Long) {
        // In a real app, this would be connected to location services
        // For now, we provide static mock data
        val mockLocation = LocationData(
            latitude = 41.8781,  // Chicago coordinates
            longitude = -87.6298,
            accuracy = 5.0f,
            speed = 3.5f, // m/s (about 7.8 mph)
            timestamp = System.currentTimeMillis()
        )
        
        val mockMetrics = RunMetrics(
            distance = 0f,
            duration = 0L,
            currentSpeed = 3.5f,
            currentLocation = mockLocation,
            totalLocationPoints = 1,
            lastLocationTimestamp = System.currentTimeMillis()
        )
        
        _realTimeMetrics.value = mockMetrics
        _locationHistory.value = listOf(mockLocation)
    }
}