package com.runningcoach.v2.domain.repository

import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for run session operations following Clean Architecture principles.
 * Provides methods for CRUD operations and real-time metrics updates via Flow.
 */
interface RunSessionRepository {
    
    /**
     * Starts a new run session for the given user
     * @param userId The ID of the user starting the run
     * @return Result containing the session ID if successful, or error if failed
     */
    suspend fun startRunSession(userId: Long): Result<Long>
    
    /**
     * Ends the current run session
     * @param sessionId The ID of the session to end
     * @param runMetrics Final metrics for the completed run
     * @return Result indicating success or failure
     */
    suspend fun endRunSession(sessionId: Long, runMetrics: RunMetrics): Result<Unit>
    
    /**
     * Updates real-time metrics for an active run session
     * @param sessionId The ID of the session to update
     * @param runMetrics Current metrics
     * @return Result indicating success or failure
     */
    suspend fun updateRunMetrics(sessionId: Long, runMetrics: RunMetrics): Result<Unit>
    
    /**
     * Adds location data to a run session
     * @param sessionId The ID of the session
     * @param locationData New location point
     * @return Result indicating success or failure
     */
    suspend fun addLocationData(sessionId: Long, locationData: LocationData): Result<Unit>
    
    /**
     * Gets the current active run session for a user
     * @param userId The user ID
     * @return Result containing the session ID if active session exists
     */
    suspend fun getActiveSession(userId: Long): Result<Long?>
    
    /**
     * Gets run sessions for a user with pagination
     * @param userId The user ID
     * @param limit Number of sessions to retrieve
     * @param offset Offset for pagination
     * @return Flow of run sessions
     */
    fun getRunSessions(userId: Long, limit: Int = 20, offset: Int = 0): Flow<List<RunSession>>
    
    /**
     * Gets real-time metrics updates for an active session
     * @param sessionId The session ID
     * @return Flow of current run metrics
     */
    fun getRealTimeMetrics(sessionId: Long): Flow<RunMetrics>
    
    /**
     * Gets location history for a run session
     * @param sessionId The session ID
     * @return Flow of location data points
     */
    fun getLocationHistory(sessionId: Long): Flow<List<LocationData>>
    
    /**
     * Gets completed runs count for a user
     * @param userId The user ID
     * @return Total number of completed runs
     */
    suspend fun getCompletedRunsCount(userId: Long): Result<Int>
    
    /**
     * Gets total distance for a user across all completed runs
     * @param userId The user ID
     * @return Total distance in meters
     */
    suspend fun getTotalDistance(userId: Long): Result<Float>
    
    /**
     * Deletes a run session
     * @param sessionId The session ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRunSession(sessionId: Long): Result<Unit>
}

/**
 * Domain model representing a run session
 */
enum class SessionSource { FITFOAI, GOOGLE_FIT }

data class RunSession(
    val id: Long,
    val userId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long? = null, // in milliseconds
    val distance: Float? = null, // in meters
    val averagePace: Float? = null, // in minutes per kilometer
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val caloriesBurned: Int? = null,
    val routePoints: List<LocationData>? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val source: SessionSource? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Gets formatted distance display
     */
    fun getFormattedDistance(): String {
        return distance?.let { distMeters ->
            val miles = distMeters / 1609.344f
            "${String.format("%.2f", miles)} mi"
        } ?: "0.00 mi"
    }
    
    /**
     * Gets formatted duration display
     */
    fun getFormattedDuration(): String {
        return duration?.let { dur ->
            val totalSeconds = dur / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                else -> String.format("%d:%02d", minutes, seconds)
            }
        } ?: "00:00"
    }
    
    /**
     * Gets formatted pace display
     */
    fun getFormattedPace(): String {
        return averagePace?.let { paceMinPerKm ->
            val pacePerMile = paceMinPerKm / 0.621371f
            val minutes = pacePerMile.toInt()
            val seconds = ((pacePerMile - minutes) * 60).toInt()
            String.format("%d:%02d /mi", minutes, seconds)
        } ?: "--:-- /mi"
    }
}
