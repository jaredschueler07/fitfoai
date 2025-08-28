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
 * Enhanced for background service compatibility and crash recovery.
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
    
    // Background service integration
    private val _serviceCompatibilityMode = MutableStateFlow(false)
    val serviceCompatibilityMode: StateFlow<Boolean> = _serviceCompatibilityMode.asStateFlow()
    
    private val _lastKnownAccuracy = MutableStateFlow<Float?>(null)
    val lastKnownAccuracy: StateFlow<Float?> = _lastKnownAccuracy.asStateFlow()
    
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
     * Enable service compatibility mode for background operation
     */
    fun enableServiceCompatibilityMode() {
        _serviceCompatibilityMode.value = true
    }
    
    /**
     * Disable service compatibility mode
     */
    fun disableServiceCompatibilityMode() {
        _serviceCompatibilityMode.value = false
    }
    
    /**
     * Check if location service is ready for background operation
     */
    fun isReadyForBackgroundOperation(): Boolean {
        return hasLocationPermission() && hasBackgroundLocationPermission()
    }
    
    /**
     * Get comprehensive location status for service management
     */
    fun getLocationStatus(): LocationServiceStatus {
        return LocationServiceStatus(
            hasLocationPermission = hasLocationPermission(),
            hasBackgroundPermission = hasBackgroundLocationPermission(),
            isTracking = _isTracking.value,
            currentAccuracy = _locationAccuracy.value,
            lastAccuracyValue = _lastKnownAccuracy.value,
            isServiceCompatible = _serviceCompatibilityMode.value,
            locationHistorySize = _locationHistory.value.size
        )
    }
    
    data class LocationServiceStatus(
        val hasLocationPermission: Boolean,
        val hasBackgroundPermission: Boolean,
        val isTracking: Boolean,
        val currentAccuracy: LocationAccuracy,
        val lastAccuracyValue: Float?,
        val isServiceCompatible: Boolean,
        val locationHistorySize: Int
    )
    
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
                    _lastKnownAccuracy.value = accuracy
                    
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
                    } else if (_serviceCompatibilityMode.value) {
                        // In service mode, accept lower accuracy to maintain tracking
                        val currentHistory = _locationHistory.value.toMutableList()
                        currentHistory.add(locationData)
                        _locationHistory.value = currentHistory
                        _locationAccuracy.value = LocationAccuracy.POOR
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
    
    /**
     * Enhanced location tracking with service-optimized settings
     */
    fun startServiceOptimizedTracking(
        intervalMillis: Long = 2000L, // Slightly longer for battery optimization
        fastestIntervalMillis: Long = 1000L, 
        smallestDisplacementMeters: Float = 2f // Slightly larger displacement
    ) {
        enableServiceCompatibilityMode()
        startLocationTracking(intervalMillis, fastestIntervalMillis, smallestDisplacementMeters)
    }
    
    /**
     * Get location updates optimized for background service operation
     */
    fun getServiceOptimizedLocationUpdates(): Flow<LocationData> {
        enableServiceCompatibilityMode()
        return getLocationUpdates(
            intervalMillis = 2000L,
            fastestIntervalMillis = 1000L,
            smallestDisplacementMeters = 2f
        )
    }
    
    /**
     * Restore location history from crash recovery
     */
    fun restoreLocationHistory(history: List<LocationData>) {
        _locationHistory.value = history
        if (history.isNotEmpty()) {
            _currentLocation.value = history.last()
        }
    }
    
    /**
     * Get metrics suitable for service state persistence
     */
    fun getServiceMetrics(): LocationServiceMetrics {
        val history = _locationHistory.value
        return LocationServiceMetrics(
            totalPoints = history.size,
            totalDistance = if (history.size >= 2) calculateDistance(history) else 0f,
            totalElevationGain = if (history.size >= 2) calculateElevationGain(history) else 0f,
            averageAccuracy = _lastKnownAccuracy.value ?: 0f,
            isTracking = _isTracking.value,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    data class LocationServiceMetrics(
        val totalPoints: Int,
        val totalDistance: Float,
        val totalElevationGain: Float,
        val averageAccuracy: Float,
        val isTracking: Boolean,
        val lastUpdateTime: Long
    )
}
