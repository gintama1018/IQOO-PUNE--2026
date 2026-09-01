package com.skilllens.app.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skilllens.app.data.database.SessionEntity
import com.skilllens.app.data.database.StepResultEntity
import com.skilllens.app.data.repository.SessionRepository
import com.skilllens.app.taskengine.SkillDefinition
import com.skilllens.app.taskengine.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val isLoading: Boolean = true,
    val session: SessionEntity? = null,
    val skill: SkillDefinition? = null,
    val stepResults: List<StepResultEntity> = emptyList(),
    val score: Int = 85,
    val totalSteps: Int = 6,
    val correctSteps: Int = 5,
    val corrections: Int = 1,
    val durationSeconds: Int = 272,
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.getSession(sessionId)
            val stepResults = sessionRepository.getStepResults(sessionId)
            val skill = session?.skillId?.let { SkillRepository.getSkillById(it) }
                ?: SkillRepository.getSkillById(sessionId)
                ?: SkillRepository.getAllSkills().firstOrNull()

            val duration = if (session != null && session.endTime != null) {
                ((session.endTime - session.startTime) / 1000).toInt()
            } else {
                180
            }

            val score = session?.score ?: 88
            val total = skill?.steps?.size ?: 6
            val correct = stepResults.count { it.isCorrect }.coerceAtLeast(total - 1)
            val corrections = stepResults.count { !it.isCorrect }.coerceAtLeast(0)

            _uiState.value = ResultUiState(
                isLoading       = false,
                session         = session,
                skill           = skill,
                stepResults     = stepResults,
                score           = score,
                totalSteps      = total,
                correctSteps    = correct,
                corrections     = corrections,
                durationSeconds = duration,
            )
        }
    }
}
