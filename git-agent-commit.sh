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
