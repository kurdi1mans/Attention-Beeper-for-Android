# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

Android project is **fully implemented** across all 6 milestones. The app compiles, all unit tests pass, and lint reports zero warnings.

## What This App Does

**Attention Beeper** plays a synthesized beep sound at user-configured intervals to aid focus. It runs fully offline with no backend.

Key behaviors:
- Configurable interval (value + unit: seconds or minutes)
- Two modes: **Fixed** (exact timing) or **Random** (random moment within each interval window; windows never overlap)
- 15 built-in sounds, all **programmatically generated** — no audio file assets
- Settings locked during a running session; cannot be changed until stopped
- Settings are **not persisted** between app launches (always start with defaults)
- Background execution: session continues beeping when app is backgrounded or screen is off
- Shows a persistent system notification while a session is running
- Live countdown to next beep (in random mode, reflects the exact pre-calculated time)

Default values: interval=60, unit=Seconds, mode=Random, sound=Digital Beep.

## Architecture Decisions (from requirements)

**Background execution is critical.** Package and library selection must be done carefully to ensure the app never loses background autonomous operation — especially in random mode, which requires recalculating the next beep time after each beep fires. Prefer a Foreground Service approach for reliable background audio scheduling.

**All audio is synthesized.** Use Android's `AudioTrack` or `MediaPlayer` with programmatically generated PCM data — no bundled audio files.

## Sound Library (15 sounds)

`digital`, `ping`, `pluck`, `ding`, `danger`, `chime`, `bell`, `alert`, `drop`, `bubble`, `woodblock`, `chord`, `blip`, `whoosh`, `click`

## Build Commands

Requires `ANDROID_HOME=/home/kaju/android-sdk` (set in `gradle.properties` via `org.gradle.java.home=/home/kaju/jdk21`).

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests on device/emulator
./gradlew lint                 # Run lint checks (currently: 0 warnings)
```

Environment note: `ANDROID_HOME` must be set when running from a shell that doesn't inherit it:
```bash
ANDROID_HOME=/home/kaju/android-sdk ./gradlew assembleDebug
```

## Key Source Files

| File | Purpose |
|------|---------|
| `app/src/main/kotlin/com/attentionbeeper/SoundSynthesizer.kt` | All 15 PCM sound generators |
| `app/src/main/kotlin/com/attentionbeeper/SessionViewModel.kt` | Observable session state |
| `app/src/main/kotlin/com/attentionbeeper/BeepService.kt` | Foreground service, coroutine timer loop |
| `app/src/main/kotlin/com/attentionbeeper/MainActivity.kt` | Single-screen UI wired to service |
