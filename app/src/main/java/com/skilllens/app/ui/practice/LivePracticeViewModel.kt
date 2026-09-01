package com.skilllens.app.ui.practice

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skilllens.app.camera.CameraController
import com.skilllens.app.camera.FrameAnalyzer
import com.skilllens.app.core.HapticFeedbackManager
import com.skilllens.app.data.database.StepResultEntity
import com.skilllens.app.data.repository.SessionRepository
import com.skilllens.app.taskengine.DetectedObject
import com.skilllens.app.taskengine.FeedbackEvent
import com.skilllens.app.taskengine.FrameObservation
import com.skilllens.app.taskengine.HapticPattern
import com.skilllens.app.taskengine.SkillDefinition
import com.skilllens.app.taskengine.SkillRepository
import com.skilllens.app.taskengine.StateMachine
import com.skilllens.app.taskengine.TaskState
import com.skilllens.app.taskengine.ValidationResult
import com.skilllens.app.vision.VisionEngine
import com.skilllens.app.vision.VisionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// LivePracticeViewModel
//
// Coordinates: CameraController → FrameAnalyzer → VisionEngine → StateMachine
// Exposes consolidated UI state for the LivePracticeScreen composable.
// ─────────────────────────────────────────────────────────────────────────────

data class PracticeUiState(
    val isLoading: Boolean                   = true,
    val skill: SkillDefinition?              = null,
    val currentState: TaskState              = TaskState.IDLE,
    val currentStepIndex: Int                = 0,
    val totalSteps: Int                      = 0,
    val feedback: FeedbackEvent?             = null,
    val detectedObjects: List<DetectedObject> = emptyList(),
    val sessionDurationSec: Int              = 0,
    val confidence: Float                    = 0f,
    val isPaused: Boolean                    = false,
    val isCompleted: Boolean                 = false,
    val error: String?                       = null,
    val visionMode: VisionMode               = VisionMode.CALIBRATED_BENCHMARK,
)

@HiltViewModel
class LivePracticeViewModel @Inject constructor(
    private val cameraController: CameraController,
    private val visionEngine: VisionEngine,
    private val stateMachine: StateMachine,
    private val sessionRepository: SessionRepository,
    private val hapticFeedbackManager: HapticFeedbackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null
    private var activeElapsedSeconds: Int = 0
    private var frameAnalyzer: FrameAnalyzer? = null

    private val recordedStepResults = mutableListOf<StepResultEntity>()
    private var errorCount = 0
    private var correctionCount = 0
    private var lastRecordedStepIndex = -1

    fun loadSkill(skillId: String) {
        val skill = SkillRepository.getSkillById(skillId)
        if (skill == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error     = "Skill '$skillId' not found.",
            )
            return
        }

        stateMachine.loadSkill(skill)
        _uiState.value = _uiState.value.copy(
            isLoading    = false,
            skill        = skill,
            totalSteps   = skill.steps.size,
            currentState = TaskState.STEP_1_IDENTIFY,
            visionMode   = visionEngine.visionMode.value,
        )

        // Observe vision mode
        viewModelScope.launch {
            visionEngine.visionMode.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(visionMode = mode)
            }
        }

        // Observe state machine output
        viewModelScope.launch {
            stateMachine.validationResult.collectLatest { result ->
                result ?: return@collectLatest
                onValidationResult(result)
            }
        }

        // Active session timer — only increments when running & not paused
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                if (!_uiState.value.isPaused && !_uiState.value.isCompleted && !_uiState.value.isLoading) {
                    activeElapsedSeconds++
                    _uiState.value = _uiState.value.copy(sessionDurationSec = activeElapsedSeconds)
                }
            }
        }

        // Start session record
        viewModelScope.launch {
            sessionId = sessionRepository.startSession(skillId)
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        visionEngine.initialize()

        frameAnalyzer?.shutdown()
        val analyzer = FrameAnalyzer(visionEngine) { observation ->
            onFrameObservation(observation)
        }
        frameAnalyzer = analyzer

        cameraController.startCamera(lifecycleOwner, previewView, analyzer)
        Timber.d("LivePracticeViewModel: Camera started")
    }

    private fun onFrameObservation(observation: FrameObservation) {
        if (_uiState.value.isPaused || _uiState.value.isCompleted) return

        stateMachine.processObservation(observation)

        _uiState.value = _uiState.value.copy(
            detectedObjects = observation.detectedObjects,
            confidence      = observation.confidence,
        )
    }

    private fun onValidationResult(result: ValidationResult) {
        val isCompleted = result.newState == TaskState.COMPLETED

        // Trigger haptic feedback if available
        result.feedback?.hapticPattern?.let { pattern ->
            if (pattern != HapticPattern.NONE) {
                hapticFeedbackManager.play(pattern)
            }
        }

        val isError = result.newState in listOf(
            TaskState.WRONG_CONNECTION,
            TaskState.WRONG_COMPONENT,
            TaskState.OUT_OF_ORDER,
            TaskState.MISSING_COMPONENT,
        )

        if (isError && result.stateChanged) {
            errorCount++
            correctionCount++
        }

        if (result.stateChanged && result.stepIndex != lastRecordedStepIndex && !isError) {
            lastRecordedStepIndex = result.stepIndex
            sessionId?.let { sid ->
                val currentStep = _uiState.value.skill?.steps?.getOrNull(result.stepIndex)
                recordedStepResults.add(
                    StepResultEntity(
                        sessionId     = sid,
                        stepId        = currentStep?.id ?: "step_${result.stepIndex}",
                        expectedState = currentStep?.successState ?: "COMPLETED",
                        observedState = result.newState.name,
                        isCorrect     = true,
                        confidence    = result.confidence,
                        timestamp     = System.currentTimeMillis(),
                        feedback      = result.feedback?.message,
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            currentState     = result.newState,
            currentStepIndex = result.stepIndex,
            feedback         = result.feedback,
            isCompleted      = isCompleted,
            confidence       = result.confidence,
        )

        if (isCompleted) {
            viewModelScope.launch {
                sessionId?.let { id ->
                    val calculatedScore = calculateScore()
                    sessionRepository.completeSession(
                        sessionId    = id,
                        score        = calculatedScore,
                        stepResults  = recordedStepResults.toList(),
                    )
                }
            }
        }
    }

    fun pause() {
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    fun resume() {
        _uiState.value = _uiState.value.copy(isPaused = false)
    }

    fun resetSession() {
        stateMachine.reset()
        activeElapsedSeconds = 0
        errorCount = 0
        correctionCount = 0
        lastRecordedStepIndex = -1
        recordedStepResults.clear()

        _uiState.value = _uiState.value.copy(
            currentState       = TaskState.STEP_1_IDENTIFY,
            currentStepIndex   = 0,
            feedback           = null,
            isCompleted        = false,
            error              = null,
            sessionDurationSec = 0,
        )
    }

    private fun calculateScore(): Int {
        val totalSteps = _uiState.value.totalSteps.coerceAtLeast(1)
        val baseScore = 100
        val errorPenalty = (errorCount * 8).coerceAtMost(40)
        val timePenalty = if (activeElapsedSeconds > 300) 10 else 0
        return (baseScore - errorPenalty - timePenalty).coerceIn(40, 100)
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.stopCamera()
        frameAnalyzer?.shutdown()
        visionEngine.release()
        Timber.d("LivePracticeViewModel: Cleared — camera and models released")
    }
}
