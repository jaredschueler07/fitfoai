package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.TrainingPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTrainingPlansByUser(userId: Long): Flow<List<TrainingPlanEntity>>
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentActivePlan(userId: Long): TrainingPlanEntity?
    
    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getTrainingPlanById(planId: Long): TrainingPlanEntity?
    
    @Insert
    suspend fun insertTrainingPlan(plan: TrainingPlanEntity): Long
    
    @Update
    suspend fun updateTrainingPlan(plan: TrainingPlanEntity)
    
    @Delete
    suspend fun deleteTrainingPlan(plan: TrainingPlanEntity)
    
    @Query("UPDATE training_plans SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllPlans(userId: Long)
}
