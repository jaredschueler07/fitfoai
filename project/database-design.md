# 🗄️ RunningCoach App - Database Design & Data Management

## 📋 Overview

This document outlines the comprehensive database design for the RunningCoach app, covering local run data storage, Google Fit integration, LLM inputs/outputs, fitness data management, and the voice line database. It addresses your specific requirements for storing comprehensive fitness data and AI interactions.

## 🏗️ Database Architecture

### System Overview
```
┌─────────────────────────────────────────────────────────────┐
│                    Database Layer                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Room        │ │ SharedPrefs │ │ File System │          │
│  │ Database    │ │ (Settings)  │ │ (Audio)     │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Repositories                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Run Data    │ │ Fitness     │ │ AI/LLM      │          │
│  │ Repository  │ │ Repository  │ │ Repository  │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    External Integrations                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Google Fit  │ │ Cloud       │ │ Analytics   │          │
│  │ Sync        │ │ Storage     │ │ Platform    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## 🏃‍♀️ Core Run Data Schema

### Data Models (Based on Wireframe Reference)
The wireframe reference provides TypeScript interfaces that inform our Kotlin data classes:

```typescript
// From wireframe types.ts
interface UserProfile {
  name: string;
  age?: number;
  height?: string;
  weight?: string;
  fitnessLevel?: FitnessLevel;
  connectedApps: ConnectedApp[];
  goal?: RaceGoal;
  coach: Coach;
}

interface Coach {
  id: string;
  name: string;
  style: string;
}

interface RaceGoal {
  name: string;
  date: string;
  distance: string;
  targetTime?: string;
}

type ConnectedApp = 'fitbit' | 'google_fit' | 'spotify';
```

### Primary Run Entity
```kotlin
@Entity(
    tableName = "runs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId", "startTime"]),
        Index(value = ["userId", "createdAt"]),
        Index(value = ["planId"]),
        Index(value = ["syncStatus"])
    ]
)
data class Run(
    @PrimaryKey val id: String,
    val userId: String,
    val planId: String? = null,
    
    // Basic Run Information
    val startTime: Long, // Unix timestamp
    val endTime: Long? = null,
    val duration: Long, // milliseconds
    val distance: Float, // in user's preferred units
    val averagePace: Float, // minutes per unit
    val calories: Int? = null,
    
    // GPS and Location Data
    val route: List<LocationPoint> = emptyList(),
    val totalElevationGain: Float = 0f,
    val totalElevationLoss: Float = 0f,
    val averageAltitude: Float? = null,
    
    // Performance Metrics
    val maxPace: Float? = null,
    val minPace: Float? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val heartRateZones: Map<String, Int> = emptyMap(),
    
    // Weather and Environmental Data
    val weatherConditions: WeatherInfo? = null,
    val temperature: Float? = null,
    val humidity: Float? = null,
    val windSpeed: Float? = null,
    
    // Run Type and Classification
    val runType: RunType = RunType.FREE_RUN,
    val terrain: TerrainType = TerrainType.ROAD,
    val difficulty: RunDifficulty = RunDifficulty.MODERATE,
    
    // User Feedback and Notes
    val userRating: Int? = null, // 1-5 stars
    val userNotes: String? = null,
    val perceivedEffort: PerceivedEffort? = null,
    
    // Coaching and AI Integration
    val coachingEvents: List<CoachingEvent> = emptyList(),
    val aiInsights: List<AIInsight> = emptyList(),
    val voiceLinesPlayed: List<VoiceLineEvent> = emptyList(),
    
    // Data Source and Sync
    val dataSource: DataSource = DataSource.LOCAL,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val externalId: String? = null, // Google Fit ID, etc.
    val lastSynced: Long? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

enum class RunType {
    FREE_RUN,
    TRAINING_RUN,
    RACE,
    RECOVERY_RUN,
    TEMPO_RUN,
    LONG_RUN,
    SPEED_WORKOUT,
    STRENGTH_TRAINING
}

enum class TerrainType {
    ROAD,
    TRAIL,
    TRACK,
    TREADMILL,
    BEACH,
    MOUNTAIN,
    URBAN
}

enum class RunDifficulty {
    EASY,
    MODERATE,
    HARD,
    VERY_HARD
}

enum class PerceivedEffort {
    VERY_EASY,
    EASY,
    MODERATE,
    HARD,
    VERY_HARD,
    MAXIMUM
}

enum class DataSource {
    LOCAL,
    GOOGLE_FIT,
    FITBIT,
    STRAVA,
    GARMIN,
    APPLE_HEALTH
}

enum class SyncStatus {
    NOT_SYNCED,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
    CONFLICT
}
```

### Location Points for GPS Tracking
```kotlin
@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = Run::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["runId", "timestamp"]),
        Index(value = ["runId", "sequence"])
    ]
)
data class LocationPoint(
    @PrimaryKey val id: String,
    val runId: String,
    val sequence: Int, // Order in the run
    
    // GPS Coordinates
    val latitude: Double,
    val longitude: Double,
    val altitude: Float? = null,
    val accuracy: Float? = null,
    
    // Timing
    val timestamp: Long, // Unix timestamp
    val elapsedTime: Long, // milliseconds from run start
    
    // Performance at this point
    val pace: Float? = null, // minutes per unit
    val speed: Float? = null, // units per hour
    val heartRate: Int? = null,
    val cadence: Int? = null, // steps per minute
    
    // Additional metrics
    val grade: Float? = null, // elevation grade percentage
    val temperature: Float? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)
```

### Run Segments for Detailed Analysis
```kotlin
@Entity(
    tableName = "run_segments",
    foreignKeys = [
        ForeignKey(
            entity = Run::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["runId", "startTime"]),
        Index(value = ["segmentType"])
    ]
)
data class RunSegment(
    @PrimaryKey val id: String,
    val runId: String,
    val segmentType: SegmentType,
    
    // Segment boundaries
    val startTime: Long, // Unix timestamp
    val endTime: Long, // Unix timestamp
    val duration: Long, // milliseconds
    
    // Distance and pace
    val distance: Float,
    val averagePace: Float,
    val maxPace: Float? = null,
    val minPace: Float? = null,
    
    // Heart rate data
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    
    // Elevation data
    val elevationGain: Float = 0f,
    val elevationLoss: Float = 0f,
    val averageGrade: Float? = null,
    
    // Performance metrics
    val calories: Int? = null,
    val cadence: Int? = null,
    val strideLength: Float? = null,
    
    // AI analysis
    val aiInsights: List<SegmentInsight> = emptyList(),
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)

enum class SegmentType {
    WARM_UP,
    MAIN_SET,
    COOL_DOWN,
    INTERVAL,
    RECOVERY,
    HILL_CLIMB,
    DOWNHILL,
    FLAT_TERRAIN,
    CUSTOM
}
```

## 💓 Heart Rate and Fitness Data

### Heart Rate Zones
```kotlin
@Entity(
    tableName = "heart_rate_zones",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "zoneType"])
    ]
)
data class HeartRateZone(
    @PrimaryKey val id: String,
    val userId: String,
    val zoneType: ZoneType,
    
    // Zone boundaries
    val minHeartRate: Int,
    val maxHeartRate: Int,
    
    // Zone characteristics
    val name: String,
    val description: String,
    val color: String, // Hex color code
    val intensity: ZoneIntensity,
    
    // Training benefits
    val trainingBenefit: String,
    val recommendedDuration: Int? = null, // minutes
    
    // Metadata
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ZoneType {
    ZONE_1, // Recovery
    ZONE_2, // Aerobic base
    ZONE_3, // Aerobic threshold
    ZONE_4, // Lactate threshold
    ZONE_5  // Anaerobic
}

enum class ZoneIntensity {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    MAXIMUM
}
```

### Fitness Metrics
```kotlin
@Entity(
    tableName = "fitness_metrics",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["userId", "metricType"])
    ]
)
data class FitnessMetric(
    @PrimaryKey val id: String,
    val userId: String,
    val date: Long, // Unix timestamp for the day
    
    // Metric type and value
    val metricType: FitnessMetricType,
    val value: Float,
    val unit: String,
    
    // Data source
    val dataSource: DataSource = DataSource.LOCAL,
    val externalId: String? = null,
    
    // Metadata
    val confidence: Float? = null, // 0.0 to 1.0
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class FitnessMetricType {
    // Heart rate metrics
    RESTING_HEART_RATE,
    MAX_HEART_RATE,
    HEART_RATE_VARIABILITY,
    
    // Activity metrics
    DAILY_STEPS,
    DAILY_DISTANCE,
    DAILY_CALORIES,
    DAILY_ACTIVE_MINUTES,
    
    // Sleep metrics
    SLEEP_DURATION,
    SLEEP_EFFICIENCY,
    DEEP_SLEEP_DURATION,
    REM_SLEEP_DURATION,
    
    // Training metrics
    TRAINING_LOAD,
    RECOVERY_TIME,
    VO2_MAX,
    LACTATE_THRESHOLD,
    
    // Body metrics
    WEIGHT,
    BODY_FAT_PERCENTAGE,
    MUSCLE_MASS,
    BODY_WATER_PERCENTAGE,
    
    // Performance metrics
    RUNNING_ECONOMY,
    PACE_AT_LACTATE_THRESHOLD,
    MAX_AEROBIC_SPEED
}
```

## 🤖 AI and LLM Data Storage

### LLM Interactions
```kotlin
@Entity(
    tableName = "llm_interactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "timestamp"]),
        Index(value = ["interactionType"]),
        Index(value = ["model"])
    ]
)
data class LLMInteraction(
    @PrimaryKey val id: String,
    val userId: String,
    val interactionType: LLMInteractionType,
    
    // Model information
    val model: String, // "gemini-pro", "gpt-4", etc.
    val modelVersion: String? = null,
    
    // Input data
    val inputPrompt: String,
    val inputContext: String? = null,
    val inputTokens: Int? = null,
    
    // Output data
    val outputResponse: String,
    val outputTokens: Int? = null,
    val outputConfidence: Float? = null,
    
    // Performance metrics
    val responseTime: Long? = null, // milliseconds
    val cost: Float? = null, // API cost in USD
    val success: Boolean = true,
    val errorMessage: String? = null,
    
    // Usage tracking
    val tokensUsed: Int? = null,
    val apiCalls: Int = 1,
    
    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class LLMInteractionType {
    TRAINING_PLAN_GENERATION,
    COACHING_MESSAGE,
    PERFORMANCE_ANALYSIS,
    PERSONAL_FITNESS_AGENT,
    VOICE_LINE_GENERATION,
    SMART_TRIGGER_ANALYSIS,
    GOAL_RECOMMENDATION,
    INJURY_PREVENTION_ADVICE
}
```

### AI Insights and Analysis
```kotlin
@Entity(
    tableName = "ai_insights",
    foreignKeys = [
        ForeignKey(
            entity = Run::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["runId", "insightType"]),
        Index(value = ["userId", "timestamp"])
    ]
)
data class AIInsight(
    @PrimaryKey val id: String,
    val runId: String? = null,
    val userId: String,
    val insightType: InsightType,
    
    // Insight content
    val title: String,
    val description: String,
    val recommendation: String? = null,
    
    // Metrics and data
    val metrics: Map<String, Float> = emptyMap(),
    val confidence: Float, // 0.0 to 1.0
    val priority: InsightPriority = InsightPriority.NORMAL,
    
    // AI model information
    val model: String,
    val modelVersion: String? = null,
    val llmInteractionId: String? = null,
    
    // User interaction
    val isRead: Boolean = false,
    val isActioned: Boolean = false,
    val userFeedback: InsightFeedback? = null,
    
    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InsightType {
    PACE_ANALYSIS,
    HEART_RATE_ANALYSIS,
    PERFORMANCE_TREND,
    TRAINING_LOAD_ANALYSIS,
    RECOVERY_RECOMMENDATION,
    INJURY_RISK_ASSESSMENT,
    GOAL_PROGRESS,
    WEATHER_IMPACT,
    TERRAIN_ANALYSIS,
    COACHING_EFFECTIVENESS
}

enum class InsightPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

enum class InsightFeedback {
    HELPFUL,
    NOT_HELPFUL,
    ALREADY_KNEW,
    TOO_GENERIC,
    TOO_SPECIFIC
}
```

### Coaching Events
```kotlin
@Entity(
    tableName = "coaching_events",
    foreignKeys = [
        ForeignKey(
            entity = Run::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["runId", "timestamp"]),
        Index(value = ["eventType"]),
        Index(value = ["coachId"])
    ]
)
data class CoachingEvent(
    @PrimaryKey val id: String,
    val runId: String? = null,
    val userId: String,
    val eventType: CoachingEventType,
    
    // Coach information
    val coachId: String,
    val coachName: String,
    val coachPersonality: CoachPersonality,
    
    // Event details
    val message: String,
    val voiceLineId: String? = null,
    val triggerContext: String? = null,
    
    // Timing and location
    val timestamp: Long, // When the event occurred
    val elapsedTime: Long? = null, // Time into the run
    val distance: Float? = null, // Distance at event
    
    // Performance context
    val currentPace: Float? = null,
    val targetPace: Float? = null,
    val heartRate: Int? = null,
    val energyLevel: EnergyLevel? = null,
    
    // User response
    val userResponse: UserResponse? = null,
    val effectiveness: Float? = null, // 0.0 to 1.0
    
    // AI integration
    val llmInteractionId: String? = null,
    val aiConfidence: Float? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)

enum class CoachingEventType {
    PACE_GUIDANCE,
    MOTIVATION,
    MILESTONE_CELEBRATION,
    SAFETY_WARNING,
    RECOVERY_ADVICE,
    PERFORMANCE_FEEDBACK,
    GOAL_REMINDER,
    WEATHER_ADVICE,
    TERRAIN_ADVICE,
    PERSONAL_FITNESS_AGENT_RESPONSE
}
```

## 🔗 Google Fit Integration

### Google Fit Sync Data
```kotlin
@Entity(
    tableName = "google_fit_sync",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "syncDate"]),
        Index(value = ["externalId"]),
        Index(value = ["syncStatus"])
    ]
)
data class GoogleFitSync(
    @PrimaryKey val id: String,
    val userId: String,
    val externalId: String, // Google Fit activity ID
    
    // Sync information
    val syncDate: Long, // Date of sync
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val lastSynced: Long? = null,
    val syncAttempts: Int = 0,
    val errorMessage: String? = null,
    
    // Activity data
    val activityType: String, // Google Fit activity type
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val distance: Float? = null,
    val calories: Int? = null,
    
    // Heart rate data
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val heartRateData: List<HeartRatePoint> = emptyList(),
    
    // Location data
    val locationData: List<LocationPoint> = emptyList(),
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class HeartRatePoint(
    val timestamp: Long,
    val heartRate: Int,
    val accuracy: Int? = null
)
```

### Google Fit Daily Summary
```kotlin
@Entity(
    tableName = "google_fit_daily_summary",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
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
data class GoogleFitDailySummary(
    @PrimaryKey val id: String,
    val userId: String,
    val date: Long, // Unix timestamp for the day
    
    // Activity summary
    val steps: Int? = null,
    val distance: Float? = null,
    val calories: Int? = null,
    val activeMinutes: Int? = null,
    
    // Heart rate summary
    val restingHeartRate: Int? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val heartRateVariability: Float? = null,
    
    // Sleep data
    val sleepDuration: Long? = null, // milliseconds
    val sleepEfficiency: Float? = null,
    val deepSleepDuration: Long? = null,
    val remSleepDuration: Long? = null,
    
    // Sync information
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val lastSynced: Long? = null,
    val syncAttempts: Int = 0,
    val errorMessage: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

## 🎵 Voice Line Database

### Voice Line Storage
```kotlin
@Entity(
    tableName = "voice_lines",
    indices = [
        Index(value = ["text", "voiceId"]),
        Index(value = ["category", "voiceId"]),
        Index(value = ["context", "voiceId"]),
        Index(value = ["usageCount"]),
        Index(value = ["lastUsed"])
    ]
)
data class VoiceLine(
    @PrimaryKey val id: String,
    val text: String,
    val voiceId: String,
    
    // Audio data
    val audioData: ByteArray,
    val duration: Long, // milliseconds
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val sampleRate: Int = 22050,
    
    // Categorization
    val category: VoiceLineCategory,
    val context: String,
    val tags: List<String> = emptyList(),
    
    // Usage tracking
    val usageCount: Int = 0,
    val lastUsed: Long = 0,
    val averageEffectiveness: Float? = null,
    
    // AI generation info
    val generatedBy: String? = null, // "elevenlabs", "ai_generated"
    val llmInteractionId: String? = null,
    val generationPrompt: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AudioFormat {
    MP3,
    WAV,
    AAC,
    OGG
}
```

### Voice Line Analytics
```kotlin
@Entity(
    tableName = "voice_line_analytics",
    foreignKeys = [
        ForeignKey(
            entity = VoiceLine::class,
            parentColumns = ["id"],
            childColumns = ["voiceLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["voiceLineId", "playedAt"]),
        Index(value = ["userId", "playedAt"]),
        Index(value = ["userResponse"])
    ]
)
data class VoiceLineAnalytics(
    @PrimaryKey val id: String,
    val voiceLineId: String,
    val userId: String,
    
    // Playback information
    val playedAt: Long,
    val context: String,
    val runId: String? = null,
    val coachingEventId: String? = null,
    
    // User response
    val userResponse: UserResponse? = null,
    val effectiveness: Float? = null, // 0.0 to 1.0
    val completionRate: Float = 1.0f, // 0.0 to 1.0
    
    // Performance metrics
    val playDuration: Long? = null, // milliseconds
    val wasInterrupted: Boolean = false,
    val interruptionReason: String? = null,
    
    // Context information
    val userEnergyLevel: EnergyLevel? = null,
    val userMotivationLevel: MotivationLevel? = null,
    val runPace: Float? = null,
    val runDistance: Float? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)
```

## 📊 Analytics and Performance Data

### Performance Trends
```kotlin
@Entity(
    tableName = "performance_trends",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "trendType", "period"]),
        Index(value = ["calculatedAt"])
    ]
)
data class PerformanceTrend(
    @PrimaryKey val id: String,
    val userId: String,
    val trendType: TrendType,
    val period: TrendPeriod,
    
    // Trend data
    val startDate: Long,
    val endDate: Long,
    val value: Float,
    val previousValue: Float? = null,
    val change: Float? = null, // percentage change
    val trend: TrendDirection,
    
    // Statistical data
    val mean: Float,
    val median: Float,
    val standardDeviation: Float? = null,
    val minValue: Float,
    val maxValue: Float,
    
    // Confidence and reliability
    val confidence: Float, // 0.0 to 1.0
    val dataPoints: Int,
    val reliability: Float, // 0.0 to 1.0
    
    // AI insights
    val aiInsight: String? = null,
    val recommendation: String? = null,
    
    // Metadata
    val calculatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class TrendType {
    AVERAGE_PACE,
    MAX_DISTANCE,
    TOTAL_DISTANCE,
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    TRAINING_LOAD,
    RECOVERY_TIME,
    VO2_MAX,
    RUNNING_ECONOMY
}

enum class TrendPeriod {
    WEEK,
    MONTH,
    QUARTER,
    YEAR
}

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING,
    UNKNOWN
}
```

## 🔧 Database Configuration

### Room Database Setup
```kotlin
@Database(
    entities = [
        User::class,
        Run::class,
        LocationPoint::class,
        RunSegment::class,
        HeartRateZone::class,
        FitnessMetric::class,
        LLMInteraction::class,
        AIInsight::class,
        CoachingEvent::class,
        GoogleFitSync::class,
        GoogleFitDailySummary::class,
        VoiceLine::class,
        VoiceLineAnalytics::class,
        PerformanceTrend::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RunningDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun userDao(): UserDao
    abstract fun runDao(): RunDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun runSegmentDao(): RunSegmentDao
    abstract fun heartRateZoneDao(): HeartRateZoneDao
    abstract fun fitnessMetricDao(): FitnessMetricDao
    abstract fun llmInteractionDao(): LLMInteractionDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun coachingEventDao(): CoachingEventDao
    abstract fun googleFitSyncDao(): GoogleFitSyncDao
    abstract fun googleFitDailySummaryDao(): GoogleFitDailySummaryDao
    abstract fun voiceLineDao(): VoiceLineDao
    abstract fun voiceLineAnalyticsDao(): VoiceLineAnalyticsDao
    abstract fun performanceTrendDao(): PerformanceTrendDao
    
    companion object {
        @Volatile
        private var INSTANCE: RunningDatabase? = null
        
        fun getDatabase(context: Context): RunningDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunningDatabase::class.java,
                    "running_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialize default data
                        CoroutineScope(Dispatchers.IO).launch {
                            initializeDefaultData()
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private suspend fun initializeDefaultData() {
            // Initialize default heart rate zones, etc.
        }
    }
}
```

### Type Converters
```kotlin
class Converters {
    
    @TypeConverter
    fun fromLocationPointList(value: List<LocationPoint>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toLocationPointList(value: String): List<LocationPoint> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromCoachingEventList(value: List<CoachingEvent>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toCoachingEventList(value: String): List<CoachingEvent> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromAIInsightList(value: List<AIInsight>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toAIInsightList(value: String): List<AIInsight> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromVoiceLineEventList(value: List<VoiceLineEvent>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toVoiceLineEventList(value: String): List<VoiceLineEvent> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromMap(value: Map<String, Float>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMap(value: String): Map<String, Float> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromHeartRatePointList(value: List<HeartRatePoint>): String {
        return Json.encodeToString(value)
    }
    
    @TypeConverter
    fun toHeartRatePointList(value: String): List<HeartRatePoint> {
        return Json.decodeFromString(value)
    }
}
```

## 📈 Data Migration and Backup

### Migration Strategy
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns or tables as needed
        database.execSQL("ALTER TABLE runs ADD COLUMN weatherConditions TEXT")
        database.execSQL("ALTER TABLE runs ADD COLUMN temperature REAL")
        database.execSQL("ALTER TABLE runs ADD COLUMN humidity REAL")
    }
}
```

### Backup and Restore
```kotlin
class DatabaseBackupManager(
    private val database: RunningDatabase,
    private val context: Context
) {
    
    suspend fun createBackup(): BackupResult {
        return try {
            val backupFile = File(context.getExternalFilesDir(null), "backup_${System.currentTimeMillis()}.db")
            
            database.openHelper.writableDatabase.use { sourceDb ->
                backupFile.outputStream().use { outputStream ->
                    sourceDb.query("SELECT * FROM sqlite_master").use { cursor ->
                        // Export database schema and data
                        exportDatabase(sourceDb, outputStream)
                    }
                }
            }
            
            BackupResult.Success(backupFile.absolutePath)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Backup failed")
        }
    }
    
    suspend fun restoreBackup(backupPath: String): RestoreResult {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return RestoreResult.Error("Backup file not found")
            }
            
            // Close current database
            database.close()
            
            // Copy backup to database location
            val dbFile = context.getDatabasePath("running_database")
            backupFile.copyTo(dbFile, overwrite = true)
            
            RestoreResult.Success
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Restore failed")
        }
    }
    
    private fun exportDatabase(sourceDb: SQLiteDatabase, outputStream: OutputStream) {
        // Implementation for database export
    }
}

sealed class BackupResult {
    data class Success(val backupPath: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    object Success : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
```

---

## ✅ IMPLEMENTATION STATUS UPDATE

### **🎯 Phase 2 Complete - Google Fit Integration**
The database design has been **successfully implemented** with the following entities now live in production:

#### **✅ Implemented Entities (Room v2)**
- **✅ UserEntity**: Complete user profile with Google Fit integration
- **✅ GoogleFitDailySummaryEntity**: Daily fitness data with offline caching
- **✅ ConnectedAppEntity**: App connection status tracking
- **✅ AIConversationEntity**: Chat history and AI interactions
- **✅ RunSessionEntity**: Basic run tracking (ready for GPS enhancement)
- **✅ TrainingPlanEntity**: Training plan management

#### **✅ Database Features Implemented**
- **✅ Room v2 Schema**: Successfully migrated from v1 to v2
- **✅ Foreign Key Relationships**: Proper entity relationships established
- **✅ Repository Pattern**: GoogleFitRepository, UserRepository implemented
- **✅ Data Persistence**: All user and fitness data survives app restarts
- **✅ Offline Caching**: Google Fit data cached for offline access
- **✅ Error Handling**: Comprehensive error logging and recovery

#### **🔄 Next Phase - Advanced Entities (Phase 3)**
The following entities from this design document are ready for implementation in Phase 3:
- **⏳ LocationPoint**: GPS tracking for run routes
- **⏳ RunSegment**: Detailed run analysis and coaching
- **⏳ HeartRateZone**: Personalized heart rate training zones
- **⏳ LLMInteraction**: Enhanced AI conversation tracking
- **⏳ VoiceLine**: Voice coaching system
- **⏳ PerformanceTrend**: Advanced analytics and insights

### **🏗️ Current Database Architecture (Live)**
```
✅ FITFOAI Database v2 (Production Ready)
├── ✅ users (UserEntity)
├── ✅ google_fit_daily_summary (GoogleFitDailySummaryEntity)
├── ✅ connected_apps (ConnectedAppEntity)
├── ✅ ai_conversations (AIConversationEntity)
├── ✅ run_sessions (RunSessionEntity)
└── ✅ training_plans (TrainingPlanEntity)

⏳ Phase 3 Extensions (Ready to Implement)
├── ⏳ location_points (GPS tracking)
├── ⏳ run_segments (Advanced analysis)
├── ⏳ heart_rate_zones (Personalized training)
├── ⏳ llm_interactions (Enhanced AI)
├── ⏳ voice_lines (Voice coaching)
└── ⏳ performance_trends (Analytics)
```

---

**Document Version**: 2.0 - Updated with Phase 2 Implementation Status  
**Last Updated**: January 2025 - Google Fit Integration Complete  
**Next Review**: February 2025 - Phase 3 GPS Tracking Implementation
