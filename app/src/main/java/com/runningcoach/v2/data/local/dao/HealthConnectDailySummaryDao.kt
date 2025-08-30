package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.HealthConnectDailySummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Health Connect daily summary data operations
 */
@Dao
interface HealthConnectDailySummaryDao {
    
    /**
     * Insert or update a daily summary entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySummary(summary: HealthConnectDailySummaryEntity): Long
    
    /**
     * Insert multiple daily summaries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummaries(summaries: List<HealthConnectDailySummaryEntity>)
    
    /**
     * Get daily summary for a specific date and user
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date = :date
    """)
    suspend fun getDailySummaryForDate(userId: Long, date: Long): HealthConnectDailySummaryEntity?
    
    /**
     * Get all daily summaries for a user (most recent first)
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE userId = :userId 
        ORDER BY date DESC
    """)
    fun getUserDailySummaries(userId: Long): Flow<List<HealthConnectDailySummaryEntity>>
    
    /**
     * Get daily summaries for a user within a date range
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getDailySummariesForDateRange(
        userId: Long, 
        startDate: Long, 
        endDate: Long
    ): List<HealthConnectDailySummaryEntity>
    
    /**
     * Get daily summaries flow for a user within a date range
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    fun getDailySummariesForDateRangeFlow(
        userId: Long, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<HealthConnectDailySummaryEntity>>
    
    /**
     * Get the most recent daily summary for a user
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE userId = :userId 
        ORDER BY date DESC 
        LIMIT 1
    """)
    suspend fun getLatestDailySummary(userId: Long): HealthConnectDailySummaryEntity?
    
    /**
     * Get summaries that need syncing
     */
    @Query("""
        SELECT * FROM health_connect_daily_summaries 
        WHERE syncStatus IN ('PENDING', 'FAILED')
        ORDER BY date DESC
    """)
    suspend fun getSummariesPendingSync(): List<HealthConnectDailySummaryEntity>
    
    /**
     * Update sync status for a summary
     */
    @Query("""
        UPDATE health_connect_daily_summaries 
        SET syncStatus = :status, syncError = :error, lastSynced = :timestamp
        WHERE id = :summaryId
    """)
    suspend fun updateSyncStatus(
        summaryId: Long, 
        status: HealthConnectDailySummaryEntity.SyncStatus, 
        error: String?, 
        timestamp: Long
    )
    
    /**
     * Get total steps for the last N days
     */
    @Query("""
        SELECT COALESCE(SUM(steps), 0) as totalSteps
        FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate
    """)
    suspend fun getTotalStepsForPeriod(userId: Long, startDate: Long): Int
    
    /**
     * Get average daily steps for the last N days (excluding days with 0 steps)
     */
    @Query("""
        SELECT COALESCE(AVG(steps), 0) as avgSteps
        FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate AND steps > 0
    """)
    suspend fun getAverageDailySteps(userId: Long, startDate: Long): Float
    
    /**
     * Get total distance for the last N days
     */
    @Query("""
        SELECT COALESCE(SUM(distance), 0) as totalDistance
        FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate
    """)
    suspend fun getTotalDistanceForPeriod(userId: Long, startDate: Long): Float
    
    /**
     * Get weekly activity summary (7-day rolling window)
     */
    @Query("""
        SELECT 
            COALESCE(SUM(steps), 0) as totalSteps,
            COALESCE(SUM(distance), 0) as totalDistance,
            COALESCE(SUM(calories), 0) as totalCalories,
            COALESCE(SUM(activeMinutes), 0) as totalActiveMinutes,
            COALESCE(AVG(CASE WHEN avgHeartRate IS NOT NULL THEN avgHeartRate END), 0) as avgHeartRate,
            COUNT(*) as daysWithData
        FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getWeeklyActivitySummary(
        userId: Long, 
        startDate: Long, 
        endDate: Long
    ): WeeklyActivitySummary
    
    /**
     * Delete old summaries beyond retention period
     */
    @Query("""
        DELETE FROM health_connect_daily_summaries 
        WHERE date < :cutoffDate
    """)
    suspend fun deleteOldSummaries(cutoffDate: Long): Int
    
    /**
     * Delete all summaries for a user (for account deletion)
     */
    @Query("""
        DELETE FROM health_connect_daily_summaries 
        WHERE userId = :userId
    """)
    suspend fun deleteAllUserSummaries(userId: Long): Int
    
    /**
     * Get days with significant activity (for streaks calculation)
     */
    @Query("""
        SELECT date 
        FROM health_connect_daily_summaries 
        WHERE userId = :userId 
        AND (steps >= :minSteps OR activeMinutes >= :minActiveMinutes)
        AND date >= :startDate 
        ORDER BY date ASC
    """)
    suspend fun getActiveDays(
        userId: Long,
        minSteps: Int = 1000,
        minActiveMinutes: Int = 15,
        startDate: Long
    ): List<Long>
    
    /**
     * Get heart rate trends for the last N days
     */
    @Query("""
        SELECT date, avgHeartRate, minHeartRate, maxHeartRate
        FROM health_connect_daily_summaries 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND avgHeartRate IS NOT NULL
        ORDER BY date ASC
    """)
    suspend fun getHeartRateTrends(userId: Long, startDate: Long): List<HeartRateTrend>
    
    /**
     * Check if there's any data for a specific day
     */
    @Query("""
        SELECT COUNT(*) > 0
        FROM health_connect_daily_summaries 
        WHERE userId = :userId AND date = :date
    """)
    suspend fun hasDataForDate(userId: Long, date: Long): Boolean
    
    /**
     * Update last sync time for all summaries
     */
    @Query("""
        UPDATE health_connect_daily_summaries 
        SET lastSynced = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateLastSyncTime(userId: Long, timestamp: Long)
}

/**
 * Data class for weekly activity summary query result
 */
data class WeeklyActivitySummary(
    val totalSteps: Int,
    val totalDistance: Float,
    val totalCalories: Int,
    val totalActiveMinutes: Int,
    val avgHeartRate: Float,
    val daysWithData: Int
)

/**
 * Data class for heart rate trend query result
 */
data class HeartRateTrend(
    val date: Long,
    val avgHeartRate: Float?,
    val minHeartRate: Float?,
    val maxHeartRate: Float?
)