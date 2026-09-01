package com.skilllens.app.taskengine

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Task State Engine — Core Domain Models
//
// Design principle:
//   AI/ML handles PERCEPTION (what does the camera see?).
//   This engine handles VALIDATION (is the task being done correctly?).
//   The two concerns are separated deliberately.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents every possible state the task state machine can be in.
 * Ordered states are prefixed with STEP_.
 * Error states are terminal until the user corrects the action.
 */
enum class TaskState {
    // ── Lifecycle states ───────────────────────────────────────────────────
    IDLE,               // App opened, no task started
    TASK_DETECTED,      // Task board / components visible in frame

    // ── Task steps (electrical wiring MVP) ────────────────────────────────
    STEP_1_IDENTIFY,    // User identifies the correct component
    STEP_2_PICK_WIRE,   // User picks up the wire
    STEP_3_CONNECT_L,   // Wire connected to L terminal
    STEP_4_CONNECT_N,   // Wire connected to N terminal
    STEP_5_CONNECT_E,   // Earth wire connected
    STEP_6_VERIFY,      // All connections present, verifying

    // ── Terminal success ───────────────────────────────────────────────────
    COMPLETED,          // Task fully correct and confirmed

    // ── Error states (recovery possible) ──────────────────────────────────
    WRONG_CONNECTION,   // Wire connected to wrong terminal
    WRONG_COMPONENT,    // Wrong component selected
    OUT_OF_ORDER,       // Steps performed in wrong sequence
    MISSING_COMPONENT,  // Required component not visible

    // ── Perception states (not errors, but action required) ───────────────
    LOW_CONFIDENCE,     // Model cannot make a confident determination
    OCCLUDED,           // Task area blocked / hidden
    POOR_FRAMING,       // Camera too far / too close / angled
    UNKNOWN_STATE,      // Observed state cannot be mapped to any known state
}

/**
 * Describes a single step in the task sequence.
 *
 * @param id             Unique step identifier
 * @param title          Short human-readable step name
 * @param instruction    What the user should do
 * @param requiredObjects Visual objects that must be visible to validate this step
 * @param requiredRelationships Spatial / logical relationships between detected objects
 * @param successState   TaskState to transition to on success
 * @param errorStates    TaskStates that can be reached from this step on failure
 * @param timeoutMs      Max ms to wait before flagging stall (0 = no timeout)
 * @param minConfidence  Minimum model confidence to act on (0.0–1.0)
 * @param debounceFrames Consecutive frames at threshold before state transition
 */
@Serializable
data class TaskStep(
    val id: String,
    val title: String,
    val instruction: String,
    val requiredObjects: List<String>,
    val requiredRelationships: List<String> = emptyList(),
    val successState: String,
    val errorStates: List<String> = emptyList(),
    val timeoutMs: Long = 30_000L,
    val minConfidence: Float = 0.65f,
    val debounceFrames: Int = 5,
)

/**
 * Complete task definition.
 * A SkillDefinition encapsulates everything the engine needs to validate one skill.
 *
 * Adding a new skill = adding a new SkillDefinition. No engine code changes needed.
 */
@Serializable
data class SkillDefinition(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val safetyNote: String,
    val estimatedDurationMin: Int,
    val requiredEquipment: List<String>,
    val steps: List<TaskStep>,
    val scoringWeights: ScoringWeights = ScoringWeights(),
)

@Serializable
data class ScoringWeights(
    val completionWeight: Float    = 0.40f,
    val accuracyWeight: Float      = 0.35f,
    val sequenceWeight: Float      = 0.15f,
    val speedWeight: Float         = 0.10f,
)

/**
 * Observed frame result from the vision pipeline.
 * This is the contract between the ML layer and the task engine.
 */
data class FrameObservation(
    val detectedObjects: List<DetectedObject>,
    val relationships: List<SpatialRelationship>,
    val handLandmarks: List<HandLandmark>?,
    val frameTimestamp: Long,
    val confidence: Float,
    val frameQuality: FrameQuality,
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val trackingId: Int? = null,
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width  get() = right - left
    val height get() = bottom - top
    val centerX get() = left + width / 2f
    val centerY get() = top + height / 2f
}

data class SpatialRelationship(
    val subject: String,
    val relation: String,   // e.g. "connected_to", "near", "touching"
    val target: String,
    val confidence: Float,
)

data class HandLandmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val landmarkType: Int,
)

enum class FrameQuality {
    GOOD,       // Well-lit, stable, task area clearly visible
    LOW_LIGHT,  // Underexposed
    BLURRY,     // Motion blur / out of focus
    OCCLUDED,   // Task area partially hidden
    FRAMING_BAD,// Camera not aimed correctly
}

/**
 * Result of a single validation pass by the TaskEngine.
 */
data class ValidationResult(
    val previousState: TaskState,
    val newState: TaskState,
    val stateChanged: Boolean,
    val feedback: FeedbackEvent?,
    val stepIndex: Int,
    val totalSteps: Int,
    val confidence: Float,
)

/**
 * A feedback event to be presented to the user.
 */
data class FeedbackEvent(
    val type: FeedbackType,
    val title: String,
    val message: String,
    val hapticPattern: HapticPattern,
    val isCorrection: Boolean = false,
)

enum class FeedbackType {
    CORRECT,    // Step verified successfully
    ERROR,      // Mistake detected
    WARNING,    // Caution / uncertain
    INFO,       // Informational prompt
    COMPLETION, // Full task done
}

enum class HapticPattern {
    NONE,
    TICK,        // Subtle confirmation
    SUCCESS,     // Positive double-tap
    ERROR,       // Warning buzz
    COMPLETION,  // Celebration pattern
}
