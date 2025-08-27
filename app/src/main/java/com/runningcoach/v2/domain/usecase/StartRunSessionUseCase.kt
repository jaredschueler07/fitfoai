package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.domain.repository.RunSessionRepository

/**
 * Use case for starting a new run session.
 * Follows Clean Architecture principles by encapsulating business logic.
 */
class StartRunSessionUseCase(
    private val repository: RunSessionRepository
) {
    /**
     * Starts a new run session for the specified user.
     * 
     * @param userId The ID of the user starting the run
     * @return Result containing the session ID if successful, error otherwise
     */
    suspend operator fun invoke(userId: Long): Result<Long> {
        return try {
            // Check if user already has an active session
            val activeSessionResult = repository.getActiveSession(userId)
            if (activeSessionResult.isSuccess && activeSessionResult.getOrNull() != null) {
                return Result.failure(IllegalStateException("User already has an active run session"))
            }
            
            // Start new session
            repository.startRunSession(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}