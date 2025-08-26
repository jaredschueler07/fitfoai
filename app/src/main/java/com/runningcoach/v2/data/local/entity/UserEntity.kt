package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val age: Int,
    val height: Int, // in cm
    val weight: Float, // in kg
    val experienceLevel: String, // BEGINNER, INTERMEDIATE, ADVANCED
    val runningGoals: List<String>, // stored as JSON
    val selectedCoach: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
