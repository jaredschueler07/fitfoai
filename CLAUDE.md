# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Canonical Bug Report List

### Critical Issues - Immediate Action Required

1. **Google Fit Profile Auto-Fill Not Working**
   - **Status**: Critical - blocks onboarding flow
   - **Description**: Profile fields (name, height, weight) not pre-filled from Google Fit data
   - **Expected Behavior**: After successful Google Fit connection, user profile should auto-populate
   - **Current Behavior**: Fields remain empty despite successful connection
   - **Impact**: Forces manual data entry, poor UX
   - **Files Affected**: `PersonalizeProfileScreen.kt`, `GoogleFitService.kt`

2. **No Persistent User Profiles**
   - **Status**: Critical - testing workflow blocked
   - **Description**: Cannot save/restore onboarding progress, forces complete re-onboarding
   - **Expected Behavior**: Save profile once, allow skipping onboarding on subsequent launches
   - **Current Behavior**: Must complete full onboarding flow every time
   - **Impact**: Severely hampers testing and development workflow
   - **Files Affected**: `MainActivity.kt`, `UserRepository.kt`, navigation flow

3. **Google Maps Slow Load Performance**
   - **Status**: High Priority - UX issue
   - **Description**: Google Maps takes excessive time to initialize and display
   - **Expected Behavior**: Map should load within 2-3 seconds
   - **Current Behavior**: Long delays before map becomes interactive
   - **Impact**: Poor user experience during run tracking setup
   - **Files Affected**: `RunTrackingScreen.kt`, map integration components

4. **Extensive Placeholder Content**
   - **Status**: Medium Priority - production readiness
   - **Description**: Numerous TODO placeholders and mock data instead of functional implementation
   - **Expected Behavior**: Real functionality with proper data handling
   - **Current Behavior**: Placeholder text and stub implementations
   - **Impact**: App appears incomplete and unprofessional
   - **Files Affected**: Multiple service and UI files

5. **Metric Units Instead of Imperial**
   - **Status**: Medium Priority - localization issue
   - **Description**: Height/weight inputs expect metric units (cm/kg) instead of imperial (ft/lbs)
   - **Expected Behavior**: Imperial units for US-based usage
   - **Current Behavior**: Metric unit placeholders and validation
   - **Impact**: Confusing UX for American users
   - **Files Affected**: `PersonalizeProfileScreen.kt`, data models

### Resolution Plan Requirements

- Google Fit integration must provide at minimum a valid name to proceed with profile setup
- User profiles must persist between app sessions with option to reset for testing
- Replace all placeholder content with functional implementations
- Convert all units to imperial system (feet/inches, pounds, miles, Fahrenheit)
- Investigate alternative mapping solutions for performance comparison

### Next Steps

Sprint planning required with task delegation to specialized agents for systematic resolution of all identified issues.

## Project Overview

FITFO AI (RunningCoach v2) is an Android application built with Kotlin and Jetpack Compose. It's an AI-powered running coach app that provides personalized training plans, real-time coaching, and integrations with fitness apps like Fitbit, Google Fit, and Spotify.

**Current Status**: 85% Production Ready - P0 blockers resolved, voice coaching complete
**Architecture**: Clean Architecture with MVVM pattern, background services
**Target SDK**: API 36 (Android 14)
**Min SDK**: API 26 (Android 8.0)

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug version to connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Build and install debug version
./gradlew clean assembleDebug installDebug
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run specific test class (examples)
./gradlew test --tests "com.runningcoach.v2.data.service.BackgroundLocationServiceTest"
./gradlew test --tests "com.runningcoach.v2.data.service.ElevenLabsIntegrationTest"
./gradlew test --tests "com.runningcoach.v2.data.service.VoiceCoachingManagerTest"

# Run P0 blocker critical tests
./gradlew test --tests "*BackgroundLocationService*" --tests "*PermissionManager*" --tests "*SessionRecovery*"

# Run voice coaching tests
./gradlew test --tests "*ElevenLabs*" --tests "*SmartTrigger*" --tests "*VoiceCoaching*"

# Generate test coverage report
./gradlew jacocoTestReport
```

### Code Quality
```bash
# Lint checks (when configured)
./gradlew lint

# Check for outdated dependencies
./gradlew dependencyUpdates
```

## Architecture Overview

### Package Structure
```
com.runningcoach.v2/
├── domain/
│   ├── model/          # Core data models (Coach, RaceGoal, TrainingPlan, User, LocationData, RunMetrics)
│   ├── repository/     # Repository interfaces (RunSessionRepository)
│   └── usecase/        # Business logic use cases (TrackRunSessionUseCase, etc.)
├── data/
│   ├── local/          # Room database, DAOs, entities (VoiceLineEntity, CoachPersonalityEntity)
│   ├── remote/         # API services, DTOs
│   ├── repository/     # Repository implementations (RunSessionRepositoryImpl)
│   └── service/        # Critical services (BackgroundLocationService, VoiceCoachingManager, etc.)
├── presentation/
│   ├── screen/         # UI screens (permissions/, settings/, runtracking/, etc.)
│   ├── components/     # Reusable UI components (VoiceCoachingCard, PermissionDialog, etc.)
│   ├── navigation/     # Navigation definitions (Screen.kt)
│   └── theme/          # App theming (Colors, Typography, Theme)
└── di/                 # Dependency injection (Manual DI - AppModule)
```

### Key Architectural Decisions
- **Clean Architecture**: Separation of concerns with domain, data, and presentation layers
- **MVVM Pattern**: ViewModels manage UI state and business logic coordination
- **Single Activity Architecture**: MainActivity with Compose Navigation
- **Jetpack Compose**: Modern declarative UI toolkit with Material 3
- **Room Database**: Local data persistence with voice caching entities (v4 schema)
- **Background Services**: Foreground services for GPS tracking and crash recovery
- **Ktor Client**: HTTP networking for API calls (ElevenLabs, Gemini, etc.)
- **Coroutines + Flow**: Asynchronous programming and reactive data streams
- **Manual Dependency Injection**: AppModule pattern (Hilt disabled due to compatibility)

### Navigation Flow
1. **Onboarding**: Welcome → Connect Apps → Personalize Profile → Set Event Goal
2. **Main App**: Dashboard (with bottom nav to AI Coach, Progress, Profile)
3. **Permission Flow**: Permissions screen for Android 12+ location access
4. **Settings**: Comprehensive voice coaching and GPS preferences

## Key Features

### Core Screens (Production Ready - 85%)
- **WelcomeScreen**: App introduction and onboarding start
- **ConnectAppsScreen**: Third-party app integration (Fitbit, Google Fit, Spotify)
- **PersonalizeProfileScreen**: User profile setup with fitness level and coach selection
- **SetEventGoalScreen**: Race goal configuration
- **DashboardScreen**: Main hub with training plan, progress, and quick actions
- **AICoachScreen**: AI-powered fitness coaching interface
- **RunTrackingScreen**: Real-time GPS tracking with background service support
- **PermissionScreen**: Modern Android 12+ permission flow with education
- **SettingsScreen**: Comprehensive voice coaching and GPS configuration

### Critical Background Services
- **BackgroundLocationService**: Foreground service for continuous GPS tracking
- **SessionRecoveryManager**: WorkManager-based crash recovery with 3-retry logic
- **VoiceCoachingManager**: AI voice coaching with 4 personalities (Bennett, Mariana, Becs, Goggins)
- **SmartTriggerEngine**: Context-aware coaching triggers (18+ types)
- **AudioFocusManager**: Music ducking and audio focus management
- **PermissionManager**: Android 12+ permission handling with rationale dialogs

### Voice Coaching System
- **ElevenLabs Integration**: <200ms voice synthesis with personality-based coaching
- **4 AI Coach Personalities**: Professional, energetic, mindful, and intense coaching styles
- **Smart Triggers**: Pace guidance, milestone celebrations, form corrections, motivation
- **Audio Management**: Music ducking, priority queues, Bluetooth support
- **Voice Caching**: Room database caching for offline coaching support

### AI Coach Chat (New)
- **Provider-Agnostic LLM**: `LLMService` abstracts provider choice for chat.
- **Providers**: `OpenAIService` (GPT) and `GeminiLLMAdapter` (wraps `GeminiService`).
- **Context Grounding**: `ChatContextProvider` summarizes profile + recent Google Fit data for better answers.
- **DI**: `aiChatAgent` uses GPT/Gemini per `AI_PROVIDER`; voice pipeline remains Gemini-backed.
- **Config**: Set `AI_PROVIDER`, `OPENAI_API_KEY`, `OPENAI_MODEL` in `local.properties`.

### UI Components
- **AppButton**: Consistent button styling across the app
- **AppCard**: Card-based layout system with athletic blue theme
- **BottomNavigation**: Four-tab navigation (Home, AI Coach, Progress, Profile)
- **VoiceCoachingCard**: Voice controls and coach personality selection
- **AudioFeedbackOverlay**: Real-time visual feedback during voice coaching
- **PermissionDialog**: Educational permission request dialogs
- **Custom Icons**: App-specific iconography

## Development Notes

### Current Status - Production Ready (85%)
- **P0 Blockers**: All resolved - background GPS, permissions, session recovery
- **Voice Coaching**: Complete with 4 AI personalities and smart triggers
- **Testing**: 159 comprehensive tests with 85%+ coverage on critical components
- **Build System**: Production-ready with ProGuard/R8 optimization

### Current Limitations (15% remaining)
- **Minor compilation fixes**: Some service implementations need final method completions
- **Hilt Dependency Injection**: Temporarily disabled, using manual DI (AppModule pattern)
- **Spotify Integration**: Planned for Sprint 3.3 (OAuth, BPM matching, playlist AI)

### Technology Stack
- **Build System**: Gradle with version catalogs (`gradle/libs.versions.toml`)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Components**: Navigation Compose, Lifecycle, ViewModel
- **Background Services**: Foreground services with WorkManager
- **Networking**: Ktor Client with JSON serialization (ElevenLabs, Gemini APIs)
- **Database**: Room v4 with voice caching entities and migration scripts
- **Audio**: Media3 for advanced audio management and focus handling
- **Coroutines**: 1.8.0 for asynchronous operations and reactive programming

### Testing Strategy (Complete)
- **Unit Tests**: 159 tests covering services, repositories, and business logic
- **Integration Tests**: End-to-end GPS tracking and voice coaching workflows  
- **UI Tests**: Compose UI testing for permission flows and voice controls
- **Performance Tests**: Voice synthesis latency, GPS accuracy, battery optimization
- **Coverage Achieved**: 85%+ on critical components, 70%+ overall

### Important Files
- `app/build.gradle.kts`: Main build configuration with Media3, WorkManager, ProGuard rules
- `gradle/libs.versions.toml`: Dependency version management
- `app/src/main/AndroidManifest.xml`: Foreground service declarations and permissions
- `MainActivity.kt`: Single activity with navigation setup
- `data/service/BackgroundLocationService.kt`: Critical GPS tracking foreground service
- `data/service/VoiceCoachingManager.kt`: AI voice coaching orchestration
- `data/service/PermissionManager.kt`: Android 12+ permission handling
- `presentation/screen/runtracking/RunTrackingScreen.kt`: Main GPS tracking UI
- `project/`: Comprehensive documentation and planning materials
- `project/current-status.md`: Latest project status and production readiness
- `project/SPRINT_SUMMARY_P0_VOICE_COACHING.md`: Complete sprint achievements
- `project/SPRINT_3.3_SPOTIFY_INTEGRATION_PLAN.md`: Next sprint planning

### Next Development Phase - Sprint 3.3
- **Spotify OAuth Integration**: Complete authentication and token management
- **BPM Cadence Matching**: Music tempo analysis and cadence synchronization  
- **AI Playlist Recommendations**: Intelligent music suggestions based on workout type
- **Voice + Music Integration**: Seamless audio ducking and coaching timing
- **Final Production Polish**: Complete app store submission preparation

## Common Development Patterns

### Working with Background Services
- **BackgroundLocationService**: Use `BackgroundLocationService.startService()` for GPS tracking
- **Service Integration**: Access via `RunSessionRepositoryImpl.startSessionWithBackgroundService()`
- **Crash Recovery**: Use `SessionRecoveryManager.saveRecoveryData()` for persistence
- **Permission Handling**: Use `PermissionManager.requestAllPermissions()` before service start

### Voice Coaching Integration
- **Coach Selection**: Use `CoachPersonalitySelector` component with `VoiceCoachingManager`
- **Trigger Configuration**: Configure coaching via `SmartTriggerEngine.configureTriggers()`
- **Audio Management**: Handle music ducking via `AudioFocusManager.requestAudioFocus()`
- **Voice Caching**: Use `VoiceCacheManager.preloadCoachingLines()` for offline support

### Adding New Screens
1. Create screen in `presentation/screen/[feature]/`
2. Add screen route to `presentation/navigation/Screen.kt`
3. Update navigation in `MainActivity.kt`
4. Create corresponding ViewModel if needed
5. Add comprehensive tests (unit + UI)
6. Ensure permission flows are handled if GPS/location needed

### Working with State and Background Data
- Use `StateFlow` for UI state management in ViewModels
- Leverage `RunSessionRepository.getRealTimeMetrics()` for live GPS data
- Follow unidirectional data flow patterns with background service integration
- Handle service lifecycle with proper cleanup in ViewModels

### Database Operations
- Implement Repository pattern for data access with caching
- Use Room v4 schema with voice caching entities (`VoiceLineEntity`, `CoachPersonalityEntity`)
- Handle migrations properly when adding new entities
- Use coroutines for asynchronous database operations
- Follow transaction patterns for multi-entity operations

### Testing Critical Functionality
- **P0 Tests**: Always test GPS background continuity, permission flows, crash recovery
- **Voice Tests**: Verify latency <200ms, audio focus management, coach personalities
- **Integration Tests**: Test complete run session workflows with background services
- **Performance Tests**: Battery usage, GPS accuracy, voice synthesis performance

### API Key Management
- Store keys in `local.properties` (never commit)
- Access via `BuildConfig.ELEVENLABS_API_KEY`, etc.
- Use test keys for development and CI/CD
- Implement proper error handling for API rate limits

This codebase represents a production-ready Android application with advanced background services, AI voice coaching, and comprehensive testing following modern development practices and clean architecture principles.

## 🚨 Common LLM Coding Mistakes - Critical Learning Log

**RULE**: This section MUST be updated every time a mistake is identified. Always check this list before making changes.

### Health Connect Migration Mistakes

18. **Referenced Non-Existent Database Dependencies** ⚠️
    - **Mistake**: Using `HealthConnectDailySummaryEntity` and `healthConnectDao` before creating them
    - **Impact**: Compilation errors, missing database infrastructure
    - **Fix**: Always create database entities and DAOs before using them in managers
    - **Occurred**: HealthConnectManager implementation - NEEDS IMMEDIATE FIX
    - **Prevention**: Check all dependencies exist before using them in business logic

### Database & Entity Mistakes

1. **Property Name Inconsistencies** ⚠️
   - **Mistake**: Using different property names between entities and domain models (`averageHeartRate` vs `avgHeartRate`)
   - **Impact**: Compilation errors, data mapping failures
   - **Fix**: Always align property names across all layers (Entity → Domain → UI)
   - **Occurred**: Sprint 4.1, resolved by QA agent

2. **Missing Required Fields in Constructor** ⚠️
   - **Mistake**: Adding new fields to entities but forgetting to update all instantiation sites
   - **Impact**: Compilation errors, runtime crashes
   - **Fix**: Use IDE "Find Usages" to locate all constructor calls when adding fields
   - **Occurred**: RunSessionEntity updates in Sprint 4.1

3. **Invalid Flow Method Usage** ⚠️
   - **Mistake**: Using `replayCache` and `value` on StateFlow incorrectly
   - **Impact**: Compilation errors, incorrect data access patterns
   - **Fix**: Use `first()` for single values, `collect` for observation
   - **Occurred**: SettingsScreen.kt line 369, 385 - NEEDS FIX

### API Integration Mistakes

4. **Deprecated API Method Usage** ⚠️
   - **Mistake**: Using `accessSessionsRead()/Write()` methods that don't exist in FitnessOptions
   - **Impact**: Compilation errors, broken Google Fit integration
   - **Fix**: Check Google Fit API documentation for current methods
   - **Occurred**: GoogleFitManager initial implementation

5. **Missing Import Statements** ⚠️
   - **Mistake**: Creating new classes without proper imports, especially for WorkManager
   - **Impact**: Unresolved reference errors
   - **Fix**: Always verify imports when creating new files
   - **Occurred**: GoogleFitSyncWorker implementation

### Architecture Mistakes

6. **Context Casting in Wrong Places** ⚠️
   - **Mistake**: Casting Application context to ComponentActivity
   - **Impact**: Runtime crashes on app startup
   - **Fix**: Only create Activity-dependent services at Activity level
   - **Occurred**: AppContainer PermissionManager setup

7. **Database Singleton Pattern Violations** ⚠️
   - **Mistake**: Creating multiple database instances instead of using singleton
   - **Impact**: Data inconsistency, performance issues
   - **Fix**: Always use `FITFOAIDatabase.getDatabase(context)` pattern
   - **Occurred**: Sprint 3.4 AppContainer fixes

### Type System Mistakes

8. **Incorrect Type Conversions** ⚠️
   - **Mistake**: Int/Float mismatches in heart rate data, pace calculations
   - **Impact**: Compilation errors, data precision loss
   - **Fix**: Use explicit type casting and check data types in entities
   - **Occurred**: Multiple repository implementations

9. **Enum Constant Replacement** ⚠️
   - **Mistake**: Using hardcoded strings instead of proper enum constants
   - **Impact**: Magic string usage, type safety loss
   - **Fix**: Create proper enums and use constants
   - **Occurred**: FitnessActivities.RUNNING → "running"

### State Management Mistakes

10. **Improper StateFlow Usage** ⚠️
    - **Mistake**: Accessing `value` property on StateFlow instead of collecting
    - **Impact**: Non-reactive UI, stale data
    - **Fix**: Use `collectAsState()` in Compose, `collect` in ViewModels
    - **Occurred**: Multiple ViewModel implementations

11. **Missing DAO Methods** ⚠️
    - **Mistake**: Calling DAO methods that don't exist (getActiveSession)
    - **Impact**: Compilation errors, missing functionality
    - **Fix**: Implement all required DAO methods before using them
    - **Occurred**: RunSessionRepositoryImpl

### Testing Mistakes

12. **Package Name Mismatches in Tests** ⚠️
    - **Mistake**: Using wrong package names in test files
    - **Impact**: Test failures, incorrect package assertions
    - **Fix**: Verify package names match actual app package structure
    - **Occurred**: Initial test setup

13. **Missing Test Coverage for Critical Paths** ⚠️
    - **Mistake**: Not testing edge cases like null values, concurrent access
    - **Impact**: Production bugs, data integrity issues
    - **Fix**: Always test happy path, edge cases, and error scenarios
    - **Prevention**: QA agent comprehensive test planning

### Performance Mistakes

14. **Main Thread Database Access** ⚠️
    - **Mistake**: Performing database operations without Dispatchers.IO
    - **Impact**: ANRs, poor user experience
    - **Fix**: Always use `withContext(Dispatchers.IO)` for database operations
    - **Prevention**: StrictMode detection in debug builds

15. **Memory Leaks in Singletons** ⚠️
    - **Mistake**: Storing Context references in static fields
    - **Impact**: Memory leaks, potential crashes
    - **Fix**: Use ApplicationContext and be careful with singleton lifecycle
    - **Occurred**: GoogleFitManager singleton implementation

### Build System Mistakes

16. **Missing Gradle Dependencies** ⚠️
    - **Mistake**: Using classes without adding required dependencies
    - **Impact**: Compilation errors, missing functionality
    - **Fix**: Check `libs.versions.toml` before using external libraries
    - **Prevention**: Verify dependencies when importing new classes

17. **Incorrect WorkManager Builder Syntax** ⚠️
    - **Mistake**: Using wrong PeriodicWorkRequest constructor parameters
    - **Impact**: Build failures in background services
    - **Fix**: Use correct builder pattern for WorkManager
    - **Occurred**: GoogleFitManager background sync setup

### Self-Reflection Questions (Check Before Every Change):

1. ❓ Did I verify all property names match between layers?
2. ❓ Did I check that all required constructor parameters are provided?
3. ❓ Did I use `first()` instead of `value` on StateFlow?
4. ❓ Did I add `withContext(Dispatchers.IO)` for database operations?
5. ❓ Did I check the API documentation for deprecated methods?
6. ❓ Did I run a build test after making changes?
7. ❓ Did I verify all imports are correct?
8. ❓ Did I check for proper enum usage instead of magic strings?
9. ❓ Did I test edge cases and null scenarios?
10. ❓ Did I follow the existing code patterns in the file?

### 🎯 Learning Protocol:
- **BEFORE** making any change: Review mistakes #1-17 relevant to the task
- **DURING** implementation: Stop if patterns match previous mistakes
- **AFTER** error occurs: Add to this list with mistake details and fix
- **WEEKLY** review: Analyze patterns and create prevention strategies

This critical learning log ensures continuous improvement and prevents repeated mistakes across sprints.
