# 🚨 FITFOAI - Critical Bug Resolution Sprint Plan

**Document Version**: 1.0  
**Created**: August 28, 2025  
**Sprint Duration**: 5 Days (Aug 28 - Sep 1, 2025)  
**Sprint Goal**: Resolve all critical bugs identified in canonical bug report list  
**Production Readiness Target**: 95% (from current 85%)

---

## 📋 Executive Summary

This sprint addresses the 5 critical issues identified in the canonical bug report list that are currently blocking the testing workflow and preventing full app functionality. The sprint prioritizes Google Fit integration and user profile persistence as P0 blockers, followed by performance optimization and imperial unit conversion.

## 🎯 Sprint Goals & Success Criteria

### Primary Sprint Goal
**"Deliver a fully functional, testable app with persistent profiles and working Google Fit integration"**

### Acceptance Criteria
- ✅ Google Fit integration auto-fills profile data with at minimum a valid name
- ✅ User profiles persist between app sessions with option to reset
- ✅ All placeholder content replaced with functional implementations
- ✅ Imperial units (feet/inches, pounds, miles, Fahrenheit) throughout the app
- ✅ Mapping performance improved or alternative solution implemented
- ✅ Testing workflow unblocked for continuous development

### Success Metrics
- **Profile Auto-Fill Success Rate**: 90%+ from Google Fit
- **Profile Persistence Rate**: 100% between sessions
- **Unit Conversion Accuracy**: 100% imperial units
- **Performance Improvement**: 50% reduction in map load times
- **Testing Efficiency**: 0 manual re-onboarding required

---

## 📊 Task Breakdown & Agent Assignments

### 🚨 P0 - Critical Path Tasks (Must Complete Day 1-2)

#### Task 1: Google Fit Profile Auto-Fill Integration
**Agent**: @agent-backend-ml-database-expert  
**Priority**: P0-CRITICAL  
**Estimated Effort**: 8 hours  
**Dependencies**: None

**User Story**: 
"As a user, I want my profile fields (name, height, weight) auto-populated from Google Fit data so that I can skip manual data entry during onboarding."

**Acceptance Criteria**:
- GIVEN I successfully connect to Google Fit
- WHEN I navigate to PersonalizeProfileScreen  
- THEN my name MUST be auto-filled from Google account
- AND my height MUST be auto-filled if available in Google Fit
- AND my weight MUST be auto-filled if available in Google Fit
- AND empty fields should show placeholder hints for manual entry
- AND auto-fill should occur within 2 seconds of screen load

**Technical Implementation**:
- Extend `GoogleFitService.kt` with profile data retrieval
- Add `getUserProfileData()` method returning name, height, weight
- Integrate Google Sign-In account name extraction  
- Update `PersonalizeProfileScreen.kt` to call auto-fill on screen load
- Handle permission errors gracefully with fallback to manual entry

**Files to Modify**:
- `app/src/main/java/com/runningcoach/v2/data/service/GoogleFitService.kt`
- `app/src/main/java/com/runningcoach/v2/presentation/screen/profile/PersonalizeProfileScreen.kt`

**Testing Requirements**:
- Unit tests for GoogleFitService profile data methods
- Integration tests for PersonalizeProfileScreen auto-fill behavior
- Edge case testing: no permissions, network errors, missing data

---

#### Task 2: Persistent User Profile & Navigation Logic
**Agent**: @agent-backend-ml-database-expert  
**Priority**: P0-CRITICAL  
**Estimated Effort**: 6 hours  
**Dependencies**: Task 1 (GoogleFitService updates)

**User Story**: 
"As a developer, I want user profiles to persist between app sessions so that I can test the app without repeating onboarding every time."

**Acceptance Criteria**:
- GIVEN a user completes onboarding once
- WHEN the app is restarted
- THEN the app MUST skip to Dashboard screen
- AND provide a "Reset Profile" option in settings for testing
- AND preserve all profile data (name, fitness level, goals, coach selection)
- AND maintain Google Fit connection status

**Technical Implementation**:
- Update `MainActivity.kt` navigation logic to check for existing profile
- Add `UserRepository.hasCompletedOnboarding()` method
- Implement profile-based routing in `RunningCoachApp()`
- Add "Reset Profile" feature in Settings screen
- Create migration path for existing users

**Files to Modify**:
- `app/src/main/java/com/runningcoach/v2/MainActivity.kt`
- `app/src/main/java/com/runningcoach/v2/data/repository/UserRepository.kt`
- `app/src/main/java/com/runningcoach/v2/presentation/screen/settings/SettingsScreen.kt`

**Testing Requirements**:
- Unit tests for UserRepository profile check methods
- Integration tests for navigation flow based on profile state
- UI tests for reset profile functionality

---

#### Task 3: Imperial Unit System Implementation
**Agent**: @agent-android-ui-designer  
**Priority**: P0-CRITICAL  
**Estimated Effort**: 8 hours  
**Dependencies**: Task 1 (PersonalizeProfileScreen updates)

**User Story**: 
"As an American user, I want to input and see my measurements in imperial units (feet/inches, pounds, miles, Fahrenheit) so that the app is intuitive for me to use."

**Acceptance Criteria**:
- GIVEN I am on the PersonalizeProfileScreen
- WHEN I input height
- THEN I can enter in feet and inches format (e.g., "5'8\"" or "5 feet 8 inches")
- AND weight input accepts pounds (e.g., "150 lbs" or "150 pounds")
- WHEN viewing run data
- THEN distances are shown in miles (e.g., "3.2 miles")
- AND pace is shown in minutes per mile (e.g., "7:30/mile")
- AND temperature (if applicable) is shown in Fahrenheit

**Technical Implementation**:
- Create `ImperialUnitConverter.kt` utility class
- Update height parsing in `UserRepository.kt` to handle feet/inches
- Update weight parsing in `UserRepository.kt` to handle pounds
- Modify `RunMetrics.kt` to store and display imperial units
- Update all distance calculations to use miles
- Add input formatters for height (feet/inches) and weight (lbs)

**Files to Modify**:
- `app/src/main/java/com/runningcoach/v2/presentation/screen/profile/PersonalizeProfileScreen.kt`
- `app/src/main/java/com/runningcoach/v2/data/repository/UserRepository.kt`
- `app/src/main/java/com/runningcoach/v2/domain/model/RunMetrics.kt`
- `app/src/main/java/com/runningcoach/v2/domain/model/User.kt`
- Create: `app/src/main/java/com/runningcoach/v2/utils/ImperialUnitConverter.kt`

**Testing Requirements**:
- Unit tests for ImperialUnitConverter methods
- UI tests for height/weight input validation
- Integration tests for run data display in imperial units

---

### 📈 P1 - High Priority Tasks (Complete Day 2-3)

#### Task 4: Google Maps Performance Optimization
**Agent**: @agent-devops-architecture-engineer  
**Priority**: P1-HIGH  
**Estimated Effort**: 10 hours  
**Dependencies**: None

**User Story**: 
"As a user, I want Google Maps to load quickly (within 2-3 seconds) so that I can start tracking my runs without delays."

**Acceptance Criteria**:
- GIVEN I navigate to RunTrackingScreen
- WHEN Google Maps initializes
- THEN the map MUST be interactive within 3 seconds
- AND show my current location within 5 seconds
- AND handle low connectivity scenarios gracefully
- AND provide fallback options if maps fail to load

**Technical Implementation**:
- Implement map preloading in background during onboarding
- Add map tile caching for offline usage
- Optimize map initialization parameters
- Research and implement alternative mapping solution (OpenStreetMap/Mapbox) for A/B testing
- Add map load timeout handling
- Implement progressive map loading (basic map first, details after)

**Files to Modify**:
- `app/src/main/java/com/runningcoach/v2/presentation/components/maps/RunTrackingMap.kt`
- `app/src/main/java/com/runningcoach/v2/presentation/screen/runtracking/RunTrackingScreen.kt`
- `app/build.gradle.kts` (for alternative mapping libraries)

**Testing Requirements**:
- Performance tests for map load times
- Network connectivity variation testing
- Memory usage optimization testing

---

#### Task 5: Replace Placeholder Content with Functional Implementations
**Agent**: @agent-backend-ml-database-expert + @agent-android-ui-designer  
**Priority**: P1-HIGH  
**Estimated Effort**: 12 hours  
**Dependencies**: Tasks 1-3 completion

**User Story**: 
"As a user, I want all app features to work with real data instead of showing placeholder text so that the app feels complete and professional."

**Acceptance Criteria**:
- GIVEN I use any app feature
- WHEN I interact with the interface
- THEN all data MUST be functional (no "TODO" or placeholder content)
- AND all buttons/actions MUST perform their intended functions
- AND all screens MUST display real or calculated data
- AND error states MUST be handled appropriately

**Technical Implementation**:
- Audit all files for "TODO", "placeholder", "mock" content
- Implement actual data retrieval methods
- Replace mock data with database queries
- Add proper error handling for all data operations
- Implement loading states for async operations
- Add input validation for all user input fields

**Files to Audit and Fix**:
- All services in `app/src/main/java/com/runningcoach/v2/data/service/`
- All screens in `app/src/main/java/com/runningcoach/v2/presentation/screen/`
- All ViewModels and repositories

**Testing Requirements**:
- Comprehensive integration testing
- Error scenario testing
- Data validation testing

---

## 🔄 Dependencies & Sequencing

### Day 1 (Aug 28)
**Morning (4 hours)**:
- Task 1: Google Fit auto-fill implementation (@agent-backend-ml-database-expert)
- Task 3: Imperial unit converter creation (@agent-android-ui-designer)

**Afternoon (4 hours)**:
- Task 1: Complete Google Fit integration (@agent-backend-ml-database-expert)
- Task 3: PersonalizeProfileScreen imperial units (@agent-android-ui-designer)

### Day 2 (Aug 29)
**Morning (4 hours)**:
- Task 2: Profile persistence & navigation (@agent-backend-ml-database-expert)  
- Task 3: Run metrics imperial conversion (@agent-android-ui-designer)

**Afternoon (4 hours)**:
- Task 2: Complete profile persistence (@agent-backend-ml-database-expert)
- Task 4: Begin maps performance optimization (@agent-devops-architecture-engineer)

### Day 3 (Aug 30)
**Full Day (8 hours)**:
- Task 4: Complete maps optimization (@agent-devops-architecture-engineer)
- Task 5: Begin placeholder content audit (All agents)

### Day 4 (Aug 31)
**Full Day (8 hours)**:
- Task 5: Replace placeholder implementations (All agents)
- Integration testing and bug fixes

### Day 5 (Sep 1)
**Full Day (8 hours)**:
- Final testing and validation
- Bug fixes and polish
- Sprint retrospective and documentation

---

## 🧪 Testing Strategy & Validation

### Testing Phases

#### Phase 1: Unit Testing (Continuous)
- **@agent-qa-testing-specialist**: Create unit tests for all modified methods
- **Coverage Target**: 90%+ on modified code
- **Focus Areas**: GoogleFitService, UserRepository, ImperialUnitConverter

#### Phase 2: Integration Testing (Day 3-4)  
- **@agent-qa-testing-specialist**: End-to-end onboarding flow testing
- **Test Scenarios**: New user, returning user, various data states
- **Performance Validation**: Map load times, profile persistence speed

#### Phase 3: User Acceptance Testing (Day 4-5)
- **@agent-qa-testing-specialist**: Full app workflow testing
- **Test Cases**: Complete user journey from welcome to run tracking
- **Edge Cases**: Network failures, permission denials, data errors

### Quality Gates
1. **Code Review**: All changes must be reviewed by relevant specialist agent
2. **Test Coverage**: Minimum 85% on critical path code
3. **Performance Benchmarks**: Map load <3s, profile persistence <1s
4. **Bug Resolution**: All P0 bugs must be resolved, P1 bugs triaged

---

## 📱 Definition of Done

### Task-Level DoD
- [ ] Code implemented according to acceptance criteria
- [ ] Unit tests written and passing (>85% coverage)
- [ ] Integration tests written for user workflows  
- [ ] Code reviewed by designated agent
- [ ] No P0 bugs remaining
- [ ] Performance requirements met
- [ ] Documentation updated

### Sprint-Level DoD  
- [ ] Google Fit auto-fills name with 90%+ success rate
- [ ] Profile persistence works 100% between app sessions
- [ ] All measurements display in imperial units
- [ ] Google Maps loads within 3 seconds
- [ ] Zero placeholder/TODO content in user-facing features
- [ ] Full onboarding → dashboard → run tracking workflow functional
- [ ] Testing workflow requires no manual re-onboarding
- [ ] All P0 bugs resolved, P1 bugs documented for next sprint

---

## 🚨 Risk Management & Contingencies

### High-Risk Areas

#### Risk 1: Google Fit API Complexity  
**Impact**: High - blocks auto-fill feature  
**Probability**: Medium  
**Mitigation**: Start with Google account name only, expand to fitness data
**Contingency**: Manual profile entry with improved UX

#### Risk 2: Imperial Unit Conversion Complexity
**Impact**: Medium - affects US user experience  
**Probability**: Low  
**Mitigation**: Use proven conversion libraries, extensive testing
**Contingency**: Configuration option to switch between metric/imperial

#### Risk 3: Map Performance on Older Devices
**Impact**: Medium - affects run tracking UX  
**Probability**: Medium  
**Mitigation**: Device-specific optimization, alternative map providers
**Contingency**: Simplified map interface for low-end devices

### Escalation Procedures
1. **Blocked Task**: Tag with `[BLOCKED-CRITICAL]` and escalate to Product Manager
2. **Technical Issues**: Cross-agent consultation within 2 hours
3. **Timeline Risks**: Daily progress assessment with scope adjustment if needed

---

## 📊 Sprint Metrics & KPIs

### Development Metrics
- **Story Points Planned**: 44 points
- **Velocity Target**: 44 points (100% completion)
- **Daily Commit Target**: Minimum 2 commits per agent per day
- **Bug Fix Rate**: <2 hours average resolution time for P0 bugs

### Quality Metrics  
- **Test Coverage**: >85% on modified code
- **Performance Benchmarks**: Map load <3s, profile operations <1s
- **User Experience**: Complete onboarding in <5 minutes
- **Technical Debt**: Zero TODO/placeholder content in production code

### Success Indicators
- **Primary**: Testing workflow no longer requires manual re-onboarding
- **Secondary**: App feels "production-ready" for beta testing
- **Tertiary**: Development velocity increases due to improved testing efficiency

---

## 🎯 Agent-Specific Deliverables

### @agent-backend-ml-database-expert
**Primary Responsibilities**:
- Google Fit profile auto-fill implementation
- User profile persistence logic  
- Database schema optimization for imperial units
- Service layer placeholder content replacement

**Deliverables**:
- Enhanced `GoogleFitService.kt` with profile data methods
- Updated `UserRepository.kt` with persistence checks
- Imperial unit support in data models
- Comprehensive unit tests for all backend changes

### @agent-android-ui-designer
**Primary Responsibilities**:
- Imperial unit UI implementation
- PersonalizeProfileScreen auto-fill integration
- Run tracking display updates for imperial units
- UI placeholder content replacement

**Deliverables**:
- `ImperialUnitConverter.kt` utility class
- Updated PersonalizeProfileScreen with auto-fill and imperial inputs
- Imperial unit display in run tracking and progress screens
- Comprehensive UI tests for imperial unit functionality

### @agent-devops-architecture-engineer  
**Primary Responsibilities**:
- Google Maps performance optimization
- Alternative mapping solution research and implementation
- Build system optimization for new dependencies
- Performance monitoring and benchmarking

**Deliverables**:
- Optimized map loading implementation
- Alternative mapping solution (if implemented)
- Performance benchmarking report
- Build configuration updates for mapping libraries

### @agent-qa-testing-specialist
**Primary Responsibilities**:
- Comprehensive test suite creation for all new features
- Integration testing for complete user workflows
- Performance testing for map loading and profile operations
- Bug validation and regression testing

**Deliverables**:
- Complete test suite covering all modified functionality
- Performance test results and recommendations
- Integration test suite for onboarding → run tracking workflow
- Bug report summaries and resolution validation

---

## 🚀 Sprint Execution Commands

### Initialize Sprint
```bash
# All agents begin sprint execution
@all-agents BEGIN [CRITICAL-BUG-RESOLUTION-SPRINT]
Sprint: Critical Bug Resolution (Aug 28 - Sep 1)
Mode: HIGH-PRIORITY-PARALLEL
Goal: Resolve canonical bug report issues
Communication: Tag all updates with sprint identifier
GO!
```

### Daily Agent Check-ins
```bash
# Daily progress updates required
@agent-backend-ml-database-expert Report progress on Tasks 1, 2, 5
@agent-android-ui-designer Report progress on Tasks 3, 5  
@agent-devops-architecture-engineer Report progress on Task 4
@agent-qa-testing-specialist Report testing status all tasks
```

### Quality Gates Validation
```bash
# Before sprint completion
@agent-qa-testing-specialist VALIDATE [ALL-ACCEPTANCE-CRITERIA]
@all-agents CONFIRM [DEFINITION-OF-DONE] compliance
PRODUCT-MANAGER APPROVE sprint completion
```

---

## 📋 Sprint Backlog Priority Matrix

| Task | Priority | Agent | Effort | Dependencies | Risk Level |
|------|----------|-------|--------|--------------|-----------|
| Google Fit Auto-Fill | P0 | Backend | 8h | None | Medium |
| Profile Persistence | P0 | Backend | 6h | Task 1 | Low |
| Imperial Units | P0 | UI Designer | 8h | Task 1 | Low |
| Maps Performance | P1 | DevOps | 10h | None | Medium |
| Placeholder Removal | P1 | All | 12h | Tasks 1-3 | Low |

**Total Effort**: 44 hours across 4 agents over 5 days = 88% utilization (healthy sprint load)

---

## 🎉 Sprint Success Vision

**Upon completion of this sprint**:
- New users can connect Google Fit and have their profiles auto-populated
- Developers can test features without repeating onboarding  
- American users see familiar imperial units throughout
- Maps load quickly enabling smooth run tracking
- The app feels professional with no placeholder content
- Testing velocity increases dramatically
- App achieves 95% production readiness

**This sprint eliminates the primary blockers preventing efficient development and testing, setting the stage for rapid feature development in subsequent sprints.**

---

*Sprint plan optimized for Claude Code multi-agent execution. All agents should begin execution immediately upon sprint initialization.*