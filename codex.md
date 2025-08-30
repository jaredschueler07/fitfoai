# Codex Change Log (FITFOAI)

This file tracks detailed, developer‑focused changes applied via Codex.

## 2025-08-29 – Debug banner, Fit controls, Imperial standardization, Run history UI, Plan generation

### Build & Diagnostics
- Added `BuildConfig.GIT_SHA` in `app/build.gradle.kts` using `git rev-parse --short HEAD`.
- Logged one-line startup info in `MainActivity.onCreate` with `APPLICATION_ID`, `VERSION_NAME`, `BUILD_TYPE`, `AI_PROVIDER`, `GIT_SHA`.
- Added `DebugBuildBanner` composable and overlaid it in `RunningCoachApp` (top-left) for debug builds only.

Files:
- app/build.gradle.kts
- app/src/main/java/com/runningcoach/v2/MainActivity.kt
- app/src/main/java/com/runningcoach/v2/presentation/components/DebugBuildBanner.kt

### Settings: Google Fit controls
- In Settings, added Google Fit connection utilities: retry sign-in/permissions and disconnect.

File:
- app/src/main/java/com/runningcoach/v2/presentation/screen/settings/SettingsScreen.kt

### Profile: Google Fit prefill restored
- Restored auto-fill (name/height/weight) from Google Fit in `PersonalizeProfileScreen`.
- Re-enabled body measurement update from Fit during daily sync in `GoogleFitRepository`.

Files:
- app/src/main/java/com/runningcoach/v2/presentation/screen/profile/PersonalizeProfileScreen.kt
- app/src/main/java/com/runningcoach/v2/data/repository/GoogleFitRepository.kt

### Profile: Height & Weight UX
- Height: replaced free-text field with dropdown (4'10"–7'0").
- Weight: numeric-only input with small “lbs” suffix.

File:
- app/src/main/java/com/runningcoach/v2/presentation/screen/profile/PersonalizeProfileScreen.kt

### Imperial Standardization (UI/display) & Unit Parsing
- User profile save now interprets height entered as feet/inches → stores cm; weight entered as lbs → stores kg (DB continues metric for compatibility).
- Chat context strings now display miles, lbs, and ft/in as appropriate.
- Domain RunSession display now shows miles and min/mi.

Files:
- app/src/main/java/com/runningcoach/v2/data/repository/UserRepository.kt (imperial parsing → metric storage)
- app/src/main/java/com/runningcoach/v2/data/service/ChatContextProvider.kt (miles/lbs formatting)
- app/src/main/java/com/runningcoach/v2/domain/repository/RunSessionRepository.kt (imperial display formatting)

### Google Fit Import Enhancements
- Import window extended to last 90 days to support plan baselines.
- Imported sessions now set `source=GOOGLE_FIT` for attribution.

File:
- app/src/main/java/com/runningcoach/v2/data/manager/GoogleFitManager.kt

### Domain: Run session source surfaced
- Added `SessionSource` to domain model; repository now maps Room `DataSource` → domain `SessionSource`.

Files:
- app/src/main/java/com/runningcoach/v2/domain/repository/RunSessionRepository.kt
- app/src/main/java/com/runningcoach/v2/data/repository/RunSessionRepositoryImpl.kt

### Progress: Run history UI & weekly stats by source
- New `ProgressViewModel`:
  - Loads recent runs (limit 20) via `GetRunSessionsUseCase` and maps to `RunListItem` with imperial formatting + source.
  - Computes last 7 days stats split by source using `RunSessionDao.getSessionsInDateRange`.
- Updated `ProgressScreen` to render:
  - “This Week (by Source)” card showing FITFOAI/Google Fit miles and counts.
  - “Recent Runs” list with compact source badges.
- Integrated viewmodel wiring in `MainActivity` for `Screen.Progress` route.

Files:
- app/src/main/java/com/runningcoach/v2/presentation/screen/progress/ProgressViewModel.kt (new)
- app/src/main/java/com/runningcoach/v2/presentation/screen/progress/ProgressScreen.kt
- app/src/main/java/com/runningcoach/v2/MainActivity.kt

### Plan Generation (Gemini) – Use Case + Settings trigger
- New `GenerateTrainingPlanUseCase`:
  - Computes 90-day baseline (weekly avg miles, longest run, approx runs/week).
  - Calls `LLMService.generateTrainingPlan` with goals, fitness level, race, timeframe, embedding baseline.
  - Persists `TrainingPlanEntity` with plan text and baseline JSON.
- DI: Exposed in `AppContainer` as `generateTrainingPlanUseCase`.
- Settings: Added “Generate Training Plan” action under Connected Apps to open `PlanGenerationDialog` and run the use case.

Files:
- app/src/main/java/com/runningcoach/v2/domain/usecase/GenerateTrainingPlanUseCase.kt (new)
- app/src/main/java/com/runningcoach/v2/di/AppModule.kt
- app/src/main/java/com/runningcoach/v2/presentation/screen/settings/SettingsScreen.kt

### Security Note
- Ensure there are no real API keys committed. Rotate any found in docs. All runtime keys should stay in `local.properties`.

### Follow-ups / Next Steps
- Calendar month view and workout drill‑in (pending; not implemented in this batch).
- Hook plan generation automatically post Fit connect + profile complete (if desired).
- Add DAO queries for per-source aggregates over periods to reduce app-side processing.

