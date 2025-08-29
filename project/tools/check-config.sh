#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PROP_FILE="$ROOT_DIR/local.properties"

if [[ ! -f "$PROP_FILE" ]]; then
  echo "ERROR: local.properties not found at $PROP_FILE" >&2
  exit 1
fi

AI_PROVIDER=$(grep -E '^AI_PROVIDER=' "$PROP_FILE" | cut -d'=' -f2 | tr -d '\r' || true)
OPENAI_KEY=$(grep -E '^OPENAI_API_KEY=' "$PROP_FILE" | cut -d'=' -f2 | tr -d '\r' || true)
GEMINI_KEY=$(grep -E '^GEMINI_API_KEY=' "$PROP_FILE" | cut -d'=' -f2 | tr -d '\r' || true)

echo "AI_PROVIDER=${AI_PROVIDER:-GEMINI}"

if [[ "${AI_PROVIDER:-GEMINI}" =~ ^(?i)GPT$ ]]; then
  if [[ -z "${OPENAI_KEY:-}" ]]; then
    echo "ERROR: AI_PROVIDER=GPT requires OPENAI_API_KEY in local.properties" >&2
    exit 2
  fi
  echo "OK: OPENAI_API_KEY present"
else
  if [[ -z "${GEMINI_KEY:-}" ]]; then
    echo "WARN: GEMINI_API_KEY not set; Gemini calls will fail" >&2
  else
    echo "OK: GEMINI_API_KEY present"
  fi
fi

echo "Config check passed."

