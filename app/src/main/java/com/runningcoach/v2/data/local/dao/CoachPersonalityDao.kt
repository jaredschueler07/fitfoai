package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.CoachPersonalityEntity
import kotlinx.coroutines.flow.Flow

/**
 * [BACKEND-UPDATE] DAO for coach personality settings
 */
@Dao
interface CoachPersonalityDao {
    
    // Basic CRUD operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoachPersonality(coach: CoachPersonalityEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoachPersonalities(coaches: List<CoachPersonalityEntity>)
    
    @Update
    suspend fun updateCoachPersonality(coach: CoachPersonalityEntity)
    
    @Delete
    suspend fun deleteCoachPersonality(coach: CoachPersonalityEntity)
    
    // Query operations
    
    @Query("SELECT * FROM coach_personalities WHERE coachId = :coachId")
    suspend fun getCoachPersonality(coachId: String): CoachPersonalityEntity?
    
    @Query("SELECT * FROM coach_personalities WHERE isEnabled = 1 ORDER BY name")
    suspend fun getAllEnabledCoaches(): List<CoachPersonalityEntity>
    
    @Query("SELECT * FROM coach_personalities ORDER BY name")
    suspend fun getAllCoaches(): List<CoachPersonalityEntity>
    
    @Query("SELECT * FROM coach_personalities WHERE isSelected = 1")
    suspend fun getSelectedCoach(): CoachPersonalityEntity?
    
    @Query("SELECT coachId FROM coach_personalities WHERE isSelected = 1")
    suspend fun getSelectedCoachId(): String?
    
    // Update operations
    
    @Query("UPDATE coach_personalities SET isSelected = 0")
    suspend fun deselectAllCoaches()
    
    @Query("UPDATE coach_personalities SET isSelected = 1 WHERE coachId = :coachId")
    suspend fun selectCoach(coachId: String)
    
    @Query("UPDATE coach_personalities SET isEnabled = :enabled WHERE coachId = :coachId")
    suspend fun setCoachEnabled(coachId: String, enabled: Boolean)
    
    @Query("""
        UPDATE coach_personalities 
        SET totalUseCount = totalUseCount + 1, 
            lastUsed = :timestamp,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun incrementUseCount(coachId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE coach_personalities 
        SET averageLatency = (averageLatency * totalUseCount + :latency) / (totalUseCount + 1)
        WHERE coachId = :coachId
    """)
    suspend fun updateLatency(coachId: String, latency: Float)
    
    @Query("""
        UPDATE coach_personalities 
        SET successRate = :successRate,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updateSuccessRate(coachId: String, successRate: Float, timestamp: Long = System.currentTimeMillis())
    
    // Voice settings updates
    
    @Query("""
        UPDATE coach_personalities 
        SET stability = :stability,
            similarityBoost = :similarityBoost,
            style = :style,
            useSpeakerBoost = :useSpeakerBoost,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updateVoiceSettings(
        coachId: String,
        stability: Float,
        similarityBoost: Float,
        style: Float,
        useSpeakerBoost: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        UPDATE coach_personalities 
        SET voiceVolume = :volume,
            speechRate = :speechRate,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updateAudioSettings(
        coachId: String,
        volume: Float,
        speechRate: Float,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Coaching behavior updates
    
    @Query("""
        UPDATE coach_personalities 
        SET motivationalFrequency = :frequency,
            paceWarningThreshold = :threshold,
            milestoneAnnouncements = :milestones,
            formReminders = :formReminders,
            encouragementLevel = :encouragement,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updateCoachingBehavior(
        coachId: String,
        frequency: Int,
        threshold: Float,
        milestones: Boolean,
        formReminders: Boolean,
        encouragement: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        UPDATE coach_personalities 
        SET customPhrases = :phrases,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updateCustomPhrases(
        coachId: String,
        phrases: String,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        UPDATE coach_personalities 
        SET preferredUrgency = :urgency,
            updatedAt = :timestamp
        WHERE coachId = :coachId
    """)
    suspend fun updatePreferredUrgency(
        coachId: String,
        urgency: String,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Statistics queries
    
    @Query("""
        SELECT coachId, totalUseCount, lastUsed, averageLatency, successRate
        FROM coach_personalities 
        WHERE isEnabled = 1
        ORDER BY totalUseCount DESC
    """)
    suspend fun getUsageStatistics(): List<CoachUsageStats>
    
    @Query("SELECT coachId FROM coach_personalities ORDER BY totalUseCount DESC LIMIT 1")
    suspend fun getMostUsedCoach(): String?
    
    @Query("SELECT coachId FROM coach_personalities WHERE lastUsed IS NOT NULL ORDER BY lastUsed DESC LIMIT 1")
    suspend fun getMostRecentlyUsedCoach(): String?
    
    @Query("""
        SELECT AVG(averageLatency) as avgLatency,
               MIN(averageLatency) as minLatency,
               MAX(averageLatency) as maxLatency,
               AVG(successRate) as avgSuccessRate
        FROM coach_personalities 
        WHERE isEnabled = 1
    """)
    suspend fun getOverallPerformanceStats(): PerformanceStats?
    
    // Reactive queries for UI
    
    @Query("SELECT * FROM coach_personalities WHERE isEnabled = 1 ORDER BY name")
    fun observeEnabledCoaches(): Flow<List<CoachPersonalityEntity>>
    
    @Query("SELECT * FROM coach_personalities WHERE isSelected = 1")
    fun observeSelectedCoach(): Flow<CoachPersonalityEntity?>
    
    @Query("SELECT coachId FROM coach_personalities WHERE isSelected = 1")
    fun observeSelectedCoachId(): Flow<String?>
    
    @Query("SELECT * FROM coach_personalities WHERE coachId = :coachId")
    fun observeCoachPersonality(coachId: String): Flow<CoachPersonalityEntity?>
    
    // Utility methods for coach selection
    
    @Transaction
    suspend fun selectNewCoach(newCoachId: String) {
        deselectAllCoaches()
        selectCoach(newCoachId)
    }
    
    @Transaction
    suspend fun resetCoachToDefaults(coachId: String) {
        val defaultCoach = getCoachPersonality(coachId)?.copy(
            stability = 0.75f,
            similarityBoost = 0.85f,
            style = 0.0f,
            useSpeakerBoost = true,
            voiceVolume = 1.0f,
            speechRate = 1.0f,
            motivationalFrequency = 5,
            paceWarningThreshold = 0.5f,
            milestoneAnnouncements = true,
            formReminders = true,
            encouragementLevel = 3,
            customPhrases = "",
            preferredUrgency = "NORMAL",
            updatedAt = System.currentTimeMillis()
        )
        
        defaultCoach?.let { updateCoachPersonality(it) }
    }
    
    // Initialization and setup
    
    @Query("SELECT COUNT(*) FROM coach_personalities")
    suspend fun getCoachCount(): Int
    
    suspend fun initializeDefaultCoaches() {
        val defaultCoaches = listOf(
            CoachPersonalityEntity(
                coachId = "bennett",
                name = "Bennett",
                description = "Professional, encouraging, data-driven",
                voiceId = "pNInz6obpgDQGcFmaJgB",
                stability = 0.85f,
                similarityBoost = 0.80f,
                style = 0.1f,
                isSelected = true
            ),
            CoachPersonalityEntity(
                coachId = "mariana",
                name = "Mariana",
                description = "Energetic, motivational, upbeat",
                voiceId = "EXAVITQu4vr4xnSDxMaL",
                stability = 0.70f,
                similarityBoost = 0.90f,
                style = 0.4f
            ),
            CoachPersonalityEntity(
                coachId = "becs",
                name = "Becs",
                description = "Calm, supportive, mindful",
                voiceId = "oWAxZDx7w5VEj9dCyTzz",
                stability = 0.90f,
                similarityBoost = 0.75f,
                style = 0.0f
            ),
            CoachPersonalityEntity(
                coachId = "goggins",
                name = "Goggins",
                description = "Intense, challenging, no-excuses",
                voiceId = "VR6AewLTigWG4xSOukaG",
                stability = 0.65f,
                similarityBoost = 0.95f,
                style = 0.6f
            )
        )
        
        insertCoachPersonalities(defaultCoaches)
    }
    
    // Data classes for query results
    
    data class CoachUsageStats(
        val coachId: String,
        val totalUseCount: Int,
        val lastUsed: Long?,
        val averageLatency: Float,
        val successRate: Float
    )
    
    data class PerformanceStats(
        val avgLatency: Float,
        val minLatency: Float,
        val maxLatency: Float,
        val avgSuccessRate: Float
    )
}