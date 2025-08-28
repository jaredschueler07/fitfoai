# 🎯 FITFOAI SPRINT EXECUTION: P0 BLOCKERS + VOICE COACHING

@all-agents ACTIVATE for critical fixes and Sprint 3.2 implementation. This sprint addresses:
1. P0 BLOCKERS (GPS/Permission/Persistence issues)
2. Sprint 3.2: Voice Coaching System with ElevenLabs

## 🔴 P0 BLOCKERS CRITICAL PATH (Must Complete First)

@product-manager Begin immediately:
1. Review /Users/jaredschueler07/AndroidStudioProjects/FITFOAI and assess current state
2. Create user stories for P0 blockers:
   - "As a runner, I need GPS tracking to continue when app is backgrounded"
   - "As a user, I need proper permission flow for Android 12+ location access"
   - "As a runner, I need my run session to recover after app restart"
3. Define acceptance criteria for each blocker
4. Update /project/fitfoai-claude-prd.md with blocker status and Sprint 3.2 details
5. Create technical specification for voice coaching integration
6. Commit: `git add . && git commit -m "[PM][P0+3.2] Sprint planning complete"`
7. Tag: [P0-READY-FOR-DEV] and [3.2-READY-FOR-DEV]

@backend-ml-database-expert Implement P0 blockers and voice backend:

**P0 BLOCKERS (Priority 0):**
1. **Background Location Service**:
   ```kotlin
   // Create LocationService.kt in com.runningcoach.v2.services
   - Implement foreground service with notification
   - Use FusedLocationProviderClient with 5-second updates
   - Handle Doze mode and battery optimization
   - Persist location updates to Room database
   - Implement automatic restart on crash
   ```

2. **Modern Permission Flow**:
   ```kotlin
   // Create PermissionManager.kt
   - Handle ACCESS_FINE_LOCATION permission
   - Handle ACCESS_COARSE_LOCATION fallback
   - Implement Android 12+ precise/approximate location
   - Handle ACCESS_BACKGROUND_LOCATION for Android 10+
   - Create permission rationale dialogs
   ```

3. **Run Session Persistence**:
   ```kotlin
   // Update RunSessionRepository.kt
   - Implement session recovery mechanism
   - Create RunSessionEntity with state management
   - Add crash recovery with last known state
   - Implement WorkManager for periodic sync
   - Handle incomplete sessions on app restart
   ```

**SPRINT 3.2 VOICE BACKEND:**
4. **ElevenLabs Integration**:
   ```kotlin
   // Create ElevenLabsTTSManager.kt
   - API integration with voice synthesis
   - Voice line caching in Room database
   - Audio queue management with priority
   - Implement voice personalities (Bennett, Mariana, Becs, Goggins)
   ```

5. **Smart Trigger Engine**:
   ```kotlin
   // Create SmartTriggerEngine.kt
   - Context analysis for coaching triggers
   - Pace/HR zone guidance logic
   - Milestone detection (1km, 5km, etc.)
   - Motivational message timing
   ```

Commit: `git add . && git commit -m "[BACKEND][P0+3.2] Location service, permissions, and voice backend complete"`
Hand over to @android-ui-designer

@android-ui-designer Build UI components for fixes and voice coaching:

**P0 UI FIXES:**
1. **Permission Request Flow**:
   ```kotlin
   // Create PermissionScreen.kt
   - Modern permission request UI with rationale
   - Step-by-step permission flow for Android 12+
   - Educational cards explaining why permissions needed
   - Gradient background (#1e3a5f to #4a7c97)
   ```

2. **Background Tracking UI**:
   ```kotlin
   // Update RunTrackingScreen.kt
   - Persistent notification design
   - Background tracking indicator
   - Battery optimization warning dialog
   - Session recovery UI after crash
   ```

**SPRINT 3.2 VOICE UI:**
3. **Voice Coaching Controls**:
   ```kotlin
   // Create VoiceCoachingCard.kt
   - Coach selection UI (4 personalities)
   - Volume/mute controls
   - Voice preview button
   - Coaching frequency slider
   ```

4. **Audio Feedback UI**:
   ```kotlin
   // Create AudioFeedbackOverlay.kt
   - Visual indicator when coach speaks
   - Transcript display of last message
   - Audio ducking indicator
   - Coaching history view
   ```

5. **Settings Screen Updates**:
   ```kotlin
   // Update SettingsScreen.kt
   - Voice coaching preferences section
   - Coach personality selection
   - Coaching triggers configuration
   - Audio output device selection
   ```

Style: Use blue gradient (#1e3a5f to #4a7c97) with coral accents (#ff6b6b)
Commit: `git add . && git commit -m "[UI][P0+3.2] Permission flow, background UI, and voice controls complete"`
Hand over to @devops-architecture-engineer

@devops-architecture-engineer Fix build issues and ensure proper architecture:

**P0 INFRASTRUCTURE FIXES:**
1. **AndroidManifest.xml Updates**:
   ```xml
   - Add FOREGROUND_SERVICE permission
   - Add FOREGROUND_SERVICE_LOCATION permission (Android 14+)
   - Add POST_NOTIFICATIONS permission
   - Add WAKE_LOCK permission
   - Configure foreground service types
   - Add background location permission declarations
   ```

2. **Build Configuration**:
   ```kotlin
   // Update build.gradle.kts
   - Add ElevenLabs SDK dependencies
   - Configure ProGuard rules for background services
   - Add WorkManager dependency
   - Ensure minSdk 26 for foreground services
   ```

3. **Dependency Injection**:
   ```kotlin
   // Update Hilt modules
   - Provide LocationService binding
   - Provide ElevenLabsTTSManager
   - Provide PermissionManager
   - Fix any Hilt/KSP compatibility issues
   ```

**SPRINT 3.2 ARCHITECTURE:**
4. **Audio Architecture**:
   ```kotlin
   // Create AudioModule.kt
   - Configure MediaPlayer providers
   - Set up AudioFocusManager
   - Configure audio caching strategy
   ```

5. **Service Architecture**:
   ```kotlin
   // Ensure proper service lifecycle
   - Implement ServiceLocator pattern
   - Configure WorkManager for background tasks
   - Set up proper error boundaries
   ```

Commit: `git add . && git commit -m "[DEVOPS][P0+3.2] Manifest, dependencies, and architecture complete"`
Hand over to @qa-testing-specialist

@qa-testing-specialist Create comprehensive test coverage:

**P0 BLOCKER TESTS:**
1. **Background Location Tests**:
   ```kotlin
   // LocationServiceTest.kt
   - Test GPS continues when app backgrounded
   - Test location updates persist to database
   - Test service restart after crash
   - Test battery optimization handling
   - Test accuracy < 5 meters requirement
   ```

2. **Permission Flow Tests**:
   ```kotlin
   // PermissionManagerTest.kt
   - Test Android 12+ permission flow
   - Test permission denial handling
   - Test rationale display logic
   - Test background location permission
   ```

3. **Session Persistence Tests**:
   ```kotlin
   // RunSessionPersistenceTest.kt
   - Test session recovery after crash
   - Test incomplete session handling
   - Test data integrity after restart
   - Test WorkManager sync
   ```

**SPRINT 3.2 VOICE TESTS:**
4. **Voice Integration Tests**:
   ```kotlin
   // ElevenLabsIntegrationTest.kt
   - Test API connection (use test API key)
   - Test voice synthesis < 200ms latency
   - Test audio playback
   - Test voice line caching
   ```

5. **Coaching Logic Tests**:
   ```kotlin
   // SmartTriggerEngineTest.kt
   - Test pace guidance triggers
   - Test milestone celebrations
   - Test motivation timing
   - Test audio priority queue
   ```

6. **UI Tests**:
   ```kotlin
   // VoiceCoachingUITest.kt
   - Test coach selection flow
   - Test audio controls
   - Test visual feedback
   - Test settings persistence
   ```

**Test Report Format**:
```
P0 BLOCKERS:
✅/❌ Background GPS tracking works
✅/❌ Permissions properly requested
✅/❌ Session recovery functional

SPRINT 3.2:
✅/❌ Voice synthesis works
✅/❌ Coaching triggers fire correctly
✅/❌ Audio plays during runs
✅/❌ All 4 coach personalities work

BUGS FOUND:
[P0] - Description and reproduction steps
[P1] - Description and reproduction steps
```

Commit: `git add . && git commit -m "[QA][P0+3.2] Test suite complete with X tests passing"`
Hand over to @product-manager

@product-manager Provide sprint summary and prepare next sprint:

1. **Review all completed work**
2. **Update /project/fitfoai-claude-prd.md with:**
   - P0 blocker resolution status
   - Sprint 3.2 completion status
   - Updated metrics and KPIs
   - Risk assessment updates
3. **Create Sprint 3.3 plan (Spotify Integration)**
4. **Document lessons learned**
5. **Update CHANGELOG.md**

**Summary Report Template**:
```markdown
## Sprint Summary: P0 Blockers + 3.2 Voice Coaching

### P0 BLOCKERS RESOLVED:
✅ Background Location Service - GPS now works when backgrounded
✅ Modern Permission Flow - Android 12+ properly handled
✅ Run Session Persistence - Sessions recover after crashes

### SPRINT 3.2 COMPLETED:
✅ ElevenLabs TTS integration
✅ 4 coach personalities implemented
✅ Smart coaching triggers
✅ Audio management system
✅ Voice UI controls

### Key Metrics:
- GPS accuracy: < 5m achieved
- Voice latency: < 200ms achieved
- Test coverage: XX% unit, XX% integration
- Build time: X minutes

### Next Sprint 3.3: Spotify Integration
- OAuth authentication
- Playlist recommendations
- BPM matching to cadence
- Automatic track selection
```

Commit: `git add . && git commit -m "[PM][SPRINT-COMPLETE] P0 blockers resolved, Sprint 3.2 complete"`

## 🔄 EXECUTION FLOW

1. **IMMEDIATE**: All agents read current code at /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
2. **PHASE 1 (Day 1-2)**: Fix P0 blockers - these are CRITICAL
3. **PHASE 2 (Day 2-4)**: Implement Sprint 3.2 Voice Coaching
4. **PHASE 3 (Day 4-5)**: Testing and integration
5. **PHASE 4 (Day 5)**: Documentation and handoff

## 📊 SUCCESS CRITERIA

**P0 Blockers (MUST HAVE):**
- [ ] GPS tracking continues when app backgrounded
- [ ] Android 12+ permissions properly requested
- [ ] Run sessions recover after app restart
- [ ] All tests passing for critical paths

**Sprint 3.2 Voice Coaching (MUST HAVE):**
- [ ] ElevenLabs API integrated
- [ ] Voice synthesis < 200ms latency
- [ ] 4 coach personalities working
- [ ] Context-aware coaching triggers
- [ ] Audio plays over music with ducking
- [ ] Settings persist across sessions

## 🚨 CRITICAL NOTES

1. **Test with REAL DEVICE** - Emulator GPS is unreliable
2. **Use TEST API KEYS** - Don't commit production keys
3. **Handle API limits** - ElevenLabs has rate limits
4. **Battery optimization** - Critical for background GPS
5. **Audio focus** - Must handle music app interactions

## 🎯 BEGIN EXECUTION

All agents start immediately. P0 blockers are CRITICAL and block production release.
Sprint 3.2 builds the core voice coaching experience.

Project location: /Users/jaredschueler07/AndroidStudioProjects/FITFOAI

GO! Report progress every 30 minutes.