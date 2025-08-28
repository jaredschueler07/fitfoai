package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [DATA-MODEL: CoachPersonalityEntity] Room entity for coach personality settings
 * 
 * Stores user preferences for each coach personality including voice settings,
 * usage statistics, and personalization data.
 */
@Entity(tableName = "coach_personalities")
data class CoachPersonalityEntity(
    @PrimaryKey
    val coachId: String,               // Coach identifier (bennett, mariana, becs, goggins)
    
    // Coach metadata
    val name: String,                  // Display name
    val description: String,           // Coach personality description
    val isEnabled: Boolean = true,     // Whether this coach is available
    val isSelected: Boolean = false,   // Whether this is the user's current coach
    
    // Voice settings
    val voiceId: String,              // ElevenLabs voice ID
    val stability: Float = 0.75f,     // Voice stability setting
    val similarityBoost: Float = 0.85f, // Voice similarity boost
    val style: Float = 0.0f,          // Voice style setting
    val useSpeakerBoost: Boolean = true, // Use speaker boost
    
    // Usage statistics
    val totalUseCount: Int = 0,        // How many times this coach has been used
    val lastUsed: Long? = null,        // Last time this coach was active
    val averageLatency: Float = 0f,    // Average API response time
    val successRate: Float = 100f,     // Success rate percentage
    
    // Personalization
    val preferredUrgency: String = "NORMAL", // User's preferred urgency level for this coach
    val customPhrases: String = "",    // JSON array of custom coaching phrases
    val voiceVolume: Float = 1.0f,     // Relative volume adjustment
    val speechRate: Float = 1.0f,      // Speech rate multiplier
    
    // Coaching behavior settings
    val motivationalFrequency: Int = 5,    // Minutes between motivational messages
    val paceWarningThreshold: Float = 0.5f, // Pace deviation threshold (min/km)
    val milestoneAnnouncements: Boolean = true, // Enable milestone celebrations
    val formReminders: Boolean = true,     // Enable form/technique reminders
    val encouragementLevel: Int = 3,       // 1-5 scale for encouragement frequency
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1               // Entity version for migrations
)