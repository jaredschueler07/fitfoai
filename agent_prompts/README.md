# FITFOAI Agent Prompts

This directory contains specialized prompts for 5 Claude sub-agents working on the FITFOAI Android fitness coaching app.

## 📁 Files

- **`product_manager_agent.md`** - Product Manager Agent prompt (Vision, Strategy, Prioritization)
- **`frontend_agent.md`** - Frontend Developer Agent prompt (UI/UX with Jetpack Compose)
- **`backend_agent.md`** - Backend Developer Agent prompt (Vertex AI, Database, APIs)
- **`qa_testing_agent.md`** - QA & Testing Agent prompt (Testing and Quality Assurance)
- **`devops_agent.md`** - DevOps & Architecture Agent prompt (Infrastructure and Deployment)
- **`coordination_guide.md`** - Inter-agent coordination protocols and workflows

## 🚀 Quick Start

### 1. Choose Your Agent
Each developer should pick an agent role and use the corresponding prompt file.

### 2. Set Up Communication
```bash
# Create communication directory
mkdir -p ../.agent_comm
echo '{"messages":[]}' > ../.agent_comm/messages.json
```

### 3. Start Your Agent Session
Open a new terminal and:
1. Copy the entire content of your agent's `.md` file
2. Paste it as the system prompt in your Claude session
3. Begin working on your assigned tasks

### 4. Use Coordination Protocol
Follow the guidelines in `coordination_guide.md` for:
- Communication between agents
- Git branching strategy
- Priority levels and tags
- Conflict resolution

## 🎯 Current Priorities (Phase 3)

### Product Manager
- Define sprint goals and acceptance criteria
- Prioritize feature backlog
- Track KPIs and success metrics
- Ensure vision alignment

### Frontend Agent
- Implement new athletic blue/coral theme
- Create runner silhouette illustrations
- Design Chicago Marathon branding

### Backend Agent  
- Set up Vertex AI integration
- Implement GPS tracking service
- Create ML models for fitness predictions

### QA Agent
- Write tests for run tracking
- Test Vertex AI integrations
- Performance testing for new UI

### DevOps Agent
- Fix Hilt/KSP compatibility issue
- Set up GCP infrastructure
- Configure CI/CD pipeline

## 💬 Communication Tags

Use these standardized tags in your commits and messages:

- `[PRODUCT-DECISION]`, `[SCOPE-ALERT]`, `[ACCEPTANCE-FAILED]` for product
- `[UI-UPDATE]`, `[BACKEND-UPDATE]`, `[TEST-RESULT]`, `[ARCH-CHANGE]`
- `[BUG-P1]`, `[BUG-P2]`, `[BUG-P3]` for bug priorities
- `[NEED-BACKEND]`, `[NEED-UI]`, `[FIX-NEEDED]` for requests
- `[RELEASE-BLOCKER]` for critical issues

## 📊 Agent Responsibilities

| Agent | Primary Focus | Key Technologies |
|-------|--------------|------------------|
| Product | Strategy & Vision | User Stories, KPIs, Roadmapping, Analytics |
| Frontend | UI/UX Development | Jetpack Compose, Material 3, Navigation |
| Backend | Data & AI | Vertex AI, Room, Ktor, Coroutines |
| QA | Testing | JUnit, Compose Testing, Espresso |
| DevOps | Infrastructure | Gradle, GCP, GitHub Actions, Firebase |

## 🔄 Workflow

1. **Morning**: Check messages, pull latest, plan work
2. **Development**: Work on branch, commit frequently
3. **Communication**: Update messages on blockers/needs
4. **Evening**: Push changes, update status

## 📚 Resources

- Main README: `/Users/jaredschueler07/AndroidStudioProjects/FITFOAI/README.md`
- Architecture docs: `/Users/jaredschueler07/AndroidStudioProjects/FITFOAI/project/`
- Claude integration: `/Users/jaredschueler07/AndroidStudioProjects/FITFOAI/CLAUDE.md`

## ⚠️ Important Notes

- Always work on your designated branch type
- Run tests before pushing changes
- Document breaking changes
- Communicate blockers immediately
- Follow the coordination guide for conflicts

---

*For detailed coordination protocols, see `coordination_guide.md`*
