package com.runningcoach.v2.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.runningcoach.v2.domain.model.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class LocationService(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    val locationHistory: StateFlow<List<LocationData>> = _locationHistory.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _locationAccuracy = MutableStateFlow(LocationAccuracy.UNKNOWN)
    val locationAccuracy: StateFlow<LocationAccuracy> = _locationAccuracy.asStateFlow()
    
    private var locationListener: LocationListener? = null
    
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
    
    fun startLocationTracking() {
        if (!hasLocationPermission()) {
            return
        }
        
        if (_isTracking.value) {
            return
        }
        
        _isTracking.value = true
        _locationHistory.value = emptyList()
        
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    timestamp = location.time
                )
                
                _currentLocation.value = locationData
                
                // Add to history if accuracy is good enough
                if (location.accuracy <= 20f) { // 20 meters or better
                    val currentHistory = _locationHistory.value.toMutableList()
                    currentHistory.add(locationData)
                    _locationHistory.value = currentHistory
                    
                    // Update accuracy status
                    _locationAccuracy.value = when {
                        location.accuracy <= 5f -> LocationAccuracy.EXCELLENT
                        location.accuracy <= 10f -> LocationAccuracy.GOOD
                        else -> LocationAccuracy.POOR
                    }
                }
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            // Request location updates with high accuracy
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second
                1f, // 1 meter
                locationListener!!
            )
            
            // Also request from network provider as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L, // 2 seconds
                    5f, // 5 meters
                    locationListener!!
                )
            }
        } catch (e: SecurityException) {
            _isTracking.value = false
        }
    }
    
    fun stopLocationTracking() {
        if (!_isTracking.value) {
            return
        }
        
        _isTracking.value = false
        locationListener?.let { listener ->
            locationManager.removeUpdates(listener)
        }
        locationListener = null
    }
    
    fun getLastKnownLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.accuracy <= networkLocation.accuracy) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
            
            bestLocation?.let { location ->
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    timestamp = location.time
                )
            }
        } catch (e: SecurityException) {
            null
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
