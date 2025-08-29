# 🔌 RunningCoach App - API Reference
## ✅ Phase 2 Complete - Production APIs

## 📋 Overview

This document provides comprehensive API specifications for all external integrations and internal service interfaces **successfully implemented** in the RunningCoach app. All Phase 2 APIs are production-ready with complete Google Fit integration, error handling, and offline caching.

## ✅ IMPLEMENTED APIs - Production Ready

### 🎯 Google Fit API (COMPLETE)

#### ✅ GoogleFitService - Core Integration
```kotlin
class GoogleFitService(private val context: Context) {
    
    // ✅ Connection Management
    fun initiateConnection(): Intent
    fun checkConnectionStatus()
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    fun disconnect()
    
    // ✅ Data Retrieval
    suspend fun getDailySteps(): Result<Int>
    suspend fun getWeeklySteps(): Result<List<DailyStepsData>>
    suspend fun getHeartRateData(hoursBack: Int = 24): Result<List<HeartRateData>>
    suspend fun getLatestWeight(): Result<Float?>
    suspend fun getLatestHeight(): Result<Float?>
    suspend fun getComprehensiveFitnessData(): Result<FitnessData>
    
    // ✅ Connection State
    val isConnected: StateFlow<Boolean>
    val connectionStatus: StateFlow<String>
}
```

#### ✅ GoogleFitRepository - Data Management
```kotlin
class GoogleFitRepository(
    private val context: Context,
    private val database: FITFOAIDatabase
) {
    // ✅ Connection Management
    fun connectGoogleFit(): Intent
    suspend fun isGoogleFitConnected(): Boolean
    suspend fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    suspend fun disconnect()
    
    // ✅ Data Synchronization
    suspend fun syncTodaysFitnessData(): Result<GoogleFitDailySummaryEntity>
    suspend fun getTodaysFitnessData(): GoogleFitDailySummaryEntity?
    
    // ✅ Connection Status Management
    private suspend fun updateConnectionStatus(userId: Long, isConnected: Boolean)
}
```

#### ✅ Data Models
```kotlin
// ✅ Fitness Data Structure
data class FitnessData(
    val steps: Int = 0,
    val distance: Float = 0f, // in meters
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val heartRate: Float? = null,
    val weight: Float? = null, // in kg
    val height: Float? = null // in meters
)

// ✅ Daily Steps Data
data class DailyStepsData(
    val date: String,
    val steps: Int
)

// ✅ Heart Rate Data
data class HeartRateData(
    val timestamp: Long,
    val bpm: Float
)
```

## 🤖 AI Services (IMPLEMENTED)

### ✅ Google Gemini API (READY)

#### ✅ GeminiService - AI Integration
```kotlin
class GeminiService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    // ✅ Core AI Functions
    suspend fun generateCoachingMessage(
        context: CoachingContext
    ): Result<String>
    
    suspend fun analyzePerformance(
        runs: List<RunData>
    ): Result<PerformanceAnalysis>
    
    suspend fun generateTrainingPlan(
        userProfile: UserProfile,
        goal: RunningGoal
    ): Result<TrainingPlan>
    
    // ✅ Connection Testing
    suspend fun testConnection(): Result<Boolean>
}
```

#### Request Models
```kotlin
data class PlanGenerationRequest(
    val userProfile: UserProfile,
    val goal: RunningGoal,
    val preferences: TrainingPreferences
)

data class UserProfile(
    val name: String,
    val age: Int,
    val gender: Gender,
    val weight: Float,
    val height: Float,
    val experienceLevel: ExperienceLevel,
    val currentFitness: FitnessLevel,
    val injuries: List<String>,
    val preferences: List<String>
)

data class RunningGoal(
    val distance: RaceDistance,
    val targetTime: Duration,
    val raceDate: LocalDate,
    val priority: GoalPriority
)

enum class RaceDistance {
    FIVE_K, TEN_K, HALF_MARATHON, MARATHON
}

enum class GoalPriority {
    COMPLETE, COMPETITIVE, PERSONAL_BEST
}
```

#### Response Models
```kotlin
data class TrainingPlan(
    val id: String,
    val goal: RunningGoal,
    val weeks: List<TrainingWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val adaptations: List<PlanAdaptation>
)

data class TrainingWeek(
    val weekNumber: Int,
    val runs: List<TrainingRun>,
    val totalDistance: Float,
    val totalTime: Duration,
    val intensity: TrainingIntensity
)

data class TrainingRun(
    val dayOfWeek: DayOfWeek,
    val type: RunType,
    val distance: Float,
    val duration: Duration,
    val pace: Pace,
    val description: String,
    val coachingNotes: String
)

enum class RunType {
    EASY_RUN, TEMPO_RUN, LONG_RUN, SPEED_WORK, REST, CROSS_TRAINING
}
```

#### Error Handling
```kotlin
sealed class AIError : Exception() {
    object NetworkError : AIError()
    object RateLimitExceeded : AIError()
    object InvalidRequest : AIError()
    object ServiceUnavailable : AIError()
    data class ApiError(val code: Int, val message: String) : AIError()
}

class AIServiceException(
    val error: AIError,
    override val message: String
) : Exception(message)
```

### Coaching Message Generation

#### Context Models
```kotlin
data class CoachingContext(
    val runType: RunType,
    val currentPace: Pace,
    val targetPace: Pace,
    val distance: Float,
    val elapsedTime: Duration,
    val remainingDistance: Float,
    val userState: UserState,
    val coachPersonality: CoachPersonality,
    val previousMessages: List<CoachingMessage>
)

data class UserState(
    val energyLevel: EnergyLevel,
    val motivation: MotivationLevel,
    val fatigue: FatigueLevel,
    val performance: PerformanceLevel
)

enum class EnergyLevel {
    HIGH, MEDIUM, LOW, EXHAUSTED
}

enum class MotivationLevel {
    MOTIVATED, NEUTRAL, STRUGGLING, DISCOURAGED
}
```

## 🔊 Text-to-Speech Services

### ElevenLabs TTS API

#### Voice Synthesis
```kotlin
interface ElevenLabsTTSService {
    suspend fun synthesizeSpeech(
        text: String,
        voice: Voice,
        settings: TTSSettings
    ): AudioData
    
    suspend fun getAvailableVoices(): List<Voice>
    
    suspend fun getVoiceSettings(voiceId: String): VoiceSettings
}
```

#### Request Models
```kotlin
data class TTSRequest(
    val text: String,
    val voiceId: String,
    val settings: TTSSettings
)

data class TTSSettings(
    val stability: Float = 0.5f, // 0.0 to 1.0
    val similarityBoost: Float = 0.75f, // 0.0 to 1.0
    val style: Float = 0.0f, // 0.0 to 1.0
    val useSpeakerBoost: Boolean = true
)

data class Voice(
    val id: String,
    val name: String,
    val category: VoiceCategory,
    val description: String,
    val settings: VoiceSettings,
    val coachPersonality: CoachPersonality?
)

enum class VoiceCategory {
    COACH_BENNETT, MARIANA_FERNANDEZ, BECS_GENTRY, DAVID_GOGGINS
}
```

#### Response Models
```kotlin
data class AudioData(
    val audioBytes: ByteArray,
    val format: AudioFormat,
    val duration: Duration,
    val sampleRate: Int
)

enum class AudioFormat {
    MP3, WAV, FLAC
}
```

### Google Cloud TTS (Fallback)

#### Fallback Service
```kotlin
interface GoogleCloudTTSService {
    suspend fun synthesizeSpeech(
        text: String,
        voice: GoogleVoice,
        settings: GoogleTTSSettings
    ): AudioData
}

data class GoogleVoice(
    val name: String,
    val languageCode: String,
    val gender: Gender
)

data class GoogleTTSSettings(
    val speakingRate: Float = 1.0f,
    val pitch: Float = 0.0f,
    val volumeGainDb: Float = 0.0f
)
```

## 📍 Location Services

### GPS Tracking API

#### Location Management
```kotlin
interface LocationService {
    suspend fun getCurrentLocation(): Location
    
    suspend fun startTracking(runId: String)
    
    suspend fun stopTracking()
    
    suspend fun getRoute(runId: String): List<Location>
    
    suspend fun getLocationAccuracy(): LocationAccuracy
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val timestamp: Instant,
    val speed: Float?,
    val bearing: Float?
)

enum class LocationAccuracy {
    HIGH, MEDIUM, LOW, UNAVAILABLE
}
```

#### Location Updates
```kotlin
interface LocationCallback {
    fun onLocationUpdate(location: Location)
    fun onLocationError(error: LocationError)
}

sealed class LocationError {
    object PermissionDenied : LocationError()
    object LocationDisabled : LocationError()
    object NetworkError : LocationError()
    object Timeout : LocationError()
}
```

### Google Maps Integration

#### Map Services
```kotlin
interface MapService {
    suspend fun getRoutePolyline(
        startLocation: Location,
        endLocation: Location
    ): Polyline
    
    suspend fun getElevationData(
        locations: List<Location>
    ): List<ElevationPoint>
    
    suspend fun getAddressFromLocation(
        location: Location
    ): String
}

data class Polyline(
    val points: List<Location>,
    val encodedString: String,
    val distance: Float,
    val duration: Duration
)

data class ElevationPoint(
    val location: Location,
    val elevation: Double,
    val resolution: Float
)
```

## 🎵 Audio Management

### Audio Focus Management

#### Audio Control
```kotlin
interface AudioManager {
    suspend fun requestAudioFocus(
        request: AudioFocusRequest
    ): AudioFocusResult
    
    suspend fun abandonAudioFocus()
    
    suspend fun playAudio(audioData: AudioData)
    
    suspend fun pauseAudio()
    
    suspend fun setVolume(volume: Float)
}

data class AudioFocusRequest(
    val usage: AudioUsage,
    val contentType: AudioContentType,
    val gainType: AudioGainType,
    val willPauseWhenDucked: Boolean = true
)

enum class AudioUsage {
    ASSISTANCE_NAVIGATION_GUIDANCE,
    ASSISTANCE_SONIFICATION,
    MEDIA,
    VOICE_COMMUNICATION
}

enum class AudioContentType {
    SPEECH, MUSIC, MOVIE, SONIFICATION
}

enum class AudioGainType {
    GAIN, GAIN_TRANSIENT, GAIN_TRANSIENT_MAY_DUCK
}
```

### Music Integration

#### Music App Integration
```kotlin
interface MusicIntegrationService {
    suspend fun getCurrentTrack(): MusicTrack?
    
    suspend fun playMusic()
    
    suspend fun pauseMusic()
    
    suspend fun skipToNext()
    
    suspend fun skipToPrevious()
    
    suspend fun setVolume(volume: Float)
}

data class MusicTrack(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Duration,
    val artwork: String?
)
```

## 💾 Data Management APIs

### Local Database (Room)

#### User Management
```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
}
```

#### Training Plans
```kotlin
@Dao
interface PlanDao {
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActivePlan(userId: String): TrainingPlan?
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY startDate DESC")
    suspend fun getPlanHistory(userId: String): List<TrainingPlan>
    
    @Insert
    suspend fun insertPlan(plan: TrainingPlan): Long
    
    @Update
    suspend fun updatePlan(plan: TrainingPlan)
    
    @Delete
    suspend fun deletePlan(plan: TrainingPlan)
}
```

#### Run Tracking
```kotlin
@Dao
interface RunDao {
    @Query("SELECT * FROM runs WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentRuns(userId: String, limit: Int): List<Run>
    
    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getRunById(runId: String): Run?
    
    @Insert
    suspend fun insertRun(run: Run): Long
    
    @Update
    suspend fun updateRun(run: Run)
    
    @Query("SELECT * FROM runs WHERE userId = :userId AND startTime >= :startDate")
    suspend fun getRunsInDateRange(userId: String, startDate: Instant): List<Run>
}
```

### Repository Interfaces

#### Plan Repository
```kotlin
interface PlanRepository {
    suspend fun getActivePlan(): TrainingPlan?
    
    suspend fun createPlan(plan: TrainingPlan): String
    
    suspend fun updatePlan(plan: TrainingPlan)
    
    suspend fun deletePlan(planId: String)
    
    suspend fun getPlanHistory(): List<TrainingPlan>
    
    suspend fun adaptPlan(plan: TrainingPlan, analysis: PerformanceAnalysis): TrainingPlan
}
```

#### Run Repository
```kotlin
interface RunRepository {
    suspend fun createRun(plan: TrainingPlan?): Run
    
    suspend fun updateRun(runId: String, location: Location, metrics: RunMetrics)
    
    suspend fun completeRun(runId: String, finalMetrics: RunMetrics)
    
    suspend fun getRecentRuns(limit: Int = 10): List<Run>
    
    suspend fun getRunById(runId: String): Run?
    
    suspend fun getRunsInDateRange(startDate: LocalDate, endDate: LocalDate): List<Run>
}
```

## 🔄 Data Models

### Core Entities

#### User Profile
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val gender: Gender,
    val weight: Float,
    val height: Float,
    val experienceLevel: ExperienceLevel,
    val preferredUnits: Units,
    val timezone: String,
    val location: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}

enum class ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}

enum class Units {
    MILES, KILOMETERS
}
```

#### Run Data
```kotlin
@Entity(tableName = "runs")
data class Run(
    @PrimaryKey val id: String,
    val userId: String,
    val planId: String?,
    val startTime: Instant,
    val endTime: Instant?,
    val distance: Float,
    val duration: Duration,
    val averagePace: Pace,
    val calories: Int?,
    val route: String, // JSON encoded route
    val metrics: String, // JSON encoded metrics
    val coachingEvents: String, // JSON encoded events
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RunMetrics(
    val currentPace: Pace,
    val averagePace: Pace,
    val bestPace: Pace,
    val elevation: Float,
    val heartRate: Int?,
    val cadence: Int?,
    val strideLength: Float?
)

data class Pace(
    val minutesPerUnit: Float,
    val units: Units
)
```

## 🔐 Security & Authentication

### API Key Management
```kotlin
interface SecureKeyManager {
    suspend fun getApiKey(service: ServiceType): String
    
    suspend fun storeApiKey(service: ServiceType, key: String)
    
    suspend fun removeApiKey(service: ServiceType)
    
    suspend fun isKeyValid(service: ServiceType): Boolean
}

enum class ServiceType {
    GEMINI_AI, ELEVENLABS_TTS, GOOGLE_MAPS, GOOGLE_FIT
}
```

### Network Security
```kotlin
interface NetworkSecurityManager {
    suspend fun validateCertificate(hostname: String, certificate: Certificate): Boolean
    
    suspend fun isNetworkSecure(): Boolean
    
    suspend fun getNetworkConfig(): NetworkConfig
}

data class NetworkConfig(
    val useHttps: Boolean = true,
    val certificatePinning: Boolean = true,
    val timeoutSeconds: Int = 30,
    val retryAttempts: Int = 3
)
```

## 📊 Analytics & Monitoring

### Performance Monitoring
```kotlin
interface PerformanceMonitor {
    suspend fun trackEvent(event: AnalyticsEvent)
    
    suspend fun trackError(error: AppError)
    
    suspend fun trackPerformance(metric: PerformanceMetric)
    
    suspend fun getAnalyticsData(): AnalyticsData
}

data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, Any>,
    val timestamp: Instant,
    val userId: String?
)

data class PerformanceMetric(
    val name: String,
    val value: Double,
    val unit: String,
    val timestamp: Instant
)
```

### Error Tracking
```kotlin
interface ErrorTracker {
    suspend fun logError(error: AppError)
    
    suspend fun logCrash(crash: CrashReport)
    
    suspend fun getErrorReports(): List<ErrorReport>
}

data class AppError(
    val type: ErrorType,
    val message: String,
    val stackTrace: String?,
    val context: Map<String, Any>,
    val timestamp: Instant
)

enum class ErrorType {
    NETWORK, DATABASE, AI_SERVICE, TTS_SERVICE, LOCATION, AUDIO, UI, UNKNOWN
}
```

## 🧪 Testing APIs

### Mock Services
```kotlin
interface MockServiceProvider {
    fun getMockAIService(): AIService
    fun getMockTTSService(): TTSService
    fun getMockLocationService(): LocationService
    fun getMockAudioService(): AudioService
}

class MockAIService : AIService {
    override suspend fun generateTrainingPlan(
        userProfile: UserProfile,
        goal: RunningGoal
    ): TrainingPlan {
        return createMockTrainingPlan(userProfile, goal)
    }
    
    override suspend fun generateCoachingMessage(
        context: CoachingContext
    ): String {
        return "Mock coaching message for ${context.runType}"
    }
}
```

### Test Utilities
```kotlin
object TestDataFactory {
    fun createTestUser(): User {
        return User(
            id = "test-user-1",
            name = "Test User",
            age = 30,
            gender = Gender.MALE,
            weight = 70.0f,
            height = 175.0f,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            preferredUnits = Units.MILES,
            timezone = "America/New_York",
            location = "New York, NY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    fun createTestTrainingPlan(): TrainingPlan {
        return TrainingPlan(
            id = "test-plan-1",
            goal = createTestGoal(),
            weeks = createTestWeeks(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusWeeks(8),
            adaptations = emptyList()
        )
    }
}
```

## 📝 API Versioning

### Version Management
```kotlin
object ApiVersions {
    const val CURRENT_VERSION = "v1"
    const val GEMINI_API_VERSION = "v1beta"
    const val ELEVENLABS_API_VERSION = "v1"
    const val GOOGLE_MAPS_API_VERSION = "v1"
}

interface VersionedApi {
    val version: String
    val baseUrl: String
    val headers: Map<String, String>
}
```

### Migration Strategy
```kotlin
interface ApiMigrationManager {
    suspend fun migrateToVersion(targetVersion: String)
    
    suspend fun isMigrationRequired(): Boolean
    
    suspend fun getMigrationPlan(): MigrationPlan
}

data class MigrationPlan(
    val currentVersion: String,
    val targetVersion: String,
    val steps: List<MigrationStep>,
    val estimatedDuration: Duration
)
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
### ✅ LLM Abstraction (Chat Agent)

- `LLMService`: common interface for generating responses used by the chat agent and (optionally) voice text.
- `OpenAIService`: OpenAI Chat Completions client (uses `OPENAI_API_KEY`, `OPENAI_MODEL`).
- `GeminiLLMAdapter`: adapter that exposes `GeminiService` via `LLMService`.

Config via `local.properties`:
- `AI_PROVIDER=GPT` or `GEMINI`
- `OPENAI_API_KEY=sk-...` (only for GPT)
- `OPENAI_MODEL=gpt-4o-mini` (optional)
