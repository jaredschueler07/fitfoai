package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.TrainingPlanEntity
import com.runningcoach.v2.data.service.LLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

data class PlanParams(
    val userId: Long,
    val goals: String,
    val fitnessLevel: String,
    val targetRace: String?,
    val raceDate: LocalDate
)

class GenerateTrainingPlanUseCase(
    private val db: FITFOAIDatabase,
    private val llm: LLMService
) {
    suspend fun generate(params: PlanParams): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val (baselineJson, weeklyAvgMiles, longestMiles, freqPerWeek) = computeBaseline(params.userId)

            val timeframe = buildTimeframeString(LocalDate.now(), params.raceDate)
            val promptGoals = buildCompositeGoals(params.goals, weeklyAvgMiles, longestMiles, freqPerWeek)

            val ai = llm.generateTrainingPlan(
                userGoals = promptGoals,
                currentFitnessLevel = params.fitnessLevel,
                targetRace = params.targetRace,
                timeframe = timeframe
            )

            if (ai.isFailure) return@withContext Result.failure(ai.exceptionOrNull()!!)
            val planText = ai.getOrThrow()

            val planEntity = TrainingPlanEntity(
                userId = params.userId,
                name = params.targetRace ?: "Personalized Plan",
                description = "AI-generated training plan",
                targetRace = params.targetRace,
                raceDate = params.raceDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000,
                planStartDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000,
                planEndDate = params.raceDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000,
                targetDistance = inferTargetDistanceMeters(params.targetRace),
                targetTime = null,
                modelVersion = "gemini-pro",
                generationPrompt = buildStoredPrompt(promptGoals, params.fitnessLevel, params.targetRace, timeframe),
                weeklyMileageProgression = "[]",
                baselineStats = baselineJson,
                planData = planText,
                isActive = true
            )

            val id = db.trainingPlanDao().insertTrainingPlan(planEntity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun computeBaseline(userId: Long): Baseline {
        val now = System.currentTimeMillis()
        val start = now - 90L * 24 * 60 * 60 * 1000
        val sessions = db.runSessionDao().getSessionsInDateRange(userId, start, now)

        var totalMiles = 0.0
        var longestMiles = 0.0
        var runDays = mutableSetOf<Long>()
        sessions.forEach { s ->
            val miles = s.distance / 1609.344
            totalMiles += miles
            if (miles > longestMiles) longestMiles = miles
            val day = (s.startTime / (24 * 60 * 60 * 1000))
            runDays.add(day)
        }
        val weeks = 12.86 // 90 days ≈ 12.86 weeks
        val weeklyAvg = if (weeks > 0) totalMiles / weeks else 0.0
        val freqPerWeek = if (weeks > 0) (runDays.size / weeks) else 0.0

        val json = JSONObject().apply {
            put("window_days", 90)
            put("total_miles", round1(totalMiles))
            put("weekly_avg_miles", round1(weeklyAvg))
            put("longest_run_miles", round1(longestMiles))
            put("estimated_runs_per_week", round1(freqPerWeek))
        }.toString()

        return Baseline(json, round1(weeklyAvg), round1(longestMiles), round1(freqPerWeek))
    }

    private fun buildCompositeGoals(goals: String, weeklyAvg: Double, longest: Double, freq: Double): String {
        return "$goals\n\nBaseline: ~${weeklyAvg} mi/week, longest ${longest} mi, ~${String.format("%.1f", freq)} runs/week over last 90 days."
    }

    private fun buildTimeframeString(start: LocalDate, end: LocalDate): String {
        return "from ${start} to ${end}"
    }

    private fun buildStoredPrompt(goals: String, level: String, race: String?, timeframe: String): String {
        return "Goals: $goals\nLevel: $level\nRace: ${race ?: "General"}\nTimeframe: $timeframe"
    }

    private fun inferTargetDistanceMeters(targetRace: String?): Float {
        return when (targetRace?.lowercase()) {
            "5k", "5km" -> 5000f
            "10k", "10km" -> 10000f
            "half marathon", "half" -> 21097f
            "marathon", "full marathon" -> 42195f
            else -> 5000f
        }
    }

    private fun round1(value: Double): Double {
        return kotlin.math.round(value * 10.0) / 10.0
    }

    private data class Baseline(
        val json: String,
        val weeklyAvg: Double,
        val longestMiles: Double,
        val freqPerWeek: Double
    )
}

