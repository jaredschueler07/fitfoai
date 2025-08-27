package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.repository.RunSessionRepository

/**
 * Use case for ending an active run session.
 * Validates the session and saves final metrics.
 */
class EndRunSessionUseCase(
    private val repository: RunSessionRepository
) {
    /**
     * Ends an active run session with final metrics.
     * 
     * @param sessionId The ID of the session to end
     * @param finalMetrics Final run metrics to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(sessionId: Long, finalMetrics: RunMetrics): Result<Unit> {
        return try {
            // Validate that metrics contain meaningful data
            if (finalMetrics.duration <= 0) {
                return Result.failure(IllegalArgumentException("Run session must have positive duration"))
            }
            
            if (finalMetrics.distance < 0) {
                return Result.failure(IllegalArgumentException("Distance cannot be negative"))
            }
            
            // End the session with validated metrics
            repository.endRunSession(sessionId, finalMetrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}