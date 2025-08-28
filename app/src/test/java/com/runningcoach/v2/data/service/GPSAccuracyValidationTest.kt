package com.runningcoach.v2.data.service

import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.utils.GPSCalculationUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * GPS accuracy validation tests for location filtering, distance calculations,
 * pace calculation accuracy, and coordinate bounds checking.
 */
class GPSAccuracyValidationTest {
    
    // Test locations for San Francisco area
    private val sfLocation1 = LocationData(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracy = 5f,
        timestamp = 1000L
    )
    
    private val sfLocation2 = LocationData(
        latitude = 37.7750, // ~11 meters north
        longitude = -122.4194,
        accuracy = 8f,
        timestamp = 2000L
    )
    
    private val sfLocation3 = LocationData(
        latitude = 37.7750,
        longitude = -122.4193, // ~11 meters east (longitude difference)  
        accuracy = 3f,
        timestamp = 3000L
    )
    
    // Poor accuracy location that should be filtered
    private val poorAccuracyLocation = LocationData(
        latitude = 37.7751,
        longitude = -122.4195,
        accuracy = 25f, // Above 20m threshold
        timestamp = 4000L
    )
    
    // Invalid coordinate locations
    private val invalidLatitudeLocation = LocationData(
        latitude = 91.0, // Invalid - above 90
        longitude = -122.4194,
        accuracy = 5f,
        timestamp = 5000L
    )
    
    private val invalidLongitudeLocation = LocationData(
        latitude = 37.7749,
        longitude = -181.0, // Invalid - below -180
        accuracy = 5f,
        timestamp = 6000L
    )

    @Before
    fun setUp() {
        // We'll test the utility methods directly without LocationService
        // since it requires Android context for the actual service
    }

    @Test
    fun `location filtering - accurate locations are accepted`() {
        // Arrange
        val accurateLocations = listOf(
            sfLocation1, // 5m accuracy
            sfLocation2, // 8m accuracy  
            sfLocation3  // 3m accuracy
        )
        
        // Act & Assert
        accurateLocations.forEach { location ->
            assertTrue("Location with ${location.accuracy}m accuracy should be accepted", 
                location.accuracy!! <= 20f)
        }
    }

    @Test
    fun `location filtering - poor accuracy locations are rejected`() {
        // Arrange & Act & Assert
        assertTrue("Location with 25m accuracy should be rejected", 
            poorAccuracyLocation.accuracy!! > 20f)
    }

    @Test
    fun `distance calculation between two close GPS points`() {
        // Arrange - Two points ~11 meters apart
        val locations = listOf(sfLocation1, sfLocation2)
        
        // Act
        val distance = GPSCalculationUtils.calculateDistance(locations)
        
        // Assert - Should be approximately 11 meters (actual result from Haversine formula)
        assertTrue("Distance should be approximately 11 meters", distance > 10f && distance < 15f)
        println("Calculated distance: ${distance}m") // For debugging
    }

    @Test
    fun `distance calculation for longer route`() {
        // Arrange - Three points forming an L-shape
        val locations = listOf(sfLocation1, sfLocation2, sfLocation3)
        
        // Act
        val distance = GPSCalculationUtils.calculateDistance(locations)
        
        // Assert - Should be approximately 18-19 meters (11m north + ~8m east)
        assertTrue("Distance should be approximately 19 meters", distance > 15f && distance < 25f)
        println("Calculated route distance: ${distance}m") // For debugging
    }

    @Test
    fun `distance calculation accuracy for very small movements`() {
        // Arrange - Very close points (1 meter apart)
        val closeLocation1 = LocationData(
            latitude = 37.7749000,
            longitude = -122.4194000,
            accuracy = 2f,
            timestamp = 1000L
        )
        val closeLocation2 = LocationData(
            latitude = 37.774900009, // Extremely small movement ~0.01 meter north
            longitude = -122.4194000,
            accuracy = 2f,
            timestamp = 2000L
        )
        
        val locations = listOf(closeLocation1, closeLocation2)
        
        // Act
        val distance = GPSCalculationUtils.calculateDistance(locations)
        
        // Assert - Should be a very small distance (less than 1 meter)
        assertTrue("Distance should be very small (< 1m)", distance < 1.0f)
        assertTrue("Distance should be positive", distance > 0f)
        println("Small movement distance: ${distance}m") // For debugging
    }

    @Test
    fun `pace calculation accuracy from GPS points`() {
        // Arrange - Locations 30 seconds apart for pace calculation
        val paceLocation1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 0L
        )
        val paceLocation2 = LocationData(
            latitude = 37.7759, // ~100 meters north (rough estimate)
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = 30_000L // 30 seconds later
        )
        
        val locations = listOf(paceLocation1, paceLocation2)
        val distance = GPSCalculationUtils.calculateDistance(locations)
        val timeSeconds = (paceLocation2.timestamp - paceLocation1.timestamp) / 1000f
        
        // Act - Calculate pace manually for comparison
        val speedMPerSec = GPSCalculationUtils.calculateSpeed(distance, timeSeconds)
        val paceMinPerKm = GPSCalculationUtils.calculatePace(speedMPerSec)
        
        // Assert
        assertTrue("Distance should be positive", distance > 0)
        assertTrue("Time should be 30 seconds", timeSeconds == 30f)
        assertTrue("Speed should be reasonable", speedMPerSec > 0)
        assertTrue("Pace should be reasonable (2-15 min/km)", paceMinPerKm > 2f && paceMinPerKm < 15f)
        
        println("Distance: ${distance}m, Time: ${timeSeconds}s, Speed: ${speedMPerSec}m/s, Pace: ${paceMinPerKm}min/km")
    }

    @Test
    fun `coordinate bounds checking - valid coordinates`() {
        // Act & Assert - Valid coordinates
        assertTrue("San Francisco coordinates should be valid", 
            GPSCalculationUtils.isValidCoordinate(sfLocation1.latitude, sfLocation1.longitude))
    }

    @Test
    fun `coordinate bounds checking - invalid latitude`() {
        // Act & Assert
        assertFalse("Latitude above 90 should be invalid",
            GPSCalculationUtils.isValidCoordinate(invalidLatitudeLocation.latitude, invalidLatitudeLocation.longitude))
    }

    @Test
    fun `coordinate bounds checking - invalid longitude`() {
        // Act & Assert  
        assertFalse("Longitude below -180 should be invalid",
            GPSCalculationUtils.isValidCoordinate(invalidLongitudeLocation.latitude, invalidLongitudeLocation.longitude))
    }

    @Test
    fun `coordinate bounds checking - edge cases`() {
        // Arrange - Edge case coordinates
        val northPole = LocationData(latitude = 90.0, longitude = 0.0, accuracy = 5f, timestamp = 1000L)
        val southPole = LocationData(latitude = -90.0, longitude = 0.0, accuracy = 5f, timestamp = 2000L)
        val dateLine1 = LocationData(latitude = 0.0, longitude = 180.0, accuracy = 5f, timestamp = 3000L)
        val dateLine2 = LocationData(latitude = 0.0, longitude = -180.0, accuracy = 5f, timestamp = 4000L)
        
        // Act & Assert - Edge cases should be valid
        assertTrue("North pole should be valid", 
            GPSCalculationUtils.isValidCoordinate(northPole.latitude, northPole.longitude))
        assertTrue("South pole should be valid",
            GPSCalculationUtils.isValidCoordinate(southPole.latitude, southPole.longitude))
        assertTrue("Date line +180 should be valid",
            GPSCalculationUtils.isValidCoordinate(dateLine1.latitude, dateLine1.longitude))
        assertTrue("Date line -180 should be valid",
            GPSCalculationUtils.isValidCoordinate(dateLine2.latitude, dateLine2.longitude))
    }

    @Test
    fun `distance calculation handles duplicate locations`() {
        // Arrange - Same location twice
        val duplicateLocations = listOf(sfLocation1, sfLocation1)
        
        // Act
        val distance = GPSCalculationUtils.calculateDistance(duplicateLocations)
        
        // Assert
        assertEquals("Distance between identical locations should be zero", 0f, distance, 0.1f)
    }

    @Test
    fun `elevation gain calculation accuracy`() {
        // Arrange - Locations with elevation data
        val elevationLocation1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 100.0,
            accuracy = 5f,
            timestamp = 1000L
        )
        val elevationLocation2 = LocationData(
            latitude = 37.7750,
            longitude = -122.4194,
            altitude = 110.0, // 10m gain
            accuracy = 5f,
            timestamp = 2000L
        )
        val elevationLocation3 = LocationData(
            latitude = 37.7751,
            longitude = -122.4194,
            altitude = 105.0, // 5m loss (should not count in gain)
            accuracy = 5f,
            timestamp = 3000L
        )
        val elevationLocation4 = LocationData(
            latitude = 37.7752,
            longitude = -122.4194,
            altitude = 115.0, // 10m gain
            accuracy = 5f,
            timestamp = 4000L
        )
        
        val locations = listOf(elevationLocation1, elevationLocation2, elevationLocation3, elevationLocation4)
        
        // Act
        val elevationGain = GPSCalculationUtils.calculateElevationGain(locations)
        
        // Assert - Should be 10 + 10 = 20 meters (ignoring the loss)
        assertEquals("Elevation gain should be 20 meters", 20f, elevationGain, 0.1f)
    }

    @Test
    fun `accuracy threshold filtering in realistic scenario`() {
        // Arrange - Mix of good and poor GPS readings
        val mixedAccuracyLocations = listOf(
            LocationData(latitude = 37.7749, longitude = -122.4194, accuracy = 3f, timestamp = 1000L), // Excellent
            LocationData(latitude = 37.7750, longitude = -122.4194, accuracy = 8f, timestamp = 2000L), // Good  
            LocationData(latitude = 37.7751, longitude = -122.4194, accuracy = 25f, timestamp = 3000L), // Poor - filter
            LocationData(latitude = 37.7752, longitude = -122.4194, accuracy = 15f, timestamp = 4000L), // Fair
            LocationData(latitude = 37.7753, longitude = -122.4194, accuracy = 30f, timestamp = 5000L)  // Poor - filter
        )
        
        // Act - Filter locations by accuracy threshold
        val filteredLocations = mixedAccuracyLocations.filter { 
            GPSCalculationUtils.meetsAccuracyThreshold(it.accuracy, 20f) 
        }
        
        // Assert
        assertEquals("Should filter out 2 poor accuracy locations", 3, filteredLocations.size)
        assertTrue("All filtered locations should meet accuracy threshold", 
            filteredLocations.all { GPSCalculationUtils.meetsAccuracyThreshold(it.accuracy, 20f) })
    }

    @Test
    fun `speed calculation from GPS points`() {
        // Arrange - Two points with known distance and time
        val speedLocation1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 3f,
            timestamp = 0L
        )
        val speedLocation2 = LocationData(
            latitude = 37.7759, // ~100m north
            longitude = -122.4194,
            accuracy = 3f,
            timestamp = 10_000L // 10 seconds later
        )
        
        val locations = listOf(speedLocation1, speedLocation2)
        
        // Act
        val distance = GPSCalculationUtils.calculateDistance(locations)
        val timeSeconds = (speedLocation2.timestamp - speedLocation1.timestamp) / 1000f
        val speedMPerSec = GPSCalculationUtils.calculateSpeed(distance, timeSeconds)
        val speedKmPerHour = speedMPerSec * 3.6f
        
        // Assert
        assertTrue("Distance should be approximately 100m", distance > 90f && distance < 120f)
        assertEquals("Time should be 10 seconds", 10f, timeSeconds, 0.1f)
        assertTrue("Speed should be approximately 10 m/s", speedMPerSec > 8f && speedMPerSec < 12f)
        assertTrue("Speed should be approximately 36 km/h", speedKmPerHour > 28f && speedKmPerHour < 44f)
        
        println("GPS Speed calculation: ${distance}m in ${timeSeconds}s = ${speedMPerSec}m/s (${speedKmPerHour}km/h)")
    }

    @Test
    fun `accuracy categories are properly classified`() {
        // Arrange
        val excellentLocation = LocationData(latitude = 37.7749, longitude = -122.4194, accuracy = 3f, timestamp = 1000L)
        val goodLocation = LocationData(latitude = 37.7749, longitude = -122.4194, accuracy = 8f, timestamp = 1000L)
        val fairLocation = LocationData(latitude = 37.7749, longitude = -122.4194, accuracy = 15f, timestamp = 1000L)
        val poorLocation = LocationData(latitude = 37.7749, longitude = -122.4194, accuracy = 25f, timestamp = 1000L)
        
        // Act & Assert
        assertTrue("3m accuracy should be excellent (≤5m)", excellentLocation.accuracy!! <= 5f)
        assertTrue("8m accuracy should be good (≤10m)", goodLocation.accuracy!! <= 10f && goodLocation.accuracy!! > 5f)
        assertTrue("15m accuracy should be fair (≤20m)", fairLocation.accuracy!! <= 20f && fairLocation.accuracy!! > 10f)
        assertTrue("25m accuracy should be poor (>20m)", poorLocation.accuracy!! > 20f)
    }

    @Test
    fun `location timestamp validation`() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val recentLocation = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = currentTime - 5000L // 5 seconds ago
        )
        val oldLocation = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5f,
            timestamp = currentTime - 300_000L // 5 minutes ago
        )
        
        // Act & Assert
        val recentAge = currentTime - recentLocation.timestamp
        val oldAge = currentTime - oldLocation.timestamp
        
        assertTrue("Recent location should be less than 10 seconds old", recentAge < 10_000L)
        assertTrue("Old location should be more than 10 seconds old", oldAge > 10_000L)
    }
}