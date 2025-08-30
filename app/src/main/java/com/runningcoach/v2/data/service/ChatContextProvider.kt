package com.runningcoach.v2.data.service

import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.UserEntity
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.ZoneId

/**
 * Builds concise, text-friendly context for the AI chat agent
 * from the local database (profile + latest Google Fit summary).
 */
class ChatContextProvider(
    private val database: FITFOAIDatabase
) {
    private val userDao = database.userDao()
    private val fitDao = database.googleFitDailySummaryDao()
    private val runDao = database.runSessionDao()
    private val connectedAppDao = database.connectedAppDao()

    suspend fun buildRecentActivityString(user: UserEntity?): String {
        if (user == null) return "No user profile yet"

        val latest = fitDao.getLatestDailySummary(user.id)
        return if (latest != null) {
            val steps = latest.steps ?: 0
            val distanceMiles = (latest.distance ?: 0f) / 1609.344f
            val distanceStr = String.format("%.2f mi", distanceMiles)
            val calories = latest.calories ?: 0
            val hr = latest.avgHeartRate?.let { "avg HR ${it.toInt()} bpm" } ?: ""
            listOf(
                "Today: $steps steps",
                "Distance: $distanceStr",
                "Calories: $calories",
                hr
            ).filter { it.isNotBlank() }.joinToString(", ")
        } else {
            "No recent Google Fit data"
        }
    }

    suspend fun buildTrainingHistoryString(user: UserEntity?): String {
        if (user == null) return "No training history yet"
        val today = LocalDate.now()
        val start = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val week = fitDao.getDailySummariesInRange(user.id, start, end)
        if (week.isEmpty()) return "No activity in the last 7 days"

        val totalSteps = week.sumOf { (it.steps ?: 0) }
        val totalDistanceMiles = week.sumOf { ((it.distance ?: 0f).toDouble()) } / 1609.344
        val avgDailySteps = if (week.isNotEmpty()) totalSteps / week.size else 0
        return "Last 7 days: ${week.size} days, ${String.format("%.1f", totalDistanceMiles)} mi total, avg ${avgDailySteps} steps/day"
    }

    suspend fun buildProfileString(user: UserEntity?): String {
        if (user == null) return "No profile set"
        val goals = user.runningGoals.joinToString(", ").ifBlank { "General fitness" }
        val heightInches = user.height?.toFloat()?.div(2.54f)
        val heightStr = heightInches?.let {
            val totalInches = it.toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            "${feet}'${inches}\""
        } ?: "n/a"
        val weightLbs = user.weight?.let { it * 2.20462f }
        val weightStr = weightLbs?.let { String.format("%.1f lbs", it) } ?: "n/a"
        return "Level: ${user.experienceLevel}, Goals: $goals, Height: $heightStr, Weight: $weightStr"
    }

    suspend fun buildRecentRunsString(user: UserEntity?): String {
        if (user == null) return "No runs yet"
        val totalRuns = runDao.getTotalCompletedRuns(user.id)
        val totalDistanceMiles = (runDao.getTotalDistance(user.id) ?: 0f) / 1609.344f
        return "Runs: $totalRuns completed, Distance total: ${String.format("%.1f", totalDistanceMiles)} mi"
    }

    suspend fun buildConnectedAppsString(user: UserEntity?): String {
        if (user == null) return "No connections"
        val active = connectedAppDao.getActiveConnectedApps(user.id)
        return if (active.isEmpty()) "No connected apps" else active.joinToString(
            prefix = "Connected: ", separator = ", "
        ) { it.appName }
    }

    suspend fun buildFullContextBlock(): String {
        val user = getCurrentUser()
        val profile = buildProfileString(user)
        val recent = buildRecentActivityString(user)
        val weekly = buildTrainingHistoryString(user)
        val runs = buildRecentRunsString(user)
        val apps = buildConnectedAppsString(user)
        return listOf(
            "Profile: $profile",
            "Activity: $recent",
            "History: $weekly",
            "Runs: $runs",
            apps
        ).joinToString("\n")
    }

    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser().firstOrNull()
}
