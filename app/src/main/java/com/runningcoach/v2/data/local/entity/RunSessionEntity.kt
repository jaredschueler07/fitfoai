package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing running sessions
 * Can be synced bidirectionally with Google Fit
 */
@Entity(
    tableName = "run_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["startTime"]),
        Index(value = ["googleFitSessionId"], unique = true),
        Index(value = ["syncedWithGoogleFit"]),
        Index(value = ["source"])
    ]
)
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    
    // Session timing
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long, // in milliseconds
    
    // Distance and pace
    val distance: Float, // in meters
    val avgSpeed: Float? = null, // m/s
    val maxSpeed: Float? = null, // m/s
    val avgPace: Float? = null, // min/km
    val bestPace: Float? = null, // min/km
    
    // Heart rate data
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val heartRateZones: String? = null, // JSON string with zone distribution
    
    // Elevation data
    val totalElevationGain: Float? = null, // meters
    val totalElevationLoss: Float? = null, // meters
    val maxElevation: Float? = null, // meters
    val minElevation: Float? = null, // meters
    
    // Calories and metrics
    val calories: Int? = null,
    val steps: Int? = null,
    val cadence: Int? = null, // steps per minute
    
    // Route data
    val route: String? = null, // JSON string with lat/lng points
    val routeName: String? = null,
    
    // Weather conditions (optional)
    val temperature: Float? = null, // Celsius
    val humidity: Int? = null, // percentage
    val weatherCondition: String? = null,
    
    // User notes and rating
    val notes: String? = null,
    val rating: Int? = null, // 1-5 stars
    val perceivedEffort: Int? = null, // 1-10 RPE scale
    
    // Training plan association
    val trainingPlanId: Long? = null,
    val workoutType: String? = null, // EASY, TEMPO, INTERVAL, LONG, RECOVERY
    
    // Data source tracking
    val source: DataSource = DataSource.FITFOAI,
    
    // Google Fit sync
    val googleFitSessionId: String? = null,
    val syncedWithGoogleFit: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncError: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)