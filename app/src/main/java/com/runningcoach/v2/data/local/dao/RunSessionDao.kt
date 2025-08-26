package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {
    @Query("SELECT * FROM run_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getRunSessionsByUser(userId: Long): Flow<List<RunSessionEntity>>
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedRuns(userId: Long, limit: Int = 10): Flow<List<RunSessionEntity>>
    
    @Query("SELECT * FROM run_sessions WHERE id = :sessionId")
    suspend fun getRunSessionById(sessionId: Long): RunSessionEntity?
    
    @Query("SELECT * FROM run_sessions WHERE userId = :userId AND isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentActiveSession(userId: Long): RunSessionEntity?
    
    @Insert
    suspend fun insertRunSession(session: RunSessionEntity): Long
    
    @Update
    suspend fun updateRunSession(session: RunSessionEntity)
    
    @Delete
    suspend fun deleteRunSession(session: RunSessionEntity)
    
    @Query("SELECT COUNT(*) FROM run_sessions WHERE userId = :userId AND isCompleted = 1")
    suspend fun getTotalCompletedRuns(userId: Long): Int
    
    @Query("SELECT SUM(distance) FROM run_sessions WHERE userId = :userId AND isCompleted = 1")
    suspend fun getTotalDistance(userId: Long): Float?
}
