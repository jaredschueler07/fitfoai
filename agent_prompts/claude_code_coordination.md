# FITFOAI Claude Code Sub-Agents Setup & Coordination

## Overview

This guide explains how to use Claude Code's built-in sub-agent feature for the FITFOAI project. Your sub-agents are already created in the `.claude/agents` directory and can be invoked directly within Claude Code.

## Your Current Sub-Agents

Located in `/Users/jaredschueler07/AndroidStudioProjects/FITFOAI/.claude/agents`:

1. **android-ui-designer** (Frontend)
2. **backend-ml-database-expert** (Backend)
3. **qa-testing-specialist** (QA)
4. **devops-architecture-engineer** (DevOps)
5. **product-manager** (Product)

## How Claude Code Sub-Agents Work

### Invoking Agents
You can call agents directly in your Claude Code session using:
```
@android-ui-designer can you implement the RunTrackingScreen with the new blue theme?
@backend-ml-database-expert please set up the Vertex AI integration
@qa-testing-specialist write tests for the GPS tracking feature
@devops-architecture-engineer fix the Hilt/KSP compatibility issue
@product-manager what are our priorities for this sprint?
```

### Agent Communication
Agents can communicate with each other through:
1. **Direct mentions** - One agent can tag another
2. **Shared context** - All agents see the conversation history
3. **File system** - Agents work on the same codebase

## Coordination Workflow

### 1. Sprint Planning (Product Manager Leads)
```
@product-manager please review our current sprint goals and create user stories for the team
```

The PM agent will:
- Define sprint goals
- Create user stories with acceptance criteria
- Prioritize the backlog
- Assign work to appropriate agents

### 2. Parallel Development
You can assign tasks to multiple agents in one message:
```
Based on US-301 (GPS Run Tracking):
@android-ui-designer implement the RunTrackingScreen UI with start/stop button and metrics display
@backend-ml-database-expert create the LocationService for GPS tracking
@qa-testing-specialist prepare test cases for GPS accuracy
```

### 3. Cross-Agent Collaboration
When agents need to work together:
```
@android-ui-designer and @backend-ml-database-expert please collaborate on the data model for RunSession - backend define it, frontend consume it
```

### 4. Code Review & Integration
```
@devops-architecture-engineer please review the changes from frontend and backend for architectural compliance
@qa-testing-specialist run tests on the integrated feature
```

## Effective Sub-Agent Patterns

### Pattern 1: Feature Development Flow
```markdown
1. PM defines requirements
   @product-manager create user story for [feature]

2. Technical design
   @backend-ml-database-expert design the data model
   @android-ui-designer mock up the UI

3. Implementation
   @backend-ml-database-expert implement the service
   @android-ui-designer build the screen

4. Testing
   @qa-testing-specialist test the feature end-to-end

5. Deployment prep
   @devops-architecture-engineer prepare for release
```

### Pattern 2: Bug Fix Flow
```markdown
1. QA identifies issue
   @qa-testing-specialist document the bug with reproduction steps

2. Triage
   @product-manager assign priority to this bug

3. Fix
   @android-ui-designer OR @backend-ml-database-expert fix the issue

4. Verify
   @qa-testing-specialist verify the fix
```

### Pattern 3: Architecture Decision
```markdown
1. Identify need
   @devops-architecture-engineer we need to decide on [technical choice]

2. Research
   @backend-ml-database-expert what are the implications for data layer?
   @android-ui-designer what are the UI impacts?

3. Decision
   @product-manager given the trade-offs, what's your recommendation?
   @devops-architecture-engineer make the final architecture decision
```

## Daily Workflow Example

### Morning Session
```
// Start with status check
@product-manager what's our focus for today based on sprint goals?

// Get updates from each agent
@android-ui-designer what's the status of the theme migration?
@backend-ml-database-expert how's the Vertex AI integration going?
@qa-testing-specialist any blockers from testing?
@devops-architecture-engineer is the Hilt issue resolved?
```

### Development Session
```
// Parallel work assignment
Let's work on US-301 (GPS Tracking):
@backend-ml-database-expert implement LocationService with FusedLocationProvider
@android-ui-designer create the RunTrackingScreen with these requirements: [list]
@qa-testing-specialist prepare unit tests for LocationService
```

### Integration Session
```
// Coordinate integration
@android-ui-designer the LocationService is ready, please integrate it with your UI
@backend-ml-database-expert the UI needs this additional data, can you provide it?
@qa-testing-specialist test the integrated feature
```

### End of Day
```
// Wrap up
@product-manager update the sprint dashboard with today's progress
All agents: commit your changes with appropriate tags
```

## Best Practices

### 1. Clear Task Assignment
Be specific about what each agent should do:
```
❌ Bad: @android-ui-designer make it look better
✅ Good: @android-ui-designer apply the blue gradient (#1e3a5f to #4a7c97) to all screen backgrounds
```

### 2. Context Sharing
Provide relevant context when switching between agents:
```
@backend-ml-database-expert the frontend needs a RunMetrics data class with pace, distance, and duration
Context: @android-ui-designer will display this in real-time during runs
```

### 3. Dependency Management
Clearly state dependencies:
```
@android-ui-designer wait for @backend-ml-database-expert to complete the data model before implementing the UI
```

### 4. Incremental Development
Break large tasks into smaller chunks:
```
@android-ui-designer first implement the basic layout, then we'll add animations
@backend-ml-database-expert start with basic GPS tracking, we'll add filters later
```

### 5. Regular Sync Points
Check alignment regularly:
```
@product-manager does the current implementation meet the acceptance criteria for US-301?
@qa-testing-specialist any issues found that would block this story?
```

## Communication Tags

Use these tags in your messages for clarity:

### Task Types
- `[IMPLEMENT]` - Build new feature
- `[FIX]` - Fix a bug
- `[REFACTOR]` - Improve code structure
- `[TEST]` - Write or run tests
- `[REVIEW]` - Code review request
- `[DOCUMENT]` - Update documentation

### Priorities
- `[P0-BLOCKER]` - Stop everything and fix
- `[P1-CRITICAL]` - Must complete today
- `[P2-IMPORTANT]` - Should complete this sprint
- `[P3-NICE]` - If time permits

### Status Updates
- `[COMPLETED]` - Task finished
- `[IN-PROGRESS]` - Currently working
- `[BLOCKED]` - Can't proceed
- `[READY-FOR-REVIEW]` - Needs review

## Example Multi-Agent Session

```markdown
User: Let's implement the run tracking feature

@product-manager First, what are the acceptance criteria for run tracking?

Product Manager: The acceptance criteria for US-301 are:
1. User can start/stop recording
2. GPS tracks accurately
3. Display pace, distance, time
4. Save to database

User: Great. Let's start implementation.

@backend-ml-database-expert [IMPLEMENT][P1-CRITICAL] Create LocationService using FusedLocationProvider
@android-ui-designer [IMPLEMENT][P1-CRITICAL] Design RunTrackingScreen based on our blue theme

Backend Agent: I'll implement LocationService with:
- FusedLocationProvider for accuracy
- 5-second update intervals
- Background service support
[Creates service code]

UI Designer: I'll create RunTrackingScreen with:
- Blue gradient background
- Circular start/stop button (coral accent)
- Metric cards for pace/distance/time
[Creates UI code]

User: @qa-testing-specialist [TEST][P2-IMPORTANT] Write tests for both components

QA Agent: Creating test suite:
- GPS accuracy tests
- UI interaction tests
- Integration tests
[Creates test code]

User: @devops-architecture-engineer [REVIEW] Check if this follows our architecture

DevOps Agent: Reviewing...
- ✅ Follows Clean Architecture
- ⚠️ Need to add to dependency injection
- ✅ Proper separation of concerns

User: @product-manager [REVIEW] Does this meet our acceptance criteria?

Product Manager: Checking implementation:
- ✅ AC1: Start/stop implemented
- ✅ AC2: GPS tracking functional
- ✅ AC3: Metrics displayed
- ⚠️ AC4: Need to verify database save
Overall: 75% complete, need database integration

User: @backend-ml-database-expert [FIX][P1-CRITICAL] Add database save functionality

Backend Agent: Adding Room database integration...
[Updates code with database functionality]

User: Perfect! @qa-testing-specialist [TEST] Run final integration tests

QA Agent: Running complete test suite...
All tests passing! Feature ready for release.
```

## Tips for Maximum Efficiency

1. **Batch Related Tasks**: Give multiple related tasks to agents at once
2. **Use Agent Expertise**: Let each agent focus on their strengths
3. **Maintain Context**: Keep conversation focused on current sprint goals
4. **Document Decisions**: Have PM agent track important decisions
5. **Regular Checkpoints**: Use PM agent to verify progress against goals

## Troubleshooting

### If agents seem confused:
- Provide more context about the current state
- Reference specific files or code sections
- Break complex tasks into smaller steps

### If agents conflict:
- Use @product-manager to make priority decisions
- Use @devops-architecture-engineer for technical decisions
- Clearly state which agent has final say

### If progress is slow:
- Focus on one user story at a time
- Reduce scope to MVP features
- Have @product-manager re-prioritize

## Quick Command Reference

```markdown
# Planning
@product-manager define sprint goals
@product-manager prioritize backlog

# Development
@android-ui-designer implement [screen]
@backend-ml-database-expert create [service]

# Testing
@qa-testing-specialist test [feature]
@qa-testing-specialist verify bug fix

# Architecture
@devops-architecture-engineer review architecture
@devops-architecture-engineer optimize build

# Integration
All agents: integrate your components for [feature]

# Status
All agents: provide status update
@product-manager update sprint dashboard
```

---

*This coordination system leverages Claude Code's native sub-agent capabilities for efficient parallel development*