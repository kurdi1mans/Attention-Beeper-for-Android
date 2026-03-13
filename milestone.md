# Attention Beeper — Build Milestones

Milestones are ordered and sequential. Complete each milestone's verification step before proceeding.

---

## Milestone 1 — Project Scaffolding

**Goal:** A compilable Android project with no logic yet.

### Tasks
- Initialize Android project (Kotlin, minSdk 26, targetSdk 35)
- Configure root and app-level `build.gradle.kts` (Kotlin DSL)
- Set up `settings.gradle.kts` and `.gitignore` additions
- Enable ViewBinding in `build.gradle.kts`; no other third-party dependencies
- Remove boilerplate activity template code (blank slate `MainActivity`)

### Verify
```bash
./gradlew assembleDebug
```
Build succeeds with zero errors.

---

## Milestone 2 — Audio Engine

**Goal:** All 15 sounds playable programmatically with no audio file assets.

### Tasks
- Create `SoundSynthesizer` class using `AudioTrack` (MODE_STATIC, ENCODING_PCM_16BIT, 44100 Hz)
- Implement all 15 synthesized sounds as named generator functions:
  `digital`, `ping`, `pluck`, `ding`, `danger`, `chime`, `bell`, `alert`, `drop`, `bubble`, `woodblock`, `chord`, `blip`, `whoosh`, `click`
- Expose a `play(soundId: String)` method that selects and plays the corresponding sound
- Each generator returns a `ShortArray` of PCM samples

### Verify
- Unit test: each sound generator returns a non-zero `ShortArray`
- Manual test: call `play()` for each sound from a temporary button in `MainActivity`; confirm audible output via logcat tag

---

## Milestone 3 — Session Logic

**Goal:** Pure, testable session state and scheduling logic with no Android framework dependencies.

### Tasks
- Create `SessionViewModel` (extends `ViewModel`) with `StateFlow` for:
  - `sessionRunning: Boolean`
  - `intervalValue: Int`
  - `intervalUnit: IntervalUnit` (SECONDS, MINUTES)
  - `intervalMode: IntervalMode` (FIXED, RANDOM)
  - `selectedSound: String`
  - `countdown: Long` (milliseconds remaining to next beep; `-1` when stopped)
- Default values: `intervalValue=60`, `intervalUnit=SECONDS`, `intervalMode=RANDOM`, `selectedSound="digital"`
- Implement Fixed mode scheduling: next beep fires exactly `intervalMs` after the last
- Implement Random mode scheduling: at the start of each window, pick a random offset in `[0, intervalMs)`, schedule the beep at that offset, begin the next window immediately when the current window ends
- Session logic must be independent of `Service`/`Context` so it can be unit tested

### Verify
- Unit test: Fixed mode — beep times are separated by exactly `intervalMs`
- Unit test: Random mode — each beep fires within its window; windows do not overlap
- Unit test: countdown decrements correctly from the scheduled beep time
- Unit test: defaults match spec on fresh `ViewModel` instantiation

---

## Milestone 4 — Foreground Service

**Goal:** Session runs reliably in the background; beeps continue when app is backgrounded or screen is off.

### Tasks
- Create `BeepService : Service` as a Foreground Service
- Service owns a `CoroutineScope` (using `Dispatchers.Default`) that drives the session timer loop
- Service uses `delay()` inside a coroutine loop — no `AlarmManager`, no `WorkManager`, no `Handler.postDelayed`
- Service calls `SoundSynthesizer.play()` at each scheduled beep time
- Service binds to `MainActivity` via `ServiceConnection`; exposes a `StateFlow` for UI to observe (mirrors `SessionViewModel` state)
- On start: show a persistent notification
  - Title: "Attention Beeper"
  - Content: "Session Running"
  - Action button: "Stop" (sends stop intent to service)
- On stop: cancel the notification
- Handle `START_STICKY` so the service restarts if killed by the system
- Register service in `AndroidManifest.xml` with `FOREGROUND_SERVICE` permission and `foregroundServiceType="mediaPlayback"` (or appropriate type for audio)

### Verify
- Start a session, background the app — beeps continue
- Turn off the screen — beeps continue
- Persistent notification appears on Start; tap Stop action — session stops and notification disappears
- Re-open app after backgrounding — UI reflects correct running state and countdown

---

## Milestone 5 — Main UI (MainActivity)

**Goal:** Single-screen UI wired to the service with all controls, indicators, and responsive layout.

### Tasks
- Layout file: single `ConstraintLayout`, no horizontal scroll on any screen width
- Controls:
  - Interval value: `EditText` (numeric input, integer only)
  - Unit selector: `Spinner` (Seconds / Minutes)
  - Mode selector: `RadioGroup` (Fixed / Random)
  - Sound selector: `Spinner` (lists all 15 sounds by display name)
  - Test button: always enabled regardless of session state
  - Start button: visible only when session is stopped
  - Stop button: visible only when session is running
  - Countdown `TextView`: visible only while session is running; format MM:SS
  - Status indicator: `TextView` + animated drawable (distinct visual states for stopped vs running)
- Bind to `BeepService` on `onStart`; unbind on `onStop`
- Observe `StateFlow` from service; update all UI elements reactively
- Lock all settings controls (interval value, unit, mode, sound) while session is running
- Unlock all settings controls when session stops

### Verify
- All controls render correctly on a 360dp-wide screen (no horizontal scroll)
- All controls render correctly on a 600dp-wide tablet screen
- Start → controls lock, countdown appears, status indicator animates
- Stop → controls unlock, countdown disappears, status indicator returns to stopped state
- Rotating the screen during a running session preserves state correctly

---

## Milestone 6 — Integration & Polish

**Goal:** All features connected end-to-end; app passes the full verification checklist.

### Tasks
- Wire Test button to `SoundSynthesizer.play(selectedSound)` — works independently of session state
- Ensure Stop via notification action propagates back to `MainActivity` and updates UI (not just stops the service)
- Confirm fresh launch always resets to defaults (no `SharedPreferences` writes)
- Run lint and fix all warnings:
  ```bash
  ./gradlew lint
  ```
- Update `CLAUDE.md` build commands section with final confirmed commands

### End-to-End Verification Checklist
- [ ] App launches with correct defaults: interval=60, unit=Seconds, mode=Random, sound=Digital Beep
- [ ] All 15 sounds are audible via the Test button
- [ ] Fixed mode: beep fires at the exact interval boundary
- [ ] Random mode: beep fires at a random moment within each window; windows do not overlap
- [ ] Settings controls are locked after pressing Start
- [ ] Settings controls are unlocked after pressing Stop
- [ ] Countdown displays in MM:SS and counts down while running
- [ ] Countdown disappears immediately when session is stopped
- [ ] App backgrounded → beeps continue uninterrupted
- [ ] Screen turned off → beeps continue uninterrupted
- [ ] Persistent notification appears when session starts
- [ ] Persistent notification disappears when session stops
- [ ] Tapping Stop in the notification stops the session and updates the in-app UI
- [ ] Fresh app launch always resets all settings to defaults
- [ ] No horizontal scroll on narrow screens (360dp)
- [ ] `./gradlew lint` passes with zero warnings
