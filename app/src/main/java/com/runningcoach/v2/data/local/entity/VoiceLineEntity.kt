package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * [DATA-MODEL: VoiceLineEntity] Room entity for caching voice coaching lines
 * 
 * Stores generated voice audio files with metadata for offline access,
 * LRU cache management, and performance optimization.
 */
@Entity(
    tableName = "voice_lines",
    indices = [
        Index(value = ["cacheKey"], unique = true),
        Index(value = ["coachId"]),
        Index(value = ["urgency"]),
        Index(value = ["lastUsed"]),
        Index(value = ["createdAt"])
    ]
)
data class VoiceLineEntity(
    @PrimaryKey
    val cacheKey: String,              // Unique cache identifier (hash of content)
    
    val text: String,                  // Original text that was synthesized
    val coachId: String,               // Coach personality (bennett, mariana, becs, goggins)
    val urgency: String,               // Coaching urgency level (CALM, NORMAL, ENERGETIC, URGENT)
    
    val filePath: String,              // Absolute path to cached audio file
    val fileSize: Long,                // File size in bytes for cache management
    val version: Int = 1,              // Voice line version for updates/migration
    
    val createdAt: Long,               // Timestamp when voice line was created
    val lastUsed: Long,                // Last access timestamp for LRU eviction
    
    // Optional metadata for advanced caching
    val duration: Float = 0f,          // Audio duration in seconds
    val checksum: String? = null,      // File integrity check
    val category: String? = null,      // Category for grouping (milestone, pace, motivation)
    val priority: String? = null,      // Original priority level
    val apiLatency: Long = 0L,         // API generation latency for performance tracking
    
    // Usage statistics
    val useCount: Int = 1,             // How many times this voice line was played
    val lastError: String? = null,     // Last error encountered (if any)
    val isPreloaded: Boolean = false   // Whether this was preloaded or generated on-demand
)