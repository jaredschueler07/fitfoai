package com.runningcoach.v2.data.service

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runningcoach.v2.domain.model.LocationData
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

/**
 * SessionRecoveryManager handles crash detection, session state persistence, and automatic recovery.
 * Uses SharedPreferences for immediate persistence and WorkManager for periodic sync operations.
 * 
 * Features:
 * - Automatic crash detection on app restart
 * - Real-time session state persistence
 * - Location history backup and recovery
 * - Metrics state preservation
 * - WorkManager integration for periodic sync
 * - Graceful error handling and cleanup
 */
class SessionRecoveryManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "session_recovery"
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_LOCATION_HISTORY = "location_history"
        private const val KEY_CURRENT_METRICS = "current_metrics"
        private const val KEY_ERROR_STATE = "error_state"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val KEY_APP_VERSION = "app_version"
        private const val KEY_CRASH_COUNT = "crash_count"
        
        private const val SYNC_WORK_NAME = "session_recovery_sync"
        private const val CLEANUP_WORK_NAME = "session_recovery_cleanup"
        
        // Recovery thresholds
        private const val MAX_RECOVERY_AGE_HOURS = 24
        private const val MAX_CRASH_ATTEMPTS = 3
        private const val SYNC_INTERVAL_MINUTES = 15L
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val workManager = WorkManager.getInstance(context)
    
    // Type tokens for JSON serialization
    private val locationListType = object : TypeToken<List<LocationData>>() {}.type
    private val metricsType = object : TypeToken<RunMetrics>() {}.type
    
    data class RecoveryData(
        val sessionId: Long,
        val userId: Long,
        val locationHistory: List<LocationData>,
        val metrics: RunMetrics?
    )
    
    data class SessionState(
        val sessionId: Long,
        val userId: Long,
        val startTime: Long,
        val lastUpdateTime: Long,
        val isActive: Boolean,
        val crashCount: Int
    )
    
    /**
     * Save active session for crash recovery
     */
    fun saveActiveSession(sessionId: Long, userId: Long) {
        prefs.edit()
            .putLong(KEY_ACTIVE_SESSION_ID, sessionId)
            .putLong(KEY_ACTIVE_USER_ID, userId)
            .putLong(KEY_SESSION_START_TIME, System.currentTimeMillis())
            .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            .putString(KEY_APP_VERSION, getCurrentAppVersion())
            .putInt(KEY_CRASH_COUNT, 0)
            .apply()
        
        // Schedule periodic sync
        schedulePeriodicSync()
    }
    
    /**
     * Save location history for recovery
     */
    fun saveLocationHistory(sessionId: Long, locationHistory: List<LocationData>) {
        try {
            val json = gson.toJson(locationHistory)
            prefs.edit()
                .putString(KEY_LOCATION_HISTORY, json)
                .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            saveErrorState(sessionId, "Failed to save location history: ${e.message}")
        }
    }
    
    /**
     * Save current metrics for recovery
     */
    fun saveMetrics(sessionId: Long, metrics: RunMetrics) {
        try {
            val json = gson.toJson(metrics)
            prefs.edit()
                .putString(KEY_CURRENT_METRICS, json)
                .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            saveErrorState(sessionId, "Failed to save metrics: ${e.message}")
        }
    }
    
    /**
     * Save error state for debugging
     */
    fun saveErrorState(sessionId: Long, errorMessage: String) {
        val errorInfo = mapOf(
            "sessionId" to sessionId,
            "error" to errorMessage,
            "timestamp" to System.currentTimeMillis(),
            "version" to getCurrentAppVersion()
        )
        
        prefs.edit()
            .putString(KEY_ERROR_STATE, gson.toJson(errorInfo))
            .apply()
    }
    
    /**
     * Clear active session (normal completion or stop)
     */
    fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_SESSION_START_TIME)
            .remove(KEY_LOCATION_HISTORY)
            .remove(KEY_CURRENT_METRICS)
            .remove(KEY_ERROR_STATE)
            .remove(KEY_LAST_UPDATE_TIME)
            .apply()
        
        // Cancel periodic sync
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
    }
    
    /**
     * Check if there's an active session that needs recovery
     */
    fun hasRecoverableSession(): Boolean {
        val sessionId = prefs.getLong(KEY_ACTIVE_SESSION_ID, -1L)
        if (sessionId == -1L) return false
        
        val lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)
        val ageHours = (System.currentTimeMillis() - lastUpdateTime) / (1000 * 60 * 60)
        
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        
        return ageHours < MAX_RECOVERY_AGE_HOURS && crashCount < MAX_CRASH_ATTEMPTS
    }
    
    /**
     * Get recovery data for session restoration
     */
    fun getRecoveryData(): RecoveryData? {
        if (!hasRecoverableSession()) return null
        
        val sessionId = prefs.getLong(KEY_ACTIVE_SESSION_ID, -1L)
        val userId = prefs.getLong(KEY_ACTIVE_USER_ID, -1L)
        
        if (sessionId == -1L || userId == -1L) return null
        
        // Increment crash count
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        prefs.edit()
            .putInt(KEY_CRASH_COUNT, crashCount + 1)
            .apply()
        
        // Get location history
        val locationHistory = try {
            val json = prefs.getString(KEY_LOCATION_HISTORY, null)
            if (json != null) {
                gson.fromJson<List<LocationData>>(json, locationListType)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList<LocationData>()
        }
        
        // Get metrics
        val metrics = try {
            val json = prefs.getString(KEY_CURRENT_METRICS, null)
            if (json != null) {
                gson.fromJson<RunMetrics>(json, metricsType)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        
        return RecoveryData(sessionId, userId, locationHistory, metrics)
    }
    
    /**
     * Get current session state information
     */
    fun getCurrentSessionState(): SessionState? {
        val sessionId = prefs.getLong(KEY_ACTIVE_SESSION_ID, -1L)
        if (sessionId == -1L) return null
        
        return SessionState(
            sessionId = sessionId,
            userId = prefs.getLong(KEY_ACTIVE_USER_ID, -1L),
            startTime = prefs.getLong(KEY_SESSION_START_TIME, 0L),
            lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L),
            isActive = true,
            crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        )
    }
    
    /**
     * Get error state information for debugging
     */
    fun getErrorState(): Map<String, Any>? {
        val errorJson = prefs.getString(KEY_ERROR_STATE, null)
        return if (errorJson != null) {
            try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(errorJson, type)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Force cleanup of old recovery data
     */
    fun forceCleanup() {
        clearActiveSession()
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        workManager.cancelUniqueWork(CLEANUP_WORK_NAME)
    }
    
    /**
     * Schedule periodic session data sync using WorkManager
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SessionSyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .setInitialDelay(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .addTag("session_recovery")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Schedule cleanup of old recovery data
     */
    fun scheduleCleanup(delayHours: Long = 24) {
        val cleanupRequest = OneTimeWorkRequestBuilder<SessionCleanupWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .addTag("session_cleanup")
            .build()
        
        workManager.enqueueUniqueWork(
            CLEANUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            cleanupRequest
        )
    }
    
    /**
     * Get diagnostic information for troubleshooting
     */
    fun getDiagnostics(): Map<String, Any> {
        val state = getCurrentSessionState()
        val error = getErrorState()
        
        return mapOf(
            "hasRecoverableSession" to hasRecoverableSession(),
            "currentState" to (state ?: "None"),
            "lastError" to (error ?: "None"),
            "appVersion" to getCurrentAppVersion(),
            "systemTime" to System.currentTimeMillis(),
            "prefsSize" to prefs.all.size
        )
    }
    
    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

/**
 * WorkManager worker for periodic session data sync
 */
class SessionSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val recoveryManager = SessionRecoveryManager(applicationContext)
            val state = recoveryManager.getCurrentSessionState()
            
            if (state != null) {
                // Perform any necessary sync operations
                // This could include syncing to remote servers, validating data integrity, etc.
                
                // Update last sync time
                recoveryManager.saveErrorState(state.sessionId, "Sync completed successfully")
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * WorkManager worker for cleaning up old recovery data
 */
class SessionCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val recoveryManager = SessionRecoveryManager(applicationContext)
            
            // Clean up old recovery data
            if (!recoveryManager.hasRecoverableSession()) {
                recoveryManager.forceCleanup()
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}