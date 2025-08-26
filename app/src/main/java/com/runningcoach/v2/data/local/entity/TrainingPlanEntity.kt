package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String,
    val targetRace: String? = null,
    val targetDate: Long? = null,
    val planData: String, // JSON string containing the full plan
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
