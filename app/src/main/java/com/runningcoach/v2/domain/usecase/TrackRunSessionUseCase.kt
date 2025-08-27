package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Use case for real-time GPS run session tracking.
 * Combines location updates with metrics calculations and provides real-time updates.
 */
class TrackRunSessionUseCase(
    private val repository: RunSessionRepository
) {
    /**
     * Gets real-time metrics for an active run session.
     * 
     * @param sessionId The ID of the session to track
     * @return Flow of current RunMetrics
     */
    fun getRealTimeMetrics(sessionId: Long): Flow<RunMetrics> {
        return repository.getRealTimeMetrics(sessionId)
    }
    
    /**
     * Gets location history for a run session.
     * 
     * @param sessionId The ID of the session
     * @return Flow of location data points
     */
    fun getLocationHistory(sessionId: Long): Flow<List<LocationData>> {
        return repository.getLocationHistory(sessionId)
    }
    
    /**
     * Updates metrics for an active session.
     * 
     * @param sessionId The session ID
     * @param metrics Updated metrics
     * @return Result indicating success or failure
     */
    suspend fun updateMetrics(sessionId: Long, metrics: RunMetrics): Result<Unit> {
        return try {
            // Validate metrics before updating
            if (metrics.distance < 0) {
                return Result.failure(IllegalArgumentException("Distance cannot be negative"))
            }
            
            if (metrics.duration < 0) {
                return Result.failure(IllegalArgumentException("Duration cannot be negative"))
            }
            
            repository.updateRunMetrics(sessionId, metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Adds a location point to the session.
     * 
     * @param sessionId The session ID
     * @param location New location data
     * @return Result indicating success or failure
     */
    suspend fun addLocationPoint(sessionId: Long, location: LocationData): Result<Unit> {
        return try {
            // Validate location data
            if (location.latitude < -90 || location.latitude > 90) {
                return Result.failure(IllegalArgumentException("Invalid latitude: ${location.latitude}"))
            }
            
            if (location.longitude < -180 || location.longitude > 180) {
                return Result.failure(IllegalArgumentException("Invalid longitude: ${location.longitude}"))
            }
            
            repository.addLocationData(sessionId, location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets combined real-time tracking data including metrics and route.
     * 
     * @param sessionId The session ID
     * @return Flow of tracking data containing metrics and latest location points
     */
    fun getTrackingData(sessionId: Long): Flow<TrackingData> {
        return combine(
            getRealTimeMetrics(sessionId),
            getLocationHistory(sessionId)
        ) { metrics, locationHistory ->
            TrackingData(
                metrics = metrics,
                locationHistory = locationHistory,
                lastLocationPoint = locationHistory.lastOrNull()
            )
        }
    }
    
    /**
     * Calculates pace from location history for more accurate real-time pace.
     * 
     * @param locations List of location points
     * @param timeWindowSeconds Time window in seconds to calculate pace over
     * @return Current pace in minutes per kilometer
     */
    fun calculateRealTimePace(locations: List<LocationData>, timeWindowSeconds: Long = 30): Float {
        if (locations.size < 2) return 0f
        
        val currentTime = System.currentTimeMillis()
        val windowStartTime = currentTime - (timeWindowSeconds * 1000)
        
        // Filter locations within the time window
        val recentLocations = locations.filter { it.timestamp >= windowStartTime }
        if (recentLocations.size < 2) return 0f
        
        // Calculate distance and time for recent locations
        var totalDistance = 0f
        for (i in 0 until recentLocations.size - 1) {
            val loc1 = recentLocations[i]
            val loc2 = recentLocations[i + 1]
            
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                loc1.latitude, loc1.longitude,
                loc2.latitude, loc2.longitude,
                results
            )
            totalDistance += results[0]
        }
        
        val timeDiffSeconds = (recentLocations.last().timestamp - recentLocations.first().timestamp) / 1000f
        
        if (timeDiffSeconds <= 0 || totalDistance <= 0) return 0f
        
        // Convert to pace (minutes per kilometer)
        val speedMPerSec = totalDistance / timeDiffSeconds
        val paceMinPerKm = (1000.0 / speedMPerSec) / 60.0
        
        return paceMinPerKm.toFloat()
    }
}

/**
 * Data class containing all real-time tracking information
 */
data class TrackingData(
    val metrics: RunMetrics,
    val locationHistory: List<LocationData>,
    val lastLocationPoint: LocationData?
) {
    val hasGPSSignal: Boolean = lastLocationPoint != null
    val routePointCount: Int = locationHistory.size
}