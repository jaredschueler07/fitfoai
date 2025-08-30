# Product Requirements Document: FitFoAI Health Connect Implementation

## Executive Summary

### Project Overview
Implement Health Connect as the primary fitness data integration for FitFoAI, replacing the deprecated Google Fit APIs. Since the app is pre-release, we'll build directly on Health Connect without migration complexity.

### Success Metrics
- Health Connect fully integrated before first release
- All fitness tracking features functional
- Battery efficiency baseline established
- Privacy-first architecture implemented

## Current State Analysis

### Existing Architecture Strengths
- ✅ Room database with well-structured entities
- ✅ Local-first data architecture
- ✅ Clean separation of concerns
- ✅ Compose UI ready for permission flows

### Code to Remove
```kotlin
// Dependencies to remove from build.gradle.kts
implementation(libs.google.play.services.fitness) // REMOVE
buildConfigField("String", "GOOGLE_FIT_CLIENT_ID", "...") // REMOVE
```

## Implementation Strategy (Order of Operations)

### Phase 1: Foundation Setup

#### 1.1 Add Health Connect Dependencies
```kotlin
// app/build.gradle.kts
dependencies {
    // Add Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
    
    // Keep all existing dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // ... rest of your dependencies
}
```

#### 1.2 Update AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Health Connect Permissions -->
    <uses-permission android:name="android.permission.health.READ_EXERCISE" />
    <uses-permission android:name="android.permission.health.WRITE_EXERCISE" />
    <uses-permission android:name="android.permission.health.READ_DISTANCE" />
    <uses-permission android:name="android.permission.health.WRITE_DISTANCE" />
    <uses-permission android:name="android.permission.health.READ_STEPS" />
    <uses-permission android:name="android.permission.health.WRITE_STEPS" />
    <uses-permission android:name="android.permission.health.READ_HEART_RATE" />
    <uses-permission android:name="android.permission.health.WRITE_HEART_RATE" />
    <uses-permission android:name="android.permission.health.READ_SPEED" />
    <uses-permission android:name="android.permission.health.WRITE_SPEED" />
    <uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED" />
    
    <!-- Query Health Connect availability -->
    <queries>
        <package android:name="com.google.android.apps.healthdata" />
    </queries>
    
    <application>
        <!-- Your existing activities -->
    </application>
</manifest>
```

### Phase 2: Core Implementation

#### 2.1 Health Connect Manager
Create a central manager for Health Connect operations:

```kotlin
@Singleton
class HealthConnectManager @Inject constructor(
    private val context: Context
) {
    private val healthConnectClient = HealthConnectClient.getOrCreate(context)
    
    // Check availability
    suspend fun isAvailable(): Boolean
    
    // Permission management
    suspend fun hasAllPermissions(): Boolean
    fun createPermissionRequestIntent(): Intent
    
    // Open Health Connect app
    fun openHealthConnectSettings(): Intent
}
```

#### 2.2 Update Repository Layer
Replace any Google Fit repository with Health Connect:

```kotlin
interface FitnessDataRepository {
    // Core operations
    suspend fun saveRun(run: Run): Result<String>
    suspend fun getRuns(startDate: LocalDate, endDate: LocalDate): Result<List<Run>>
    suspend fun saveSteps(steps: Int, date: LocalDate): Result<Unit>
    suspend fun getSteps(date: LocalDate): Result<Int>
    
    // Real-time observations
    fun observeSteps(): Flow<Int>
    fun observeActiveSession(): Flow<ExerciseSession?>
}

@Singleton
class HealthConnectFitnessRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val runDao: RunDao
) : FitnessDataRepository {
    // Implementation using Health Connect APIs
}
```

#### 2.3 Update Database Schema
Enhance your existing Run entity:

```kotlin
@Entity(tableName = "runs")
data class Run(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Float? = null,
    val durationSeconds: Long? = null,
    val averagePaceSeconds: Int? = null,
    val calories: Int? = null,
    val heartRateAverage: Int? = null,
    val gpsRoute: String? = null, // JSON encoded
    
    // Health Connect specific
    val healthConnectId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    LOCAL,      // Only in local database
    SYNCED,     // Synced with Health Connect
    PENDING,    // Waiting to sync
    FAILED      // Sync failed
}
```

### Phase 3: Permission Flow Implementation

#### 3.1 Permission Check on App Launch
```kotlin
@Composable
fun MainScreen(
    healthConnectManager: HealthConnectManager
) {
    var healthConnectState by remember { mutableStateOf(HealthConnectState.CHECKING) }
    
    LaunchedEffect(Unit) {
        healthConnectState = when {
            !healthConnectManager.isAvailable() -> HealthConnectState.NOT_AVAILABLE
            !healthConnectManager.hasAllPermissions() -> HealthConnectState.NEEDS_PERMISSIONS
            else -> HealthConnectState.READY
        }
    }
    
    when (healthConnectState) {
        HealthConnectState.NEEDS_PERMISSIONS -> HealthConnectOnboarding()
        HealthConnectState.NOT_AVAILABLE -> LocalOnlyModeScreen()
        HealthConnectState.READY -> YourNormalAppContent()
        HealthConnectState.CHECKING -> LoadingScreen()
    }
}
```

#### 3.2 Onboarding Screen
```kotlin
@Composable
fun HealthConnectOnboarding(
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit
) {
    // Beautiful onboarding UI explaining benefits:
    // - Sync with other fitness apps
    // - Automatic step counting
    // - Privacy-first (on-device storage)
    // - No account required
    // - Battery efficient
}
```

### Phase 4: Feature Integration

#### 4.1 Run Tracking Integration
```kotlin
class RunTrackingViewModel @Inject constructor(
    private val fitnessRepository: FitnessDataRepository,
    private val locationService: LocationService
) : ViewModel() {
    
    fun startRun() {
        // Track with GPS as normal
        locationService.startTracking()
        
        // Create local run record
        val run = Run(
            startTime = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        
        // Save to local DB first
        runDao.insert(run)
        
        // Async sync to Health Connect
        viewModelScope.launch {
            fitnessRepository.saveRun(run)
                .onSuccess { healthConnectId ->
                    runDao.update(run.copy(
                        healthConnectId = healthConnectId,
                        syncStatus = SyncStatus.SYNCED
                    ))
                }
        }
    }
}
```

#### 4.2 Step Counting Integration
```kotlin
class StepsViewModel @Inject constructor(
    private val fitnessRepository: FitnessDataRepository
) : ViewModel() {
    
    val dailySteps = fitnessRepository.observeSteps()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )
    
    fun refreshSteps() {
        viewModelScope.launch {
            fitnessRepository.getSteps(LocalDate.now())
                .onSuccess { steps ->
                    // Update UI
                }
        }
    }
}
```

#### 4.3 Data Reading from Other Apps
```kotlin
class HealthDataAggregator @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {
    
    suspend fun getWeeklyStats(): WeeklyFitnessStats {
        val endTime = Instant.now()
        val startTime = endTime.minus(7, ChronoUnit.DAYS)
        
        // Read all exercise sessions (from any app)
        val sessions = healthConnectClient.readRecords(
            ReadRecordsRequest(
                ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        
        // Aggregate data
        return WeeklyFitnessStats(
            totalRuns = sessions.records.count { 
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING 
            },
            totalDistance = calculateTotalDistance(sessions.records),
            totalCalories = calculateTotalCalories(sessions.records)
        )
    }
}
```

### Phase 5: Background Sync

#### 5.1 Sync Worker
```kotlin
class HealthConnectSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Get pending runs from local database
        val pendingRuns = runDao.getPendingSync()
        
        pendingRuns.forEach { run ->
            fitnessRepository.saveRun(run)
                .onSuccess {
                    runDao.update(run.copy(syncStatus = SyncStatus.SYNCED))
                }
                .onFailure {
                    // Retry later
                }
        }
        
        return Result.success()
    }
}

// Schedule periodic sync
fun scheduleSyncWork(context: Context) {
    val syncRequest = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
        repeatInterval = 6,
        repeatIntervalTimeUnit = TimeUnit.HOURS
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()
    
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "health_connect_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
}
```

### Phase 6: Testing

#### 6.1 Unit Tests
```kotlin
class HealthConnectRepositoryTest {
    @Mock lateinit var healthConnectClient: HealthConnectClient
    @Mock lateinit var runDao: RunDao
    
    @Test
    fun `save run creates proper Health Connect records`() {
        // Test implementation
    }
    
    @Test
    fun `read steps aggregates correctly`() {
        // Test implementation
    }
}
```

#### 6.2 Integration Tests
```kotlin
@MediumTest
class HealthConnectIntegrationTest {
    @get:Rule
    val healthConnectRule = HealthConnectTestRule()
    
    @Test
    fun `end to end run tracking flow`() {
        // Test complete flow from start to sync
    }
}
```

### Phase 7: Local-Only Fallback

#### 7.1 Graceful Degradation
```kotlin
class AdaptiveFitnessRepository @Inject constructor(
    private val healthConnectRepo: HealthConnectFitnessRepository,
    private val localOnlyRepo: LocalOnlyFitnessRepository,
    private val healthConnectManager: HealthConnectManager
) : FitnessDataRepository {
    
    override suspend fun saveRun(run: Run): Result<String> {
        // Always save locally first
        localOnlyRepo.saveRun(run)
        
        // Try Health Connect if available
        return if (healthConnectManager.isAvailable() && 
                   healthConnectManager.hasAllPermissions()) {
            healthConnectRepo.saveRun(run)
        } else {
            Result.success(run.id)
        }
    }
}
```

## Architecture Decisions

### Data Flow Architecture
```
User Action → UI Layer → ViewModel → Repository
                                          ↓
                              ┌─────────────────────────┐
                              │   Adaptive Repository   │
                              └─────────────────────────┘
                                     ↓            ↓
                          ┌──────────────┐  ┌──────────────┐
                          │ Health       │  │ Local Only   │
                          │ Connect      │  │ Repository   │
                          └──────────────┘  └──────────────┘
                                  ↓                ↓
                          ┌──────────────┐  ┌──────────────┐
                          │ Health       │  │ Room         │
                          │ Connect API  │  │ Database     │
                          └──────────────┘  └──────────────┘
```

### Key Design Principles

1. **Local-First**: Always save to Room database immediately
2. **Async Sync**: Sync to Health Connect in background
3. **Graceful Degradation**: App works without Health Connect
4. **Privacy-First**: Explicit user consent for all data sharing
5. **Battery Efficient**: Batch operations and smart scheduling

## Testing Strategy

### Test Coverage Requirements
- Unit tests for all repository methods
- Integration tests for Health Connect flows
- UI tests for permission flows
- Performance tests for large datasets
- Battery impact testing

### Manual Testing Checklist
- [ ] Fresh install flow
- [ ] Permission grant flow
- [ ] Permission denial flow
- [ ] Health Connect not installed
- [ ] Health Connect needs update
- [ ] Sync with multiple apps
- [ ] Offline functionality
- [ ] Background sync
- [ ] Data accuracy verification

## Privacy & Security

### Data Handling
- All data stored on-device by Health Connect
- No cloud sync without explicit user consent
- Clear data deletion options
- Transparent permission explanations

### Compliance
- Follow Google Play Health & Fitness policy
- Implement proper data retention policies
- Clear privacy policy required
- User consent for any data sharing

## Performance Considerations

### Optimization Strategies
```kotlin
// Batch read operations
suspend fun getMonthlyRuns(): List<Run> {
    return healthConnectClient.readRecords(
        ReadRecordsRequest(
            ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                startOfMonth,
                endOfMonth
            ),
            pageSize = 100 // Paginate large datasets
        )
    )
}

// Use aggregations for summaries
suspend fun getWeeklyStepCount(): Long {
    return healthConnectClient.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(
                startOfWeek,
                endOfWeek
            )
        )
    )[StepsRecord.COUNT_TOTAL] ?: 0L
}
```

## Error Handling

### Common Scenarios
```kotlin
sealed class HealthConnectError : Exception() {
    object NotInstalled : HealthConnectError()
    object UpdateRequired : HealthConnectError()
    object PermissionDenied : HealthConnectError()
    data class SyncFailed(val reason: String) : HealthConnectError()
}

fun handleHealthConnectError(error: HealthConnectError) {
    when (error) {
        is HealthConnectError.NotInstalled -> {
            // Prompt to install Health Connect
        }
        is HealthConnectError.UpdateRequired -> {
            // Prompt to update Health Connect
        }
        is HealthConnectError.PermissionDenied -> {
            // Show rationale and settings option
        }
        is HealthConnectError.SyncFailed -> {
            // Log and retry with backoff
        }
    }
}
```

## Future Enhancements

### Potential Features
- Sleep tracking integration
- Nutrition data support
- Workout plans sync
- Heart rate zone training
- Recovery metrics
- Social features via shared Health Connect data

### Ecosystem Integration
- Sync with Fitbit (via Health Connect)
- Sync with Samsung Health (via Health Connect)
- Sync with Garmin Connect (via Health Connect)
- Import from Strava (via Health Connect)

## Resources & Documentation

### Essential Links
- [Health Connect Guide](https://developer.android.com/health-and-fitness/guides/health-connect)
- [Health Connect Permissions](https://developer.android.com/health-and-fitness/guides/health-connect/develop/request-permissions)
- [Data Types Reference](https://developer.android.com/reference/androidx/health/connect/client/records/package-summary)
- [Testing Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect/test)

### Code Samples
- [Official Samples](https://github.com/android/health-samples)
- [Codelab](https://developer.android.com/codelabs/health-connect)

## Decision Log

### Why Health Connect over alternatives:
1. **Native Android solution** - First-party support
2. **Privacy-first** - On-device storage
3. **No account required** - Unlike Google Fit
4. **Growing ecosystem** - Major apps adopting
5. **Future-proof** - Active development by Google
6. **Battery efficient** - Optimized for background ops