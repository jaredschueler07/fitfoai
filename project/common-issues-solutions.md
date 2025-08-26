# 🛠️ RunningCoach App - Common Issues & Solutions

## 📋 Overview

This document compiles common issues encountered during development of the RunningCoach app, along with proven solutions and best practices. It's based on the change logs and development experience to help prevent and resolve similar issues in the new implementation.

## 🔧 Build & Compilation Issues

### Material Icons Compatibility Issues

**Problem**: Missing or incompatible Material Icons causing compilation errors.

**Root Cause**: 
- Using non-existent icon names
- Version mismatches between Compose and Material Icons libraries
- Missing Material Icons Extended dependency

**Solutions**:
```kotlin
// 1. Add Material Icons Extended dependency
dependencies {
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
}

// 2. Use valid icon names
// ❌ Wrong
Icons.Default.Analytics
Icons.Default.ChevronRight
Icons.Default.GpsFixed

// ✅ Correct
Icons.Default.BarChart
Icons.Default.KeyboardArrowRight
Icons.Default.LocationOn
```

**Icon Mapping Reference**:
```kotlin
// Navigation Icons
Icons.Default.ChevronRight → Icons.Default.KeyboardArrowRight
Icons.Default.Analytics → Icons.Default.BarChart
Icons.Default.History → Icons.Default.AccessTime
Icons.Default.GpsFixed → Icons.Default.LocationOn

// Action Icons
Icons.Default.Pause → Icons.Default.PauseCircleFilled
Icons.Default.Stop → Icons.Default.StopCircle
Icons.Default.Flag → Icons.Default.LocationOn

// Feature Icons
Icons.Default.AutoAwesome → Icons.Default.Star
Icons.Default.TrendingUp → Icons.Default.ShowChart
Icons.Default.Speed → Icons.Default.Timer
Icons.Default.EmojiEvents → Icons.Default.Star
Icons.Default.CalendarToday → Icons.Default.DateRange
```

### Compose Compiler Version Mismatch

**Problem**: Compose Compiler version incompatible with Kotlin version.

**Root Cause**: Version catalog misalignment between Compose Compiler and Kotlin.

**Solutions**:
```kotlin
// build.gradle.kts
android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.10" // Match Kotlin 1.9.22
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    // Use BOM for consistent versions
}
```

**Version Compatibility Chart**:
```
Kotlin Version | Compose Compiler | Compose BOM
1.9.22        | 1.6.10          | 2024.05.00
1.9.21        | 1.5.8           | 2024.02.00
1.9.20        | 1.5.6           | 2024.01.00
```

### Repository Import Issues

**Problem**: Incorrect import paths for Room repositories and database classes.

**Root Cause**: Package structure changes or incorrect import statements.

**Solutions**:
```kotlin
// ❌ Wrong imports
import com.example.runningcoach.data.RoomRunDataRepository
import com.example.runningcoach.data.RunningDatabase

// ✅ Correct imports
import com.example.runningcoach.data.repository.RoomRunDataRepository
import com.example.runningcoach.data.db.RunningDatabase
```

## 🏃‍♀️ Run Tracking Issues

### Timer Not Updating in Real-Time

**Problem**: Timer display not updating during active runs.

**Root Cause**: 
- Local state variables instead of StateFlow
- Not connected to RunningState singleton
- Missing StateFlow collection

**Solutions**:
```kotlin
// ❌ Wrong approach - Local state
@Composable
fun ModernTrackingScreen() {
    var elapsedTime by remember { mutableStateOf(0L) }
    // Timer won't update
}

// ✅ Correct approach - StateFlow collection
@Composable
fun ModernTrackingScreen() {
    val runningState = RunningState.getInstance()
    val elapsedTime by runningState.elapsedTime.collectAsState()
    val isRunning by runningState.isRunning.collectAsState()
    
    // Timer updates automatically
}
```

### GPS Location Not Updating

**Problem**: GPS location not updating or showing on map.

**Root Cause**:
- Missing location permissions
- Not requesting location updates
- Incorrect location request configuration

**Solutions**:
```kotlin
// 1. Request permissions
private fun requestLocationPermissions() {
    if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
}

// 2. Configure location request
private fun createLocationRequest(): LocationRequest {
    return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
        .setIntervalMillis(5000) // 5 seconds
        .setMinUpdateIntervalMillis(2000) // 2 seconds
        .setMaxUpdateDelayMillis(10000) // 10 seconds
        .build()
}

// 3. Request location updates
private fun startLocationUpdates() {
    if (ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}
```

### Service Integration Issues

**Problem**: RunningService not starting or stopping properly.

**Root Cause**:
- Incorrect service binding
- Missing service lifecycle management
- Improper intent configuration

**Solutions**:
```kotlin
// 1. Proper service start
private fun startRunningService() {
    val intent = Intent(this, RunningService::class.java).apply {
        action = RunningService.ACTION_START
        putExtra(RunningService.EXTRA_USER_ID, userId)
        putExtra(RunningService.EXTRA_PLAN_ID, planId)
    }
    startForegroundService(intent)
}

// 2. Proper service stop
private fun stopRunningService() {
    val intent = Intent(this, RunningService::class.java).apply {
        action = RunningService.ACTION_STOP
    }
    startService(intent)
}

// 3. Service lifecycle management
class RunningService : Service() {
    companion object {
        const val ACTION_START = "START_RUNNING"
        const val ACTION_STOP = "STOP_RUNNING"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_PLAN_ID = "plan_id"
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRunning()
            ACTION_STOP -> stopRunning()
        }
        return START_STICKY
    }
}
```

## 📊 Data Management Issues

### Units Conversion Problems

**Problem**: Hardcoded units or inconsistent unit display.

**Root Cause**:
- Not using UnitsManager consistently
- Missing unit conversion in UI components
- Scope issues with UnitsManager parameter

**Solutions**:
```kotlin
// ❌ Wrong - Hardcoded units
Text("5:30/km")
Text("5km Easy")

// ✅ Correct - Dynamic units
Text(unitsManager.formatPace(8.85)) // Auto-converts to user preference
Text("${unitsManager.formatDistance(3.1, 1)} Easy") // Auto-converts 5km/3.1mi

// Pass UnitsManager to child components
@Composable
fun ActivePlanCard(
    plan: TrainingPlan,
    unitsManager: UnitsManager, // Add parameter
    onViewPlan: () -> Unit
) {
    // Use unitsManager for all unit formatting
}
```

### Repository Method Resolution Issues

**Problem**: Repository methods not found or incorrect method calls.

**Root Cause**:
- Method name changes
- Incorrect repository interface
- Missing method implementations

**Solutions**:
```kotlin
// ❌ Wrong method calls
repository.loadEnhancedRunHistory()
repository.getRuns()

// ✅ Correct method calls
repository.loadRunHistory().map { it.toEnhancedRunData() }
repository.getRecentRuns(limit = 10)

// Ensure repository interface matches implementation
interface RunRepository {
    suspend fun getRecentRuns(limit: Int): List<Run>
    suspend fun getRunById(id: String): Run?
    suspend fun createRun(run: Run): String
}
```

## 🎵 Audio System Issues

### TTS Overlap and Queue Problems

**Problem**: Multiple TTS messages playing simultaneously or out of order.

**Root Cause**:
- No audio queue management
- Missing audio focus handling
- Thread safety issues

**Solutions**:
```kotlin
// 1. Implement TTS Queue
class TTSQueue {
    private val queue = ConcurrentLinkedQueue<TTSRequest>()
    private var isPlaying = false
    
    suspend fun addToQueue(request: TTSRequest) {
        queue.offer(request)
        if (!isPlaying) {
            playNext()
        }
    }
    
    private suspend fun playNext() {
        if (queue.isEmpty()) {
            isPlaying = false
            return
        }
        
        isPlaying = true
        val request = queue.poll()
        playAudio(request)
    }
}

// 2. Audio Focus Management
class AudioFocusManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    fun requestAudioFocus(): Boolean {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .build()
        
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}
```

### Thread Safety Issues

**Problem**: Audio operations causing crashes or blocking UI.

**Root Cause**:
- Audio operations on main thread
- Missing coroutine scope management
- Race conditions

**Solutions**:
```kotlin
// ❌ Wrong - Blocking main thread
fun playAudio(audioData: AudioData) {
    mediaPlayer.setDataSource(audioData.audioBytes)
    mediaPlayer.prepare() // Blocks main thread
    mediaPlayer.start()
}

// ✅ Correct - Background thread
suspend fun playAudio(audioData: AudioData) {
    withContext(Dispatchers.IO) {
        mediaPlayer.setDataSource(audioData.audioBytes)
        mediaPlayer.prepare()
        withContext(Dispatchers.Main) {
            mediaPlayer.start()
        }
    }
}
```

## 🤖 AI Integration Issues

### API Key Security

**Problem**: API keys exposed in code or logs.

**Root Cause**:
- Hardcoded API keys
- Missing encryption
- Logging sensitive data

**Solutions**:
```kotlin
// ❌ Wrong - Hardcoded API key
class AIService {
    private val apiKey = "sk-1234567890abcdef" // Exposed!
}

// ✅ Correct - Encrypted storage
class AIService(private val context: Context) {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "ai_credentials",
        MasterKey.Builder(context).build()
    )
    
    private val apiKey: String
        get() = encryptedPrefs.getString("gemini_api_key", "") ?: ""
}

// Store API key securely
fun storeApiKey(apiKey: String) {
    encryptedPrefs.edit().putString("gemini_api_key", apiKey).apply()
}
```

### Network Error Handling

**Problem**: AI API calls failing without proper error handling.

**Root Cause**:
- Missing timeout configuration
- No retry logic
- Poor error messages

**Solutions**:
```kotlin
// 1. Configure HTTP client with timeouts
val httpClient = HttpClient(OkHttp) {
    engine {
        connectTimeout = 30_000
        socketTimeout = 60_000
    }
    install(Retry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
    }
}

// 2. Proper error handling
suspend fun generateTrainingPlan(userProfile: UserProfile, goal: RunningGoal): Result<TrainingPlan> {
    return try {
        val response = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent") {
            headers {
                append("x-goog-api-key", apiKey)
                append("Content-Type", "application/json")
            }
            setBody(request)
        }
        
        if (response.status.isSuccess()) {
            Result.success(parseResponse(response.bodyAsText()))
        } else {
            Result.failure(AIServiceException("API error: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(AIServiceException("Network error: ${e.message}"))
    }
}
```

## 📱 UI/UX Issues

### Navigation Parameter Issues

**Problem**: Navigation parameters not passing correctly.

**Root Cause**:
- Incorrect parameter types
- Missing parameter definitions
- Navigation route mismatches

**Solutions**:
```kotlin
// 1. Define navigation routes with parameters
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object RunDetails : Screen("run_details/{runId}") {
        fun createRoute(runId: String) = "run_details/$runId"
    }
    object CoachSelection : Screen("coach_selection")
}

// 2. Pass parameters correctly
navController.navigate(Screen.RunDetails.createRoute(runId))

// 3. Extract parameters in destination
@Composable
fun RunDetailsScreen(
    navController: NavController,
    runId: String? = null
) {
    // Handle parameter
    val actualRunId = runId ?: return
}
```

### State Management Issues

**Problem**: UI state not updating or inconsistent.

**Root Cause**:
- Missing StateFlow collection
- Incorrect state updates
- Memory leaks

**Solutions**:
```kotlin
// ❌ Wrong - MutableState without proper collection
@Composable
fun DashboardScreen() {
    var uiState by remember { mutableStateOf(DashboardUiState.Loading) }
    // State won't update from ViewModel
}

// ✅ Correct - StateFlow collection
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is DashboardUiState.Loading -> LoadingScreen()
        is DashboardUiState.Success -> SuccessScreen(uiState.data)
        is DashboardUiState.Error -> ErrorScreen(uiState.message)
    }
}
```

## 🔄 Sync and Data Issues

### Google Fit Sync Problems

**Problem**: Google Fit data not syncing or duplicates appearing.

**Root Cause**:
- Missing permissions
- Incorrect data mapping
- Sync conflicts

**Solutions**:
```kotlin
// 1. Request proper permissions
private fun requestGoogleFitPermissions() {
    val scopes = listOf(
        Scope.ACTIVITY_READ,
        Scope.ACTIVITY_WRITE,
        Scope.BODY_READ,
        Scope.BODY_WRITE,
        Scope.LOCATION_READ
    )
    
    val account = GoogleSignIn.getAccountForExtension(context, FitnessOptions.builder().build())
    if (!GoogleSignIn.hasPermissions(account, scopes)) {
        GoogleSignIn.requestPermissions(
            activity,
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
            account,
            FitnessOptions.builder().build()
        )
    }
}

// 2. Handle sync conflicts
private fun handleSyncConflict(localRun: Run, googleFitRun: GoogleFitRun): Run {
    return when {
        localRun.updatedAt > googleFitRun.updatedAt -> localRun
        googleFitRun.updatedAt > localRun.updatedAt -> googleFitRun.toLocalRun()
        else -> mergeRuns(localRun, googleFitRun)
    }
}
```

### Database Migration Issues

**Problem**: Database schema changes causing crashes.

**Root Cause**:
- Missing migration strategies
- Incorrect migration implementation
- Data loss during migration

**Solutions**:
```kotlin
// 1. Define proper migrations
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns safely
        database.execSQL("ALTER TABLE runs ADD COLUMN weatherConditions TEXT")
        database.execSQL("ALTER TABLE runs ADD COLUMN temperature REAL")
        
        // Migrate existing data if needed
        database.execSQL("""
            UPDATE runs 
            SET weatherConditions = 'unknown' 
            WHERE weatherConditions IS NULL
        """)
    }
}

// 2. Test migrations
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test
    fun migrate1To2() {
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RunningDatabase::class.java,
            "test-db"
        ).addMigrations(MIGRATION_1_2).build()
        
        db.openHelper.writableDatabase
        db.close()
    }
}
```

## 🧪 Testing Issues

### Unit Test Dependencies

**Problem**: Unit tests failing due to missing dependencies or incorrect setup.

**Root Cause**:
- Missing test dependencies
- Incorrect mocking setup
- Coroutine testing issues

**Solutions**:
```kotlin
// 1. Add proper test dependencies
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

// 2. Proper coroutine testing
@RunWith(MockKJUnitRunner::class)
class RepositoryTest {
    @Test
    fun `test repository method`() = runTest {
        // Test implementation
    }
}

// 3. Mock setup
@MockK
private lateinit var mockDao: RunDao

@Before
fun setup() {
    MockKAnnotations.init(this)
}
```

### UI Test Issues

**Problem**: UI tests failing or not finding elements.

**Root Cause**:
- Missing test tags
- Incorrect element identification
- Timing issues

**Solutions**:
```kotlin
// 1. Add test tags
@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier.testTag("dashboard_screen")
    ) {
        Text(
            text = "Welcome",
            modifier = Modifier.testTag("welcome_text")
        )
    }
}

// 2. Proper element identification
@Test
fun testDashboardElements() {
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("welcome_text").assertIsDisplayed()
}

// 3. Handle timing issues
@Test
fun testAsyncOperation() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithText("Loading").fetchSemanticsNodes().isEmpty()
    }
}
```

## 📋 Prevention Best Practices

### Code Organization

1. **Consistent Package Structure**:
```
com.example.runningcoach/
├── ui/
│   ├── screens/
│   ├── theme/
│   └── navigation/
├── data/
│   ├── repository/
│   ├── db/
│   └── models/
├── domain/
│   ├── usecase/
│   └── entities/
└── di/
```

2. **Dependency Management**:
```kotlin
// Use version catalogs
dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.room.runtime)
}
```

3. **Error Handling**:
```kotlin
// Consistent error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
}

// Use throughout the app
suspend fun getData(): Result<Data> {
    return try {
        Result.Success(repository.getData())
    } catch (e: Exception) {
        Result.Error("Failed to get data", e)
    }
}
```

### Testing Strategy

1. **Test Coverage**:
- Unit tests for all business logic
- Integration tests for repositories
- UI tests for critical user flows

2. **Mock Strategy**:
- Mock external dependencies
- Use test doubles for complex objects
- Test error scenarios

3. **CI/CD Integration**:
```yaml
# .github/workflows/test.yml
- name: Run tests
  run: ./gradlew test connectedAndroidTest
- name: Generate coverage report
  run: ./gradlew jacocoTestReport
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
