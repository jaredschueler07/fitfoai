package com.runningcoach.v2.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runningcoach.v2.data.manager.GoogleFitManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for periodic Google Fit data synchronization
 */
class GoogleFitSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "GoogleFitSyncWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting Google Fit background sync")
            
            val manager = GoogleFitManager.getInstance(applicationContext)
            
            // Check if we're connected
            if (manager.connectionState.value != GoogleFitManager.ConnectionState.CONNECTED) {
                Log.w(TAG, "Google Fit not connected, skipping sync")
                return@withContext Result.success()
            }
            
            // Perform sync
            val syncResult = manager.performFullSync()
            
            return@withContext if (syncResult.isSuccess) {
                Log.i(TAG, "Google Fit background sync completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "Google Fit background sync failed", syncResult.exceptionOrNull())
                // Retry later
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Google Fit sync worker", e)
            // Don't retry on unexpected errors
            Result.failure()
        }
    }
}