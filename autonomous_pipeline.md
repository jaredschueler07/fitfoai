# FITFOAI Autonomous Agent Pipeline

## 🔄 Continuous Development Orchestration

This system enables your Claude Code agents to work autonomously in parallel cycles, maximizing productivity until quota limits are reached.

---

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CONTINUOUS PIPELINE                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Sprint N:          Sprint N+1:         Sprint N+2:         │
│  ┌──────────┐      ┌──────────┐       ┌──────────┐        │
│  │    PM    │─────▶│    PM    │──────▶│    PM    │────▶   │
│  └─────┬────┘      └─────┬────┘       └─────┬────┘        │
│        │                  │                  │              │
│  ┌─────▼────┐      ┌─────▼────┐       ┌─────▼────┐        │
│  │ DEV TEAM │─────▶│ DEV TEAM │──────▶│ DEV TEAM │────▶   │
│  └─────┬────┘      └─────┬────┘       └─────┬────┘        │
│        │                  │                  │              │
│  ┌─────▼────┐      ┌─────▼────┐       ┌─────▼────┐        │
│  │    QA    │─────▶│    QA    │──────▶│    QA    │────▶   │
│  └──────────┘      └──────────┘       └──────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Master Orchestration Command

Copy and paste this entire block to start the autonomous pipeline:

```markdown
# AUTONOMOUS DEVELOPMENT PIPELINE - FITFOAI
# This will run continuously until stopped or quota reached

## Initialize Pipeline State
Let's establish a continuous development pipeline that will run autonomously. Each agent will work in parallel cycles, passing work to the next stage while starting new tasks.

## Pipeline Rules:
1. Each stage has 30 minutes to complete their work
2. Work is passed downstream automatically
3. Upstream stages begin next iteration immediately after handoff
4. All work is tagged with sprint numbers for tracking
5. Continue until explicitly stopped or quota limit reached

## Sprint Backlog:
- Sprint 3.1: GPS Run Tracking (Current)
- Sprint 3.2: Voice Coaching with ElevenLabs
- Sprint 3.3: Spotify Integration
- Sprint 3.4: Training Plan Generation
- Sprint 3.5: Social Features
- Sprint 3.6: Achievement System

Now beginning autonomous execution:

---

### CYCLE 1 - Sprint 3.1: GPS Run Tracking

@product-manager [AUTONOMOUS-MODE][SPRINT-3.1] 
Create detailed user stories for GPS run tracking with full acceptance criteria. Once complete, tag with [READY-FOR-DEV] and immediately begin planning Sprint 3.2 (Voice Coaching).

Once PM tags [READY-FOR-DEV]:

@backend-ml-database-expert [AUTONOMOUS-MODE][SPRINT-3.1]
Implement LocationService with:
- FusedLocationProvider setup
- 5-second update intervals  
- Flow<RunMetrics> emission
- Background service support
- Database persistence
When complete, tag [READY-FOR-QA] and immediately start Sprint 3.2 work from PM.

@android-ui-designer [AUTONOMOUS-MODE][SPRINT-3.1]
Implement RunTrackingScreen with:
- Blue gradient theme (#1e3a5f to #4a7c97)
- Start/stop button (coral accent)
- Live metrics display
- Route map view
When complete, tag [READY-FOR-QA] and immediately start Sprint 3.2 work from PM.

@devops-architecture-engineer [AUTONOMOUS-MODE][SPRINT-3.1]
In parallel:
- Fix Hilt/KSP compatibility
- Set up location permissions
- Configure build optimization
When complete, tag [READY-FOR-QA] and immediately start Sprint 3.2 work from PM.

Once all developers tag [READY-FOR-QA]:

@qa-testing-specialist [AUTONOMOUS-MODE][SPRINT-3.1]
Execute comprehensive testing:
- GPS accuracy validation
- UI interaction tests
- Integration testing
- Performance benchmarks
Tag [SPRINT-3.1-COMPLETE] when done and immediately start testing Sprint 3.2 deliverables.

---

### CYCLE 2 - Sprint 3.2: Voice Coaching (Starts while QA tests 3.1)

@product-manager [AUTONOMOUS-MODE][SPRINT-3.2]
You should already be working on this. Create user stories for voice coaching with ElevenLabs TTS. Tag [READY-FOR-DEV] and begin Sprint 3.3 planning.

@backend-ml-database-expert [AUTONOMOUS-MODE][SPRINT-3.2]
Once PM provides requirements:
- Implement ElevenLabsService
- Create VoiceCoachingManager
- Set up audio triggers
Tag [READY-FOR-QA] and move to Sprint 3.3.

@android-ui-designer [AUTONOMOUS-MODE][SPRINT-3.2]
Once PM provides requirements:
- Add voice control UI
- Create coaching feedback visuals
- Implement audio indicators
Tag [READY-FOR-QA] and move to Sprint 3.3.

@qa-testing-specialist [AUTONOMOUS-MODE][SPRINT-3.2]
Test voice coaching once developers complete.
Continue this pattern.

---

### CONTINUOUS EXECUTION RULES

All agents follow these autonomous rules:

1. **No Waiting**: As soon as you hand off work, immediately start the next sprint's tasks
2. **Tag Everything**: Use [SPRINT-X.Y-STATUS] tags for tracking
3. **Auto-Escalate**: If blocked, tag [BLOCKED] and move to next available task
4. **Parallel Work**: Multiple sprints can be in different stages simultaneously
5. **Self-Document**: Update sprint_dashboard.md automatically
6. **Quality Gates**: Don't skip testing, but continue working on next items

### PIPELINE MONITORING

@product-manager every 30 minutes, provide a pipeline status report:
```
PIPELINE STATUS:
- Sprint 3.1: [Stage] [% Complete]
- Sprint 3.2: [Stage] [% Complete] 
- Sprint 3.3: [Stage] [% Complete]
- Velocity: X story points/hour
- Blockers: None/[List]
- Next Action: [What's happening]
```

### EMERGENCY STOP

To halt the pipeline, simply say: "STOP AUTONOMOUS PIPELINE"

### GO! 
Begin autonomous execution now. All agents start your assigned work for Sprint 3.1 and continue cycling through sprints without waiting for further instruction.
```

---

## 📊 Parallel Execution Matrix

| Time | Product Manager | Dev Team | QA Testing |
|------|----------------|----------|------------|
| 0-30m | Sprint 3.1 Planning | Waiting | Waiting |
| 30-60m | Sprint 3.2 Planning | Sprint 3.1 Dev | Waiting |
| 60-90m | Sprint 3.3 Planning | Sprint 3.2 Dev | Sprint 3.1 Testing |
| 90-120m | Sprint 3.4 Planning | Sprint 3.3 Dev | Sprint 3.2 Testing |
| Continuous... | Planning N+2 | Developing N+1 | Testing N |

---

## 🤖 Individual Agent Autonomous Instructions

### Product Manager Autonomous Loop
```markdown
@product-manager [AUTONOMOUS-LOOP]
Your continuous cycle:
1. Create detailed user stories for current sprint
2. Tag [READY-FOR-DEV] 
3. Immediately start next sprint planning
4. Every 30 mins: Generate pipeline status report
5. Repeat until [STOP] command

Sprint sequence: 3.1 GPS → 3.2 Voice → 3.3 Spotify → 3.4 Training Plans → 3.5 Social → 3.6 Achievements
```

### Development Team Autonomous Loop
```markdown
@backend-ml-database-expert @android-ui-designer @devops-architecture-engineer [AUTONOMOUS-LOOP]
Your continuous cycle:
1. Wait for [READY-FOR-DEV] tag from PM
2. Implement all requirements in parallel
3. Tag [READY-FOR-QA] when complete
4. Immediately pull next sprint from PM queue
5. Repeat until [STOP] command
```

### QA Autonomous Loop
```markdown
@qa-testing-specialist [AUTONOMOUS-LOOP]
Your continuous cycle:
1. Wait for [READY-FOR-QA] tag from developers
2. Execute comprehensive test suite
3. Tag [SPRINT-X-COMPLETE] or [BUGS-FOUND]
4. Immediately pull next sprint from testing queue
5. Generate test report
6. Repeat until [STOP] command
```

---

## 🔥 Optimization Strategies

### Maximum Throughput Mode
```markdown
Enable parallel sprint execution:
- PM works 2 sprints ahead
- Developers work on current + fixes from QA
- QA tests everything as it arrives
- No blocking - use [SKIP-IF-BLOCKED] tags
```

### Quality-First Mode
```markdown
Enable quality gates:
- Each sprint must pass QA before next begins
- Developers fix bugs before new features
- PM adjusts scope based on velocity
- Use [MUST-FIX] tags for blockers
```

### Hybrid Smart Mode
```markdown
Balance speed and quality:
- P0 bugs block progression
- P1-P3 bugs go to backlog
- Parallel work on non-dependent features
- Smart batching of related changes
```

---

## 📈 Auto-Scaling Triggers

The pipeline automatically adjusts based on:

### Velocity Monitoring
```markdown
If velocity < expected:
  @devops-architecture-engineer optimize build times
  @product-manager reduce scope
  
If velocity > expected:
  @product-manager add stretch goals
  All agents: maintain quality standards
```

### Bottleneck Detection
```markdown
If QA queue > 3 sprints:
  @qa-testing-specialist prioritize P0/P1 testing
  @product-manager slow feature delivery
  
If Dev queue > 2 sprints:
  @android-ui-designer @backend-ml-database-expert pair program
  @product-manager simplify requirements
```

---

## 🛑 Stopping Conditions

The pipeline automatically stops when:

1. **Explicit Stop**: You say "STOP AUTONOMOUS PIPELINE"
2. **Quota Warning**: Agents detect approaching rate limits
3. **Critical Failure**: P0 blocker can't be resolved
4. **Completion**: All 6 sprints finished
5. **Time Limit**: 4-hour maximum runtime

---

## 📊 Automated Reporting

Every 30 minutes, the pipeline generates:

```markdown
AUTONOMOUS PIPELINE REPORT [Timestamp]
=====================================
Sprint Status:
- 3.1 GPS Tracking: Dev 100% | QA 60%
- 3.2 Voice Coach: Planning 100% | Dev 30%
- 3.3 Spotify: Planning 50%

Metrics:
- Story Points Completed: 24
- Bugs Found: 3 (2 fixed, 1 pending)
- Code Coverage: 72%
- Build Time: 1m 45s

Next Actions:
- PM: Complete Sprint 3.3 planning
- Dev: Start Sprint 3.2 implementation
- QA: Finish Sprint 3.1 testing

Health: 🟢 All systems operational
```

---

## 🚀 Quick Start Commands

### Start Maximum Velocity Pipeline
```markdown
ALL AGENTS: Begin [AUTONOMOUS-LOOP] with [MAX-VELOCITY] mode. PM start with Sprint 3.1, everyone follow pipeline rules. No manual intervention needed. Go!
```

### Start Quality-Focused Pipeline
```markdown
ALL AGENTS: Begin [AUTONOMOUS-LOOP] with [QUALITY-FIRST] mode. Ensure all tests pass before progression. PM manage scope accordingly. Go!
```

### Start Balanced Pipeline (Recommended)
```markdown
ALL AGENTS: Begin [AUTONOMOUS-LOOP] with [HYBRID-SMART] mode. Balance speed with quality, fix P0 bugs immediately, batch P1-P3. PM start Sprint 3.1 planning now. Execute continuously until stopped. Go!
```

---

## 💡 Pro Tips

1. **Start on Monday morning**: Maximum uninterrupted time
2. **Check every hour**: Quick glance at progress
3. **Let it run**: Agents self-correct and adapt
4. **Trust the process**: They'll escalate if truly blocked
5. **Review at end**: Learn and optimize for next run

---

*Ready to go autonomous? Just copy the master command above and watch your app build itself!*