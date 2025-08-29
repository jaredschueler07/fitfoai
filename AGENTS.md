# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android app (Kotlin, Jetpack Compose). Main code under `app/src/main/java/com/runningcoach/v2/`; resources under `app/src/main/res/`; manifest at `app/src/main/AndroidManifest.xml`.
- Tests: unit tests in `app/src/test/`; instrumentation/UI tests in `app/src/androidTest/`.
- Web prototype: `project/runningcoach-ai/` (Vite + React + TypeScript).
- Docs and plans: `project/` and root `*.md` files (e.g., Google Fit, architecture, sprint plans).

## Build, Test, and Development Commands
- Android build (debug): `./gradlew assembleDebug` — compiles APK.
- Install/run on device: `./gradlew installDebug` — installs debug build to a connected device/emulator.
- Unit tests: `./gradlew testDebugUnitTest` — runs JVM tests in `app/src/test`.
- Instrumented/UI tests: `./gradlew connectedDebugAndroidTest` — runs tests in `app/src/androidTest` on a device/emulator.
- Lint: `./gradlew :app:lint` — Android static analysis report.
- Web prototype: `cd project/runningcoach-ai && npm ci && npm run dev` — local dev server.

## Coding Style & Naming Conventions
- Kotlin: 2‑space indent, `camelCase` for vars/functions, `PascalCase` for classes/files, package names `lowercase.dot.separated` (e.g., `com.runningcoach.v2`).
- Compose: stateless UI where possible; `@Composable` names use `PascalCase` (e.g., `RunTrackingScreen`).
- Resources: `snake_case` (e.g., `ic_launcher_foreground.xml`, `voice_status_indicator.png`); strings in `res/values/strings.xml`.
- Tests: mirror package structure; name with `...Test.kt` or `...UITest.kt`.

## Testing Guidelines
- Frameworks: JUnit4, MockK, Robolectric for unit; Espresso and Compose UI Test for instrumentation.
- Coverage: add tests for new logic and critical flows (run session, permissions, voice coaching, Google Fit).
- Run locally with the Gradle commands above; prefer fast unit tests before UI tests.

## Commit & Pull Request Guidelines
- Commits follow Conventional Commits where practical: `feat:`, `fix:`, `docs:`; tags like `[ARCH-CHANGE]`, `[UI-UPDATE]`, and sprint markers appear in history.
- Helper: `./git-agent-commit.sh PM 3.1 "Updated PRD"` → commits as `[PM][3.1] Updated PRD`.
- PRs: include clear description, linked issues, screenshots/screen recordings for UI, and test evidence (commands/output). Keep PRs focused and buildable.

## Security & Configuration Tips
- Secrets load from `local.properties` (see `app/build.gradle.kts`): set `GEMINI_API_KEY`, `ELEVENLABS_API_KEY`, `GOOGLE_MAPS_API_KEY`, `SPOTIFY_*`, `GOOGLE_FIT_CLIENT_ID`.
- Do not commit keys; `.gitignore` covers local files. Validate keys via debug builds before PRs.
