package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * HealthConnectDailySummaryEntity - Health Connect fitness data daily summary
 * 
 * Stores aggregated daily fitness data from Health Connect, similar to GoogleFitDailySummaryEntity
 * but specifically for Health Connect data sources.
 */
@Entity(
    tableName = "health_connect_daily_summaries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("date"),
        Index(value = ["userId", "date"], unique = true)
    ]
)
data class HealthConnectDailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: Long,
    
    /**
     * Date for this summary as milliseconds since epoch (start of day in local timezone)
     */
    val date: Long,
    
    /**
     * Total steps for the day
     */
    val steps: Int = 0,
    
    /**
     * Total distance in meters
     */
    val distance: Float = 0f,
    
    /**
     * Total calories burned
     */
    val calories: Int = 0,
    
    /**
     * Active minutes for the day
     */
    val activeMinutes: Int = 0,
    
    /**
     * Average heart rate (BPM) if available
     */
    val avgHeartRate: Float? = null,
    
    /**
     * Maximum heart rate (BPM) if available
     */
    val maxHeartRate: Float? = null,
    
    /**
     * Minimum heart rate (BPM) if available
     */
    val minHeartRate: Float? = null,
    
    /**
     * Total floors climbed if available
     */
    val floorsClimbed: Int? = null,
    
    /**
     * Total exercise time in minutes
     */
    val exerciseMinutes: Int = 0,
    
    /**
     * Sleep duration in minutes if available
     */
    val sleepMinutes: Int? = null,
    
    /**
     * Average sleep quality score (0-100) if available
     */
    val sleepQuality: Int? = null,
    
    /**
     * Water intake in milliliters if available
     */
    val waterIntakeMl: Int? = null,
    
    /**
     * Weight in kg if recorded on this day
     */
    val weight: Float? = null,
    
    /**
     * Last successful sync timestamp
     */
    val lastSynced: Long,
    
    /**
     * Data source identifier for tracking
     */
    val dataSource: String = "HEALTH_CONNECT",
    
    /**
     * Health Connect record IDs for this summary (JSON array)
     */
    val recordIds: String? = null,
    
    /**
     * Sync status for this summary
     */
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    
    /**
     * Error message if sync failed
     */
    val syncError: String? = null
) {
    
    enum class SyncStatus {
        PENDING,
        SYNCING,
        SYNCED,
        FAILED,
        PARTIAL
    }
    
    /**
     * Check if this summary has meaningful data
     */
    fun hasSignificantData(): Boolean {
        return steps > 100 || distance > 0.1f || calories > 50 || 
               exerciseMinutes > 0 || avgHeartRate != null
    }
    
    /**
     * Calculate estimated BMR contribution to calories
     */
    fun estimatedBmrCalories(): Int {
        // Basic BMR estimate: ~1800 calories per day average
        return (1800 * activeMinutes / (24 * 60)).coerceAtMost(calories)
    }
    
    /**
     * Get active calories (excluding BMR)
     */
    fun getActiveCalories(): Int {
        return (calories - estimatedBmrCalories()).coerceAtLeast(0)
    }
    
    /**
     * Check if heart rate data is available
     */
    fun hasHeartRateData(): Boolean {
        return avgHeartRate != null && avgHeartRate > 0
    }
    
    /**
     * Get heart rate range if available
     */
    fun getHeartRateRange(): Pair<Float, Float>? {
        return if (minHeartRate != null && maxHeartRate != null) {
            Pair(minHeartRate, maxHeartRate)
        } else null
    }
    
    /**
     * Calculate activity level based on steps and active minutes
     */
    fun getActivityLevel(): ActivityLevel {
        return when {
            steps >= 12000 || activeMinutes >= 60 -> ActivityLevel.VERY_ACTIVE
            steps >= 8000 || activeMinutes >= 30 -> ActivityLevel.ACTIVE
            steps >= 5000 || activeMinutes >= 15 -> ActivityLevel.MODERATELY_ACTIVE
            steps >= 2000 || activeMinutes >= 5 -> ActivityLevel.LIGHTLY_ACTIVE
            else -> ActivityLevel.SEDENTARY
        }
    }
    
    enum class ActivityLevel {
        SEDENTARY,
        LIGHTLY_ACTIVE,
        MODERATELY_ACTIVE,
        ACTIVE,
        VERY_ACTIVE
    }
}