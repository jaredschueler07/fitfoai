package com.runningcoach.v2.utils

import com.runningcoach.v2.domain.model.LocationData
import kotlin.math.*

/**
 * Utility class for GPS calculations that can be tested without Android context.
 * Uses the Haversine formula for distance calculations.
 */
object GPSCalculationUtils {
    
    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth radius in meters
    
    /**
     * Calculates distance between two GPS coordinates using Haversine formula
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees  
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))
        
        return (EARTH_RADIUS_METERS * c).toFloat()
    }
    
    /**
     * Calculates total distance for a list of location points
     * @param locations List of location data points
     * @return Total distance in meters
     */
    fun calculateDistance(locations: List<LocationData>): Float {
        if (locations.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 0 until locations.size - 1) {
            val location1 = locations[i]
            val location2 = locations[i + 1]
            
            totalDistance += calculateDistance(
                location1.latitude, location1.longitude,
                location2.latitude, location2.longitude
            )
        }
        
        return totalDistance
    }
    
    /**
     * Calculates elevation gain from a list of locations
     * @param locations List of location data points with altitude
     * @return Total elevation gain in meters (only positive changes)
     */
    fun calculateElevationGain(locations: List<LocationData>): Float {
        if (locations.size < 2) return 0f
        
        var totalGain = 0f
        for (i in 0 until locations.size - 1) {
            val current = locations[i].altitude ?: 0.0
            val next = locations[i + 1].altitude ?: 0.0
            
            val elevationChange = next - current
            if (elevationChange > 0) {
                totalGain += elevationChange.toFloat()
            }
        }
        
        return totalGain
    }
    
    /**
     * Validates GPS coordinate bounds
     * @param latitude Latitude to validate
     * @param longitude Longitude to validate
     * @return True if coordinates are valid, false otherwise
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude >= -90.0 && latitude <= 90.0 && 
               longitude >= -180.0 && longitude <= 180.0
    }
    
    /**
     * Checks if location accuracy meets quality threshold
     * @param accuracy Accuracy in meters
     * @param threshold Maximum acceptable accuracy in meters
     * @return True if accuracy meets threshold
     */
    fun meetsAccuracyThreshold(accuracy: Float?, threshold: Float = 20f): Boolean {
        return accuracy != null && accuracy <= threshold
    }
    
    /**
     * Calculates speed from distance and time
     * @param distanceMeters Distance in meters
     * @param timeSeconds Time in seconds
     * @return Speed in meters per second
     */
    fun calculateSpeed(distanceMeters: Float, timeSeconds: Float): Float {
        return if (timeSeconds > 0) distanceMeters / timeSeconds else 0f
    }
    
    /**
     * Calculates pace from speed
     * @param speedMeterPerSec Speed in meters per second
     * @return Pace in minutes per kilometer
     */
    fun calculatePace(speedMeterPerSec: Float): Float {
        return if (speedMeterPerSec > 0) (1000.0f / speedMeterPerSec) / 60.0f else 0f
    }
}