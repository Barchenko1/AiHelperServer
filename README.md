# AiHelperCLI

Ai integration service for screenshots, voice and extension help

# Build

` ./gradlew clean build `

# Run

java -DOPEN_AI_API_KEY='open-ai-token' \
-jar build/libs/AiHelperServer-0.0.1-SNAPSHOT.jar
# Switch to Claude

Set `ai.provider=claude` (or `-Dai.provider=claude`) and pass `-DANTHROPIC_API_KEY=...`; model is `claudeModelVersion`.
Voice still needs `OPEN_AI_API_KEY` for Whisper transcription.
