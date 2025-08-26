# 🧪 RunningCoach App - Testing Strategy & Quality Assurance

## 📋 Overview

This document outlines the comprehensive testing strategy for the RunningCoach app, covering unit tests, integration tests, UI tests, performance testing, and quality assurance processes. It addresses common issues identified in the change logs and provides robust testing frameworks.

## 🎯 Testing Philosophy

### Testing Principles
- **Test-Driven Development**: Write tests before implementation
- **Comprehensive Coverage**: Aim for 90%+ code coverage
- **Automated Testing**: CI/CD pipeline with automated test execution
- **Real-World Scenarios**: Test actual user workflows and edge cases
- **Performance Testing**: Ensure app performance under various conditions
- **Accessibility Testing**: Ensure app is accessible to all users

### Testing Pyramid
```
┌─────────────────────────────────────────────────────────────┐
│                    E2E Tests (10%)                          │
│              Full user workflows, critical paths            │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                  Integration Tests (20%)                    │
│           Component interactions, API integrations          │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Unit Tests (70%)                         │
│              Individual functions, business logic           │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 Unit Testing

### Test Dependencies
```kotlin
dependencies {
    // Core Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
    
    // Android Testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    // Room Testing
    testImplementation("androidx.room:room-testing:2.6.1")
    
    // Network Testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
```

### Repository Testing
```kotlin
@RunWith(MockKJUnitRunner::class)
class RunRepositoryTest {
    
    @MockK
    private lateinit var runDao: RunDao
    
    @MockK
    private lateinit var runMapper: RunMapper
    
    private lateinit var runRepository: RunRepositoryImpl
    
    @Before
    fun setup() {
        runRepository = RunRepositoryImpl(runDao, runMapper)
    }
    
    @Test
    fun `getRecentRuns returns mapped runs`() = runTest {
        // Given
        val runEntities = listOf(
            RunEntity(id = "1", distance = 5.0f, duration = 1800000L),
            RunEntity(id = "2", distance = 10.0f, duration = 3600000L)
        )
        val expectedRuns = listOf(
            Run(id = "1", distance = 5.0f, duration = Duration.ofMinutes(30)),
            Run(id = "2", distance = 10.0f, duration = Duration.ofMinutes(60))
        )
        
        coEvery { runDao.getRecentRuns(any(), any()) } returns runEntities
        coEvery { runMapper.mapToDomain(any()) } returnsMany expectedRuns
        
        // When
        val result = runRepository.getRecentRuns(limit = 10)
        
        // Then
        assertEquals(expectedRuns, result)
        coVerify { runDao.getRecentRuns(any(), 10) }
    }
    
    @Test
    fun `createRun saves run to database`() = runTest {
        // Given
        val run = Run(id = "1", distance = 5.0f, duration = Duration.ofMinutes(30))
        val runEntity = RunEntity(id = "1", distance = 5.0f, duration = 1800000L)
        
        coEvery { runMapper.mapToEntity(any()) } returns runEntity
        coEvery { runDao.insertRun(any()) } returns 1L
        
        // When
        val result = runRepository.createRun(run)
        
        // Then
        assertEquals("1", result)
        coVerify { 
            runMapper.mapToEntity(run)
            runDao.insertRun(runEntity)
        }
    }
    
    @Test
    fun `getRunById returns null when run not found`() = runTest {
        // Given
        coEvery { runDao.getRunById(any()) } returns null
        
        // When
        val result = runRepository.getRunById("nonexistent")
        
        // Then
        assertNull(result)
    }
}
```

### Use Case Testing
```kotlin
@RunWith(MockKJUnitRunner::class)
class GenerateTrainingPlanUseCaseTest {
    
    @MockK
    private lateinit var planRepository: PlanRepository
    
    @MockK
    private lateinit var aiService: AIService
    
    private lateinit var useCase: GenerateTrainingPlanUseCase
    
    @Before
    fun setup() {
        useCase = GenerateTrainingPlanUseCase(planRepository, aiService)
    }
    
    @Test
    fun `generatePlan creates and saves plan successfully`() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val goal = createTestGoal()
        val generatedPlan = createTestTrainingPlan()
        
        coEvery { aiService.generatePlan(any(), any()) } returns generatedPlan
        coEvery { planRepository.createPlan(any()) } returns "plan-123"
        
        // When
        val result = useCase(userProfile, goal)
        
        // Then
        assertEquals("plan-123", result)
        coVerify { 
            aiService.generatePlan(userProfile, goal)
            planRepository.createPlan(generatedPlan)
        }
    }
    
    @Test
    fun `generatePlan throws exception when AI service fails`() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val goal = createTestGoal()
        val exception = AIServiceException(AIError.NetworkError, "Network failed")
        
        coEvery { aiService.generatePlan(any(), any()) } throws exception
        
        // When & Then
        assertThrows<AIServiceException> {
            useCase(userProfile, goal)
        }
    }
    
    private fun createTestUserProfile() = UserProfile(
        name = "Test User",
        age = 30,
        gender = Gender.MALE,
        weight = 70.0f,
        height = 175.0f,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        currentFitness = FitnessLevel.MODERATE,
        injuries = emptyList(),
        preferences = listOf("morning_runs", "trail_running")
    )
    
    private fun createTestGoal() = RunningGoal(
        distance = RaceDistance.FIVE_K,
        targetTime = Duration.ofMinutes(25),
        raceDate = LocalDate.now().plusWeeks(8),
        priority = GoalPriority.COMPLETE
    )
    
    private fun createTestTrainingPlan() = TrainingPlan(
        id = "test-plan",
        goal = createTestGoal(),
        weeks = createTestWeeks(),
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusWeeks(8),
        adaptations = emptyList()
    )
}
```

### ViewModel Testing
```kotlin
@RunWith(MockKJUnitRunner::class)
class DashboardViewModelTest {
    
    @MockK
    private lateinit var planRepository: PlanRepository
    
    @MockK
    private lateinit var runRepository: RunRepository
    
    @MockK
    private lateinit var coachRepository: CoachRepository
    
    private lateinit var viewModel: DashboardViewModel
    
    @Before
    fun setup() {
        viewModel = DashboardViewModel(planRepository, runRepository, coachRepository)
    }
    
    @Test
    fun `initial state is loading`() = runTest {
        // When
        val initialState = viewModel.uiState.first()
        
        // Then
        assertTrue(initialState is DashboardUiState.Loading)
    }
    
    @Test
    fun `loadDashboardData updates state with data`() = runTest {
        // Given
        val plan = createTestTrainingPlan()
        val runs = listOf(createTestRun())
        val coach = createTestCoach()
        
        coEvery { planRepository.getActivePlan() } returns plan
        coEvery { runRepository.getRecentRuns(any()) } returns runs
        coEvery { coachRepository.getSelectedCoach() } returns coach
        
        // When
        val states = mutableListOf<DashboardUiState>()
        val job = launch { viewModel.uiState.toList(states) }
        
        // Wait for state updates
        delay(100)
        
        // Then
        assertTrue(states.any { it is DashboardUiState.Success })
        val successState = states.last { it is DashboardUiState.Success } as DashboardUiState.Success
        assertEquals(plan, successState.activePlan)
        assertEquals(runs, successState.recentRuns)
        assertEquals(coach, successState.selectedCoach)
        
        job.cancel()
    }
    
    @Test
    fun `loadDashboardData shows error when repository fails`() = runTest {
        // Given
        coEvery { planRepository.getActivePlan() } throws Exception("Database error")
        
        // When
        val states = mutableListOf<DashboardUiState>()
        val job = launch { viewModel.uiState.toList(states) }
        
        // Wait for state updates
        delay(100)
        
        // Then
        assertTrue(states.any { it is DashboardUiState.Error })
        val errorState = states.last { it is DashboardUiState.Error } as DashboardUiState.Error
        assertTrue(errorState.message.contains("Database error"))
        
        job.cancel()
    }
}
```

## 🔗 Integration Testing

### Database Integration Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class RunRepositoryIntegrationTest {
    
    private lateinit var database: RunningDatabase
    private lateinit var runDao: RunDao
    private lateinit var runRepository: RunRepositoryImpl
    private lateinit var runMapper: RunMapper
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RunningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runDao = database.runDao()
        runMapper = RunMapper()
        runRepository = RunRepositoryImpl(runDao, runMapper)
    }
    
    @After
    fun cleanup() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveRun() = runTest {
        // Given
        val run = Run(
            id = "test-run",
            userId = "user-1",
            startTime = Instant.now(),
            distance = 5.0f,
            duration = Duration.ofMinutes(30),
            averagePace = Pace(6.0f, Units.KILOMETERS)
        )
        
        // When
        val runId = runRepository.createRun(run)
        val retrievedRun = runRepository.getRunById(runId)
        
        // Then
        assertNotNull(retrievedRun)
        assertEquals(run.distance, retrievedRun?.distance)
        assertEquals(run.duration, retrievedRun?.duration)
    }
    
    @Test
    fun getRecentRunsReturnsCorrectOrder() = runTest {
        // Given
        val run1 = createTestRun("run-1", Instant.now().minusSeconds(3600))
        val run2 = createTestRun("run-2", Instant.now())
        
        runRepository.createRun(run1)
        runRepository.createRun(run2)
        
        // When
        val recentRuns = runRepository.getRecentRuns(limit = 10)
        
        // Then
        assertEquals(2, recentRuns.size)
        assertEquals("run-2", recentRuns[0].id) // Most recent first
        assertEquals("run-1", recentRuns[1].id)
    }
}
```

### API Integration Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class AIServiceIntegrationTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var aiService: GeminiAIService
    private lateinit var httpClient: HttpClient
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        httpClient = HttpClient(OkHttp) {
            engine {
                addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    response
                }
            }
        }
        
        aiService = GeminiAIService("test-api-key", httpClient)
    }
    
    @After
    fun cleanup() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun generatePlanReturnsValidPlan() = runTest {
        // Given
        val mockResponse = """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"plan\": {\"id\": \"test-plan\", \"weeks\": []}}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setBody(mockResponse))
        
        val userProfile = createTestUserProfile()
        val goal = createTestGoal()
        
        // When
        val plan = aiService.generateTrainingPlan(userProfile, goal)
        
        // Then
        assertNotNull(plan)
        assertEquals("test-plan", plan.id)
        assertTrue(plan.weeks.isEmpty())
    }
    
    @Test
    fun generatePlanHandlesApiError() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        
        val userProfile = createTestUserProfile()
        val goal = createTestGoal()
        
        // When & Then
        assertThrows<AIServiceException> {
            aiService.generateTrainingPlan(userProfile, goal)
        }
    }
}
```

## 🎨 UI Testing

### Compose UI Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboardShowsLoadingState() {
        // Given
        val uiState = DashboardUiState.Loading
        
        // When
        composeTestRule.setContent {
            RunningCoachTheme {
                DashboardScreen(
                    uiState = uiState,
                    onStartRun = {},
                    onSelectCoach = {},
                    onViewPlan = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }
    
    @Test
    fun dashboardShowsActivePlan() {
        // Given
        val plan = createTestTrainingPlan()
        val uiState = DashboardUiState.Success(
            activePlan = plan,
            recentRuns = emptyList(),
            selectedCoach = null
        )
        
        // When
        composeTestRule.setContent {
            RunningCoachTheme {
                DashboardScreen(
                    uiState = uiState,
                    onStartRun = {},
                    onSelectCoach = {},
                    onViewPlan = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Active Training Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("5K Training").assertIsDisplayed()
    }
    
    @Test
    fun startRunButtonTriggersCallback() {
        // Given
        var startRunCalled = false
        val uiState = DashboardUiState.Success(
            activePlan = null,
            recentRuns = emptyList(),
            selectedCoach = null
        )
        
        // When
        composeTestRule.setContent {
            RunningCoachTheme {
                DashboardScreen(
                    uiState = uiState,
                    onStartRun = { startRunCalled = true },
                    onSelectCoach = {},
                    onViewPlan = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Start Run").performClick()
        
        // Then
        assertTrue(startRunCalled)
    }
    
    @Test
    fun coachSelectionShowsAllCoaches() {
        // Given
        val coaches = listOf(
            Coach(id = "1", name = "Coach Bennett", personalityType = CoachPersonality.BENNETT),
            Coach(id = "2", name = "Mariana Fernández", personalityType = CoachPersonality.MARIANA),
            Coach(id = "3", name = "Becs Gentry", personalityType = CoachPersonality.BECS),
            Coach(id = "4", name = "David Goggins", personalityType = CoachPersonality.GOGGINS)
        )
        
        // When
        composeTestRule.setContent {
            RunningCoachTheme {
                CoachSelectionScreen(
                    coaches = coaches,
                    selectedCoach = null,
                    onCoachSelected = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Coach Bennett").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mariana Fernández").assertIsDisplayed()
        composeTestRule.onNodeWithText("Becs Gentry").assertIsDisplayed()
        composeTestRule.onNodeWithText("David Goggins").assertIsDisplayed()
    }
}
```

### Navigation Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun bottomNavigationSwitchesScreens() {
        // Given
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        
        // When
        composeTestRule.setContent {
            RunningCoachTheme {
                AppNavigation(navController = navController)
            }
        }
        
        // Then - Dashboard is shown by default
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // When - Navigate to Analytics
        composeTestRule.onNodeWithText("Analytics").performClick()
        
        // Then - Analytics screen is shown
        composeTestRule.onNodeWithText("Performance Analytics").assertIsDisplayed()
        
        // When - Navigate to Plans
        composeTestRule.onNodeWithText("Plans").performClick()
        
        // Then - Plans screen is shown
        composeTestRule.onNodeWithText("Training Plans").assertIsDisplayed()
    }
}
```

## 🎵 Audio Testing

### TTS Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class TTSServiceTest {
    
    private lateinit var ttsService: ElevenLabsTTSService
    private lateinit var mockWebServer: MockWebServer
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val httpClient = HttpClient(OkHttp) {
            engine {
                addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    response
                }
            }
        }
        
        ttsService = ElevenLabsTTSService("test-api-key", httpClient)
    }
    
    @After
    fun cleanup() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun synthesizeSpeechReturnsAudioData() = runTest {
        // Given
        val mockAudioData = "mock-audio-data".toByteArray()
        val mockResponse = """
        {
            "audio": "${Base64.getEncoder().encodeToString(mockAudioData)}",
            "generation_id": "test-generation"
        }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setBody(mockResponse))
        
        val text = "Great job! Keep up the pace."
        val voice = Voice(id = "test-voice", name = "Test Voice")
        val settings = TTSSettings()
        
        // When
        val result = ttsService.synthesizeSpeech(text, voice, settings)
        
        // Then
        assertNotNull(result)
        assertEquals(mockAudioData.size, result.audioBytes.size)
        assertEquals(AudioFormat.MP3, result.format)
    }
    
    @Test
    fun synthesizeSpeechHandlesApiError() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        
        val text = "Test message"
        val voice = Voice(id = "test-voice", name = "Test Voice")
        val settings = TTSSettings()
        
        // When & Then
        assertThrows<Exception> {
            ttsService.synthesizeSpeech(text, voice, settings)
        }
    }
}
```

## 📍 Location Testing

### GPS Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class LocationServiceTest {
    
    private lateinit var locationService: LocationService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationService = LocationManager(context, fusedLocationClient)
    }
    
    @Test
    fun startTrackingRequestsLocationUpdates() {
        // Given
        val runId = "test-run"
        
        // When
        locationService.startTracking(runId)
        
        // Then
        // Verify that location updates are requested
        // This would require mocking the FusedLocationProviderClient
    }
    
    @Test
    fun getCurrentLocationReturnsLocation() = runTest {
        // Given
        val expectedLocation = Location(
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f,
            timestamp = Instant.now()
        )
        
        // Mock the location client to return expected location
        
        // When
        val result = locationService.getCurrentLocation()
        
        // Then
        assertEquals(expectedLocation.latitude, result.latitude, 0.001)
        assertEquals(expectedLocation.longitude, result.longitude, 0.001)
    }
}
```

## 🧠 AI Testing

### Coaching Engine Testing
```kotlin
@RunWith(MockKJUnitRunner::class)
class CoachingEngineTest {
    
    @MockK
    private lateinit var aiService: AIService
    
    @MockK
    private lateinit var ttsService: TTSService
    
    private lateinit var coachingEngine: CoachingEngine
    
    @Before
    fun setup() {
        coachingEngine = CoachingEngine(aiService, ttsService)
    }
    
    @Test
    fun processRunUpdateGeneratesCoachingMessage() = runTest {
        // Given
        val runId = "test-run"
        val metrics = RunMetrics(
            currentPace = Pace(6.0f, Units.KILOMETERS),
            averagePace = Pace(6.2f, Units.KILOMETERS),
            distance = 2.5f,
            elapsedTime = Duration.ofMinutes(15)
        )
        
        val expectedMessage = "Great pace! You're ahead of your target."
        
        coEvery { aiService.generateCoachingMessage(any()) } returns expectedMessage
        coEvery { ttsService.synthesizeSpeech(any(), any(), any()) } returns createMockAudioData()
        
        // When
        coachingEngine.processRunUpdate(runId, metrics)
        
        // Then
        coVerify { 
            aiService.generateCoachingMessage(any())
            ttsService.synthesizeSpeech(expectedMessage, any(), any())
        }
    }
    
    @Test
    fun coachingMessageIncludesCorrectContext() = runTest {
        // Given
        val runId = "test-run"
        val metrics = RunMetrics(
            currentPace = Pace(5.5f, Units.KILOMETERS),
            averagePace = Pace(6.0f, Units.KILOMETERS),
            distance = 5.0f,
            elapsedTime = Duration.ofMinutes(30)
        )
        
        var capturedContext: CoachingContext? = null
        coEvery { aiService.generateCoachingMessage(any()) } answers {
            capturedContext = firstArg()
            "Test message"
        }
        
        // When
        coachingEngine.processRunUpdate(runId, metrics)
        
        // Then
        assertNotNull(capturedContext)
        assertEquals(5.0f, capturedContext?.distance)
        assertEquals(Duration.ofMinutes(30), capturedContext?.elapsedTime)
    }
}
```

## 📊 Performance Testing

### Database Performance Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {
    
    private lateinit var database: RunningDatabase
    private lateinit var runDao: RunDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RunningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runDao = database.runDao()
    }
    
    @After
    fun cleanup() {
        database.close()
    }
    
    @Test
    fun bulkInsertPerformance() {
        // Given
        val runs = (1..1000).map { createTestRun("run-$it") }
        
        // When
        val startTime = System.currentTimeMillis()
        runs.forEach { run ->
            runDao.insertRun(RunMapper().mapToEntity(run))
        }
        val endTime = System.currentTimeMillis()
        
        // Then
        val duration = endTime - startTime
        assertTrue("Bulk insert took too long: ${duration}ms", duration < 5000)
    }
    
    @Test
    fun queryPerformance() {
        // Given
        val runs = (1..100).map { createTestRun("run-$it") }
        runs.forEach { run ->
            runDao.insertRun(RunMapper().mapToEntity(run))
        }
        
        // When
        val startTime = System.currentTimeMillis()
        val recentRuns = runDao.getRecentRuns("user-1", 50)
        val endTime = System.currentTimeMillis()
        
        // Then
        val duration = endTime - startTime
        assertTrue("Query took too long: ${duration}ms", duration < 100)
        assertEquals(50, recentRuns.size)
    }
}
```

### UI Performance Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class UIPerformanceTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboardRenderingPerformance() {
        // Given
        val largeRunList = (1..100).map { createTestRun("run-$it") }
        val uiState = DashboardUiState.Success(
            activePlan = createTestTrainingPlan(),
            recentRuns = largeRunList,
            selectedCoach = createTestCoach()
        )
        
        // When
        val startTime = System.currentTimeMillis()
        composeTestRule.setContent {
            RunningCoachTheme {
                DashboardScreen(
                    uiState = uiState,
                    onStartRun = {},
                    onSelectCoach = {},
                    onViewPlan = {}
                )
            }
        }
        val endTime = System.currentTimeMillis()
        
        // Then
        val duration = endTime - startTime
        assertTrue("Dashboard rendering took too long: ${duration}ms", duration < 1000)
    }
    
    @Test
    fun listScrollingPerformance() {
        // Given
        val largeRunList = (1..500).map { createTestRun("run-$it") }
        
        composeTestRule.setContent {
            RunningCoachTheme {
                RunHistoryScreen(runs = largeRunList)
            }
        }
        
        // When
        val startTime = System.currentTimeMillis()
        composeTestRule.onNodeWithTag("run_list").performScrollToIndex(400)
        val endTime = System.currentTimeMillis()
        
        // Then
        val duration = endTime - startTime
        assertTrue("Scrolling took too long: ${duration}ms", duration < 500)
    }
}
```

## 🔄 Integration Testing

### End-to-End Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun completeUserWorkflow() {
        // 1. User opens app and sees dashboard
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // 2. User creates a new training plan
        composeTestRule.onNodeWithText("Create Plan").performClick()
        composeTestRule.onNodeWithText("5K").performClick()
        composeTestRule.onNodeWithText("Complete").performClick()
        composeTestRule.onNodeWithText("Generate Plan").performClick()
        
        // Wait for plan generation
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("5K Training").fetchSemanticsNodes().size == 1
        }
        
        // 3. User selects a coach
        composeTestRule.onNodeWithText("Coach Selection").performClick()
        composeTestRule.onNodeWithText("Coach Bennett").performClick()
        
        // 4. User starts a run
        composeTestRule.onNodeWithText("Start Run").performClick()
        
        // Verify run tracking screen is shown
        composeTestRule.onNodeWithText("Run Tracking").assertIsDisplayed()
        
        // 5. User stops the run
        composeTestRule.onNodeWithText("Stop").performClick()
        
        // 6. User views run history
        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.onNodeWithText("Run History").performClick()
        
        // Verify run appears in history
        composeTestRule.onNodeWithText("Recent Runs").assertIsDisplayed()
    }
}
```

## 🧪 Test Utilities

### Test Data Factory
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
            preferredUnits = Units.KILOMETERS,
            timezone = "America/New_York",
            location = "New York, NY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    fun createTestRun(id: String = "test-run", startTime: Instant = Instant.now()): Run {
        return Run(
            id = id,
            userId = "test-user-1",
            startTime = startTime,
            endTime = startTime.plus(Duration.ofMinutes(30)),
            distance = 5.0f,
            duration = Duration.ofMinutes(30),
            averagePace = Pace(6.0f, Units.KILOMETERS),
            calories = 300,
            route = emptyList(),
            metrics = RunMetrics(
                currentPace = Pace(6.0f, Units.KILOMETERS),
                averagePace = Pace(6.0f, Units.KILOMETERS),
                bestPace = Pace(5.5f, Units.KILOMETERS),
                elevation = 50.0f
            ),
            coachingEvents = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    fun createTestTrainingPlan(): TrainingPlan {
        return TrainingPlan(
            id = "test-plan",
            goal = createTestGoal(),
            weeks = createTestWeeks(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusWeeks(8),
            adaptations = emptyList()
        )
    }
    
    fun createTestGoal(): RunningGoal {
        return RunningGoal(
            distance = RaceDistance.FIVE_K,
            targetTime = Duration.ofMinutes(25),
            raceDate = LocalDate.now().plusWeeks(8),
            priority = GoalPriority.COMPLETE
        )
    }
    
    fun createTestWeeks(): List<TrainingWeek> {
        return (1..8).map { weekNumber ->
            TrainingWeek(
                id = "week-$weekNumber",
                planId = "test-plan",
                weekNumber = weekNumber,
                runs = createTestRunsForWeek(weekNumber),
                totalDistance = 20.0f,
                totalTime = Duration.ofHours(2),
                intensity = TrainingIntensity.MODERATE,
                createdAt = Instant.now()
            )
        }
    }
    
    fun createTestRunsForWeek(weekNumber: Int): List<TrainingRun> {
        return listOf(
            TrainingRun(
                id = "run-1-week-$weekNumber",
                weekId = "week-$weekNumber",
                dayOfWeek = DayOfWeek.MONDAY,
                type = RunType.EASY_RUN,
                distance = 3.0f,
                duration = Duration.ofMinutes(18),
                pace = Pace(6.0f, Units.KILOMETERS),
                description = "Easy recovery run",
                coachingNotes = "Keep it conversational",
                createdAt = Instant.now()
            ),
            TrainingRun(
                id = "run-2-week-$weekNumber",
                weekId = "week-$weekNumber",
                dayOfWeek = DayOfWeek.WEDNESDAY,
                type = RunType.TEMPO_RUN,
                distance = 5.0f,
                duration = Duration.ofMinutes(25),
                pace = Pace(5.0f, Units.KILOMETERS),
                description = "Tempo run",
                coachingNotes = "Push the pace in the middle",
                createdAt = Instant.now()
            )
        )
    }
    
    fun createTestCoach(): Coach {
        return Coach(
            id = "test-coach",
            name = "Coach Bennett",
            personalityType = CoachPersonality.BENNETT,
            voiceId = "voice-1",
            colorTheme = "#1976D2",
            description = "Enthusiastic and philosophical coach",
            isActive = true,
            createdAt = Instant.now()
        )
    }
    
    fun createMockAudioData(): AudioData {
        return AudioData(
            audioBytes = "mock-audio-data".toByteArray(),
            format = AudioFormat.MP3,
            duration = Duration.ofSeconds(5),
            sampleRate = 22050
        )
    }
}
```

### Test Rules and Extensions
```kotlin
@ExperimentalCoroutinesApi
class MainCoroutineRule : TestWatcher() {
    
    private val testDispatcher = StandardTestDispatcher()
    
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

fun <T> Flow<T>.test(
    timeout: Duration = Duration.seconds(1)
): FlowTurbine<T> = test(timeout.inWholeMilliseconds)

fun <T> StateFlow<T>.test(
    timeout: Duration = Duration.seconds(1)
): FlowTurbine<T> = test(timeout.inWholeMilliseconds)
```

## 📋 Test Configuration

### Test Configuration
```kotlin
// build.gradle.kts
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }
}

dependencies {
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.5")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    
    // Debug Testing
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
```

### CI/CD Test Configuration
```yaml
# .github/workflows/test.yml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew test
    
    - name: Run instrumented tests
      run: ./gradlew connectedAndroidTest
    
    - name: Generate test report
      run: ./gradlew jacocoTestReport
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: app/build/reports/
```

## 📊 Test Coverage

### Coverage Goals
- **Unit Tests**: 90%+ code coverage
- **Integration Tests**: 80%+ coverage of critical paths
- **UI Tests**: 70%+ coverage of user workflows
- **Performance Tests**: All critical performance benchmarks

### Coverage Reporting
```kotlin
// build.gradle.kts
plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )
    
    val debugTree = fileTree("${buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(buildDir) {
        include("/jacoco/testDebugUnitTest.exec")
    })
}
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
