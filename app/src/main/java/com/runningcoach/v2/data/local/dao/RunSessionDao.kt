package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunSession(runSession: RunSessionEntity): Long
    
    @Update
    suspend fun updateRunSession(runSession: RunSessionEntity)
    
    @Delete
    suspend fun deleteRunSession(runSession: RunSessionEntity)
    
    @Query("SELECT * FROM run_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): RunSessionEntity?
    
    @Query("SELECT * FROM run_sessions WHERE googleFitSessionId = :googleFitId LIMIT 1")
    suspend fun getSessionByGoogleFitId(googleFitId: String): RunSessionEntity?
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getUserSessions(userId: Long): Flow<List<RunSessionEntity>>
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(userId: Long, limit: Int = 10): List<RunSessionEntity>
    
    @Query("""
        SELECT * FROM run_sessions 
        WHERE userId = :userId 
        AND startTime >= :startTime 
        AND startTime <= :endTime 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsInDateRange(
        userId: Long,
        startTime: Long,
        endTime: Long
    ): List<RunSessionEntity>
    
    @Query("""
        SELECT * FROM run_sessions 
        WHERE userId = :userId 
        AND workoutType = :workoutType 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsByWorkoutType(
        userId: Long,
        workoutType: String
    ): List<RunSessionEntity>
    
    @Query("""
        SELECT * FROM run_sessions 
        WHERE userId = :userId 
        AND trainingPlanId = :planId 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsForTrainingPlan(
        userId: Long,
        planId: Long
    ): List<RunSessionEntity>
    
    @Query("""
        UPDATE run_sessions 
        SET syncedWithGoogleFit = :synced, 
            googleFitSessionId = :googleFitId,
            lastSyncTime = :syncTime 
        WHERE id = :sessionId
    """)
    suspend fun updateGoogleFitSync(
        sessionId: Long,
        synced: Boolean,
        googleFitId: String? = null,
        syncTime: Long = System.currentTimeMillis()
    )
    
    @Query("""
        SELECT * FROM run_sessions 
        WHERE userId = :userId 
        AND syncedWithGoogleFit = 0 
        ORDER BY startTime DESC
    """)
    suspend fun getUnsyncedSessions(userId: Long): List<RunSessionEntity>
    
    @Query("""
        SELECT SUM(distance) as totalDistance,
               SUM(duration) as totalDuration,
               SUM(calories) as totalCalories,
               COUNT(*) as sessionCount,
               AVG(avgPace) as averagePace
        FROM run_sessions
        WHERE userId = :userId
        AND startTime >= :startTime
    """)
    suspend fun getUserStats(userId: Long, startTime: Long): UserRunStats?
    
    @Query("DELETE FROM run_sessions WHERE userId = :userId")
    suspend fun deleteAllUserSessions(userId: Long)
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(userId: Long): RunSessionEntity?
    
    // Keep existing compatibility methods
    @Query("SELECT * FROM run_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getRunSessionsByUser(userId: Long): Flow<List<RunSessionEntity>>
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedRuns(userId: Long, limit: Int = 10): Flow<List<RunSessionEntity>>
    
    @Query("SELECT * FROM run_sessions WHERE id = :sessionId")
    suspend fun getRunSessionById(sessionId: Long): RunSessionEntity?
    
    @Query("SELECT COUNT(*) FROM run_sessions WHERE userId = :userId")
    suspend fun getTotalCompletedRuns(userId: Long): Int
    
    @Query("SELECT SUM(distance) FROM run_sessions WHERE userId = :userId")
    suspend fun getTotalDistance(userId: Long): Float?
    
    // Data class for aggregated stats
    data class UserRunStats(
        val totalDistance: Float?,
        val totalDuration: Long?,
        val totalCalories: Int?,
        val sessionCount: Int,
        val averagePace: Float?
    )
}