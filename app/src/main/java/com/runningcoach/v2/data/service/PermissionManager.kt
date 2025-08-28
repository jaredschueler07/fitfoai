package com.runningcoach.v2.data.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PermissionManager handles all location-related permissions for Android 12+ compatibility.
 * Manages the complex permission flow for location access including background location permissions.
 * 
 * Features:
 * - Progressive permission requests (Fine Location → Background Location)
 * - Android 12+ specific permission handling
 * - Rationale dialogs and user education
 * - Graceful permission denial handling
 * - Battery optimization bypass guidance
 */
class PermissionManager(
    private val activity: ComponentActivity
) {
    
    companion object {
        // Required permissions for location tracking
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Background location permission (Android 10+)
        private const val BACKGROUND_LOCATION_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        
        // Activity recognition for fitness data (Android 10+)
        private const val ACTIVITY_RECOGNITION_PERMISSION = Manifest.permission.ACTIVITY_RECOGNITION
    }
    
    // Permission status tracking
    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()
    
    // Permission request callbacks
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var onBackgroundPermissionResult: ((Boolean) -> Unit)? = null
    
    // Permission request launchers
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        updatePermissionStatus()
        onPermissionResult?.invoke(granted)
        
        if (granted) {
            // If basic location permissions granted, proceed to background if needed
            requestBackgroundLocationIfNeeded()
        }
    }
    
    private val backgroundLocationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updatePermissionStatus()
        onBackgroundPermissionResult?.invoke(granted)
    }
    
    private val activityRecognitionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updatePermissionStatus()
    }
    
    data class PermissionStatus(
        val hasLocationPermission: Boolean = false,
        val hasBackgroundLocationPermission: Boolean = false,
        val hasActivityRecognitionPermission: Boolean = false,
        val canRequestBackgroundLocation: Boolean = false,
        val needsRationale: Boolean = false,
        val isBackgroundLocationSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    )
    
    enum class PermissionResult {
        GRANTED,
        DENIED,
        DENIED_PERMANENTLY,
        REQUIRES_RATIONALE
    }
    
    init {
        updatePermissionStatus()
    }
    
    /**
     * Check if basic location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity, 
                BACKGROUND_LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Check if activity recognition permission is granted (Android 10+)
     */
    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity, 
                ACTIVITY_RECOGNITION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Request basic location permissions with educational rationale
     */
    fun requestLocationPermissions(callback: (Boolean) -> Unit) {
        onPermissionResult = callback
        
        // Check if we need to show rationale
        if (shouldShowLocationRationale()) {
            showLocationPermissionRationale {
                locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
            }
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }
    
    /**
     * Request background location permission (must be called after basic location permissions)
     */
    fun requestBackgroundLocationPermission(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            callback(true) // Not required for older versions
            return
        }
        
        if (!hasLocationPermission()) {
            callback(false) // Basic location permission required first
            return
        }
        
        onBackgroundPermissionResult = callback
        
        // For Android 11+, show educational dialog first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showBackgroundLocationRationale {
                backgroundLocationLauncher.launch(BACKGROUND_LOCATION_PERMISSION)
            }
        } else {
            backgroundLocationLauncher.launch(BACKGROUND_LOCATION_PERMISSION)
        }
    }
    
    /**
     * Request activity recognition permission for fitness data
     */
    fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityRecognitionLauncher.launch(ACTIVITY_RECOGNITION_PERMISSION)
        }
    }
    
    /**
     * Request all permissions in proper order
     */
    fun requestAllPermissions(callback: (Boolean) -> Unit) {
        requestLocationPermissions { locationGranted ->
            if (locationGranted) {
                requestBackgroundLocationPermission { backgroundGranted ->
                    // Request activity recognition regardless of background permission result
                    requestActivityRecognitionPermission()
                    
                    // Consider success if we have basic location permissions
                    // Background location is nice-to-have for full functionality
                    callback(true)
                }
            } else {
                callback(false)
            }
        }
    }
    
    /**
     * Check permission status and provide detailed result
     */
    fun checkPermissionStatus(): PermissionResult {
        return when {
            hasLocationPermission() -> PermissionResult.GRANTED
            shouldShowLocationRationale() -> PermissionResult.REQUIRES_RATIONALE
            hasRequestedPermissionBefore() -> PermissionResult.DENIED_PERMANENTLY
            else -> PermissionResult.DENIED
        }
    }
    
    /**
     * Guide user to app settings for manual permission grant
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
    
    /**
     * Guide user to battery optimization settings
     */
    fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        activity.startActivity(intent)
    }
    
    /**
     * Check if battery optimization is disabled for the app
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true // Not applicable for older versions
        }
    }
    
    // Private helper methods
    
    private fun updatePermissionStatus() {
        val status = PermissionStatus(
            hasLocationPermission = hasLocationPermission(),
            hasBackgroundLocationPermission = hasBackgroundLocationPermission(),
            hasActivityRecognitionPermission = hasActivityRecognitionPermission(),
            canRequestBackgroundLocation = hasLocationPermission(),
            needsRationale = shouldShowLocationRationale()
        )
        _permissionStatus.value = status
    }
    
    private fun shouldShowLocationRationale(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    private fun hasRequestedPermissionBefore(): Boolean {
        // This is a simplified check - in a real app you might want to store this in SharedPreferences
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission()) {
            // Auto-request background location after basic location is granted
            onBackgroundPermissionResult = { /* Optional callback */ }
            backgroundLocationLauncher.launch(BACKGROUND_LOCATION_PERMISSION)
        }
    }
    
    // Educational dialogs for permission rationales
    
    private fun showLocationPermissionRationale(onContinue: () -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Location Permission Required")
            .setMessage(
                "FITFOAI needs location access to:\n\n" +
                "• Track your running route with GPS\n" +
                "• Calculate distance and pace\n" +
                "• Provide real-time coaching\n" +
                "• Save your running history\n\n" +
                "Your location data stays private and is only used for fitness tracking."
            )
            .setPositiveButton("Grant Permission") { _, _ -> onContinue() }
            .setNegativeButton("Cancel") { _, _ -> 
                onPermissionResult?.invoke(false)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showBackgroundLocationRationale(onContinue: () -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Background Location Access")
            .setMessage(
                "For the best experience, FITFOAI needs background location access to:\n\n" +
                "• Continue GPS tracking when you switch apps\n" +
                "• Provide coaching even when screen is off\n" +
                "• Ensure your run data is always saved\n\n" +
                "Please select 'Allow all the time' on the next screen.\n\n" +
                "Note: This only works during active run sessions."
            )
            .setPositiveButton("Continue") { _, _ -> onContinue() }
            .setNegativeButton("Skip") { _, _ -> 
                onBackgroundPermissionResult?.invoke(false)
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show educational dialog about battery optimization
     */
    fun showBatteryOptimizationDialog() {
        if (isBatteryOptimizationDisabled()) return
        
        android.app.AlertDialog.Builder(activity)
            .setTitle("Optimize Battery Settings")
            .setMessage(
                "To ensure reliable GPS tracking during your runs:\n\n" +
                "1. Disable battery optimization for FITFOAI\n" +
                "2. This prevents Android from stopping GPS tracking\n" +
                "3. Your runs will be tracked more accurately\n\n" +
                "This setting helps maintain continuous location updates."
            )
            .setPositiveButton("Open Settings") { _, _ -> 
                openBatteryOptimizationSettings()
            }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }
    
    /**
     * Comprehensive permission check with user guidance
     */
    fun checkAndGuidePermissions(callback: (Boolean) -> Unit) {
        when (checkPermissionStatus()) {
            PermissionResult.GRANTED -> {
                // Check for background location and battery optimization
                if (!hasBackgroundLocationPermission()) {
                    showBackgroundLocationEducation { backgroundGranted ->
                        if (!backgroundGranted) {
                            showBatteryOptimizationDialog()
                        }
                        callback(true) // Basic permissions sufficient
                    }
                } else {
                    showBatteryOptimizationDialog()
                    callback(true)
                }
            }
            PermissionResult.REQUIRES_RATIONALE -> {
                requestLocationPermissions(callback)
            }
            PermissionResult.DENIED_PERMANENTLY -> {
                showPermissionDeniedDialog(callback)
            }
            PermissionResult.DENIED -> {
                requestLocationPermissions(callback)
            }
        }
    }
    
    private fun showBackgroundLocationEducation(callback: (Boolean) -> Unit) {
        requestBackgroundLocationPermission(callback)
    }
    
    private fun showPermissionDeniedDialog(callback: (Boolean) -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage(
                "Location permission is required for GPS tracking. " +
                "Please enable it in app settings to use FITFOAI's running features."
            )
            .setPositiveButton("Open Settings") { _, _ -> 
                openAppSettings()
                callback(false)
            }
            .setNegativeButton("Cancel") { _, _ -> 
                callback(false)
            }
            .show()
    }
}