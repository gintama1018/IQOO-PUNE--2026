package com.skilllens.app.taskengine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Deterministic Task State Machine
//
// This is intentionally NOT an LLM call.
// Correctness of a physical task is deterministic: either the wire is in the
// right terminal or it isn't. The ML layer provides observations; this layer
// makes verifiable decisions.
// ─────────────────────────────────────────────────────────────────────────────

class StateMachine @Inject constructor(
    private val validator: Validator,
) {

    // ── Internal state ────────────────────────────────────────────────────────
    private var skill: SkillDefinition? = null
    private var currentStepIndex: Int   = 0
    private var debounceCount: Int      = 0

    // Tracks consecutive frames agreeing on the same observation
    private var pendingState: TaskState = TaskState.IDLE
    private var lastTransitionTime: Long = 0L

    // ── Exposed state ─────────────────────────────────────────────────────────
    private val _taskState = MutableStateFlow(TaskState.IDLE)
    val taskState: StateFlow<TaskState> = _taskState.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun loadSkill(skillDefinition: SkillDefinition) {
        skill             = skillDefinition
        currentStepIndex  = 0
        debounceCount     = 0
        pendingState      = TaskState.IDLE
        _taskState.value  = TaskState.STEP_1_IDENTIFY
        _validationResult.value = null
        Timber.d("StateMachine: Skill loaded — ${skillDefinition.name}")
    }

    fun reset() {
        currentStepIndex        = 0
        debounceCount           = 0
        pendingState            = TaskState.IDLE
        _taskState.value        = TaskState.STEP_1_IDENTIFY
        _validationResult.value = null
        Timber.d("StateMachine: Reset")
    }

    // ── Core processing ───────────────────────────────────────────────────────

    /**
     * Feed a frame observation from the vision pipeline into the state machine.
     * Called on every sampled frame (not every camera frame).
     */
    fun processObservation(observation: FrameObservation) {
        val currentState = _taskState.value
        val skill        = skill ?: return

        // ── 1. Check frame quality ──────────────────────────────────────────
        val qualityState = resolveQualityState(observation.frameQuality)
        if (qualityState != null) {
            emitQualityFeedback(qualityState, currentState, skill)
            return
        }

        // ── 2. Guard: don't re-validate terminal states ─────────────────────
        if (currentState == TaskState.COMPLETED) return

        // ── 3. Determine current step ────────────────────────────────────────
        val steps = skill.steps
        if (currentStepIndex >= steps.size) {
            transitionTo(TaskState.COMPLETED, observation.confidence, skill)
            return
        }
        val step = steps[currentStepIndex]

        // ── 4. Low confidence guard ──────────────────────────────────────────
        if (observation.confidence < step.minConfidence) {
            if (_taskState.value != TaskState.LOW_CONFIDENCE) {
                _taskState.value = TaskState.LOW_CONFIDENCE
                _validationResult.value = ValidationResult(
                    previousState = currentState,
                    newState      = TaskState.LOW_CONFIDENCE,
                    stateChanged  = true,
                    feedback = FeedbackEvent(
                        type    = FeedbackType.WARNING,
                        title   = "Can't see clearly",
                        message = "Move the phone closer and hold steady.",
                        hapticPattern = HapticPattern.NONE,
                    ),
                    stepIndex  = currentStepIndex,
                    totalSteps = steps.size,
                    confidence = observation.confidence,
                )
            }
            return
        }

        // ── 5. Validate observation against current step ──────────────────────
        val result = validator.validate(step, observation, currentState)

        // ── 6. Debounce: require N consecutive frames before committing ────────
        if (result.proposedState == pendingState) {
            debounceCount++
        } else {
            pendingState  = result.proposedState
            debounceCount = 1
        }

        if (debounceCount < step.debounceFrames) {
            // Not enough frames yet — keep current state
            return
        }

        // ── 7. Commit state transition ────────────────────────────────────────
        debounceCount = 0
        val now = System.currentTimeMillis()

        val isErrorState = result.proposedState in listOf(
            TaskState.WRONG_CONNECTION,
            TaskState.WRONG_COMPONENT,
            TaskState.OUT_OF_ORDER,
            TaskState.MISSING_COMPONENT,
        )

        if (isErrorState) {
            // Error — stay on current step, show correction
            _taskState.value = result.proposedState
            _validationResult.value = ValidationResult(
                previousState = currentState,
                newState      = result.proposedState,
                stateChanged  = currentState != result.proposedState,
                feedback      = result.feedback,
                stepIndex     = currentStepIndex,
                totalSteps    = steps.size,
                confidence    = observation.confidence,
            )
        } else if (result.proposedState != currentState && result.proposedState != TaskState.LOW_CONFIDENCE) {
            // Step validated successfully — advance step
            val nextStepIndex = currentStepIndex + 1
            if (nextStepIndex >= steps.size) {
                currentStepIndex = nextStepIndex
                transitionTo(TaskState.COMPLETED, observation.confidence, skill)
            } else {
                currentStepIndex = nextStepIndex
                _taskState.value = result.proposedState
                _validationResult.value = ValidationResult(
                    previousState = currentState,
                    newState      = result.proposedState,
                    stateChanged  = true,
                    feedback      = result.feedback,
                    stepIndex     = currentStepIndex,
                    totalSteps    = steps.size,
                    confidence    = observation.confidence,
                )
                Timber.d("StateMachine: Advanced to step $currentStepIndex: ${steps[currentStepIndex].title}")
            }
        } else {
            // Keep in current state with informational update
            _validationResult.value = ValidationResult(
                previousState = currentState,
                newState      = currentState,
                stateChanged  = false,
                feedback      = result.feedback,
                stepIndex     = currentStepIndex,
                totalSteps    = steps.size,
                confidence    = observation.confidence,
            )
        }

        lastTransitionTime = now
        Timber.d("StateMachine: ${currentState.name} → ${_taskState.value.name} (step=$currentStepIndex/${steps.size}, conf=${observation.confidence})")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun resolveQualityState(quality: FrameQuality): TaskState? = when (quality) {
        FrameQuality.GOOD        -> null
        FrameQuality.LOW_LIGHT   -> TaskState.POOR_FRAMING
        FrameQuality.BLURRY      -> TaskState.POOR_FRAMING
        FrameQuality.OCCLUDED    -> TaskState.OCCLUDED
        FrameQuality.FRAMING_BAD -> TaskState.POOR_FRAMING
    }

    private fun emitQualityFeedback(
        qualityState: TaskState,
        previousState: TaskState,
        skill: SkillDefinition,
    ) {
        val message = when (qualityState) {
            TaskState.POOR_FRAMING -> "Move the phone to clearly show the task board."
            TaskState.OCCLUDED     -> "Something is blocking the view. Move hands aside."
            else                   -> "Adjust the camera position."
        }
        _taskState.value = qualityState
        _validationResult.value = ValidationResult(
            previousState = previousState,
            newState      = qualityState,
            stateChanged  = previousState != qualityState,
            feedback = FeedbackEvent(
                type    = FeedbackType.INFO,
                title   = "Adjust View",
                message = message,
                hapticPattern = HapticPattern.NONE,
            ),
            stepIndex  = currentStepIndex,
            totalSteps = skill.steps.size,
            confidence = 0f,
        )
    }

    private fun transitionTo(state: TaskState, confidence: Float, skill: SkillDefinition) {
        val previous     = _taskState.value
        _taskState.value = state

        val feedback = when (state) {
            TaskState.COMPLETED -> FeedbackEvent(
                type          = FeedbackType.COMPLETION,
                title         = "Task Complete",
                message       = "All steps verified. Excellent work!",
                hapticPattern = HapticPattern.COMPLETION,
            )
            else -> null
        }

        _validationResult.value = ValidationResult(
            previousState = previous,
            newState      = state,
            stateChanged  = true,
            feedback      = feedback,
            stepIndex     = currentStepIndex.coerceAtMost(skill.steps.size - 1),
            totalSteps    = skill.steps.size,
            confidence    = confidence,
        )
    }

    fun getCurrentStep(): TaskStep? =
        skill?.steps?.getOrNull(currentStepIndex)

    fun getStepCount(): Int = skill?.steps?.size ?: 0
}
