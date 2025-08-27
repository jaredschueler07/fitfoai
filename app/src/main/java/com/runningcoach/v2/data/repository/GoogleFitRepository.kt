package com.runningcoach.v2.data.repository

import android.content.Context
import android.util.Log
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.ConnectedAppEntity
import com.runningcoach.v2.data.local.entity.GoogleFitDailySummaryEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.data.service.GoogleFitService
import com.runningcoach.v2.domain.model.AppType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class GoogleFitRepository(
    context: Context,
    private val database: FITFOAIDatabase
) {
    companion object {
        private const val TAG = "GoogleFitRepository"
    }
    
    private val googleFitService = GoogleFitService(context)
    private val userDao = database.userDao()
    private val googleFitDao = database.googleFitDailySummaryDao()
    private val connectedAppDao = database.connectedAppDao()
    
    // Connection Management
    suspend fun isGoogleFitConnected(): Boolean {
        return googleFitService.isConnected.value
    }
    
    fun connectGoogleFit(): android.content.Intent {
        return try {
            Log.i("GoogleFitRepository", "Initiating Google Fit connection")
            googleFitService.initiateConnection()
        } catch (e: Exception) {
            Log.e("GoogleFitRepository", "Error in connectGoogleFit", e)
            android.content.Intent()
        }
    }
    
    suspend fun updateConnectionStatus(userId: Long, isConnected: Boolean) {
        val existingApp = connectedAppDao.getConnectedAppByType(userId, AppType.GOOGLE_FIT.name)
        
        if (existingApp != null) {
            connectedAppDao.updateConnectionStatus(
                userId = userId,
                appType = AppType.GOOGLE_FIT.name,
                isConnected = isConnected,
                lastSyncTime = if (isConnected) System.currentTimeMillis() else null
            )
        } else {
            connectedAppDao.insertConnectedApp(
                ConnectedAppEntity(
                    userId = userId,
                    appType = AppType.GOOGLE_FIT.name,
                    appName = "Google Fit",
                    isConnected = isConnected,
                    lastSyncTime = if (isConnected) System.currentTimeMillis() else null
                )
            )
        }
    }
    
    // Data Syncing
    suspend fun syncTodaysFitnessData(): Result<GoogleFitDailySummaryEntity?> {
        return try {
            val currentUser = userDao.getCurrentUser().first()
                ?: return Result.failure(Exception("No current user found"))
            
            if (!googleFitService.isConnected.value) {
                return Result.failure(Exception("Google Fit not connected"))
            }
            
            // Get comprehensive fitness data from Google Fit
            val fitnessDataResult = googleFitService.getComprehensiveFitnessData()
            if (fitnessDataResult.isFailure) {
                return Result.failure(fitnessDataResult.exceptionOrNull() ?: Exception("Failed to get fitness data"))
            }
            
            val fitnessData = fitnessDataResult.getOrNull()
                ?: return Result.failure(Exception("No fitness data available"))
            
            // Get today's date at midnight
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            
            // Create or update daily summary
            val existingSummary = googleFitDao.getDailySummaryForDate(currentUser.id, today)
            
            val dailySummary = if (existingSummary != null) {
                existingSummary.copy(
                    steps = fitnessData.steps,
                    distance = fitnessData.distance,
                    calories = fitnessData.calories,
                    averageHeartRate = fitnessData.heartRate,
                    weight = fitnessData.weight,
                    height = fitnessData.height,
                    syncStatus = "SYNCED",
                    lastSynced = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            } else {
                GoogleFitDailySummaryEntity(
                    userId = currentUser.id,
                    date = today,
                    steps = fitnessData.steps,
                    distance = fitnessData.distance,
                    calories = fitnessData.calories,
                    averageHeartRate = fitnessData.heartRate,
                    weight = fitnessData.weight,
                    height = fitnessData.height,
                    syncStatus = "SYNCED",
                    lastSynced = System.currentTimeMillis()
                )
            }
            
            val summaryId = googleFitDao.insertDailySummary(dailySummary)
            
            // Update connection status
            updateConnectionStatus(currentUser.id, true)
            
            // Update user profile with latest body measurements
            updateUserProfileWithFitnessData(currentUser, fitnessData)
            
            Log.i(TAG, "Successfully synced fitness data for user ${currentUser.id}")
            
            Result.success(dailySummary.copy(id = summaryId))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync fitness data", e)
            Result.failure(e)
        }
    }
    
    suspend fun syncWeeklyFitnessData(): Result<List<GoogleFitDailySummaryEntity>> {
        return try {
            val currentUser = userDao.getCurrentUser().first()
                ?: return Result.failure(Exception("No current user found"))
            
            if (!googleFitService.isConnected.value) {
                return Result.failure(Exception("Google Fit not connected"))
            }
            
            // Get weekly steps data
            val weeklyStepsResult = googleFitService.getWeeklySteps()
            if (weeklyStepsResult.isFailure) {
                return Result.failure(weeklyStepsResult.exceptionOrNull() ?: Exception("Failed to get weekly data"))
            }
            
            val weeklySteps = weeklyStepsResult.getOrNull() ?: emptyList()
            val summaries = mutableListOf<GoogleFitDailySummaryEntity>()
            
            for (dayData in weeklySteps) {
                // Parse date from dayData.date - this would need proper date parsing
                val dayTimestamp = System.currentTimeMillis() // Placeholder - implement proper date parsing
                
                val existingSummary = googleFitDao.getDailySummaryForDate(currentUser.id, dayTimestamp)
                
                val summary = if (existingSummary != null) {
                    existingSummary.copy(
                        steps = dayData.steps,
                        syncStatus = "SYNCED",
                        lastSynced = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    GoogleFitDailySummaryEntity(
                        userId = currentUser.id,
                        date = dayTimestamp,
                        steps = dayData.steps,
                        syncStatus = "SYNCED",
                        lastSynced = System.currentTimeMillis()
                    )
                }
                
                googleFitDao.insertDailySummary(summary)
                summaries.add(summary)
            }
            
            Log.i(TAG, "Successfully synced ${summaries.size} days of fitness data")
            Result.success(summaries)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync weekly fitness data", e)
            Result.failure(e)
        }
    }
    
    // Data Retrieval
    suspend fun getTodaysFitnessData(): GoogleFitDailySummaryEntity? {
        val currentUser = userDao.getCurrentUser().first() ?: return null
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return googleFitDao.getDailySummaryForDate(currentUser.id, today)
    }
    
    suspend fun getLatestFitnessData(): GoogleFitDailySummaryEntity? {
        val currentUser = userDao.getCurrentUser().first() ?: return null
        return googleFitDao.getLatestDailySummary(currentUser.id)
    }
    
    fun getFitnessDataFlow(): Flow<List<GoogleFitDailySummaryEntity>> {
        return database.userDao().getCurrentUser().let { userFlow ->
            // This is a simplified version - in production you'd want to properly combine flows
            database.googleFitDailySummaryDao().getDailySummariesForUser(1L) // Placeholder user ID
        }
    }
    
    // Helper Methods
    private suspend fun updateUserProfileWithFitnessData(
        user: UserEntity,
        fitnessData: GoogleFitService.FitnessData
    ) {
        try {
            var shouldUpdate = false
            var updatedUser = user
            
            // Update weight if available and different
            fitnessData.weight?.let { newWeight ->
                if (user.weight != newWeight) {
                    updatedUser = updatedUser.copy(weight = newWeight, updatedAt = System.currentTimeMillis())
                    shouldUpdate = true
                }
            }
            
            // Update height if available and different
            fitnessData.height?.let { newHeightMeters ->
                val newHeightCm = (newHeightMeters * 100).toInt()
                if (user.height != newHeightCm) {
                    updatedUser = updatedUser.copy(height = newHeightCm, updatedAt = System.currentTimeMillis())
                    shouldUpdate = true
                }
            }
            
            if (shouldUpdate) {
                userDao.updateUser(updatedUser)
                Log.i(TAG, "Updated user profile with fitness data")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user profile with fitness data", e)
        }
    }
    
    suspend fun disconnect() {
        try {
            val currentUser = userDao.getCurrentUser().first()
            if (currentUser != null) {
                updateConnectionStatus(currentUser.id, false)
            }
            googleFitService.disconnect()
            Log.i(TAG, "Disconnected from Google Fit")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from Google Fit", e)
        }
    }
}
