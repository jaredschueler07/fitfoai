# Inter-Agent Coordination Guide

## Overview

This guide describes how the 5 FITFOAI development agents coordinate when running simultaneously in different terminals.

## Agent Roles

1. **Product Manager**: Product vision, prioritization, and alignment
2. **Frontend Agent**: UI/UX development with Jetpack Compose
3. **Backend Agent**: Vertex AI, database, and API integrations
4. **QA Agent**: Testing and quality assurance
5. **DevOps Agent**: Infrastructure, CI/CD, and architecture

## Communication Channels

### 1. File-Based Communication

Create a shared coordination file at:
```bash
/Users/jaredschueler07/AndroidStudioProjects/FITFOAI/.agent_comm/messages.json
```

Message format:
```json
{
  "messages": [
    {
      "from": "frontend",
      "to": "backend",
      "timestamp": "2024-01-20T10:30:00Z",
      "type": "REQUEST",
      "tag": "NEED-BACKEND",
      "content": "Need UserProfile data model with fitness metrics",
      "priority": "P1"
    }
  ]
}
```

### 2. Git Branch Strategy

Each agent works on dedicated branches:
- Frontend: `feature/ui-[feature-name]`
- Backend: `feature/backend-[feature-name]`
- QA: `test/[test-type]-[feature]`
- DevOps: `infra/[infrastructure-change]`

### 3. Coordination Commands

Add these bash aliases to `.zshrc` or `.bash_profile`:

```bash
# Agent communication commands
alias agent-sync="git fetch --all && git pull origin main"
alias agent-status="cat /Users/jaredschueler07/AndroidStudioProjects/FITFOAI/.agent_comm/messages.json | jq '.messages[-5:]'"
alias agent-msg="function _msg() { echo '{\"from\":\"$1\",\"to\":\"$2\",\"content\":\"$3\",\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}' >> /Users/jaredschueler07/AndroidStudioProjects/FITFOAI/.agent_comm/messages.json; }; _msg"
alias agent-build="cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI && ./gradlew clean assembleDebug"
alias agent-test="cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI && ./gradlew test"
```

## Priority Levels

- **P0**: Blocker - Stop current work and address immediately
- **P1**: Critical - Address within current session
- **P2**: Important - Address within day
- **P3**: Nice-to-have - Address when possible

## Communication Tags

### Product Manager Tags
- `[PRODUCT-DECISION]`: Product strategy decision
- `[CLARIFICATION-NEEDED]`: Feature clarification required
- `[SCOPE-ALERT]`: Scope creep detected
- `[PRODUCT-SUGGESTION]`: Feature improvement idea
- `[ACCEPTANCE-FAILED]`: Feature doesn't meet criteria

### Frontend Tags
- `[UI-UPDATE]`: UI changes made
- `[NEED-BACKEND]`: Requires backend support
- `[UI-BUG]`: UI bug found
- `[TEST-REQUEST]`: Needs testing

### Backend Tags
- `[BACKEND-UPDATE]`: Backend changes made
- `[DATA-MODEL]`: New data model created
- `[API-ISSUE]`: API problem identified
- `[NEED-UI]`: Requires UI changes

### QA Tags
- `[BUG-P1]`, `[BUG-P2]`, `[BUG-P3]`: Bug priorities
- `[FIX-NEEDED]`: Fix required for component
- `[TEST-RESULT]`: Test results (pass/fail)
- `[RELEASE-BLOCKER]`: Critical issue blocking release

### DevOps Tags
- `[ARCH-CHANGE]`: Architecture modification
- `[BUILD-ISSUE]`: Build problem
- `[REVIEW-NEEDED]`: Code review required
- `[TECH-DEBT]`: Technical debt identified

## Daily Sync Points

### Morning Sync (Start of Session)
1. Check messages: `agent-status`
2. Pull latest changes: `agent-sync`
3. Review assigned tasks
4. Announce work plan

### Before Major Changes
1. Announce intent in messages
2. Check for conflicts
3. Coordinate with affected agents

### After Completion
1. Update status in messages
2. Push changes to branch
3. Request reviews if needed

### End of Session
1. Summary of work done
2. List any blockers
3. Update task status
4. Push all changes

## Conflict Resolution

### Priority Order
1. **Product Manager decides** feature priorities and scope
2. **UI takes precedence** for user-facing changes
3. **Backend takes precedence** for data models
4. **QA can block releases** with P0 bugs
5. **DevOps has final say** on architecture

### Resolution Process
1. Identify conflict in messages
2. Product Manager evaluates impact
3. Affected agents discuss solution
4. Implement agreed approach
5. Document decision in decision log

## Working Agreement

### Code Standards
- Always pull before starting work
- Commit frequently with clear messages
- Run tests before pushing
- Document breaking changes
- Communicate blockers immediately

### Commit Message Format
```
[AGENT-TYPE] Brief description

- Detailed change 1
- Detailed change 2

Related: #issue-number
```

Example:
```
[FRONTEND] Update dashboard with new athletic theme

- Implement blue gradient backgrounds
- Add runner silhouette illustrations
- Update typography to bold sans-serif

Related: #UI-REDESIGN
```

## Quick Start Checklist

### For Each Agent Terminal

1. **Set up workspace:**
```bash
cd /Users/jaredschueler07/AndroidStudioProjects/FITFOAI
git checkout -b [your-branch-name]
mkdir -p .agent_comm
touch .agent_comm/messages.json
echo '{"messages":[]}' > .agent_comm/messages.json
```

2. **Configure agent identity:**
```bash
export AGENT_NAME="frontend"  # or backend, qa, devops
export AGENT_COLOR="\033[0;34m"  # Blue theme
```

3. **Start communication monitor:**
```bash
# In a separate terminal tab
watch -n 5 'tail -10 .agent_comm/messages.json | jq .'
```

4. **Begin work with announcement:**
```bash
agent-msg $AGENT_NAME "all" "Starting work on [task]"
```

## Parallel Work Streams

### Compatible Parallel Tasks
Agents can work simultaneously on:
- **Frontend + Backend**: UI components + corresponding data models
- **QA + Any**: Testing while others develop
- **DevOps + Backend**: GCP setup alongside Vertex AI integration
- **All Agents**: Theme migration across components

### Sequential Dependencies
Must be done in order:
1. Backend creates data models → Frontend uses them
2. Frontend/Backend implement → QA tests
3. DevOps sets up infrastructure → Backend integrates

## Phase 3 Priority Matrix

| Agent | P0 (Blockers) | P1 (Critical) | P2 (Important) |
|-------|---------------|---------------|----------------|
| Product | User story clarity | Sprint planning | Backlog grooming |
| Frontend | - | Athletic theme redesign | Tablet layouts |
| Backend | Vertex AI setup | GPS tracking service | Offline sync |
| QA | - | Run tracking tests | Performance tests |
| DevOps | Fix Hilt/KSP | GCP infrastructure | CI/CD pipeline |

## Emergency Procedures

### Build Broken
1. `[BUILD-ISSUE]` tag with details
2. DevOps investigates immediately
3. Revert breaking commit if needed
4. Fix and verify locally before pushing

### Critical Bug Found
1. QA tags `[BUG-P0]` or `[RELEASE-BLOCKER]`
2. All agents stop current work
3. Responsible agent fixes immediately
4. QA verifies fix

### Merge Conflicts
1. Pull latest from main
2. Resolve conflicts locally
3. Test thoroughly
4. Coordinate with affected agents
5. Push resolved version

## Tools and Resources

### Required Tools
- Android Studio Hedgehog or later
- Git with configured SSH keys
- jq for JSON parsing (brew install jq)
- watch for monitoring (built-in on macOS)

### Useful Commands
```bash
# View recent commits from all agents
git log --oneline --graph --all --decorate -20

# Check which agent modified a file
git blame [file-path]

# See all active branches
git branch -a

# Clean up old branches
git fetch --prune

# Check app performance
adb shell dumpsys meminfo com.runningcoach.v2
```

## Contact and Escalation

For issues that can't be resolved through normal coordination:
1. Document issue in messages with `[ESCALATION]` tag
2. Create GitHub issue with full context
3. Schedule sync meeting if needed
4. Update this guide with resolution

---

*Last updated: January 2024*
*Version: 1.0*
