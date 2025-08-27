package com.runningcoach.v2.domain.model

/**
 * Data class representing real-time running metrics during a workout session.
 * Contains pace, distance, duration, and location information with formatted display methods.
 */
data class RunMetrics(
    val distance: Float = 0f, // in meters
    val duration: Long = 0L, // in seconds (changed from milliseconds for consistency)
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
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val currentLocation: LocationData? = null, // Current GPS coordinates
    val totalLocationPoints: Int = 0, // Total GPS points recorded
    val lastLocationTimestamp: Long? = null // Timestamp of last GPS update
) {
    fun getFormattedDistance(): String {
        return when {
            distance < 1000 -> "${distance.toInt()}m"
            else -> "${String.format("%.2f", distance / 1000)}km"
        }
    }
    
    fun getFormattedDuration(): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        
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
    
    /**
     * Gets formatted location coordinates if available
     */
    fun getFormattedLocation(): String {
        return currentLocation?.let { location ->
            "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
        } ?: "No GPS"
    }
    
    /**
     * Gets GPS signal strength indicator
     */
    fun getGPSAccuracy(): String {
        return currentLocation?.accuracy?.let { accuracy ->
            when {
                accuracy <= 5f -> "Excellent"
                accuracy <= 10f -> "Good"
                accuracy <= 20f -> "Fair"
                else -> "Poor"
            }
        } ?: "No Signal"
    }
    
    /**
     * Calculates pace from current speed
     */
    fun calculateCurrentPaceFromSpeed(): Float {
        return if (currentSpeed > 0) {
            // Convert m/s to minutes per km
            (1000.0 / currentSpeed / 60.0).toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Gets formatted elevation gain/loss
     */
    fun getFormattedElevation(): String {
        return if (elevationGain > 0 || elevationLoss > 0) {
            val gain = if (elevationGain > 0) "+${elevationGain.toInt()}m" else ""
            val loss = if (elevationLoss > 0) "-${elevationLoss.toInt()}m" else ""
            listOf(gain, loss).filter { it.isNotEmpty() }.joinToString(" ")
        } else {
            "0m"
        }
    }
    
    /**
     * Checks if GPS tracking is active and recent
     */
    fun hasActiveGPS(): Boolean {
        return currentLocation != null && 
               lastLocationTimestamp != null && 
               (System.currentTimeMillis() - lastLocationTimestamp!!) < 10_000 // Less than 10 seconds old
    }
    
    /**
     * Calculates estimated calories per minute based on current pace and distance
     */
    fun getEstimatedCaloriesPerMinute(): Float {
        return if (duration > 0) {
            val minutes = duration / 60f
            caloriesBurned / minutes
        } else {
            0f
        }
    }
}
