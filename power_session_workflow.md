# 🚀 FITFOAI Practical Agent Workflow - Single Session

## How to Actually Use Claude Code Agents Effectively

Since Claude Code agents work **interactively** (not autonomously), here's the most efficient workflow that maximizes productivity within Claude's capabilities.

---

## 🎯 The Power Session Approach

Work in focused 2-hour power sessions, using agents sequentially for rapid development.

### Session Structure:
```
15 min - PM: Planning
30 min - Backend: Core services  
30 min - Frontend: UI implementation
30 min - Integration: Connect components
15 min - QA: Test validation
10 min - PM: Sprint closure
```

---

## 📋 Sprint 3.1: GPS Run Tracking - Power Session

Copy and paste these commands sequentially:

### 1️⃣ Start with Planning (15 mins)
```
@product-manager Let's start Sprint 3.1 (GPS Run Tracking). Please:
1. Create detailed user stories with acceptance criteria
2. Update /project/fitfoai-claude-prd.md with sprint details
3. Define what "done" looks like for GPS tracking
Then I'll commit the PRD updates.
```

**After PM responds, commit:**
```bash
git add . && git commit -m "[PM][3.1] Sprint planning and PRD update"
```

### 2️⃣ Backend Development (30 mins)
```
@backend-ml-database-expert Based on the PM's requirements, implement:
1. LocationService with FusedLocationProvider
2. RunMetrics data class with pace, distance, duration
3. RunSessionRepository for data persistence
4. Flow emission for real-time updates
Show me the complete implementation.
```

**After implementation:**
```bash
git add . && git commit -m "[BACKEND][3.1] LocationService and data layer complete"
```

### 3️⃣ Frontend Development (30 mins)
```
@android-ui-designer Using the backend services just created, build:
1. RunTrackingScreen with blue gradient theme (#1e3a5f to #4a7c97)
2. Circular start/stop button with coral accent (#ff6b6b)
3. Live metric cards showing pace, distance, time
4. Integration with LocationService
Implement the complete UI.
```

**After implementation:**
```bash
git add . && git commit -m "[UI][3.1] RunTrackingScreen complete with theme"
```

### 4️⃣ Integration & Architecture (30 mins)
```
@devops-architecture-engineer Please:
1. Review the backend and frontend code for architecture compliance
2. Fix any Hilt/dependency injection issues
3. Add necessary permissions to AndroidManifest.xml
4. Optimize build configuration
5. Ensure proper integration between components
```

**After fixes:**
```bash
git add . && git commit -m "[DEVOPS][3.1] Integration and build configuration"
```

### 5️⃣ Testing (15 mins)
```
@qa-testing-specialist Create and run tests for:
1. LocationService unit tests
2. RunTrackingScreen UI tests  
3. GPS accuracy validation
4. Integration test for full flow
Report any bugs found.
```

**After testing:**
```bash
git add . && git commit -m "[QA][3.1] Test suite for GPS tracking"
```

### 6️⃣ Sprint Closure (10 mins)
```
@product-manager Please:
1. Review the implemented GPS tracking against acceptance criteria
2. Update PRD with completion status
3. Document any decisions or changes made
4. Plan next sprint (3.2 Voice Coaching) if time permits
```

**Final commit:**
```bash
git add . && git commit -m "[PM][3.1] Sprint 3.1 complete - GPS tracking validated"
```

---

## 🔄 Optimized Multi-Sprint Day

If you have 6-8 hours, you can complete multiple sprints:

### Morning (3 hours)
- **Sprint 3.1**: GPS Run Tracking ✅
- **Sprint 3.2**: Voice Coaching (start)

### Afternoon (3 hours)  
- **Sprint 3.2**: Voice Coaching (complete) ✅
- **Sprint 3.3**: Spotify Integration ✅

### Evening (2 hours)
- **Sprint 3.4**: Training Plans (partially)
- Update all documentation

---

## 💡 Pro Tips for Maximum Efficiency

### 1. Pre-stage Your Commands
Keep this file open and copy-paste commands in sequence.

### 2. Batch Similar Work
```
@backend-ml-database-expert Implement these 3 services:
1. LocationService for GPS
2. VoiceCoachingManager for audio
3. SpotifyService for music
Show all three implementations.
```

### 3. Use Agent Expertise Efficiently
```
@android-ui-designer Create all 3 screens for this sprint:
1. RunTrackingScreen
2. VoiceSettingsScreen  
3. RunSummaryScreen
Apply consistent theme to all.
```

### 4. Combine Review and Fix
```
@devops-architecture-engineer Review all code from this sprint, fix any issues, and optimize the build in one pass.
```

---

## 📊 Realistic Timeline for Full Pipeline

| Week | Sprints | Focus |
|------|---------|-------|
| Week 1 | 3.1-3.2 | GPS Tracking, Voice Coaching |
| Week 2 | 3.3-3.4 | Spotify, Training Plans |
| Week 3 | 3.5-3.6 | Social Features, Achievements |
| Week 4 | Testing & Polish | Full integration, bug fixes |

---

## 🚦 Quick Status Check Commands

Between sprints, use these to track progress:

```bash
# See what's been done
git log --oneline -10

# Check current status  
git status

# View PRD updates
head -50 project/fitfoai-claude-prd.md

# See code changes
git diff --stat HEAD~5
```

---

## 🎯 Start Your Power Session Now!

1. Open this file in one window
2. Open Claude Code in another
3. Start with command #1 (PM planning)
4. Work through sequentially
5. Complete Sprint 3.1 in ~2 hours!

**First Command to Start:**
```
@product-manager Let's start Sprint 3.1 (GPS Run Tracking). Create user stories and update the PRD.
```

---

## 📈 What You'll Accomplish

In one focused session:
- ✅ Complete user stories and PRD updates
- ✅ Fully implemented GPS tracking
- ✅ Beautiful UI with new theme
- ✅ Comprehensive test coverage
- ✅ 6+ git commits documenting progress
- ✅ Ready-to-merge feature

This is realistic, achievable, and leverages Claude Code's actual capabilities!

---

*Ready? Start with the PM command above and work through the session!* 🚀