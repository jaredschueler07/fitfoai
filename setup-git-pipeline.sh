#!/bin/bash

# FITFOAI Git Setup Script
# Run this before starting the autonomous pipeline

echo "🚀 FITFOAI Git Setup for Autonomous Pipeline"
echo "============================================"

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Error: Not in FITFOAI root directory"
    echo "Please run from: /Users/jaredschueler07/AndroidStudioProjects/FITFOAI"
    exit 1
fi

# Check git status
echo "📊 Checking git status..."
git status

# Ensure we're on main/master branch
CURRENT_BRANCH=$(git branch --show-current)
echo "Current branch: $CURRENT_BRANCH"

# Create .gitignore entries for sensitive files if needed
echo "📝 Checking .gitignore..."
if ! grep -q "local.properties" .gitignore; then
    echo "local.properties" >> .gitignore
    echo "Added local.properties to .gitignore"
fi

if ! grep -q ".agent_comm/" .gitignore; then
    echo ".agent_comm/" >> .gitignore
    echo "Added .agent_comm/ to .gitignore"
fi

# Commit current state before pipeline starts
echo "💾 Committing current state..."
git add .
git commit -m "[SETUP] Pre-pipeline checkpoint - all agents ready" || echo "Nothing to commit"

# Create feature branch structure
echo "🌳 Creating branch structure..."

# Array of feature branches
branches=(
    "feature/3.1-gps-tracking"
    "feature/3.2-voice-coaching"
    "feature/3.3-spotify"
    "feature/3.4-training-plans"
    "feature/3.5-social"
    "feature/3.6-achievements"
    "test/integration-suite"
    "docs/prd-updates"
)

# Create branches
for branch in "${branches[@]}"; do
    if git show-ref --verify --quiet refs/heads/$branch; then
        echo "✓ Branch $branch already exists"
    else
        git branch $branch
        echo "✓ Created branch: $branch"
    fi
done

# Set up git aliases for agents
echo "⚙️ Setting up git aliases..."
git config alias.agent-commit '!f() { git add . && git commit -m "[$1][$2] $3"; }; f'
git config alias.checkpoint '!git add . && git commit -m "[CHECKPOINT] $(date +%H:%M) - Work in progress"'
git config alias.sprint-complete '!f() { git add . && git commit -m "[COMPLETE] Sprint $1 - All acceptance criteria met"; }; f'

# Create agent commit helper script
cat > git-agent-commit.sh << 'EOF'
#!/bin/bash
# Helper script for agent commits
# Usage: ./git-agent-commit.sh AGENT SPRINT "Description"

AGENT=$1
SPRINT=$2
DESC=$3

if [ -z "$AGENT" ] || [ -z "$SPRINT" ] || [ -z "$DESC" ]; then
    echo "Usage: ./git-agent-commit.sh AGENT SPRINT \"Description\""
    echo "Example: ./git-agent-commit.sh PM 3.1 \"Updated PRD with sprint goals\""
    exit 1
fi

git add .
git commit -m "[$AGENT][$SPRINT] $DESC"
git push origin $(git branch --show-current)
EOF

chmod +x git-agent-commit.sh

echo "✅ Git setup complete!"
echo ""
echo "📋 Summary:"
echo "- Current branch: $CURRENT_BRANCH"
echo "- Feature branches created: ${#branches[@]}"
echo "- Git aliases configured"
echo "- Commit helper script created"
echo ""
echo "🎯 Next steps:"
echo "1. Run: git branch -a  (to see all branches)"
echo "2. Start the autonomous pipeline"
echo "3. Agents will auto-commit every 30 minutes"
echo ""
echo "💡 Quick commands:"
echo "- git agent-commit PM 3.1 \"Description\"  # Agent commit"
echo "- git checkpoint  # Quick checkpoint commit"
echo "- git sprint-complete 3.1  # Mark sprint complete"
echo ""
echo "Ready for autonomous pipeline! 🚀"