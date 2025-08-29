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
            val distanceKm = (latest.distance ?: 0f) / 1000f
            val distanceStr = String.format("%.2f km", distanceKm)
            val calories = latest.calories ?: 0
            val hr = latest.averageHeartRate?.let { "avg HR ${it} bpm" } ?: ""
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
        val totalDistanceKm = week.sumOf { ((it.distance ?: 0f).toDouble()) } / 1000.0
        val avgDailySteps = if (week.isNotEmpty()) totalSteps / week.size else 0
        return "Last 7 days: ${week.size} days, ${String.format("%.1f", totalDistanceKm)} km total, avg ${avgDailySteps} steps/day"
    }

    suspend fun buildProfileString(user: UserEntity?): String {
        if (user == null) return "No profile set"
        val goals = user.runningGoals.joinToString(", ").ifBlank { "General fitness" }
        val height = user.height?.let { "${it} cm" } ?: "n/a"
        val weight = user.weight?.let { String.format("%.1f kg", it) } ?: "n/a"
        return "Level: ${user.experienceLevel}, Goals: $goals, Height: $height, Weight: $weight"
    }

    suspend fun buildRecentRunsString(user: UserEntity?): String {
        if (user == null) return "No runs yet"
        val totalRuns = runDao.getTotalCompletedRuns(user.id)
        val totalDistance = (runDao.getTotalDistance(user.id) ?: 0f) / 1000f
        return "Runs: $totalRuns completed, Distance total: ${String.format("%.1f", totalDistance)} km"
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
