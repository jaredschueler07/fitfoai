# 🎯 FITFOAI Multi-Terminal Agent Workflow

## Reality Check: How Claude Code Agents Actually Work

Claude Code agents are **interactive**, not autonomous. They:
- ✅ Respond to your requests with specialized expertise
- ✅ Can be called sequentially in one session
- ✅ Maintain context within a conversation
- ❌ Cannot run continuously without prompts
- ❌ Cannot coordinate automatically between sessions
- ❌ Cannot auto-commit on schedules

## 💡 Solution: Multi-Terminal Workflow

Run separate Claude Code sessions in different terminals, each focused on a specific agent role.

---

## 🖥️ Terminal Setup (5 Terminals)

### Terminal 1: Product Manager
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b docs/prd-updates

# In Claude Code:
# Focus: Sprint planning and PRD updates
```

**Session Prompt:**
```
I'm the Product Manager for FITFOAI. I'll be working on Sprint 3.1 (GPS Run Tracking).
@product-manager Please:
1. Update /project/fitfoai-claude-prd.md with Sprint 3.1 details
2. Create detailed user stories with acceptance criteria
3. Define success metrics
4. Commit: git add . && git commit -m "[PM][3.1] Sprint planning complete"
```

### Terminal 2: Backend Developer
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b feature/3.1-backend

# In Claude Code:
# Focus: LocationService and data layer
```

**Session Prompt:**
```
I'm developing the backend for Sprint 3.1 (GPS Run Tracking).
@backend-ml-database-expert Please implement:
1. LocationService with FusedLocationProvider
2. RunSessionManager for data persistence
3. Flow<RunMetrics> for real-time updates
4. Commit progress every major component
```

### Terminal 3: Frontend Developer
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b feature/3.1-ui

# In Claude Code:
# Focus: RunTrackingScreen UI
```

**Session Prompt:**
```
I'm building the UI for Sprint 3.1 (GPS Run Tracking).
@android-ui-designer Please create:
1. RunTrackingScreen with blue gradient theme
2. Start/stop button with coral accent
3. Live metrics display (pace, distance, time)
4. Map view for route display
5. Commit after each screen component
```

### Terminal 4: DevOps Engineer
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b infra/3.1-setup

# In Claude Code:
# Focus: Build fixes and infrastructure
```

**Session Prompt:**
```
I'm handling infrastructure for Sprint 3.1.
@devops-architecture-engineer Please:
1. Fix Hilt/KSP compatibility issue
2. Set up location permissions in manifest
3. Configure build optimizations
4. Update dependencies for GPS features
5. Commit each configuration change
```

### Terminal 5: QA Tester
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b test/3.1-validation

# In Claude Code:
# Focus: Test creation and validation
```

**Session Prompt:**
```
I'm testing Sprint 3.1 (GPS Run Tracking).
@qa-testing-specialist Please:
1. Write unit tests for LocationService
2. Create UI tests for RunTrackingScreen
3. Integration tests for GPS accuracy
4. Performance benchmarks
5. Commit test suites as completed
```

---

## 📋 Coordination Protocol

### Manual Sync Points (Every 30 minutes)

1. **Check Progress** (All Terminals):
```bash
git status
git log --oneline -5
```

2. **Share Updates** (Create a shared status file):
```bash
echo "[$(date)] Terminal X - Completed: [task]" >> .agent_comm/status.log
```

3. **Merge Updates** (When ready):
```bash
git checkout main
git merge feature/3.1-backend --no-ff
git merge feature/3.1-ui --no-ff
```

---

## 🔄 Realistic Workflow

### Hour 1: Planning & Setup
- **Terminal 1 (PM)**: Create user stories, update PRD
- **Other Terminals**: Wait for PM to define requirements

### Hour 2: Parallel Development
- **Terminal 2 (Backend)**: Implement LocationService
- **Terminal 3 (Frontend)**: Build RunTrackingScreen
- **Terminal 4 (DevOps)**: Fix build issues
- **Terminal 5 (QA)**: Write test frameworks

### Hour 3: Integration
- **All Terminals**: Merge to integration branch
- **Terminal 5 (QA)**: Run integration tests
- **Terminal 1 (PM)**: Update PRD with progress

### Hour 4: Polish & Complete
- **Fix bugs found by QA**
- **Complete documentation**
- **Final PRD update**
- **Prepare for Sprint 3.2**

---

## 💬 Communication Between Terminals

### Option 1: Shared Status File
```bash
# Each terminal updates status
echo "[BACKEND] LocationService complete" >> .agent_comm/status.log
echo "[UI] Need RunMetrics data model from backend" >> .agent_comm/status.log

# Check status from any terminal
tail -f .agent_comm/status.log
```

### Option 2: Git Commits as Messages
```bash
# Communicate through commit messages
git commit -m "[BACKEND][3.1] LocationService ready - UI can integrate"
git push origin feature/3.1-backend

# Other terminals check
git fetch --all
git log --all --oneline -10
```

### Option 3: Branch PRs as Handoffs
```bash
# When ready for integration
gh pr create --title "[3.1] Backend services ready for UI integration"
```

---

## 🎯 Practical Sequential Workflow (Single Terminal Alternative)

If multiple terminals aren't practical, use this sequential approach:

```markdown
# Sprint 3.1 Sequential Development

## Phase 1: Planning (30 mins)
@product-manager Create Sprint 3.1 user stories and update PRD
git commit -m "[PM][3.1] Sprint planning complete"

## Phase 2: Backend (45 mins)
@backend-ml-database-expert Implement LocationService and data layer
git commit -m "[BACKEND][3.1] Location services complete"

## Phase 3: Frontend (45 mins)
@android-ui-designer Build RunTrackingScreen with the backend services
git commit -m "[UI][3.1] Run tracking screen complete"

## Phase 4: Infrastructure (30 mins)
@devops-architecture-engineer Fix build issues and optimize
git commit -m "[DEVOPS][3.1] Build configuration updated"

## Phase 5: Testing (30 mins)
@qa-testing-specialist Write and run comprehensive tests
git commit -m "[QA][3.1] Test suite complete"

## Phase 6: Review (15 mins)
@product-manager Validate against acceptance criteria and update PRD
git commit -m "[PM][3.1] Sprint 3.1 complete and validated"
```

---

## ✅ Key Adjustments from Original Plan

1. **Interactive, not Autonomous**: Agents respond to prompts, not run continuously
2. **Manual Coordination**: You coordinate between terminals
3. **Sequential Option**: Can work in single terminal if needed
4. **Real Git Workflow**: Actual branches and commits
5. **Practical Timing**: Based on real interaction speeds

---

## 🚀 Start Commands for Each Terminal

### Quick Copy-Paste Starters:

**Terminal 1 (PM):**
```
@product-manager Start Sprint 3.1: Create user stories for GPS run tracking with acceptance criteria. Update /project/fitfoai-claude-prd.md with requirements.
```

**Terminal 2 (Backend):**
```
@backend-ml-database-expert Implement LocationService using FusedLocationProvider for GPS tracking with 5-second updates and Flow<RunMetrics> emission.
```

**Terminal 3 (UI):**
```
@android-ui-designer Create RunTrackingScreen with blue gradient (#1e3a5f to #4a7c97), coral start/stop button, and live metrics display.
```

**Terminal 4 (DevOps):**
```
@devops-architecture-engineer Fix Hilt/KSP compatibility and set up location permissions for GPS tracking.
```

**Terminal 5 (QA):**
```
@qa-testing-specialist Create comprehensive test suite for GPS tracking including unit tests, UI tests, and integration tests.
```

---

*This workflow respects Claude Code's actual capabilities while still enabling parallel development through multiple terminal sessions!*