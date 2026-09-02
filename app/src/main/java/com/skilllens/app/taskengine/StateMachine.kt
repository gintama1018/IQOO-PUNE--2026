package com.skilllens.app.taskengine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Deterministic Task State Machine
//
// Thread-safe finite state machine executing deterministic validation rules,
// debounce verification, and proactive step timeouts.
// ─────────────────────────────────────────────────────────────────────────────

class StateMachine @Inject constructor(
    private val validator: Validator,
) {

    private val stateLock = Any()

    private var skill: SkillDefinition? = null
    private var currentStepIndex: Int   = 0
    private var debounceCount: Int      = 0
    private var pendingState: TaskState = TaskState.IDLE
    private var lastTransitionTime: Long = 0L
    private var stepStartTime: Long     = 0L
    private var timeoutNudgeEmitted: Boolean = false

    private val _taskState = MutableStateFlow(TaskState.IDLE)
    val taskState: StateFlow<TaskState> = _taskState.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    fun loadSkill(skillDefinition: SkillDefinition) {
        synchronized(stateLock) {
            val now = System.currentTimeMillis()
            skill                   = skillDefinition
            currentStepIndex        = 0
            debounceCount           = 0
            pendingState            = TaskState.IDLE
            stepStartTime           = now
            timeoutNudgeEmitted     = false
            _taskState.value        = TaskState.STEP_1_IDENTIFY
            _validationResult.value = null
            Timber.d("StateMachine: Skill loaded — ${skillDefinition.name}")
        }
    }

    fun reset() {
        synchronized(stateLock) {
            val now = System.currentTimeMillis()
            currentStepIndex        = 0
            debounceCount           = 0
            pendingState            = TaskState.IDLE
            stepStartTime           = now
            timeoutNudgeEmitted     = false
            _taskState.value        = TaskState.STEP_1_IDENTIFY
            _validationResult.value = null
            Timber.d("StateMachine: Reset")
        }
    }

    /**
     * Feed a frame observation from the vision pipeline into the state machine.
     * Serialized under stateLock to prevent race conditions with UI resets or pauses.
     */
    fun processObservation(observation: FrameObservation) {
        synchronized(stateLock) {
            val currentState = _taskState.value
            val skill        = skill ?: return
            val now          = System.currentTimeMillis()

            // 1. Check frame quality & environmental constraints
            val personObs = observation.personObservation
            when {
                personObs != null && personObs.personCount == 0 && currentState != TaskState.COMPLETED -> {
                    emitPerceptionFeedback(
                        TaskState.NO_USER_DETECTED,
                        currentState,
                        skill,
                        "No Trainee Visible",
                        "Please step in front of the camera to begin practice."
                    )
                    return
                }
                personObs != null && !personObs.isParticipantIsolated && currentState != TaskState.COMPLETED -> {
                    emitPerceptionFeedback(
                        TaskState.MULTIPLE_USERS_DETECTED,
                        currentState,
                        skill,
                        "Multiple People in Frame",
                        "Only one active trainee should be visible for accurate evaluation."
                    )
                    return
                }
                observation.boardROI == null && currentState != TaskState.COMPLETED -> {
                    emitPerceptionFeedback(
                        TaskState.TASK_OUT_OF_FRAME,
                        currentState,
                        skill,
                        "Board Out of View",
                        "Point the camera at the training board to continue."
                    )
                    return
                }
            }

            val qualityState = resolveQualityState(observation.frameQuality)
            if (qualityState != null) {
                emitQualityFeedback(qualityState, currentState, skill)
                return
            }

            // 2. Guard: don't re-validate terminal states
            if (currentState == TaskState.COMPLETED) return

            // 3. Determine current step
            val steps = skill.steps
            if (currentStepIndex >= steps.size) {
                transitionTo(TaskState.COMPLETED, observation.confidence, skill)
                return
            }
            val step = steps[currentStepIndex]

            // 4. Step Timeout & Inactivity Nudge
            if (step.timeoutMs > 0 && (now - stepStartTime) > step.timeoutMs && !timeoutNudgeEmitted) {
                timeoutNudgeEmitted = true
                _validationResult.value = ValidationResult(
                    previousState = currentState,
                    newState      = currentState,
                    stateChanged  = false,
                    feedback = FeedbackEvent(
                        type    = FeedbackType.INFO,
                        title   = "💡 Hint: ${step.title}",
                        message = step.instruction,
                        hapticPattern = HapticPattern.TICK,
                    ),
                    stepIndex  = currentStepIndex,
                    totalSteps = steps.size,
                    confidence = observation.confidence,
                )
                Timber.d("StateMachine: Step ${step.id} timeout nudge emitted")
            }

            // 5. Low confidence guard
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

            // 6. Validate observation against current step
            val result = validator.validate(step, observation, currentState)

            // 7. Debounce: require N consecutive frames before committing
            if (result.proposedState == pendingState) {
                debounceCount++
            } else {
                pendingState  = result.proposedState
                debounceCount = 1
            }

            if (debounceCount < step.debounceFrames) {
                return
            }

            // 8. Commit state transition
            debounceCount = 0

            val isErrorState = result.proposedState in listOf(
                TaskState.WRONG_CONNECTION,
                TaskState.WRONG_COMPONENT,
                TaskState.OUT_OF_ORDER,
                TaskState.MISSING_COMPONENT,
            )

            if (isErrorState) {
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
                val nextStepIndex = currentStepIndex + 1
                stepStartTime = now
                timeoutNudgeEmitted = false

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
                        stepIndex     = nextStepIndex,
                        totalSteps    = steps.size,
                        confidence    = observation.confidence,
                    )
                    Timber.d("StateMachine: Advanced to step $currentStepIndex: ${steps[currentStepIndex].title}")
                }
            } else {
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
        }
    }

    private fun resolveQualityState(quality: FrameQuality): TaskState? {
        return when (quality) {
            FrameQuality.OCCLUDED    -> TaskState.OCCLUDED
            FrameQuality.FRAMING_BAD -> TaskState.POOR_FRAMING
            FrameQuality.LOW_LIGHT   -> TaskState.LOW_CONFIDENCE
            FrameQuality.BLURRY      -> TaskState.LOW_CONFIDENCE
            FrameQuality.GOOD        -> null
        }
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
        emitPerceptionFeedback(qualityState, previousState, skill, "Adjust View", message)
    }

    private fun emitPerceptionFeedback(
        state: TaskState,
        previousState: TaskState,
        skill: SkillDefinition,
        title: String,
        message: String,
    ) {
        _taskState.value = state
        _validationResult.value = ValidationResult(
            previousState = previousState,
            newState      = state,
            stateChanged  = previousState != state,
            feedback = FeedbackEvent(
                type    = FeedbackType.WARNING,
                title   = title,
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

    fun getCurrentStep(): TaskStep? = synchronized(stateLock) {
        skill?.steps?.getOrNull(currentStepIndex)
    }

    fun getStepCount(): Int = synchronized(stateLock) {
        skill?.steps?.size ?: 0
    }
}
