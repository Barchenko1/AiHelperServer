# AiHelperCLI

Ai integration service for screenshots, voice and extension help

# Build

` ./gradlew clean build `

# Run

java -DOPEN_AI_API_KEY='open-ai-token' \
-jar build/libs/AiHelperServer-0.0.1-SNAPSHOT.jar
# Choosing the model

The client decides per request via the `X-AI-Provider` header (`claude` or `openai`):
the Chrome extension has a Model dropdown, the CLI reads `aiProvider` from its `application.properties`.
Without the header the server uses `ai.provider` (default `claude`). Pass `-DANTHROPIC_API_KEY=...` for Claude;
model is `claudeModelVersion`. Voice still needs `OPEN_AI_API_KEY` for Whisper transcription.
# Switch to Claude

Set `ai.provider=claude` (or `-Dai.provider=claude`) and pass `-DANTHROPIC_API_KEY=...`; model is `claudeModelVersion`.
Voice still needs `OPEN_AI_API_KEY` for Whisper transcription.
# Choosing the model

The client decides per request via the `X-AI-Provider` header (`claude` or `openai`):
the Chrome extension has a Model dropdown, the CLI reads `aiProvider` from its `application.properties`.
Without the header the server uses `ai.provider` (default `claude`). Pass `-DANTHROPIC_API_KEY=...` for Claude;
model is `claudeModelVersion`. Voice still needs `OPEN_AI_API_KEY` for Whisper transcription.
# Switch to Claude

Set `ai.provider=claude` (or `-Dai.provider=claude`) and pass `-DANTHROPIC_API_KEY=...`; model is `claudeModelVersion`.
Voice still needs `OPEN_AI_API_KEY` for Whisper transcription.
