# 🏗️ RunningCoach App - System Architecture

## 🎯 Architecture Overview

This document outlines the technical architecture for the new RunningCoach app, designed to be scalable, maintainable, and performant. The architecture follows modern Android development best practices with a focus on clean architecture principles.

## 🏛️ High-Level Architecture

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │   UI/UX     │ │ Navigation  │ │ ViewModels  │          │
│  │ (Compose)   │ │ (NavGraph)  │ │ (MVVM)      │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Use Cases   │ │ Entities    │ │ Repositories│          │
│  │ (Business   │ │ (Data       │ │ (Interfaces)│          │
│  │  Logic)     │ │  Models)    │ │             │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Local DB    │ │ Remote APIs │ │ File System │          │
│  │ (Room)      │ │ (REST/Graph)│ │ (Assets)    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Platform    │ │ External    │ │ System      │          │
│  │ Services    │ │ Services    │ │ Services    │          │
│  │ (Android)   │ │ (AI/TTS)    │ │ (GPS/Audio) │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## 📱 Presentation Layer

### UI Architecture (Jetpack Compose)

#### Screen Structure (Based on Wireframe Reference)
```
MainActivity
├── AppNavigation
│   ├── WelcomeScreen
│   │   ├── AppIntroduction
│   │   └── OnboardingStart
│   ├── ConnectAppsScreen
│   │   ├── FitbitIntegration
│   │   ├── GoogleFitIntegration
│   │   └── SpotifyIntegration
│   ├── PersonalizeProfileScreen
│   │   ├── NameInput
│   │   ├── FitnessLevelSelection
│   │   ├── RunningGoalsSelection
│   │   └── CoachSelection
│   ├── SetEventGoalScreen
│   │   ├── RaceSelection
│   │   ├── CustomRaceCreation
│   │   └── GoalConfiguration
│   ├── DashboardScreen
│   │   ├── WelcomeHeader
│   │   ├── TodaysGuidedRunCard
│   │   ├── WeeklyActivityChart
│   │   ├── TrainingPlanSection
│   │   └── PastWorkoutsList
│   ├── FitnessGPTScreen
│   │   ├── AICoachInterface
│   │   ├── ChatInterface
│   │   └── CoachingHistory
│   └── BottomNavigation
│       ├── HomeTab
│       ├── AICoachTab
│       ├── ProgressTab
│       └── ProfileTab
```

#### Navigation Architecture (Based on Wireframe)
```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigation(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("welcome") { WelcomeScreen() }
            composable("connect_apps") { ConnectAppsScreen() }
            composable("personalize_profile") { PersonalizeProfileScreen() }
            composable("set_event_goal") { SetEventGoalScreen() }
            composable("dashboard") { DashboardScreen() }
            composable("fitness_gpt") { FitnessGPTScreen() }
        }
    }
}

// Bottom Navigation with 4 tabs as per wireframe
@Composable
fun BottomNavigation(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { HomeIcon() },
            label = { Text("Home") },
            selected = currentRoute == "dashboard",
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { ChatIcon() },
            label = { Text("AI Coach") },
            selected = currentRoute == "fitness_gpt",
            onClick = { navController.navigate("fitness_gpt") }
        )
        NavigationBarItem(
            icon = { ChartIcon() },
            label = { Text("Progress") },
            selected = currentRoute == "progress",
            onClick = { navController.navigate("progress") }
        )
        NavigationBarItem(
            icon = { ProfileIcon() },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") }
        )
    }
}
```

### ViewModel Architecture

#### ViewModel Structure
```kotlin
class DashboardViewModel(
    private val planRepository: PlanRepository,
    private val runRepository: RunRepository,
    private val coachRepository: CoachRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                planRepository.getActivePlan(),
                runRepository.getRecentRuns(),
                coachRepository.getSelectedCoach()
            ) { plan, runs, coach ->
                DashboardUiState(
                    activePlan = plan,
                    recentRuns = runs,
                    selectedCoach = coach
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
```

## 🧠 Domain Layer

### Use Cases

#### Training Plan Use Cases
```kotlin
class GenerateTrainingPlanUseCase(
    private val planRepository: PlanRepository,
    private val aiService: AIService
) {
    suspend operator fun invoke(
        userProfile: UserProfile,
        goal: RunningGoal
    ): TrainingPlan {
        return aiService.generatePlan(userProfile, goal)
    }
}

class AdaptTrainingPlanUseCase(
    private val planRepository: PlanRepository,
    private val performanceAnalyzer: PerformanceAnalyzer
) {
    suspend operator fun invoke(
        plan: TrainingPlan,
        recentRuns: List<Run>
    ): TrainingPlan {
        val analysis = performanceAnalyzer.analyze(recentRuns)
        return plan.adapt(analysis)
    }
}
```

#### Run Tracking Use Cases
```kotlin
class StartRunUseCase(
    private val runRepository: RunRepository,
    private val locationService: LocationService,
    private val audioService: AudioService
) {
    suspend operator fun invoke(plan: TrainingPlan?): Run {
        val run = runRepository.createRun(plan)
        locationService.startTracking(run.id)
        audioService.prepareCoaching(run.id)
        return run
    }
}

class ProcessRunDataUseCase(
    private val runRepository: RunRepository,
    private val coachingEngine: CoachingEngine
) {
    suspend operator fun invoke(
        runId: String,
        location: Location,
        metrics: RunMetrics
    ) {
        runRepository.updateRun(runId, location, metrics)
        coachingEngine.processRunUpdate(runId, metrics)
    }
}
```

### Entities

#### Core Data Models
```kotlin
@Entity(tableName = "users")
data class User(
    val id: String,
    val name: String,
    val age: Int,
    val gender: Gender,
    val weight: Float,
    val height: Float,
    val experienceLevel: ExperienceLevel,
    val preferredUnits: Units,
    val timezone: String,
    val location: String
)

@Entity(tableName = "training_plans")
data class TrainingPlan(
    val id: String,
    val userId: String,
    val goal: RunningGoal,
    val weeks: List<TrainingWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: PlanStatus,
    val adaptations: List<PlanAdaptation>
)

@Entity(tableName = "runs")
data class Run(
    val id: String,
    val userId: String,
    val planId: String?,
    val startTime: Instant,
    val endTime: Instant?,
    val distance: Float,
    val duration: Duration,
    val averagePace: Pace,
    val route: List<Location>,
    val metrics: RunMetrics,
    val coachingEvents: List<CoachingEvent>
)
```

## 💾 Data Layer

### Repository Pattern

#### Repository Interfaces
```kotlin
interface PlanRepository {
    suspend fun getActivePlan(): TrainingPlan?
    suspend fun createPlan(plan: TrainingPlan): String
    suspend fun updatePlan(plan: TrainingPlan)
    suspend fun deletePlan(planId: String)
    suspend fun getPlanHistory(): List<TrainingPlan>
}

interface RunRepository {
    suspend fun createRun(plan: TrainingPlan?): Run
    suspend fun updateRun(runId: String, location: Location, metrics: RunMetrics)
    suspend fun completeRun(runId: String, finalMetrics: RunMetrics)
    suspend fun getRecentRuns(limit: Int = 10): List<Run>
    suspend fun getRunById(runId: String): Run?
}

interface CoachRepository {
    suspend fun getSelectedCoach(): Coach
    suspend fun setSelectedCoach(coach: Coach)
    suspend fun getAvailableCoaches(): List<Coach>
    suspend fun getCoachPersonality(coachId: String): CoachPersonality
}
```

### Local Database (Room)

#### Database Schema
```kotlin
@Database(
    entities = [
        User::class,
        TrainingPlan::class,
        Run::class,
        Coach::class,
        CoachingEvent::class
    ],
    version = 1
)
abstract class RunningDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun planDao(): PlanDao
    abstract fun runDao(): RunDao
    abstract fun coachDao(): CoachDao
    abstract fun coachingEventDao(): CoachingEventDao
}
```

#### Data Access Objects
```kotlin
@Dao
interface RunDao {
    @Query("SELECT * FROM runs WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentRuns(userId: String, limit: Int): List<Run>
    
    @Insert
    suspend fun insertRun(run: Run)
    
    @Update
    suspend fun updateRun(run: Run)
    
    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getRunById(runId: String): Run?
}
```

### Remote Data Sources

#### API Services
```kotlin
interface AIService {
    suspend fun generatePlan(userProfile: UserProfile, goal: RunningGoal): TrainingPlan
    suspend fun generateCoachingMessage(context: CoachingContext): String
    suspend fun analyzePerformance(runs: List<Run>): PerformanceAnalysis
}

interface TTSService {
    suspend fun synthesizeSpeech(text: String, voice: Voice): AudioData
    suspend fun getAvailableVoices(): List<Voice>
}

interface LocationService {
    suspend fun getCurrentLocation(): Location
    suspend fun startTracking(runId: String)
    suspend fun stopTracking()
    suspend fun getRoute(runId: String): List<Location>
}
```

## 🔧 Infrastructure Layer

### Platform Services

#### Location Services
```kotlin
class LocationManager(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                onLocationUpdate(location)
            }
        }
    }
    
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000 // 1 second
            fastestInterval = 500
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}
```

#### Audio Services
```kotlin
class AudioManager(
    private val context: Context,
    private val ttsService: TTSService
) {
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build())
        .build()
    
    fun playCoachingMessage(text: String, voice: Voice) {
        viewModelScope.launch {
            val audioData = ttsService.synthesizeSpeech(text, voice)
            playAudio(audioData)
        }
    }
}
```

### External Services Integration

#### AI Service Implementation
```kotlin
class GeminiAIService(
    private val apiKey: String,
    private val httpClient: HttpClient
) : AIService {
    
    override suspend fun generatePlan(
        userProfile: UserProfile,
        goal: RunningGoal
    ): TrainingPlan {
        val prompt = buildPlanGenerationPrompt(userProfile, goal)
        val response = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent") {
            headers {
                append("Authorization", "Bearer $apiKey")
            }
            setBody(PlanGenerationRequest(prompt))
        }
        
        return parsePlanResponse(response.body())
    }
}
```

#### TTS Service Implementation
```kotlin
class ElevenLabsTTSService(
    private val apiKey: String,
    private val httpClient: HttpClient
) : TTSService {
    
    override suspend fun synthesizeSpeech(text: String, voice: Voice): AudioData {
        val response = httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/${voice.id}") {
            headers {
                append("xi-api-key", apiKey)
            }
            setBody(TTSRequest(text, voice.settings))
        }
        
        return AudioData(response.bodyAsChannel())
    }
}
```

## 🔄 Data Flow Architecture

### State Management

#### UI State Flow
```kotlin
data class DashboardUiState(
    val activePlan: TrainingPlan? = null,
    val recentRuns: List<Run> = emptyList(),
    val selectedCoach: Coach? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class DashboardEvent {
    object LoadDashboard : DashboardEvent()
    object StartRun : DashboardEvent()
    data class SelectCoach(val coach: Coach) : DashboardEvent()
    data class ViewPlan(val planId: String) : DashboardEvent()
}
```

#### Repository Data Flow
```kotlin
class PlanRepositoryImpl(
    private val planDao: PlanDao,
    private val aiService: AIService,
    private val planMapper: PlanMapper
) : PlanRepository {
    
    override suspend fun getActivePlan(): TrainingPlan? {
        return planDao.getActivePlan()?.let { planEntity ->
            planMapper.mapToDomain(planEntity)
        }
    }
    
    override suspend fun createPlan(plan: TrainingPlan): String {
        val planEntity = planMapper.mapToEntity(plan)
        return planDao.insertPlan(planEntity).toString()
    }
}
```

## 🧪 Testing Architecture

### Testing Strategy

#### Unit Testing
```kotlin
class GenerateTrainingPlanUseCaseTest {
    private lateinit var useCase: GenerateTrainingPlanUseCase
    private lateinit var planRepository: FakePlanRepository
    private lateinit var aiService: FakeAIService
    
    @Before
    fun setup() {
        planRepository = FakePlanRepository()
        aiService = FakeAIService()
        useCase = GenerateTrainingPlanUseCase(planRepository, aiService)
    }
    
    @Test
    fun `generate plan returns valid plan`() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val goal = createTestGoal()
        
        // When
        val result = useCase(userProfile, goal)
        
        // Then
        assertThat(result).isNotNull()
        assertThat(result.weeks).hasSize(8)
    }
}
```

#### Integration Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class PlanRepositoryIntegrationTest {
    private lateinit var database: RunningDatabase
    private lateinit var planDao: PlanDao
    private lateinit var repository: PlanRepositoryImpl
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RunningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        planDao = database.planDao()
        repository = PlanRepositoryImpl(planDao, FakeAIService(), PlanMapper())
    }
}
```

## 🔒 Security Architecture

### Data Security
- **Local Encryption**: All sensitive data encrypted using Android Keystore
- **API Security**: API keys stored securely using EncryptedSharedPreferences
- **Network Security**: HTTPS-only communication with certificate pinning
- **Input Validation**: Comprehensive input sanitization and validation

### Privacy Protection
- **Local Processing**: AI processing done locally when possible
- **Data Minimization**: Only collect necessary data for functionality
- **User Control**: Full user control over data sharing and deletion
- **Transparency**: Clear privacy policy and data usage explanations

## 📊 Performance Architecture

### Performance Optimization
- **Background Processing**: Heavy operations moved to background threads
- **Caching Strategy**: Multi-level caching for frequently accessed data
- **Lazy Loading**: UI components loaded on-demand
- **Memory Management**: Efficient memory usage with proper cleanup

### Monitoring & Analytics
- **Performance Metrics**: App startup time, memory usage, battery consumption
- **Error Tracking**: Comprehensive error logging and crash reporting
- **User Analytics**: Anonymous usage analytics for feature improvement
- **Health Monitoring**: System health checks and automatic recovery

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
