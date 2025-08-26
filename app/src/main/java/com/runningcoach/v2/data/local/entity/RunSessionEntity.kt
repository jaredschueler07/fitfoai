package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long? = null, // in milliseconds
    val distance: Float? = null, // in meters
    val averagePace: Float? = null, // in minutes per kilometer
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val caloriesBurned: Int? = null,
    val route: String? = null, // JSON string of GPS coordinates
    val weather: String? = null, // JSON string of weather data
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
