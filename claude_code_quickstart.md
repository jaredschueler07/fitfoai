# FITFOAI Claude Code Quick Start Guide

## Your Sub-Agents

You have 5 specialized agents ready in Claude Code:

| Agent Name | Shortcut | Specialty |
|------------|----------|-----------|
| `@product-manager` | PM | Product vision, user stories, prioritization |
| `@android-ui-designer` | UI | Jetpack Compose, athletic theme, UI/UX |
| `@backend-ml-database-expert` | Backend | Vertex AI, GPS, database, APIs |
| `@qa-testing-specialist` | QA | Testing, quality assurance, bug tracking |
| `@devops-architecture-engineer` | DevOps | Build system, GCP, CI/CD, architecture |

## Quick Commands

### 🎯 Start Sprint Planning
```
@product-manager what are our priorities for Sprint 3.1? Create user stories for GPS run tracking.
```

### 🏃 Implement Run Tracking Feature
```
For US-301 (GPS Run Tracking):
@backend-ml-database-expert implement LocationService with FusedLocationProvider
@android-ui-designer create RunTrackingScreen with start/stop button and live metrics
@qa-testing-specialist write tests for GPS accuracy and UI interactions
```

### 🎨 Apply New Theme
```
@android-ui-designer update all screens with the blue gradient theme (#1e3a5f to #4a7c97) and coral accents (#ff6b6b)
```

### 🤖 Set Up Vertex AI
```
@backend-ml-database-expert migrate GeminiService to use Vertex AI endpoint
@devops-architecture-engineer help set up GCP project and authentication
```

### 🐛 Fix Bugs
```
@qa-testing-specialist document the GPS tracking bug with reproduction steps
@backend-ml-database-expert fix the LocationService crash issue
@qa-testing-specialist verify the fix works correctly
```

### 🏗️ Architecture Review
```
@devops-architecture-engineer review the current implementation for Clean Architecture compliance
@devops-architecture-engineer fix the Hilt/KSP compatibility issue
```

## Daily Workflow

### Morning Check-in
```
@product-manager what should we focus on today?
All agents: provide quick status update
```

### Feature Development
```
// Assign parallel tasks
@android-ui-designer implement [UI component]
@backend-ml-database-expert create [service/API]
@qa-testing-specialist prepare test cases

// Later: Integration
@android-ui-designer integrate with backend service
@qa-testing-specialist run integration tests
```

### End of Day
```
@product-manager update sprint progress
All agents: commit your changes
```

## Current Sprint Tasks (Jan 20-26)

### High Priority 🔴
```
@backend-ml-database-expert implement GPS LocationService
@android-ui-designer create RunTrackingScreen
@devops-architecture-engineer fix Hilt dependency injection
```

### Medium Priority 🟡
```
@backend-ml-database-expert set up Vertex AI integration
@android-ui-designer complete theme migration
@qa-testing-specialist write comprehensive tests
```

### Nice to Have 🟢
```
@android-ui-designer add runner animations
@backend-ml-database-expert optimize battery usage
@product-manager research competitor features
```

## Common Patterns

### Pattern 1: New Feature
```
@product-manager define requirements → 
@backend-ml-database-expert create data layer → 
@android-ui-designer build UI → 
@qa-testing-specialist test → 
@devops-architecture-engineer review
```

### Pattern 2: Bug Fix
```
@qa-testing-specialist report bug → 
@product-manager prioritize → 
@[relevant-agent] fix → 
@qa-testing-specialist verify
```

### Pattern 3: Technical Decision
```
@devops-architecture-engineer propose solution → 
@backend-ml-database-expert assess impact → 
@product-manager approve → 
@all implement
```

## Pro Tips

### 1. Batch Commands
Instead of:
```
@android-ui-designer update the dashboard
@android-ui-designer add the new theme
@android-ui-designer fix the navigation
```

Do:
```
@android-ui-designer please:
1. Update dashboard with new metrics cards
2. Apply blue gradient theme
3. Fix bottom navigation active states
```

### 2. Provide Context
```
@backend-ml-database-expert the UI needs real-time pace updates every 5 seconds. 
Can you modify LocationService to emit Flow<RunMetrics>?
```

### 3. Cross-Agent Collaboration
```
@android-ui-designer and @backend-ml-database-expert work together on the RunSession data model - 
backend defines it, frontend displays it
```

### 4. Clear Acceptance Criteria
```
@android-ui-designer implement RunTrackingScreen with:
- Large circular start/stop button (coral color)
- Live metrics: pace (min/km), distance (km), time (mm:ss)
- Map view showing route
- Pause/resume functionality
```

## Emergency Commands

### 🚨 Build Broken
```
@devops-architecture-engineer the build is broken, please investigate and fix immediately
```

### 🔥 Critical Bug in Production
```
@qa-testing-specialist document the critical bug
@product-manager should we rollback or hotfix?
@[relevant-agent] implement emergency fix
```

### 📱 App Crashes
```
@backend-ml-database-expert check for null pointer exceptions in LocationService
@android-ui-designer ensure proper error handling in UI
@qa-testing-specialist run crash reproduction tests
```

## File Locations Reference

```
Project Root: /Users/jaredschueler07/AndroidStudioProjects/FITFOAI/

Key Directories:
- UI Code: app/src/main/java/com/runningcoach/v2/presentation/
- Backend: app/src/main/java/com/runningcoach/v2/data/
- Domain: app/src/main/java/com/runningcoach/v2/domain/
- Tests: app/src/test/ and app/src/androidTest/
- Build: app/build.gradle.kts
```

## Status Check Commands

```
# Check specific progress
@product-manager show sprint burndown
@qa-testing-specialist what's our test coverage?
@devops-architecture-engineer check build time metrics

# General status
All agents: what did you complete today?
All agents: any blockers?
```

---

💡 **Remember**: Agents work best when given specific, actionable tasks with clear success criteria!

🚀 **Start with**: `@product-manager what's our top priority right now?`