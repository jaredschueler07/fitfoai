package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.VoiceLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data class for simple coach cache count
 */
data class CoachCacheCount(
    val coachId: String,
    val count: Int
)

/**
 * [BACKEND-UPDATE] DAO for voice line caching operations
 */
@Dao
interface VoiceLineDao {
    
    // Basic CRUD operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceLine(voiceLine: VoiceLineEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceLines(voiceLines: List<VoiceLineEntity>)
    
    @Update
    suspend fun updateVoiceLine(voiceLine: VoiceLineEntity)
    
    @Delete
    suspend fun deleteVoiceLine(voiceLine: VoiceLineEntity)
    
    // Query operations
    
    @Query("SELECT * FROM voice_lines WHERE cacheKey = :cacheKey")
    suspend fun getVoiceLineByKey(cacheKey: String): VoiceLineEntity?
    
    @Query("SELECT * FROM voice_lines WHERE coachId = :coachId ORDER BY lastUsed DESC")
    suspend fun getVoiceLinesByCoach(coachId: String): List<VoiceLineEntity>
    
    @Query("SELECT * FROM voice_lines WHERE urgency = :urgency ORDER BY lastUsed DESC")
    suspend fun getVoiceLinesByUrgency(urgency: String): List<VoiceLineEntity>
    
    @Query("SELECT * FROM voice_lines WHERE text = :text AND coachId = :coachId")
    suspend fun getVoiceLineByTextAndCoach(text: String, coachId: String): VoiceLineEntity?
    
    @Query("SELECT * FROM voice_lines ORDER BY createdAt DESC")
    suspend fun getAllVoiceLines(): List<VoiceLineEntity>
    
    @Query("SELECT * FROM voice_lines WHERE isPreloaded = 1 ORDER BY coachId, urgency")
    suspend fun getPreloadedVoiceLines(): List<VoiceLineEntity>
    
    // Cache management queries
    
    @Query("SELECT * FROM voice_lines ORDER BY lastUsed ASC LIMIT :limit")
    suspend fun getOldestEntries(limit: Int): List<VoiceLineEntity>
    
    @Query("SELECT * FROM voice_lines ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getMostRecentEntries(limit: Int): List<VoiceLineEntity>
    
    @Query("SELECT COUNT(*) FROM voice_lines")
    suspend fun getTotalCacheEntries(): Int
    
    @Query("SELECT SUM(fileSize) FROM voice_lines")
    suspend fun getTotalCacheSize(): Long
    
    @Query("SELECT coachId, COUNT(*) as count FROM voice_lines GROUP BY coachId")
    suspend fun getCacheEntriesByCoach(): List<CoachCacheCount>
    
    @Query("SELECT MIN(createdAt) FROM voice_lines")
    suspend fun getOldestCacheEntry(): Long?
    
    @Query("SELECT MAX(createdAt) FROM voice_lines")
    suspend fun getNewestCacheEntry(): Long?
    
    @Query("SELECT filePath FROM voice_lines")
    suspend fun getAllFilePaths(): List<String>
    
    // Update operations
    
    @Query("UPDATE voice_lines SET lastUsed = :timestamp, useCount = useCount + 1 WHERE cacheKey = :cacheKey")
    suspend fun updateLastUsed(cacheKey: String, timestamp: Long)
    
    @Query("UPDATE voice_lines SET useCount = useCount + 1 WHERE cacheKey = :cacheKey")
    suspend fun incrementUseCount(cacheKey: String)
    
    @Query("UPDATE voice_lines SET lastError = :error WHERE cacheKey = :cacheKey")
    suspend fun updateLastError(cacheKey: String, error: String?)
    
    // Delete operations
    
    @Query("DELETE FROM voice_lines WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)
    
    @Query("DELETE FROM voice_lines WHERE coachId = :coachId")
    suspend fun deleteByCoach(coachId: String): Int
    
    @Query("DELETE FROM voice_lines WHERE urgency = :urgency")
    suspend fun deleteByUrgency(urgency: String): Int
    
    @Query("DELETE FROM voice_lines WHERE lastUsed < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM voice_lines WHERE fileSize > :maxSize")
    suspend fun deleteLargerThan(maxSize: Long): Int
    
    @Query("DELETE FROM voice_lines")
    suspend fun deleteAllVoiceLines(): Int
    
    // Statistics queries
    
    @Query("""
        SELECT coachId, 
               COUNT(*) as totalEntries,
               SUM(fileSize) as totalSize,
               MAX(lastUsed) as lastUsed,
               AVG(apiLatency) as avgLatency,
               SUM(useCount) as totalUses
        FROM voice_lines 
        GROUP BY coachId
    """)
    suspend fun getCoachStatistics(): List<CoachCacheStats>
    
    @Query("""
        SELECT urgency,
               COUNT(*) as count,
               AVG(fileSize) as avgSize,
               AVG(duration) as avgDuration
        FROM voice_lines 
        GROUP BY urgency
    """)
    suspend fun getUrgencyStatistics(): List<UrgencyStats>
    
    @Query("""
        SELECT DATE(createdAt/1000, 'unixepoch') as date,
               COUNT(*) as entriesCreated,
               SUM(fileSize) as bytesCreated
        FROM voice_lines 
        WHERE createdAt > :sinceTimestamp
        GROUP BY DATE(createdAt/1000, 'unixepoch')
        ORDER BY date DESC
    """)
    suspend fun getDailyCreationStats(sinceTimestamp: Long): List<DailyStats>
    
    // Search queries
    
    @Query("SELECT * FROM voice_lines WHERE text LIKE '%' || :searchText || '%' ORDER BY lastUsed DESC")
    suspend fun searchByText(searchText: String): List<VoiceLineEntity>
    
    @Query("""
        SELECT * FROM voice_lines 
        WHERE coachId = :coachId AND urgency = :urgency 
        ORDER BY useCount DESC, lastUsed DESC
    """)
    suspend fun getMostUsedForCoachAndUrgency(coachId: String, urgency: String): List<VoiceLineEntity>
    
    // Flow-based reactive queries for UI
    
    @Query("SELECT * FROM voice_lines WHERE coachId = :coachId ORDER BY lastUsed DESC")
    fun observeVoiceLinesByCoach(coachId: String): Flow<List<VoiceLineEntity>>
    
    @Query("SELECT COUNT(*) FROM voice_lines")
    fun observeCacheEntryCount(): Flow<Int>
    
    @Query("SELECT SUM(fileSize) FROM voice_lines")
    fun observeCacheSize(): Flow<Long>
    
    @Query("""
        SELECT coachId, COUNT(*) as count 
        FROM voice_lines 
        GROUP BY coachId
    """)
    fun observeCacheEntriesByCoach(): Flow<List<CoachCacheCount>>
    
    // Maintenance queries
    
    @Query("""
        SELECT cacheKey FROM voice_lines 
        WHERE lastUsed < :threshold 
        ORDER BY lastUsed ASC 
        LIMIT :limit
    """)
    suspend fun getEntriesForEviction(threshold: Long, limit: Int): List<String>
    
    @Query("""
        SELECT * FROM voice_lines 
        WHERE createdAt < :threshold AND useCount = 0
    """)
    suspend fun getUnusedOldEntries(threshold: Long): List<VoiceLineEntity>
    
    @Query("SELECT COUNT(*) FROM voice_lines WHERE lastError IS NOT NULL")
    suspend fun getErrorCount(): Int
    
    @Query("DELETE FROM voice_lines WHERE lastError IS NOT NULL")
    suspend fun deleteErrorEntries(): Int
    
    // Data classes for query results
    data class CoachCacheStats(
        val coachId: String,
        val totalEntries: Int,
        val totalSize: Long,
        val lastUsed: Long?,
        val avgLatency: Float,
        val totalUses: Int
    )
    
    data class UrgencyStats(
        val urgency: String,
        val count: Int,
        val avgSize: Float,
        val avgDuration: Float
    )
    
    data class DailyStats(
        val date: String,
        val entriesCreated: Int,
        val bytesCreated: Long
    )
}