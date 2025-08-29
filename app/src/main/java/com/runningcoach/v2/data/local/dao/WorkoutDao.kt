package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.WorkoutEntity
import com.runningcoach.v2.data.local.entity.WorkoutStatus
import com.runningcoach.v2.data.local.entity.WorkoutType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkoutEntity operations
 * Provides CRUD operations and relationship queries for workout management
 */
@Dao
interface WorkoutDao {
    
    // Basic CRUD operations
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long
    
    @Insert
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>): List<Long>
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)
    
    @Query("DELETE FROM workouts WHERE planId = :planId")
    suspend fun deleteWorkoutsByPlan(planId: Long)
    
    // Basic queries
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): WorkoutEntity?
    
    @Query("SELECT * FROM workouts WHERE planId = :planId ORDER BY scheduledDate ASC")
    fun getWorkoutsByPlan(planId: Long): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE planId = :planId ORDER BY scheduledDate ASC")
    suspend fun getWorkoutsByPlanSync(planId: Long): List<WorkoutEntity>
    
    // Status-based queries
    @Query("SELECT * FROM workouts WHERE planId = :planId AND status = :status ORDER BY scheduledDate ASC")
    fun getWorkoutsByPlanAndStatus(planId: Long, status: WorkoutStatus): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE planId = :planId AND status = 'PENDING' ORDER BY scheduledDate ASC")
    fun getPendingWorkoutsByPlan(planId: Long): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE planId = :planId AND status = 'COMPLETED' ORDER BY scheduledDate ASC")
    fun getCompletedWorkoutsByPlan(planId: Long): Flow<List<WorkoutEntity>>
    
    // Date-based queries
    @Query("SELECT * FROM workouts WHERE planId = :planId AND scheduledDate >= :startDate AND scheduledDate <= :endDate ORDER BY scheduledDate ASC")
    fun getWorkoutsByPlanAndDateRange(planId: Long, startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE scheduledDate >= :startDate AND scheduledDate <= :endDate ORDER BY scheduledDate ASC")
    fun getWorkoutsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE planId = :planId AND scheduledDate = :date ORDER BY scheduledDate ASC")
    suspend fun getWorkoutsForDate(planId: Long, date: Long): List<WorkoutEntity>
    
    // Type-based queries
    @Query("SELECT * FROM workouts WHERE planId = :planId AND type = :type ORDER BY scheduledDate ASC")
    fun getWorkoutsByPlanAndType(planId: Long, type: WorkoutType): Flow<List<WorkoutEntity>>
    
    // Relationship queries with RunSession
    @Query("""
        SELECT w.* FROM workouts w 
        INNER JOIN run_sessions rs ON w.actualRunSessionId = rs.id 
        WHERE w.planId = :planId AND w.status = 'COMPLETED' 
        ORDER BY w.scheduledDate ASC
    """)
    fun getCompletedWorkoutsWithSessions(planId: Long): Flow<List<WorkoutEntity>>
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM workouts WHERE planId = :planId")
    suspend fun getWorkoutCountByPlan(planId: Long): Int
    
    @Query("SELECT COUNT(*) FROM workouts WHERE planId = :planId AND status = :status")
    suspend fun getWorkoutCountByPlanAndStatus(planId: Long, status: WorkoutStatus): Int
    
    @Query("SELECT COUNT(*) FROM workouts WHERE planId = :planId AND status = 'COMPLETED'")
    suspend fun getCompletedWorkoutCount(planId: Long): Int
    
    @Query("SELECT COUNT(*) FROM workouts WHERE planId = :planId AND status = 'PENDING'")
    suspend fun getPendingWorkoutCount(planId: Long): Int
    
    // Update operations
    @Query("UPDATE workouts SET status = :status, updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun updateWorkoutStatus(workoutId: Long, status: WorkoutStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE workouts SET actualRunSessionId = :runSessionId, status = 'COMPLETED', updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun completeWorkout(workoutId: Long, runSessionId: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE workouts SET notes = :notes, updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun updateWorkoutNotes(workoutId: Long, notes: String?, updatedAt: Long = System.currentTimeMillis())
}