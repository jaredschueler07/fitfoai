# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FITFO AI (RunningCoach v2) is an Android application built with Kotlin and Jetpack Compose. It's an AI-powered running coach app that provides personalized training plans, real-time coaching, and integrations with fitness apps like Fitbit, Google Fit, and Spotify.

**Current Status**: Phase 2 Complete - Core features and complete UI implementation finished
**Architecture**: Clean Architecture with MVVM pattern
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

# Run specific test class
./gradlew test --tests "com.runningcoach.v2.domain.usecase.GenerateTrainingPlanUseCaseTest"

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
│   ├── model/          # Core data models (Coach, RaceGoal, TrainingPlan, User)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic use cases
├── data/
│   ├── local/          # Room database, DAOs
│   ├── remote/         # API services, DTOs
│   └── repository/     # Repository implementations
├── presentation/
│   ├── screen/         # UI screens organized by feature
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation definitions
│   └── theme/          # App theming (Colors, Typography, Theme)
└── di/                 # Dependency injection (Hilt - currently disabled)
```

### Key Architectural Decisions
- **Clean Architecture**: Separation of concerns with domain, data, and presentation layers
- **MVVM Pattern**: ViewModels manage UI state and business logic coordination
- **Single Activity Architecture**: MainActivity with Compose Navigation
- **Jetpack Compose**: Modern declarative UI toolkit
- **Room Database**: Local data persistence (temporarily using basic implementation)
- **Ktor Client**: HTTP networking for API calls
- **Coroutines + Flow**: Asynchronous programming and reactive data streams

### Navigation Flow
1. **Onboarding**: Welcome → Connect Apps → Personalize Profile → Set Event Goal
2. **Main App**: Dashboard (with bottom nav to AI Coach, Progress, Profile)

## Key Features

### Core Screens (Phase 2 Complete)
- **WelcomeScreen**: App introduction and onboarding start
- **ConnectAppsScreen**: Third-party app integration (Fitbit, Google Fit, Spotify)
- **PersonalizeProfileScreen**: User profile setup with fitness level and coach selection
- **SetEventGoalScreen**: Race goal configuration
- **DashboardScreen**: Main hub with training plan, progress, and quick actions
- **AICoachScreen**: AI-powered fitness coaching interface

### UI Components
- **AppButton**: Consistent button styling across the app
- **AppCard**: Card-based layout system with dark theme
- **BottomNavigation**: Four-tab navigation (Home, AI Coach, Progress, Profile)
- **Custom Icons**: App-specific iconography

## Development Notes

### Current Limitations
- **Hilt Dependency Injection**: Temporarily disabled due to compatibility issues
- **Room Database**: Using basic implementation without KSP annotation processing
- **Placeholder Screens**: Progress and Profile screens have placeholder implementations

### Technology Stack
- **Build System**: Gradle with version catalogs (`gradle/libs.versions.toml`)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Components**: Navigation Compose, Lifecycle, ViewModel
- **Networking**: Ktor Client with JSON serialization
- **Database**: Room (planned, basic implementation currently)
- **Coroutines**: 1.8.0 for asynchronous operations

### Testing Strategy
- **Unit Tests**: Domain layer logic and ViewModels
- **Integration Tests**: Repository and database operations
- **UI Tests**: Compose UI testing with critical user workflows
- **Target Coverage**: 90%+ unit tests, 80%+ integration tests, 70%+ UI tests

### Important Files
- `app/build.gradle.kts`: Main build configuration
- `gradle/libs.versions.toml`: Dependency version management
- `MainActivity.kt`: Single activity with navigation setup
- `project/`: Comprehensive documentation and planning materials
- `project/README.md`: Detailed project overview and roadmap
- `project/architecture.md`: In-depth technical architecture documentation
- `project/testing-strategy.md`: Complete testing approach and examples

### Future Development (Phases 3-6)
- API integrations (Fitbit, Google Fit, Spotify)
- AI-powered training plan generation
- GPS tracking and real-time coaching
- Advanced analytics and progress tracking
- Production deployment and security hardening

## Common Development Patterns

### Adding New Screens
1. Create screen in `presentation/screen/[feature]/`
2. Add screen route to `presentation/navigation/Screen.kt`
3. Update navigation in `MainActivity.kt`
4. Create corresponding ViewModel if needed
5. Add tests for new components

### Working with State
- Use `StateFlow` for UI state management in ViewModels
- Leverage Compose state hoisting for component state
- Follow unidirectional data flow patterns

### Database Operations
- Implement Repository pattern for data access
- Use coroutines for asynchronous database operations
- Follow Room best practices for entity definitions

This codebase represents a well-structured Android application following modern development practices and clean architecture principles.