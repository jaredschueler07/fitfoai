package com.runningcoach.v2.data.service

import com.runningcoach.v2.domain.model.RunMetrics

/**
 * Provider-agnostic interface for large language model services
 * used by the FitnessCoachAgent.
 */
interface LLMService {
    suspend fun generateFitnessAdvice(
        userMessage: String,
        userContext: UserFitnessContext
    ): Result<String>

    suspend fun generateTrainingPlan(
        userGoals: String,
        currentFitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ): Result<String>

    suspend fun generateRunCoaching(
        metrics: RunMetricsContext,
        goals: UserGoals
    ): Result<String>
}

