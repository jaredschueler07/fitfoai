package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.AIConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY timestamp ASC")
    fun getConversationHistory(userId: Long): Flow<List<AIConversationEntity>>
    
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(userId: Long, limit: Int = 50): Flow<List<AIConversationEntity>>
    
    @Insert
    suspend fun insertMessage(message: AIConversationEntity): Long
    
    @Delete
    suspend fun deleteMessage(message: AIConversationEntity)
    
    @Query("DELETE FROM ai_conversations WHERE userId = :userId")
    suspend fun clearConversationHistory(userId: Long)
}
