# AI Coach Chat Architecture

This document summarizes the chat agent implementation so future rebases/merges are straightforward.

## Overview
- Chat agent is separate from Voice Coaching.
- Voice: ElevenLabs TTS; dynamic lines (if needed) use Gemini or cached DB.
- Chat: Uses a provider-agnostic interface to support GPT or Gemini.

## Key Components
- `LLMService`: common interface for AI providers (fitness advice, training plan, run coaching text).
- `OpenAIService`: GPT-based implementation (Chat Completions), with a PhD-level coach persona prompt.
- `GeminiLLMAdapter`: wraps existing `GeminiService` to `LLMService`.
- `ChatContextProvider`: builds concise context from Room (profile + latest/weekly Google Fit summaries).
- `ContextSource` + `ContextPipeline`: pluggable sources that compose a context block.
  - `UserDataContextSource`: wraps `ChatContextProvider`.
  - `KnowledgeBaseContextSource`: loads topic snippets from `assets/knowledge-base` based on message keywords.
- `FitnessCoachAgent`: now depends on `LLMService` and optional `ChatContextProvider`.

## DI & Wiring (`AppContainer`)
- `aiChatAgent`: uses provider-selected `LLMService` (GPT or Gemini) for the AI Coach chat screen.
- `voiceCoachingManager`: continues to use a Gemini-backed agent for dynamic voice lines.
- `ChatContextProvider`: shared for consistent chat grounding.

## Configuration (`local.properties` → BuildConfig)
- `AI_PROVIDER`: `GPT` or `GEMINI` (affects chat only).
- `OPENAI_API_KEY`: required for GPT.
- `OPENAI_MODEL`: optional, defaults to `gpt-4o-mini`.

## UI Updates
- `AICoachScreen`: provider chip (GPT/Gemini) and error banner (e.g., missing API key).

## Files Changed
- Added: `LLMService.kt`, `OpenAIService.kt`, `GeminiLLMAdapter.kt`, `ChatContextProvider.kt`.
- Updated: `FitnessCoachAgent.kt` (to use `LLMService`), `AppContainer`, `MainActivity`, `AICoachScreen`.
- `app/build.gradle.kts`: new BuildConfig fields (AI_PROVIDER, OPENAI_API_KEY, OPENAI_MODEL).

## Rebase Notes
- Touchpoints: `FitnessCoachAgent.kt`, `AppContainer` (DI), `AICoachScreen`, `assets/knowledge-base/*`.
- Conflicts to watch:
  - If `FitnessCoachAgent` signature changes elsewhere, keep `contextPipeline` and `chatContextProvider` optional.
  - DI: preserve `aiChatAgent` (provider-selected) and `voiceCoachingManager` (Gemini-backed).
  - Assets: ensure `assets/knowledge-base/index.json` remains aligned with file names.

## Config Verification
- Run `bash project/tools/check-config.sh` to validate `AI_PROVIDER` and required keys before running.

## Notes on Custom GPTs
- Direct API access to a specific ChatGPT URL (custom GPT) typically requires the GPTs/Assistants API.
- Current implementation mirrors the target GPT’s persona via a structured system prompt on standard chat models.
- If an Assistants API ID becomes available, swap `OpenAIService` to target that ID with minimal changes.
