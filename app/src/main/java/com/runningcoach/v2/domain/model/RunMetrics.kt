package com.runningcoach.v2.domain.model

/**
 * Data class representing real-time running metrics during a workout session.
 * Contains pace, distance, duration, and location information with formatted display methods.
 */
data class RunMetrics(
    val distance: Float = 0f, // in meters
    val duration: Long = 0L, // in seconds (changed from milliseconds for consistency)
    val averagePace: Float = 0f, // in minutes per kilometer (stored as metric, displayed as imperial)
    val currentPace: Float = 0f, // in minutes per kilometer (stored as metric, displayed as imperial)
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
        // Convert meters to miles (1 meter = 0.000621371 miles)
        val miles = distance * 0.000621371f
        return when {
            distance < 1609.34 -> "${String.format("%.2f", miles)} mi" // Less than 1 mile
            else -> "${String.format("%.2f", miles)} mi"
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
            // Convert pace from min/km to min/mile (1 km = 0.621371 miles)
            val pacePerMile = averagePace / 0.621371f
            val minutes = pacePerMile.toInt()
            val seconds = ((pacePerMile - minutes) * 60).toInt()
            String.format("%d:%02d /mi", minutes, seconds)
        } else {
            "--:-- /mi"
        }
    }
    
    fun getFormattedSpeed(): String {
        return if (currentSpeed > 0) {
            val mph = currentSpeed * 2.237 // convert m/s to mph (1 m/s = 2.237 mph)
            String.format("%.1f mph", mph)
        } else {
            "0.0 mph"
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
     * Note: This still returns pace in min/km for internal consistency,
     * but display formatting converts to min/mile
     */
    fun calculateCurrentPaceFromSpeed(): Float {
        return if (currentSpeed > 0) {
            // Convert m/s to minutes per km (internal storage remains metric)
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
            // Convert meters to feet (1 meter = 3.28084 feet)
            val gainFt = if (elevationGain > 0) "+${(elevationGain * 3.28084f).toInt()}ft" else ""
            val lossFt = if (elevationLoss > 0) "-${(elevationLoss * 3.28084f).toInt()}ft" else ""
            listOf(gainFt, lossFt).filter { it.isNotEmpty() }.joinToString(" ")
        } else {
            "0ft"
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
