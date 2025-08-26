package com.runningcoach.v2.data.service

import com.runningcoach.v2.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GeminiService(private val httpClient: HttpClient) {
    
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val apiKey = BuildConfig.GEMINI_API_KEY
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @Serializable
    data class GeminiRequest(
        val contents: List<Content>
    )
    
    @Serializable
    data class Content(
        val parts: List<Part>
    )
    
    @Serializable
    data class Part(
        val text: String
    )
    
    @Serializable
    data class GeminiResponse(
        val candidates: List<Candidate>
    )
    
    @Serializable
    data class Candidate(
        val content: Content,
        val finishReason: String? = null,
        val index: Int? = null
    )
    
    suspend fun generateFitnessAdvice(
        userMessage: String,
        userContext: UserFitnessContext
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildFitnessCoachPrompt(userContext)
            val fullPrompt = "$systemPrompt\n\nUser: $userMessage\n\nFitness Coach:"
            
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = fullPrompt)
                        )
                    )
                )
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/models/gemini-pro:generateContent") {
                url {
                    parameters.append("key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val geminiResponse: GeminiResponse = json.decodeFromString(response.body())
                val aiResponse = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't generate a response right now. Please try again."
                
                Result.success(aiResponse)
            } else {
                Result.failure(Exception("API call failed with status: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateTrainingPlan(
        userGoals: String,
        currentFitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildTrainingPlanPrompt(userGoals, currentFitnessLevel, targetRace, timeframe)
            
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                )
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/models/gemini-pro:generateContent") {
                url {
                    parameters.append("key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val geminiResponse: GeminiResponse = json.decodeFromString(response.body())
                val trainingPlan = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Unable to generate training plan at this time."
                
                Result.success(trainingPlan)
            } else {
                Result.failure(Exception("Training plan generation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateRunCoaching(
        currentMetrics: RunMetricsContext,
        userGoals: UserGoals
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildRunCoachingPrompt(currentMetrics, userGoals)
            
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                )
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/models/gemini-pro:generateContent") {
                url {
                    parameters.append("key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val geminiResponse: GeminiResponse = json.decodeFromString(response.body())
                val coaching = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Keep going! You're doing great!"
                
                Result.success(coaching)
            } else {
                Result.failure(Exception("Coaching generation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildFitnessCoachPrompt(context: UserFitnessContext): String {
        return """
            You are an expert PhD-level fitness coach and running specialist with years of experience helping athletes achieve their goals. You provide personalized, science-based advice that is encouraging, practical, and safe.
            
            User Profile:
            - Experience Level: ${context.experienceLevel}
            - Current Fitness Goals: ${context.goals.joinToString(", ")}
            - Age: ${context.age}
            - Recent Activity: ${context.recentActivity}
            - Training History: ${context.trainingHistory}
            
            Guidelines for your responses:
            1. Be encouraging and motivational but realistic
            2. Provide specific, actionable advice
            3. Consider the user's experience level and current fitness
            4. Include scientific reasoning when helpful
            5. Always prioritize safety and injury prevention
            6. Keep responses concise but comprehensive
            7. Use a friendly, professional tone
            8. Ask follow-up questions when needed for better advice
            
            Remember: You're not just an AI, you're their dedicated fitness coach who cares about their success and wellbeing.
        """.trimIndent()
    }
    
    private fun buildTrainingPlanPrompt(
        goals: String,
        fitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ): String {
        return """
            Create a detailed, personalized training plan as an expert running coach.
            
            User Details:
            - Goals: $goals
            - Current Fitness Level: $fitnessLevel
            - Target Race: ${targetRace ?: "General fitness improvement"}
            - Timeframe: $timeframe
            
            Please provide:
            1. Weekly training structure (number of runs, rest days)
            2. Types of runs (easy, tempo, intervals, long runs)
            3. Progression plan over the timeframe
            4. Recovery and cross-training recommendations
            5. Key milestones and checkpoints
            6. Injury prevention tips
            7. Nutrition guidelines for training
            
            Format as a structured, easy-to-follow plan that can be implemented immediately.
        """.trimIndent()
    }
    
    private fun buildRunCoachingPrompt(
        metrics: RunMetricsContext,
        goals: UserGoals
    ): String {
        return """
            Provide real-time coaching advice during a run based on current metrics.
            
            Current Run Metrics:
            - Distance: ${metrics.distance}km
            - Duration: ${metrics.duration}
            - Current Pace: ${metrics.currentPace} min/km
            - Average Pace: ${metrics.averagePace} min/km
            - Heart Rate: ${metrics.heartRate ?: "Not available"}
            
            User Goals:
            - Target Pace: ${goals.targetPace ?: "Maintain comfortable effort"}
            - Distance Goal: ${goals.targetDistance ?: "Complete the planned distance"}
            
            Provide a brief (1-2 sentences) coaching tip that is:
            1. Encouraging and motivational
            2. Specific to current performance
            3. Actionable (pace adjustment, form cue, etc.)
            4. Appropriate for voice delivery during a run
            
            Keep it concise - this will be spoken aloud while running.
        """.trimIndent()
    }
}

// Data classes for context
@Serializable
data class UserFitnessContext(
    val experienceLevel: String,
    val goals: List<String>,
    val age: Int,
    val recentActivity: String,
    val trainingHistory: String
)

@Serializable
data class RunMetricsContext(
    val distance: Float,
    val duration: String,
    val currentPace: String,
    val averagePace: String,
    val heartRate: Int?
)

@Serializable
data class UserGoals(
    val targetPace: String?,
    val targetDistance: String?
)
