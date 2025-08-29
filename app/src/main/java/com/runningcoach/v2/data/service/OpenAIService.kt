package com.runningcoach.v2.data.service

import com.runningcoach.v2.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Minimal OpenAI Chat Completions client using Ktor, implementing LLMService.
 * Aligns with the project plan to use a GPT agent ("Fitness, Workout & Diet – PhD Coach").
 */
class OpenAIService(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : LLMService {

    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val model = BuildConfig.OPENAI_MODEL.ifBlank { "gpt-4o-mini" }
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @SerialName("temperature") val temperature: Double = 0.7
    )

    @Serializable
    private data class ChatResponse(val choices: List<Choice>) {
        @Serializable
        data class Choice(val message: ChatMessage)
    }

    override suspend fun generateFitnessAdvice(
        userMessage: String,
        userContext: UserFitnessContext
    ): Result<String> {
        val system = buildCoachSystemPrompt(userContext)
        return chat(listOf(
            ChatMessage("system", system),
            ChatMessage("user", userMessage)
        ))
    }

    override suspend fun generateTrainingPlan(
        userGoals: String,
        currentFitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ): Result<String> {
        val system = buildPlanSystemPrompt()
        val user = buildPlanUserPrompt(userGoals, currentFitnessLevel, targetRace, timeframe)
        return chat(listOf(
            ChatMessage("system", system),
            ChatMessage("user", user)
        ))
    }

    override suspend fun generateRunCoaching(
        metrics: RunMetricsContext,
        goals: UserGoals
    ): Result<String> {
        val system = buildRunCoachingSystemPrompt()
        val user = buildRunCoachingUserPrompt(metrics, goals)
        return chat(listOf(
            ChatMessage("system", system),
            ChatMessage("user", user)
        ))
    }

    private suspend fun chat(messages: List<ChatMessage>): Result<String> {
        return try {
            if (apiKey.isBlank()) return Result.failure(IllegalStateException("OPENAI_API_KEY is missing"))

            val request = ChatRequest(
                model = model,
                messages = messages
            )

            val response: HttpResponse = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("OpenAI error: ${'$'}{response.status}"))
            }
            val parsed: ChatResponse = json.decodeFromString(response.body())
            val text = parsed.choices.firstOrNull()?.message?.content
                ?: "I'm having trouble responding right now. Please try again."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildCoachSystemPrompt(ctx: UserFitnessContext): String = """
        You are "Fitness, Workout & Diet – PhD Coach" by Newgen PhD, a rigorous, evidence-based fitness coach.
        Profile to consider:
        - Experience Level: ${'$'}{ctx.experienceLevel}
        - Goals: ${'$'}{ctx.goals.joinToString(", ")}
        - Age: ${'$'}{ctx.age}
        - Recent Activity: ${'$'}{ctx.recentActivity}
        - Training History: ${'$'}{ctx.trainingHistory}

        Guidelines:
        - Be concise, actionable, and safe.
        - Explain briefly with science when useful.
        - Ask clarifying follow-ups when context is insufficient.
    """.trimIndent()

    private fun buildPlanSystemPrompt(): String = """
        Create a concise, structured training plan for a runner.
        Include: weekly structure, run types, progression, recovery, milestones, nutrition.
        Keep it scannable with short sections and bullet points.
    """.trimIndent()

    private fun buildPlanUserPrompt(
        goals: String,
        fitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ) = """
        Goals: ${'$'}goals
        Fitness level: ${'$'}fitnessLevel
        Target race: ${'$'}{targetRace ?: "General fitness"}
        Timeframe: ${'$'}timeframe
    """.trimIndent()

    private fun buildRunCoachingSystemPrompt(): String = """
        Provide a 1–2 sentence real-time coaching cue for a runner.
        Be supportive, specific, and voice-friendly.
    """.trimIndent()

    private fun buildRunCoachingUserPrompt(metrics: RunMetricsContext, goals: UserGoals) = """
        Distance: ${'$'}{metrics.distance} km
        Duration: ${'$'}{metrics.duration}
        Current pace: ${'$'}{metrics.currentPace} min/km
        Average pace: ${'$'}{metrics.averagePace} min/km
        Heart rate: ${'$'}{metrics.heartRate ?: "N/A"}
        Target pace: ${'$'}{goals.targetPace ?: "comfortable"}
        Target distance: ${'$'}{goals.targetDistance ?: "planned distance"}
    """.trimIndent()
}

