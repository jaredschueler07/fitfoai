package com.runningcoach.v2.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.runningcoach.v2.domain.model.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Location service using Google Play Services FusedLocationProvider for high-accuracy GPS tracking.
 * Provides location updates via Kotlin Flow with configurable intervals and accuracy.
 */
class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    val locationHistory: StateFlow<List<LocationData>> = _locationHistory.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _locationAccuracy = MutableStateFlow(LocationAccuracy.UNKNOWN)
    val locationAccuracy: StateFlow<LocationAccuracy> = _locationAccuracy.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    
    enum class LocationAccuracy {
        UNKNOWN, POOR, GOOD, EXCELLENT
    }
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasBackgroundLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Starts location tracking with configurable intervals.
     * Uses FusedLocationProvider for improved accuracy and battery efficiency.
     */
    fun startLocationTracking(
        intervalMillis: Long = 1000L, // 1 second default
        fastestIntervalMillis: Long = 500L, // 0.5 second fastest
        smallestDisplacementMeters: Float = 1f // 1 meter minimum displacement
    ) {
        if (!hasLocationPermission()) {
            return
        }
        
        if (_isTracking.value) {
            return
        }
        
        _isTracking.value = true
        _locationHistory.value = emptyList()
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(fastestIntervalMillis)
            .setMinUpdateDistanceMeters(smallestDisplacementMeters)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(2000L)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                locationResult.locations.forEach { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestamp = location.time
                    )
                    
                    _currentLocation.value = locationData
                    
                    // Add to history if accuracy is good enough
                    val accuracy = location.accuracy
                    if (accuracy <= 20f) { // 20 meters or better
                        val currentHistory = _locationHistory.value.toMutableList()
                        currentHistory.add(locationData)
                        _locationHistory.value = currentHistory
                        
                        // Update accuracy status
                        _locationAccuracy.value = when {
                            accuracy <= 5f -> LocationAccuracy.EXCELLENT
                            accuracy <= 10f -> LocationAccuracy.GOOD
                            else -> LocationAccuracy.POOR
                        }
                    }
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                if (!locationAvailability.isLocationAvailable) {
                    _locationAccuracy.value = LocationAccuracy.UNKNOWN
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            _isTracking.value = false
        }
    }
    
    /**
     * Stops location tracking and cleans up resources
     */
    fun stopLocationTracking() {
        if (!_isTracking.value) {
            return
        }
        
        _isTracking.value = false
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }
    
    /**
     * Gets the last known location using FusedLocationProvider
     * @return LocationData if available, null otherwise
     */
    suspend fun getLastKnownLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        val locationData = location?.let { loc ->
                            LocationData(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = if (loc.hasAltitude()) loc.altitude else null,
                                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                                speed = if (loc.hasSpeed()) loc.speed else null,
                                bearing = if (loc.hasBearing()) loc.bearing else null,
                                timestamp = loc.time
                            )
                        }
                        continuation.resume(locationData)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            null
        }
    }
    
    /**
     * Creates a Flow that emits location updates
     * @param intervalMillis Location update interval
     * @param fastestIntervalMillis Fastest update interval
     * @param smallestDisplacementMeters Minimum displacement for updates
     * @return Flow of LocationData
     */
    fun getLocationUpdates(
        intervalMillis: Long = 1000L,
        fastestIntervalMillis: Long = 500L,
        smallestDisplacementMeters: Float = 1f
    ): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(fastestIntervalMillis)
            .setMinUpdateDistanceMeters(smallestDisplacementMeters)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(2000L)
            .build()
        
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestamp = location.time
                    )
                    trySend(locationData)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    fun calculateDistance(locations: List<LocationData>): Float {
        if (locations.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 0 until locations.size - 1) {
            val location1 = locations[i]
            val location2 = locations[i + 1]
            
            val results = FloatArray(1)
            Location.distanceBetween(
                location1.latitude, location1.longitude,
                location2.latitude, location2.longitude,
                results
            )
            totalDistance += results[0]
        }
        
        return totalDistance
    }
    
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
    
    fun clearLocationHistory() {
        _locationHistory.value = emptyList()
    }
}
