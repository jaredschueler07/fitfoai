package com.runningcoach.v2.domain.model

data class RunMetrics(
    val distance: Float = 0f, // in meters
    val duration: Long = 0L, // in milliseconds
    val averagePace: Float = 0f, // in minutes per kilometer
    val currentPace: Float = 0f, // in minutes per kilometer
    val averageSpeed: Float = 0f, // in m/s
    val currentSpeed: Float = 0f, // in m/s
    val caloriesBurned: Int = 0,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val currentHeartRate: Int? = null,
    val elevationGain: Float = 0f, // in meters
    val elevationLoss: Float = 0f, // in meters
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    fun getFormattedDistance(): String {
        return when {
            distance < 1000 -> "${distance.toInt()}m"
            else -> "${String.format("%.2f", distance / 1000)}km"
        }
    }
    
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getFormattedPace(): String {
        return if (averagePace > 0) {
            val minutes = averagePace.toInt()
            val seconds = ((averagePace - minutes) * 60).toInt()
            String.format("%d:%02d /km", minutes, seconds)
        } else {
            "--:-- /km"
        }
    }
    
    fun getFormattedSpeed(): String {
        return if (currentSpeed > 0) {
            val kmh = currentSpeed * 3.6 // convert m/s to km/h
            String.format("%.1f km/h", kmh)
        } else {
            "0.0 km/h"
        }
    }
}
