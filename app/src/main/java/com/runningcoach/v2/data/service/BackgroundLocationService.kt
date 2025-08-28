package com.runningcoach.v2.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.runningcoach.v2.R
import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Background Location Service implementing Android Foreground Service for continuous GPS tracking.
 * This service ensures GPS tracking continues when the app is backgrounded or the device is in Doze mode.
 * 
 * Features:
 * - Foreground service with persistent notification
 * - Wake lock management for battery optimization bypass
 * - Auto-restart capability on crash
 * - Integration with existing LocationService
 * - Session persistence and crash recovery
 */
class BackgroundLocationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "GPS_TRACKING_CHANNEL"
        const val CHANNEL_NAME = "GPS Tracking"
        
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val ACTION_UPDATE_METRICS = "UPDATE_METRICS"
        
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_USER_ID = "user_id"
        
        private const val WAKE_LOCK_TAG = "FITFOAI::LocationTracking"
        private const val WAKE_LOCK_TIMEOUT = 60 * 60 * 1000L // 1 hour
        
        fun startService(context: Context, sessionId: Long, userId: Long) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    private val binder = LocationServiceBinder()
    private lateinit var locationService: LocationService
    private lateinit var sessionRecoveryManager: SessionRecoveryManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Service state
    private var isTracking = false
    private var currentSessionId: Long? = null
    private var currentUserId: Long? = null
    
    // Metrics tracking
    private val _currentMetrics = MutableStateFlow<RunMetrics?>(null)
    val currentMetrics: StateFlow<RunMetrics?> = _currentMetrics.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    val locationHistory: StateFlow<List<LocationData>> = _locationHistory.asStateFlow()

    inner class LocationServiceBinder : Binder() {
        fun getService(): BackgroundLocationService = this@BackgroundLocationService
    }

    override fun onCreate() {
        super.onCreate()
        
        locationService = LocationService(applicationContext)
        sessionRecoveryManager = SessionRecoveryManager(applicationContext)
        
        createNotificationChannel()
        
        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
                
                if (sessionId != -1L && userId != -1L) {
                    startTracking(sessionId, userId)
                }
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
            ACTION_UPDATE_METRICS -> {
                // Update notification with latest metrics
                updateNotification()
            }
            else -> {
                // Service restart - attempt recovery
                attemptSessionRecovery()
            }
        }
        
        // Return START_STICKY to ensure service restarts after being killed
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun startTracking(sessionId: Long, userId: Long) {
        if (isTracking) return
        
        currentSessionId = sessionId
        currentUserId = userId
        isTracking = true
        
        // Acquire wake lock to prevent doze mode interference
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT)
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Save session state for crash recovery
        sessionRecoveryManager.saveActiveSession(sessionId, userId)
        
        // Start location tracking
        serviceScope.launch {
            try {
                locationService.getLocationUpdates().collect { locationData ->
                    if (isTracking) {
                        handleLocationUpdate(locationData)
                    }
                }
            } catch (e: Exception) {
                handleTrackingError(e)
            }
        }
        
        // Start location service tracking
        locationService.startLocationTracking()
    }

    private fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        
        // Stop location tracking
        locationService.stopLocationTracking()
        
        // Release wake lock
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        
        // Clear recovery data
        sessionRecoveryManager.clearActiveSession()
        
        // Reset state
        currentSessionId = null
        currentUserId = null
        _currentMetrics.value = null
        _locationHistory.value = emptyList()
        
        // Stop foreground and service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleLocationUpdate(locationData: LocationData) {
        currentSessionId?.let { sessionId ->
            // Add to history
            val currentHistory = _locationHistory.value.toMutableList()
            currentHistory.add(locationData)
            _locationHistory.value = currentHistory
            
            // Update metrics
            val updatedMetrics = calculateMetrics(currentHistory, locationData)
            _currentMetrics.value = updatedMetrics
            
            // Update notification
            updateNotification()
            
            // Save to recovery manager periodically (every 10 points)
            if (currentHistory.size % 10 == 0) {
                sessionRecoveryManager.saveLocationHistory(sessionId, currentHistory)
                sessionRecoveryManager.saveMetrics(sessionId, updatedMetrics)
            }
        }
    }

    private fun calculateMetrics(history: List<LocationData>, currentLocation: LocationData): RunMetrics {
        val distance = locationService.calculateDistance(history)
        val elevationGain = locationService.calculateElevationGain(history)
        val startTime = history.firstOrNull()?.timestamp ?: currentLocation.timestamp
        val duration = ((currentLocation.timestamp - startTime) / 1000).toInt() // seconds
        
        val averagePace = if (distance > 0f) {
            (duration.toDouble() / 60.0) / (distance / 1000.0) // minutes per kilometer
        } else 0.0
        
        return RunMetrics(
            startTime = startTime,
            duration = duration.toLong(),
            distance = distance,
            averagePace = averagePace.toFloat(),
            currentLocation = currentLocation,
            totalLocationPoints = history.size,
            lastLocationTimestamp = currentLocation.timestamp,
            elevationGain = elevationGain,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    private fun handleTrackingError(error: Exception) {
        // Log error and attempt recovery
        val sessionId = currentSessionId
        val userId = currentUserId
        
        if (sessionId != null && userId != null) {
            sessionRecoveryManager.saveErrorState(sessionId, error.message ?: "Unknown error")
            
            // Attempt to restart tracking after delay
            serviceScope.launch {
                delay(5000) // Wait 5 seconds
                if (isTracking) {
                    startTracking(sessionId, userId)
                }
            }
        }
    }

    private fun attemptSessionRecovery() {
        serviceScope.launch {
            val recoveryData = sessionRecoveryManager.getRecoveryData()
            if (recoveryData != null) {
                val (sessionId, userId, savedHistory, savedMetrics) = recoveryData
                
                // Restore session state
                currentSessionId = sessionId
                currentUserId = userId
                _locationHistory.value = savedHistory
                _currentMetrics.value = savedMetrics
                
                // Resume tracking
                startTracking(sessionId, userId)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for GPS tracking during runs"
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val metrics = _currentMetrics.value
        
        val title = "FITFOAI - GPS Tracking Active"
        val content = if (metrics != null) {
            "Distance: ${"%.2f".format(metrics.distance / 1000)}km | Duration: ${formatDuration(metrics.duration.toInt())}"
        } else {
            "Starting GPS tracking..."
        }
        
        val stopIntent = Intent(this, BackgroundLocationService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop Tracking",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        if (isTracking) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        
        locationService.stopLocationTracking()
        serviceScope.cancel()
    }

    // Public methods for service interaction
    fun isCurrentlyTracking(): Boolean = isTracking
    
    fun getCurrentSessionId(): Long? = currentSessionId
    
    fun getCurrentMetrics(): RunMetrics? = _currentMetrics.value
    
    fun getLocationHistory(): List<LocationData> = _locationHistory.value
}