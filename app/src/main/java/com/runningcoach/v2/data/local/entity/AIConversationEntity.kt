package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversations")
data class AIConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT" // TEXT, VOICE, COACHING_TIP
)
