package com.runningcoach.v2.data.service

/**
 * Adapter to expose the existing GeminiService through the provider-agnostic LLMService interface.
 */
class GeminiLLMAdapter(
    private val geminiService: GeminiService
) : LLMService {
    override suspend fun generateFitnessAdvice(
        userMessage: String,
        userContext: UserFitnessContext
    ) = geminiService.generateFitnessAdvice(userMessage, userContext)

    override suspend fun generateTrainingPlan(
        userGoals: String,
        currentFitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ) = geminiService.generateTrainingPlan(userGoals, currentFitnessLevel, targetRace, timeframe)

    override suspend fun generateRunCoaching(
        metrics: RunMetricsContext,
        goals: UserGoals
    ) = geminiService.generateRunCoaching(metrics, goals)
}

