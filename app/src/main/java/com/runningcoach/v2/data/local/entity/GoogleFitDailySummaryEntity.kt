package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "google_fit_daily_summary",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["syncStatus"])
    ]
)
data class GoogleFitDailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long, // Unix timestamp for the day (midnight)
    
    // Activity summary
    val steps: Int? = null,
    val distance: Float? = null, // in meters
    val calories: Int? = null,
    val activeMinutes: Int? = null,
    
    // Heart rate summary
    val restingHeartRate: Int? = null,
    val avgHeartRate: Float? = null,
    val maxHeartRate: Float? = null,
    val minHeartRate: Float? = null,
    
    // Additional metrics
    val heartPoints: Int? = null,
    
    // Body measurements
    val weight: Float? = null, // in kg
    val height: Float? = null, // in meters
    
    // Sync information
    val syncStatus: String = "NOT_SYNCED", // NOT_SYNCED, SYNCING, SYNCED, FAILED
    val lastSynced: Long? = null,
    val errorMessage: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
