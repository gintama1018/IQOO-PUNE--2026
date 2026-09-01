<div align="center">

# SkillLens

### Your phone can watch you learn a real-world skill.

**AI-powered, real-time practical skill verification for Android.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1.3-34A853?style=flat-square)](https://developer.android.com/training/camerax)
[![MediaPipe](https://img.shields.io/badge/MediaPipe-0.10-FF6F00?style=flat-square&logo=google&logoColor=white)](https://developers.google.com/mediapipe)
[![On-Device AI](https://img.shields.io/badge/AI-On_Device-00A8FF?style=flat-square)]()
[![Status](https://img.shields.io/badge/Status-Hackathon_MVP-FFAB00?style=flat-square)]()
[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=flat-square)](https://developer.android.com/about/versions/oreo)

---

*Point your phone camera at a physical task. SkillLens observes what you do,*
*validates each step against the expected sequence, and tells you immediately when you go wrong.*

**Built for iQOO Hackathon 2026 · Pune**

</div>

---

## Live Demo

<!-- Add demo GIF here: place a GIF at docs/demo.gif showing the full practice flow -->
<!-- ![SkillLens Demo](docs/demo.gif) -->

> **Demo assets have not been added yet.** After recording, place a GIF or video at `docs/demo.gif` showing:
>
> 1. Phone camera pointed at the electrical training board
> 2. Components detected with bounding boxes
> 3. Task step recognized and validated
> 4. Intentional mistake detected with correction feedback
> 5. Mistake corrected, practice continues
> 6. Task completed with performance summary

---

## The Problem

Traditional practical skill training often follows this pattern:

```
WATCH A DEMONSTRATION
         ↓
    TRY IT YOURSELF
         ↓
         ?
```

There is a gap between **knowing the theory** and **performing the task correctly**. A trainee may wire a circuit, connect a pipe, or assemble a component — and have no way of knowing whether they did it right until an instructor checks.

Instructors are scarce. Feedback is delayed. Mistakes compound.

**SkillLens addresses the feedback gap** by turning the phone into a real-time observer that validates physical execution as it happens.

### Target users

| User | Need |
|------|------|
| **ITI / vocational students** | Guided self-practice with live feedback |
| **Apprentices & trainees** | Immediate mistake correction without waiting for an instructor |
| **Instructors** | A supplementary tool to extend their reach across trainees |
| **Hackathon judges** | A working demo of phone-first AI applied to real-world skills |

---

## The Idea

```
TRAINEE
   ↓
PERFORMS REAL TASK (on a physical training board)
   ↓
PHONE CAMERA (observes the work)
   ↓
AI PERCEPTION (detects objects, hands, spatial relationships)
   ↓
TASK STATE ENGINE (validates against expected sequence)
   ↓
VALIDATION RULES (deterministic correctness check)
   ↓
INSTANT FEEDBACK (correct / incorrect / adjust camera)
   ↓
NEXT STEP or CORRECTION
```

> **We don't just detect objects. We detect progress and mistakes.**

The core loop:

**PERFORM → OBSERVE → UNDERSTAND → VERIFY → CORRECT → CONTINUE**

---

## Why a Smartphone?

The smartphone is not merely the UI. **It is the sensing, inference, and feedback device.**

| Phone Capability | SkillLens Use |
|---|---|
| **Camera** | Observe the physical task in real time |
| **GPU / NPU** | Accelerate local ML inference where supported |
| **Screen** | Live camera preview with HUD overlay and feedback |
| **Haptics** | Vibration patterns for correct / error / completion |
| **Local Storage** | Offline session history via Room database |
| **Microphone** | Reserved for potential future audio-based signals |
| **Sensors** | Reserved for optional motion / orientation context |

> The entire product experience — sensing, processing, inference, validation, and feedback — happens on the device in your hand.

---

## Why AI?

SkillLens uses AI **only where perception is genuinely ambiguous**. Task correctness is handled by deterministic logic.

| Responsibility | Handled by | Examples |
|---|---|---|
| **Visual recognition** | AI / ML | Object detection, hand tracking, spatial relationships |
| **Ambiguity resolution** | AI / ML | Is the wire near the terminal or connected to it? |
| **Task sequence validation** | Deterministic engine | Is this the right step? Is the connection correct? |
| **Scoring & completion** | Deterministic engine | Count correct steps, time, corrections |
| **Safety constraints** | Deterministic engine | Block unsafe transitions |
| **Natural-language feedback** | Optional generative AI | Adaptive wording for correction messages |

> A generative model should not be responsible for deterministic task correctness when explicit validation rules are possible.

---

## Why On-Device AI?

| Benefit | Rationale |
|---|---|
| **Low latency** | Real-time corrective feedback during physical execution |
| **Privacy** | Camera frames can capture sensitive environments; local processing avoids transmitting them |
| **Offline resilience** | The core demo works without internet — critical in workshops with poor connectivity |
| **Cost** | No per-inference cloud API charges |
| **Reliability** | The demo does not fail because an external API is temporarily unavailable |

> On-device inference is targeted where supported by the device and chosen runtime. The current implementation uses MediaPipe, which runs inference locally on the device.

**Note:** No `INTERNET` permission is declared in the manifest. The core demo runs fully offline.

---

## How It Works

```
CameraX (back camera, lifecycle-bound)
   ↓
Frame Acquisition (30 FPS preview, RGBA_8888)
   ↓
Frame Sampling (adaptive: ~5–10 FPS to ML inference)
   ↓
Quality Assessment (luminance check, exposure check)
   ↓
MediaPipe Object Detector (electrical components, wires, terminals)
   ↓
MediaPipe Hand Landmarker (hand position and tracking)
   ↓
Spatial Relationship Extraction (proximity, connection heuristics)
   ↓
Deterministic Task State Engine (finite state machine)
   ↓
Validation Rules (per-step correctness check)
   ↓
Feedback Engine (type, message, haptic pattern)
   ↓
Jetpack Compose UI (camera overlay, progress, feedback card)
```

### Stage details

| Stage | What it does |
|---|---|
| **Frame Sampling** | Camera runs at hardware rate; expensive ML inference runs only every Nth frame (adaptive interval 2–10 based on scene activity) |
| **Quality Gate** | Cheap luminance heuristic rejects underexposed / overexposed frames before ML inference |
| **Object Detection** | MediaPipe detects electrical components (terminals, wires, board) with bounding boxes and confidence scores |
| **Hand Tracking** | MediaPipe Hand Landmarker provides 21 landmarks per hand for hand-wire interaction detection |
| **Relationship Extraction** | Proximity-based heuristic determines whether a wire is near, touching, or connected to a terminal |
| **State Machine** | Deterministic finite automaton with debouncing (N consecutive confirming frames before state transition) |
| **Validation** | Per-step rules check required objects, required relationships, and known error conditions |
| **Feedback** | Typed events (CORRECT / ERROR / WARNING / INFO / COMPLETION) with corresponding haptic patterns |

---

## Task State Engine

SkillLens represents the physical task as a **finite sequence of known states**. The state machine is entirely deterministic — AI handles perception, the engine handles judgement.

### MVP task states (Basic Circuit Wiring)

```
IDLE
 ↓
TASK_DETECTED ──── board and terminals visible in frame
 ↓
STEP_1_IDENTIFY ── locate L, N, E terminal blocks
 ↓
STEP_2_PICK_WIRE ─ pick up the Live (red) wire
 ↓
STEP_3_CONNECT_L ─ insert red wire into L terminal
 ↓
STEP_4_CONNECT_N ─ insert black wire into N terminal
 ↓
STEP_5_CONNECT_E ─ insert earth wire into E terminal
 ↓
STEP_6_VERIFY ──── all connections confirmed
 ↓
COMPLETED
```

### Error / perception states

```
WRONG_CONNECTION ─── wire connected to wrong terminal
WRONG_COMPONENT ──── wrong component selected
OUT_OF_ORDER ─────── steps performed in wrong sequence
MISSING_COMPONENT ── required component not visible
LOW_CONFIDENCE ───── model cannot make a confident determination
OCCLUDED ─────────── task area blocked
POOR_FRAMING ─────── camera too far / too close / angled / lighting issue
UNKNOWN_STATE ────── observation cannot be mapped to any known state
```

### Key mechanisms

| Mechanism | Purpose |
|---|---|
| **Debouncing** | Require N consecutive agreeing frames (configurable per step, default 5) before committing a state transition |
| **Confidence threshold** | Each step defines a minimum confidence (0.60–0.75); observations below threshold trigger `LOW_CONFIDENCE` instead of action |
| **Quality gating** | Poor frame quality (underexposed, overexposed, occluded) is caught before the state machine runs |
| **Recovery** | Error states do not terminate the session — the user corrects the action and the engine resumes |
| **Rollback** | No forward transition on error — the user must fix the mistake before proceeding |

---

## Technical Architecture

```
┌─────────────────────────────────────────────────┐
│                 PRESENTATION                     │
│  Jetpack Compose · Material3 · StateFlow · VM    │
│  HomeScreen · SkillSelect · TaskOverview ·       │
│  CameraSetup · LivePractice · Result · History   │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│              PRACTICE ENGINE                     │
│  LivePracticeViewModel · Session · Scoring       │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│               TASK ENGINE                        │
│  StateMachine · Validator · SkillDefinition      │
│  SkillRepository · TaskState · TaskStep          │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│                AI / ML LAYER                     │
│  VisionEngine (MediaPipe ObjectDetector +        │
│  HandLandmarker) · SpatialRelationship ·         │
│  Heuristic fallback                              │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│             CAMERA / DEVICE LAYER                │
│  CameraController (CameraX) · FrameAnalyzer ·   │
│  Adaptive frame sampling · Quality assessment    │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│                  DATA LAYER                      │
│  Room (SessionEntity, StepResultEntity) ·        │
│  SessionRepository · DataStore Preferences       │
└─────────────────────────────────────────────────┘
```

<!-- Add architecture diagram image here: docs/images/architecture.png -->

---

## Android Technology Stack

Every dependency listed below is present in the project's `build.gradle.kts` and version catalog.

| Technology | Version | Role in SkillLens |
|---|---|---|
| **Kotlin** | 2.0.21 | Primary language |
| **Jetpack Compose** | BOM 2024.09.03 | Declarative UI with Material3 dark theme |
| **CameraX** | 1.3.4 | Camera lifecycle, preview, and image analysis pipeline |
| **MediaPipe Tasks Vision** | 0.10.14 | On-device object detection + hand landmark tracking |
| **Room** | 2.6.1 | Local session and step-result storage |
| **DataStore Preferences** | 1.1.1 | User settings persistence |
| **Hilt** | 2.52 | Dependency injection |
| **Navigation Compose** | 2.8.2 | Type-safe screen navigation |
| **Coroutines** | 1.8.1 | Structured concurrency for camera + ML pipeline |
| **StateFlow** | (via Lifecycle 2.8.6) | Reactive UI state management |
| **ViewModel** | (via Lifecycle 2.8.6) | Lifecycle-aware presentation logic |
| **Kotlinx Serialization** | 1.7.3 | Task definition serialization |
| **Lottie Compose** | 6.5.2 | Micro-animations |
| **Timber** | 5.0.1 | Debug logging |
| **Coil** | 2.7.0 | Image loading |
| **SplashScreen API** | 1.0.1 | Android 12+ splash screen |
| **KSP** | 2.0.21-1.0.27 | Annotation processing (Room, Hilt) |

---

## Project Structure

This is the **implemented** source tree as it exists in the repository.

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── models/                      ← ML model files (see README inside)
│
├── java/com/skilllens/app/
│   ├── SkillLensApp.kt              ← Application class (Hilt + Timber init)
│   ├── MainActivity.kt              ← Single Activity entry point
│   │
│   ├── ui/
│   │   ├── navigation/
│   │   │   ├── Screen.kt            ← Sealed route definitions
│   │   │   └── NavGraph.kt          ← Full navigation graph with transitions
│   │   ├── theme/
│   │   │   ├── Color.kt             ← Dark engineering palette (30+ tokens)
│   │   │   ├── Type.kt              ← Typography system
│   │   │   └── Theme.kt             ← Material3 dark theme + extended colors
│   │   ├── home/
│   │   │   └── HomeScreen.kt        ← Hero landing with animated glow
│   │   ├── skills/
│   │   │   ├── SkillSelectScreen.kt  ← Active + future skills
│   │   │   ├── TaskOverviewScreen.kt ← Steps, equipment, safety warning
│   │   │   └── HistoryScreen.kt      ← Session history
│   │   ├── practice/
│   │   │   ├── CameraSetupScreen.kt      ← Calibration / framing guide
│   │   │   ├── LivePracticeScreen.kt     ← Core screen: camera + HUD + feedback
│   │   │   └── LivePracticeViewModel.kt  ← Coordinates camera → vision → engine
│   │   ├── results/
│   │   │   └── ResultScreen.kt       ← Score ring, step breakdown, retry
│   │   └── settings/
│   │       └── SettingsScreen.kt     ← Preferences, privacy info, about
│   │
│   ├── camera/
│   │   ├── CameraController.kt      ← CameraX lifecycle, KEEP_ONLY_LATEST
│   │   └── FrameAnalyzer.kt         ← Adaptive frame sampling, quality check
│   │
│   ├── vision/
│   │   └── VisionEngine.kt          ← MediaPipe inference + heuristic fallback
│   │
│   ├── taskengine/
│   │   ├── TaskDefinition.kt        ← Domain models (Skill, Step, Observation)
│   │   ├── StateMachine.kt          ← Deterministic state machine with debounce
│   │   ├── Validator.kt             ← Per-step validation rules
│   │   └── SkillRepository.kt       ← Built-in skill definitions (MVP: wiring)
│   │
│   ├── data/
│   │   ├── database/
│   │   │   ├── SkillLensDatabase.kt  ← Room database
│   │   │   └── Entities.kt           ← SessionEntity, StepResultEntity, DAO
│   │   └── repository/
│   │       └── SessionRepository.kt  ← Session lifecycle abstraction
│   │
│   └── di/
│       ├── DatabaseModule.kt         ← Hilt Room provider
│       └── TaskEngineModule.kt       ← Hilt engine providers
│
└── res/
    ├── values/
    │   ├── strings.xml               ← Externalized strings (localization-ready)
    │   ├── colors.xml                ← XML color resources
    │   └── themes.xml                ← Splash + app theme
    ├── xml/
    │   ├── backup_rules.xml
    │   └── data_extraction_rules.xml
    └── mipmap-anydpi-v26/
        ├── ic_launcher.xml           ← Adaptive icon (eye/lens vector)
        └── ic_launcher_round.xml
```

---

## User Flow

### Primary flow (happy path)

```
Launch App
   ↓
Home Screen (hero, CTA, features)
   ↓
Choose Skill → "Basic Circuit Wiring"
   ↓
Task Overview (steps, equipment, safety warning)
   ↓
Camera Setup (framing guide, positioning tips)
   ↓
[Camera permission granted]
   ↓
Live Practice (camera preview + HUD overlay)
   ↓
AI observes → State Engine validates → Feedback rendered
   ↓
Step verified ✓  →  Next step
   ↓ or
Mistake detected ⚠  →  Correction displayed  →  User fixes  →  Resume
   ↓
All steps complete
   ↓
Result Screen (animated score, step breakdown, retry)
```

### Failure flows

| Situation | System behaviour |
|---|---|
| Camera permission denied | Explanation screen with re-request button |
| Camera unavailable | Error screen with human-readable message |
| Low light | Quality gate: "Move to a better-lit area" |
| Poor framing | Quality gate: "Position the board inside the frame" |
| Object not detected | Stay in current step; prompt to adjust camera |
| Low model confidence | `LOW_CONFIDENCE` state: "I can't clearly see the connection. Move the phone closer." |
| App backgrounded | Camera and inference stop; session pauses |
| Model load failure | Heuristic fallback mode (reduced accuracy); logged via Timber |

---

## Live Practice Experience

The live practice screen is the core product experience.

```
┌──────────────────────────────────────────────┐
│  ← Basic Circuit Wiring    ● STEP_3   02:34  │  ← Status bar
├──────────────────────────────────────────────┤
│                                              │
│    ┌──┐                          ┌──┐        │  ← Camera preview
│    └──┘  ╔═══════════╗           └──┘        │
│          ║ red_wire  ║ 0.82                  │  ← Bounding box + label
│          ╚═══════════╝                       │
│               ┌─────────┐                    │
│               │L terminal│                   │
│               └─────────┘                    │
│          +            +                      │  ← HUD crosshair
│                                              │
│    ┌──┐                          ┌──┐        │  ← Corner brackets
│    └──┘                          └──┘        │
├──────────────────────────────────────────────┤
│  STEP 3 / 6     ███████████░░░░░░  50%       │  ← Progress bar
├──────────────────────────────────────────────┤
│  ✓ Connect Live (L)                          │
│  Step verified. Continue to next.            │  ← Feedback card
├──────────────────────────────────────────────┤
│      [ Reset ]           [ Pause ]           │  ← Action bar
└──────────────────────────────────────────────┘
```

### Error state example

```
│  ⚠ WRONG CONNECTION                         │
│  Wire is connected to l1. Check the          │
│  expected terminal.                          │
```

### UI elements

- **Camera preview**: Full-bleed CameraX `PreviewView` with FILL_CENTER
- **Bounding boxes**: Canvas overlay; color-coded (blue = detected, green = verified, red = error)
- **HUD brackets**: Animated corner brackets and center crosshair for engineering feel
- **Progress bar**: Animated horizontal bar with gradient (primary → cyan)
- **Feedback card**: `AnimatedContent` slide-in transition; icon + title + message; background color by type
- **Pause overlay**: Scrim with Resume / Quit options

---

## Error & Uncertainty Handling

SkillLens does not fake certainty. The system **explicitly handles what it cannot see**.

```
HIGH CONFIDENCE (≥ step threshold, typically 0.65–0.75)
→ Validate and provide feedback

MEDIUM CONFIDENCE (below threshold but above 0.40)
→ Stay in current state; prompt: "Hold steady, verifying..."

LOW CONFIDENCE (below 0.40)
→ Do NOT make a judgement
→ Display: "I can't clearly see the connection. Move the phone closer."
```

| Challenge | How it's handled |
|---|---|
| Occlusion | Quality gate catches `OCCLUDED`; prompts user to move hands |
| Poor lighting | Luminance check on downsampled frame; rejects underexposed frames |
| Camera movement | KEEP_ONLY_LATEST backpressure; no stale-frame accumulation |
| Object loss | Object must be re-detected before the engine resumes |
| Ambiguous state | Debounce prevents premature transitions; N consecutive frames required |

---

## Privacy

The current implementation follows a **local-first privacy model**.

```
Camera Frame
   ↓
Local ML Inference (MediaPipe, on-device)
   ↓
Detected Objects + Relationships (structured data only)
   ↓
Task State Engine (deterministic validation)
   ↓
Feedback rendered to screen
   ↓
Raw frame discarded (not stored, not transmitted)
```

| Data | Handling |
|---|---|
| Camera frames | Processed in memory, not saved to storage, not transmitted |
| Session results | Stored locally in Room database |
| Network usage | **None** — no `INTERNET` permission declared |
| Cloud sync | Not implemented |
| Deletion | Clear data via Android system settings or app reinstall |
| Backup | Cloud backup explicitly disabled in manifest (`allowBackup=false`) |

---

## Permissions

| Permission | Purpose | Required? |
|---|---|---|
| `CAMERA` | Live observation of the physical task | **Yes** |
| `VIBRATE` | Haptic feedback (correct / error / completion) | Yes (non-dangerous, auto-granted) |
| `MANAGE_THERMAL` | Monitor device temperature for adaptive throttling | Optional (graceful fallback) |
| `FOREGROUND_SERVICE` | Maintain practice session if briefly backgrounded | Optional |
| `FOREGROUND_SERVICE_CAMERA` | Camera access in foreground service | Optional |
| `POST_NOTIFICATIONS` | Post-session summary notification | Optional (Android 13+ runtime prompt) |

**Not requested:** `INTERNET`, `RECORD_AUDIO`, `READ/WRITE_EXTERNAL_STORAGE`, `ACCESS_FINE_LOCATION`.

Unnecessary permissions are intentionally avoided.

---

## Performance & Battery Optimization

### Design targets (not measured benchmarks)

```
30 FPS camera preview (hardware default)
         ↓
~5–10 FPS ML inference (adaptive sampling)
         ↓
Task state updates (event-driven, not polling)
```

| Strategy | Implementation |
|---|---|
| **Adaptive frame sampling** | `FrameAnalyzer` runs inference every Nth frame; interval adapts (2–10) based on scene activity |
| **KEEP_ONLY_LATEST** | CameraX ImageAnalysis backpressure discards stale frames when inference is busy |
| **Single-thread executor** | One frame at a time; no infinite inference backlog |
| **Quality pre-filter** | Cheap luminance check on 64×64 downsampled bitmap before expensive ML inference |
| **Lifecycle-aware camera** | Camera bound to lifecycle; released on screen exit |
| **Lazy model loading** | ML models loaded when entering practice, released on exit |
| **No background processing** | Inference and camera stop when app is backgrounded |
| **Thermal monitoring** | `MANAGE_THERMAL` permission declared for future adaptive degradation |

---

## Offline Capability

### Works fully offline

- ✅ Camera capture and preview
- ✅ MediaPipe ML inference (models bundled in APK)
- ✅ Task state engine and validation
- ✅ Real-time feedback
- ✅ Scoring and completion
- ✅ Local session history (Room)
- ✅ Settings and preferences

### Requires network (not implemented in MVP)

- ⬜ Model updates / downloads
- ⬜ Cloud session sync
- ⬜ Analytics
- ⬜ Remote instructor monitoring

---

## Installation

### Requirements

| Requirement | Value |
|---|---|
| Android Studio | Ladybug (2024.2) or newer recommended |
| JDK | 17 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Device | Android phone with rear camera |
| Model files | See `app/src/main/assets/models/README.md` |

### Setup

```bash
git clone https://github.com/<your-username>/SkillLens.git
cd SkillLens
```

1. **Open in Android Studio** → File → Open → select the project root
2. **Gradle sync** — Android Studio will resolve all dependencies automatically
3. **Add model files** — Download or train the required MediaPipe `.task` models and place them in:
   ```
   app/src/main/assets/models/
   ├── electrical_components_detector.task
   └── hand_landmarker.task
   ```
   See [`app/src/main/assets/models/README.md`](app/src/main/assets/models/README.md) for instructions.

   > **Note:** If model files are not present, the VisionEngine falls back to a heuristic color-based detection mode (low accuracy, for basic demo continuity only).

4. **Build and install**

### Build & Run

**Debug build (recommended for hackathon):**

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**Install on connected device:**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Or directly from Android Studio:** Run ▶ on connected device.

---

## Live Demo

### 5-second pitch

> *"SkillLens watches you perform a real physical skill and immediately tells you when you go wrong."*

### 30-second demo script (for judges)

```
1. Launch SkillLens on the phone
2. Tap "Start Practice"
3. Select "Basic Circuit Wiring"
4. Read the safety note and task steps → tap "Begin Practice"
5. Position the phone above the training board inside the animated guide frame
6. Tap "Start Practice" on the calibration screen
7. Perform Step 1: let the AI identify the terminal blocks
8. Pick up the red wire — observe hand detection
9. Connect the red wire to the L terminal — see ✓ STEP VERIFIED
10. Intentionally connect the black wire to the WRONG terminal
11. Observe: ⚠ WRONG CONNECTION with correction message
12. Fix the mistake — connect to the N terminal
13. Complete all steps
14. View the result: score ring, step breakdown, best/needs-practice
15. Tap "Practice Again" to reset
```

### Demo recovery

If live computer vision produces inconsistent results during a judge demo:

- Use the designated **demo training board** under controlled lighting
- Ensure the board fills most of the camera frame
- The heuristic fallback detects wire colors even without the trained model
- Session can be reset instantly via the **Reset** button

---

## Hackathon Design Philosophy

| Principle | Application |
|---|---|
| **Phone-first** | The phone is the sensing device, not just a screen |
| **Real-time** | Feedback during execution, not after |
| **AI where necessary** | ML for perception; deterministic logic for correctness |
| **Offline-first core** | No `INTERNET` permission; core demo does not depend on cloud |
| **Honest uncertainty** | Low confidence = ask for better view, never confidently guess |
| **Constrained scope** | One reliable task > fake universal coverage |

---

## What Makes It Different

SkillLens focuses on **real-time, phone-first task-state verification during physical execution**.

| Capability | Traditional Tutorial | AI Tutor / Chatbot | Video Review | **SkillLens** |
|---|---|---|---|---|
| Observes physical task | No | No | Yes (recorded) | **Yes (live)** |
| Live during execution | No | No | Usually delayed | **Yes** |
| Task-state tracking | No | No | Partial | **Yes (FSM)** |
| Immediate correction | No | Text-based | Delayed | **Yes (visual + haptic)** |
| On-device, offline | N/A | Depends | Sometimes | **Yes** |
| Phone-native | Partial | Partial | Partial | **Core** |

> We do not claim absolute novelty. Vocational video assessment, computer-vision inspection, and AI tutoring all exist. Our approach combines phone-first live observation with a deterministic task engine for immediate, verifiable feedback.

---

## Current Limitations

Honesty strengthens credibility.

- **One task:** The MVP supports only one constrained electrical wiring exercise
- **Controlled environment:** Requires stable, well-lit conditions with the board clearly visible
- **Limited states:** 6 task steps + error states; not a comprehensive electrical curriculum
- **Lighting sensitivity:** Poor lighting degrades object detection significantly
- **Occlusion sensitivity:** Hands or tools blocking the board can interrupt detection
- **Device testing:** Tested on a limited set of devices during hackathon
- **Custom model dependency:** Full detection accuracy requires a trained MediaPipe model; heuristic fallback is low-accuracy
- **Not a certification system:** SkillLens is a practice-feedback tool, not a replacement for professional certification
- **Not a replacement for instructors:** Designed to supplement, not replace, human trainers
- **Not for hazardous work:** Must only be used with safe, low-voltage educational training boards

---

## Safety

> ⚡ **SAFETY WARNING**
>
> The MVP demo task involves **electrical wiring on a safe, low-voltage educational training board**.
>
> **Do NOT use SkillLens on live mains electricity.**
>
> SkillLens is a training and assessment aid. It is not professional safety certification. It does not guarantee correctness. Always follow proper electrical safety procedures and work under qualified supervision for any real-world electrical work.

---

## Roadmap

### Now — Hackathon MVP

- [x] Android app shell with Jetpack Compose dark theme
- [x] CameraX pipeline with adaptive frame sampling
- [x] MediaPipe VisionEngine with object detection + hand tracking
- [x] Deterministic task state machine with debouncing and confidence thresholds
- [x] One complete skill: Basic Circuit Wiring (6 steps)
- [x] Real-time bounding box overlay with HUD
- [x] Animated feedback cards (correct / error / warning / completion)
- [x] Result screen with score ring and step breakdown
- [x] Room database for session persistence
- [x] Fully offline operation (no INTERNET permission)
- [x] Heuristic fallback when ML model is unavailable

### Next — Post-hackathon improvements

- [ ] Train and package custom electrical component detector model
- [ ] Stronger action recognition (temporal, not just spatial)
- [ ] Improved calibration with automatic board detection
- [ ] More detailed scoring (per-step timing, accuracy metrics)
- [ ] Instructor mode (review trainee sessions)
- [ ] Multilingual feedback (Hindi, Marathi, regional languages)
- [ ] Thermal-aware inference throttling
- [ ] Performance benchmarking and optimization

### Future — Potential expansion (not implemented)

- [ ] Additional electrical tasks (3-phase wiring, MCB installation)
- [ ] Plumbing — pipe fitting and connection
- [ ] HVAC — thermostat wiring
- [ ] Automotive — spark plug replacement
- [ ] Manufacturing — assembly sequences
- [ ] Data-driven skill definition loading (JSON / remote)

*All future items are architectural possibilities, not current features.*

---

## Contributing

This is a hackathon project. Contributions are welcome.

```
1. Fork the repository
2. Create a feature branch: git checkout -b feature/your-feature
3. Commit with clear messages
4. Push and open a Pull Request
5. Ensure the project builds: gradlew assembleDebug
```

### Conventions

- Kotlin with Jetpack Compose
- Follow existing package structure
- Use Timber for logging (never `Log.d`)
- Externalize user-facing strings to `strings.xml`
- AI/ML handles perception only; deterministic logic handles validation

---

## License

> A license has not yet been added to this repository.

---

## Acknowledgements

Built with:

- [Android Jetpack](https://developer.android.com/jetpack) — Compose, CameraX, Room, Navigation, Lifecycle, DataStore
- [MediaPipe](https://developers.google.com/mediapipe) — On-device vision tasks (object detection, hand landmarks)
- [Hilt](https://dagger.dev/hilt/) — Dependency injection
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) — Task definition serialization
- [Lottie](https://airbnb.design/lottie/) — Micro-animations
- [Timber](https://github.com/JakeWharton/timber) — Logging
- [Coil](https://coil-kt.github.io/coil/) — Image loading
- [Material Design 3](https://m3.material.io/) — Design system

---

## Team & Hackathon

```
Built for:   iQOO Hackathon 2026 — Pune
Track:       TBD (Smart Education / Open Innovation)
Status:      Hackathon MVP
Version:     1.0.0-hackathon
```

<!-- Add team members here -->
<!-- | Name | Role | GitHub | -->
<!-- |------|------|--------| -->
<!-- | TBD  | TBD  | TBD    | -->

---

## Technical FAQ

### Why not a chatbot?

Because SkillLens observes **physical execution through a camera**, not text input. The value is in watching the real world, not generating text.

### Why a phone?

The phone provides camera, compute, and immediate feedback at the point of physical work. No additional hardware needed.

### Why AI?

Because recognising physical objects, hand positions, and spatial relationships from a camera feed is not a simple if/else problem. Visual perception is inherently ambiguous.

### Why on-device?

Latency (real-time feedback requires low latency), privacy (camera frames stay local), offline resilience (workshops may have poor connectivity), and cost (no cloud inference charges).

### What happens if the model is uncertain?

The system asks for a better view: *"I can't clearly see the connection. Move the phone closer."* It never confidently guesses when it cannot see.

### Can it replace a trainer?

No. SkillLens is a practice-feedback layer that supplements instructor-led training. It does not provide professional certification.

### Can it support other skills?

Architecturally, yes. The `SkillDefinition` data structure is generic — adding a new skill means adding a new definition (required objects, expected states, validation rules, error messages). The engine code does not change. However, the hackathon MVP is constrained to one validated task.

### Why is the state machine deterministic instead of using an LLM?

Because task correctness is not a matter of opinion. Either the wire is in the right terminal or it isn't. A deterministic rule engine is auditable, testable, predictable, and does not hallucinate. AI handles what AI is good at (perception); rules handle what rules are good at (validation).

---

<div align="center">

---

> **Don't just learn how to do it. Know when you did it right.**

<br>

**SkillLens**
Real-time practical skill verification,
built for the smartphone.

---

*Made with precision at iQOO Hackathon 2026, Pune.*

</div>
