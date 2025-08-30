package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing individual workout sessions within training plans
 * Links training plans to actual run sessions and tracks workout completion
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RunSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["actualRunSessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["scheduledDate"]),
        Index(value = ["actualRunSessionId"]),
        Index(value = ["status"]),
        Index(value = ["planId", "scheduledDate"]) // Composite index for efficient queries
    ]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Training plan association
    val planId: Long,
    
    // Workout scheduling
    val scheduledDate: Long, // Unix timestamp for the planned workout date
    val type: WorkoutType,
    
    // Target workout parameters
    val targetDistance: Float? = null, // in meters
    val targetDuration: Long? = null, // in milliseconds
    val targetPace: Float? = null, // minutes per kilometer
    val intensity: Int, // 1-10 scale where 1 = very easy, 10 = maximum effort
    
    // Actual completion tracking
    val actualRunSessionId: Long? = null, // FK to RunSessionEntity when completed
    val status: WorkoutStatus = WorkoutStatus.PENDING,
    
    // Additional information
    val notes: String? = null, // Coach instructions or user notes
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)