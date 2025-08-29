package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.GoogleFitDailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoogleFitDailySummaryDao {
    
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId ORDER BY date DESC")
    fun getDailySummariesForUser(userId: Long): Flow<List<GoogleFitDailySummaryEntity>>
    
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getDailySummaryForDate(userId: Long, date: Long): GoogleFitDailySummaryEntity?
    
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDailySummary(userId: Long): GoogleFitDailySummaryEntity?
    
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getDailySummariesInRange(userId: Long, startDate: Long, endDate: Long): List<GoogleFitDailySummaryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: GoogleFitDailySummaryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummaries(summaries: List<GoogleFitDailySummaryEntity>)
    
    @Update
    suspend fun updateDailySummary(summary: GoogleFitDailySummaryEntity)
    
    @Delete
    suspend fun deleteDailySummary(summary: GoogleFitDailySummaryEntity)
    
    @Query("DELETE FROM google_fit_daily_summary WHERE userId = :userId AND date < :beforeDate")
    suspend fun deleteOldSummaries(userId: Long, beforeDate: Long)
    
    @Query("SELECT COUNT(*) FROM google_fit_daily_summary WHERE userId = :userId")
    suspend fun getSummaryCount(userId: Long): Int
    
    // Additional methods for GoogleFitManager
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId ORDER BY date DESC")
    fun getUserDailySummaries(userId: Long): Flow<List<GoogleFitDailySummaryEntity>>
    
    @Query("SELECT * FROM google_fit_daily_summary WHERE userId = :userId AND date >= :startTime AND date <= :endTime ORDER BY date ASC")
    suspend fun getDailySummariesForDateRange(userId: Long, startTime: Long, endTime: Long): List<GoogleFitDailySummaryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySummary(summary: GoogleFitDailySummaryEntity)
}
