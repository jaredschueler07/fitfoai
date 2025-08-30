package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.TrainingPlanEntity
import kotlinx.coroutines.flow.Flow

/**
 * Enhanced Data Access Object for TrainingPlanEntity operations
 * Provides CRUD operations and advanced queries for AI-generated training plans
 */
@Dao
interface TrainingPlanDao {
    
    // Basic CRUD operations
    @Insert
    suspend fun insertTrainingPlan(plan: TrainingPlanEntity): Long
    
    @Update
    suspend fun updateTrainingPlan(plan: TrainingPlanEntity)
    
    @Delete
    suspend fun deleteTrainingPlan(plan: TrainingPlanEntity)
    
    // Basic queries
    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getTrainingPlanById(planId: Long): TrainingPlanEntity?
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTrainingPlansByUser(userId: Long): Flow<List<TrainingPlanEntity>>
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTrainingPlansByUserSync(userId: Long): List<TrainingPlanEntity>
    
    // Active plan queries
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentActivePlan(userId: Long): TrainingPlanEntity?
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActivePlansByUser(userId: Long): Flow<List<TrainingPlanEntity>>
    
    // Date-based queries for enhanced features
    @Query("""
        SELECT * FROM training_plans 
        WHERE userId = :userId 
        AND :currentTime >= planStartDate 
        AND :currentTime <= planEndDate 
        AND isActive = 1
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    suspend fun getCurrentlyActivePlan(userId: Long, currentTime: Long = System.currentTimeMillis()): TrainingPlanEntity?
    
    @Query("""
        SELECT * FROM training_plans 
        WHERE userId = :userId 
        AND raceDate >= :startDate 
        AND raceDate <= :endDate 
        ORDER BY raceDate ASC
    """)
    fun getPlansByRaceDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<TrainingPlanEntity>>
    
    @Query("""
        SELECT * FROM training_plans 
        WHERE userId = :userId 
        AND planStartDate <= :date 
        AND planEndDate >= :date 
        ORDER BY createdAt DESC
    """)
    suspend fun getPlansActiveOnDate(userId: Long, date: Long): List<TrainingPlanEntity>
    
    // AI model and generation queries
    @Query("SELECT * FROM training_plans WHERE modelVersion = :modelVersion ORDER BY createdAt DESC")
    fun getPlansByModelVersion(modelVersion: String): Flow<List<TrainingPlanEntity>>
    
    @Query("SELECT DISTINCT modelVersion FROM training_plans ORDER BY modelVersion DESC")
    suspend fun getAllModelVersions(): List<String>
    
    // Race distance and time queries
    @Query("""
        SELECT * FROM training_plans 
        WHERE userId = :userId 
        AND targetDistance = :distance 
        ORDER BY createdAt DESC
    """)
    fun getPlansByTargetDistance(userId: Long, distance: Float): Flow<List<TrainingPlanEntity>>
    
    @Query("""
        SELECT * FROM training_plans 
        WHERE userId = :userId 
        AND targetTime IS NOT NULL 
        AND targetTime <= :maxTime 
        ORDER BY targetTime ASC
    """)
    fun getPlansByMaxTargetTime(userId: Long, maxTime: Long): Flow<List<TrainingPlanEntity>>
    
    // Update operations
    @Query("UPDATE training_plans SET isActive = 0, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun deactivateAllPlans(userId: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE training_plans SET isActive = 0, updatedAt = :updatedAt WHERE id = :planId")
    suspend fun deactivatePlan(planId: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE training_plans SET isActive = 1, updatedAt = :updatedAt WHERE id = :planId")
    suspend fun activatePlan(planId: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE training_plans 
        SET baselineStats = :baselineStats, updatedAt = :updatedAt 
        WHERE id = :planId
    """)
    suspend fun updateBaselineStats(planId: Long, baselineStats: String, updatedAt: Long = System.currentTimeMillis())
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM training_plans WHERE userId = :userId")
    suspend fun getPlanCountByUser(userId: Long): Int
    
    @Query("SELECT COUNT(*) FROM training_plans WHERE userId = :userId AND isActive = 1")
    suspend fun getActivePlanCount(userId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM training_plans 
        WHERE userId = :userId 
        AND :currentTime >= planStartDate 
        AND :currentTime <= planEndDate
    """)
    suspend fun getCurrentlyActivePlanCount(userId: Long, currentTime: Long = System.currentTimeMillis()): Int
}
