package com.runningcoach.v2.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * HealthConnectPermissionManager handles Health Connect permissions and availability.
 * 
 * Features:
 * - Health Connect SDK availability checking
 * - Comprehensive permission management for fitness data
 * - Educational permission rationales
 * - Graceful fallback when Health Connect unavailable
 * - Integration with existing permission architecture
 */
class HealthConnectPermissionManager(
    private val activity: ComponentActivity
) {
    
    companion object {
        // Core Health Connect permissions required for fitness tracking
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
        )
        
        // Optional permissions for enhanced features
        val OPTIONAL_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getWritePermission(PowerRecord::class)
        )
        
        // All permissions combined
        val ALL_PERMISSIONS = REQUIRED_PERMISSIONS + OPTIONAL_PERMISSIONS
    }
    
    // Health Connect client
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(activity) }
    
    // Permission status tracking
    private val _permissionStatus = MutableStateFlow(HealthConnectPermissionStatus())
    val permissionStatus: StateFlow<HealthConnectPermissionStatus> = _permissionStatus.asStateFlow()
    
    // Permission result callback
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    
    // Health Connect permission contract
    private val healthConnectPermissionLauncher = activity.registerForActivityResult(
        healthConnectClient.permissionController.createRequestPermissionResultContract()
    ) { granted ->
        updatePermissionStatus()
        onPermissionResult?.invoke(granted.containsAll(REQUIRED_PERMISSIONS))
    }
    
    data class HealthConnectPermissionStatus(
        val isAvailable: Boolean = false,
        val needsUpdate: Boolean = false,
        val hasRequiredPermissions: Boolean = false,
        val hasOptionalPermissions: Boolean = false,
        val grantedPermissions: Set<String> = emptySet(),
        val availabilityStatus: Int = SDK_UNAVAILABLE
    )
    
    enum class HealthConnectAvailability {
        AVAILABLE,
        UNAVAILABLE,
        NEEDS_UPDATE,
        CHECKING
    }
    
    enum class PermissionResult {
        GRANTED,
        PARTIAL_GRANTED,
        DENIED,
        HEALTH_CONNECT_UNAVAILABLE,
        NEEDS_UPDATE
    }
    
    init {
        updatePermissionStatus()
    }
    
    /**
     * Check Health Connect SDK availability
     */
    suspend fun checkAvailability(): HealthConnectAvailability {
        return when (val status = HealthConnectClient.getSdkStatus(activity)) {
            SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NEEDS_UPDATE
            else -> HealthConnectAvailability.UNAVAILABLE
        }.also {
            _permissionStatus.value = _permissionStatus.value.copy(
                availabilityStatus = status,
                isAvailable = it == HealthConnectAvailability.AVAILABLE,
                needsUpdate = it == HealthConnectAvailability.NEEDS_UPDATE
            )
        }
    }
    
    /**
     * Check if required Health Connect permissions are granted
     */
    suspend fun hasRequiredPermissions(): Boolean {
        if (!_permissionStatus.value.isAvailable) return false
        
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            REQUIRED_PERMISSIONS.all { it in grantedPermissions }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if all permissions (required + optional) are granted
     */
    suspend fun hasAllPermissions(): Boolean {
        if (!_permissionStatus.value.isAvailable) return false
        
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            ALL_PERMISSIONS.all { it in grantedPermissions }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request Health Connect permissions with educational rationale
     */
    fun requestHealthConnectPermissions(callback: (Boolean) -> Unit) {
        onPermissionResult = callback
        
        // Show rationale before requesting permissions
        showHealthConnectRationale {
            healthConnectPermissionLauncher.launch(ALL_PERMISSIONS)
        }
    }
    
    /**
     * Request only required permissions (minimal set)
     */
    fun requestRequiredPermissions(callback: (Boolean) -> Unit) {
        onPermissionResult = callback
        
        showHealthConnectRationale {
            healthConnectPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }
    
    /**
     * Check overall permission status with detailed result
     */
    suspend fun checkPermissionStatus(): PermissionResult {
        return when (checkAvailability()) {
            HealthConnectAvailability.UNAVAILABLE -> PermissionResult.HEALTH_CONNECT_UNAVAILABLE
            HealthConnectAvailability.NEEDS_UPDATE -> PermissionResult.NEEDS_UPDATE
            HealthConnectAvailability.AVAILABLE -> {
                when {
                    hasRequiredPermissions() -> PermissionResult.GRANTED
                    hasPartialPermissions() -> PermissionResult.PARTIAL_GRANTED
                    else -> PermissionResult.DENIED
                }
            }
            HealthConnectAvailability.CHECKING -> PermissionResult.DENIED
        }
    }
    
    /**
     * Guide user through Health Connect setup and permissions
     */
    suspend fun requestHealthConnectSetup(callback: (Boolean) -> Unit) {
        when (checkPermissionStatus()) {
            PermissionResult.GRANTED -> {
                callback(true)
            }
            PermissionResult.PARTIAL_GRANTED -> {
                showPartialPermissionDialog { grantMore ->
                    if (grantMore) {
                        requestHealthConnectPermissions(callback)
                    } else {
                        callback(true) // Proceed with partial permissions
                    }
                }
            }
            PermissionResult.DENIED -> {
                requestRequiredPermissions(callback)
            }
            PermissionResult.HEALTH_CONNECT_UNAVAILABLE -> {
                showHealthConnectUnavailableDialog()
                callback(false)
            }
            PermissionResult.NEEDS_UPDATE -> {
                showHealthConnectUpdateDialog()
                callback(false)
            }
        }
    }
    
    /**
     * Open Health Connect app for manual permission management
     */
    fun openHealthConnectApp() {
        try {
            val intent = Intent().apply {
                action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
                putExtra("androidx.health.EXTRA_PACKAGE_NAME", activity.packageName)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to Play Store if Health Connect app not found
            openPlayStoreForHealthConnect()
        }
    }
    
    /**
     * Open Play Store to install/update Health Connect
     */
    fun openPlayStoreForHealthConnect() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
        }
        
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web Play Store
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            }
            activity.startActivity(webIntent)
        }
    }
    
    // Private helper methods
    
    private fun updatePermissionStatus() {
        // This will be called after permission requests to update the status
        // The actual permission checking is async, so we'll update status in the calling methods
    }
    
    private suspend fun hasPartialPermissions(): Boolean {
        if (!_permissionStatus.value.isAvailable) return false
        
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.isNotEmpty() && !REQUIRED_PERMISSIONS.all { it in grantedPermissions }
        } catch (e: Exception) {
            false
        }
    }
    
    // Educational dialogs for Health Connect
    
    private fun showHealthConnectRationale(onContinue: () -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Connect to Health Connect")
            .setMessage(
                "FITFOAI works better with Health Connect to:\n\n" +
                "• Sync your fitness data across all health apps\n" +
                "• Automatically backup your running history\n" +
                "• Share data with your fitness ecosystem\n" +
                "• Improve battery life and performance\n\n" +
                "Your data remains private and secure with Health Connect's advanced privacy controls."
            )
            .setPositiveButton("Connect") { _, _ -> onContinue() }
            .setNegativeButton("Skip") { _, _ -> 
                onPermissionResult?.invoke(false)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPartialPermissionDialog(callback: (Boolean) -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Enhance Your Experience")
            .setMessage(
                "You've granted some Health Connect permissions. " +
                "Granting additional permissions will:\n\n" +
                "• Enable heart rate tracking\n" +
                "• Provide more detailed fitness insights\n" +
                "• Improve coaching recommendations\n\n" +
                "Would you like to grant more permissions for the full experience?"
            )
            .setPositiveButton("Grant More") { _, _ -> callback(true) }
            .setNegativeButton("Continue") { _, _ -> callback(false) }
            .show()
    }
    
    private fun showHealthConnectUnavailableDialog() {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Health Connect Not Available")
            .setMessage(
                "Health Connect is not available on this device. " +
                "FITFOAI will work with local storage only.\n\n" +
                "Some features may be limited, but all core functionality will remain available."
            )
            .setPositiveButton("Continue") { _, _ -> }
            .setCancelable(true)
            .show()
    }
    
    private fun showHealthConnectUpdateDialog() {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Update Health Connect")
            .setMessage(
                "Health Connect needs to be updated for the best experience with FITFOAI.\n\n" +
                "Please update Health Connect from the Play Store."
            )
            .setPositiveButton("Update") { _, _ -> openPlayStoreForHealthConnect() }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }
    
    /**
     * Create intent for Health Connect permission request
     */
    fun createPermissionRequestIntent(): Intent {
        return healthConnectClient.permissionController.createRequestPermissionResultContract()
            .createIntent(activity, ALL_PERMISSIONS)
    }
}