package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.domain.repository.RunSession
import com.runningcoach.v2.domain.repository.RunSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for retrieving user's run session history and statistics.
 */
class GetRunSessionsUseCase(
    private val repository: RunSessionRepository
) {
    /**
     * Gets run sessions for a user with pagination.
     * 
     * @param userId The user ID
     * @param limit Number of sessions to retrieve (default 20)
     * @param offset Offset for pagination (default 0)
     * @return Flow of run sessions
     */
    fun getRunSessions(userId: Long, limit: Int = 20, offset: Int = 0): Flow<List<RunSession>> {
        return repository.getRunSessions(userId, limit, offset)
    }
    
    /**
     * Gets the user's current active run session if any.
     * 
     * @param userId The user ID
     * @return Result containing session ID if active session exists
     */
    suspend fun getActiveSession(userId: Long): Result<Long?> {
        return repository.getActiveSession(userId)
    }
    
    /**
     * Gets run statistics for a user.
     * 
     * @param userId The user ID
     * @return Result containing RunStatistics
     */
    suspend fun getRunStatistics(userId: Long): Result<RunStatistics> {
        return try {
            val completedRunsResult = repository.getCompletedRunsCount(userId)
            val totalDistanceResult = repository.getTotalDistance(userId)
            
            if (completedRunsResult.isFailure || totalDistanceResult.isFailure) {
                return Result.failure(
                    Exception("Failed to retrieve run statistics")
                )
            }
            
            val stats = RunStatistics(
                totalRuns = completedRunsResult.getOrElse { 0 },
                totalDistance = totalDistanceResult.getOrElse { 0f },
                averageDistance = if (completedRunsResult.getOrElse { 0 } > 0) {
                    totalDistanceResult.getOrElse { 0f } / completedRunsResult.getOrElse { 1 }
                } else 0f
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets recent run sessions with enhanced statistics.
     * 
     * @param userId The user ID
     * @param limit Number of recent sessions to retrieve
     * @return Flow of enhanced run session data
     */
    fun getRecentRunsWithStats(userId: Long, limit: Int = 10): Flow<List<EnhancedRunSession>> {
        return getRunSessions(userId, limit, 0).map { sessions ->
            sessions.map { session ->
                EnhancedRunSession(
                    runSession = session,
                    averageSpeedKmh = session.averagePace?.let { pace ->
                        if (pace > 0) 60f / pace else 0f
                    } ?: 0f,
                    routePointCount = session.routePoints?.size ?: 0,
                    hasGPSData = !session.routePoints.isNullOrEmpty()
                )
            }
        }
    }
    
    /**
     * Deletes a run session.
     * 
     * @param sessionId The session ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRunSession(sessionId: Long): Result<Unit> {
        return repository.deleteRunSession(sessionId)
    }
}

/**
 * Data class containing run statistics for a user
 */
data class RunStatistics(
    val totalRuns: Int,
    val totalDistance: Float, // in meters
    val averageDistance: Float // in meters
) {
    val totalDistanceKm: Float = totalDistance / 1000f
    val averageDistanceKm: Float = averageDistance / 1000f
    
    fun getFormattedTotalDistance(): String {
        return when {
            totalDistance < 1000 -> "${totalDistance.toInt()}m"
            else -> "${String.format("%.1f", totalDistanceKm)}km"
        }
    }
    
    fun getFormattedAverageDistance(): String {
        return when {
            averageDistance < 1000 -> "${averageDistance.toInt()}m"
            else -> "${String.format("%.1f", averageDistanceKm)}km"
        }
    }
}

/**
 * Enhanced run session data with additional calculated fields
 */
data class EnhancedRunSession(
    val runSession: RunSession,
    val averageSpeedKmh: Float,
    val routePointCount: Int,
    val hasGPSData: Boolean
) {
    fun getFormattedAverageSpeed(): String {
        return if (averageSpeedKmh > 0) {
            String.format("%.1f km/h", averageSpeedKmh)
        } else {
            "N/A"
        }
    }
}