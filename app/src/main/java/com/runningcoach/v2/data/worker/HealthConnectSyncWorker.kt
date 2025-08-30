package com.runningcoach.v2.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.manager.HealthConnectManager
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import kotlinx.coroutines.flow.first

/**
 * Worker for periodic Health Connect data synchronization
 * Runs in the background to keep fitness data up to date
 */
class HealthConnectSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "HealthConnectSyncWorker"
        const val WORK_NAME = "health_connect_sync"
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting Health Connect sync")
            
            val database = FITFOAIDatabase.getDatabase(applicationContext)
            val permissionManager = HealthConnectPermissionManager(
                // Note: This would need proper DI in production
                // For now, we'll check if we can create it without activity
                // In production, this would be injected
                null as androidx.activity.ComponentActivity? // This needs fixing
            )
            
            // Check if we have permissions and Health Connect is available
            if (permissionManager == null) {
                Log.w(TAG, "Cannot create permission manager without activity context")
                return Result.failure()
            }
            
            val healthConnectManager = HealthConnectManager.getInstance(
                applicationContext,
                permissionManager
            )
            
            // Check if we have an active user
            val currentUser = database.userDao().getCurrentUser().first()
            if (currentUser == null) {
                Log.d(TAG, "No active user, skipping sync")
                return Result.success()
            }
            
            // Check if Health Connect is connected
            val connectionState = healthConnectManager.connectionState.value
            if (connectionState != HealthConnectManager.ConnectionState.CONNECTED) {
                Log.d(TAG, "Health Connect not connected (state: $connectionState), skipping sync")
                return Result.success()
            }
            
            // Perform sync
            val syncResult = healthConnectManager.performFullSync()
            
            if (syncResult.isSuccess) {
                Log.i(TAG, "Health Connect sync completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "Health Connect sync failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Health Connect sync worker failed", e)
            
            // Retry on recoverable errors, fail on unrecoverable ones
            when {
                e.message?.contains("permission", ignoreCase = true) == true -> {
                    Log.w(TAG, "Permission error, not retrying")
                    Result.failure()
                }
                runAttemptCount < 3 -> {
                    Log.i(TAG, "Retrying sync (attempt ${runAttemptCount + 1}/3)")
                    Result.retry()
                }
                else -> {
                    Log.e(TAG, "Max retry attempts reached, failing")
                    Result.failure()
                }
            }
        }
    }
}