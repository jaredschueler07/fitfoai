package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enhanced entity for storing AI-generated training plans
 * Supports AI model versioning, baseline tracking, and detailed race preparation
 */
@Entity(
    tableName = "training_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["raceDate"]),
        Index(value = ["isActive"]),
        Index(value = ["planStartDate", "planEndDate"])
    ]
)
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String,
    
    // Enhanced race goal information
    val targetRace: String? = null, // Keep for backwards compatibility
    val raceDate: Long, // Unix timestamp for race date
    val planStartDate: Long, // When the training plan begins
    val planEndDate: Long, // When the training plan ends
    val targetDistance: Float, // Race distance in meters
    val targetTime: Long? = null, // Target finish time in milliseconds
    
    // AI generation metadata
    val modelVersion: String, // AI model version used for generation
    val generationPrompt: String, // Full prompt used to generate the plan
    val weeklyMileageProgression: String, // JSON array of weekly mileage targets
    val baselineStats: String, // JSON with user's last 90 days fitness summary
    
    // Legacy fields (keep for backwards compatibility)
    val targetDate: Long? = null, // Deprecated - use raceDate instead
    val planData: String, // JSON string containing the full plan structure
    
    // Status
    val isActive: Boolean = true,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate current week number (1-based) based on plan start date
     */
    fun getCurrentWeek(currentTime: Long = System.currentTimeMillis()): Int {
        if (currentTime < planStartDate) return 0
        if (currentTime > planEndDate) return getTotalWeeks()
        
        val daysElapsed = (currentTime - planStartDate) / (24 * 60 * 60 * 1000)
        return (daysElapsed / 7).toInt() + 1
    }
    
    /**
     * Calculate total number of weeks in the training plan
     */
    fun getTotalWeeks(): Int {
        val totalDays = (planEndDate - planStartDate) / (24 * 60 * 60 * 1000)
        return (totalDays / 7).toInt() + if (totalDays % 7 > 0) 1 else 0
    }
    
    /**
     * Calculate progress percentage (0.0 to 1.0)
     */
    fun getProgress(currentTime: Long = System.currentTimeMillis()): Float {
        if (currentTime <= planStartDate) return 0.0f
        if (currentTime >= planEndDate) return 1.0f
        
        val totalDuration = planEndDate - planStartDate
        val elapsed = currentTime - planStartDate
        return elapsed.toFloat() / totalDuration.toFloat()
    }
    
    /**
     * Check if the training plan is currently active (within date range)
     */
    fun isCurrentlyActive(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime in planStartDate..planEndDate && isActive
    }
}
