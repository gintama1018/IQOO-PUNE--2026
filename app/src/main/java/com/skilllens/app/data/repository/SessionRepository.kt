package com.skilllens.app.data.repository

import com.skilllens.app.data.database.SessionDao
import com.skilllens.app.data.database.SessionEntity
import com.skilllens.app.data.database.StepResultEntity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for practice session data.
 * Abstracts Room database access from the rest of the app.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    suspend fun startSession(skillId: String): String {
        val id = UUID.randomUUID().toString()
        sessionDao.insertSession(
            SessionEntity(
                id        = id,
                skillId   = skillId,
                startTime = System.currentTimeMillis(),
            )
        )
        Timber.d("SessionRepository: Started session $id for skill $skillId")
        return id
    }

    suspend fun completeSession(
        sessionId: String,
        score: Int,
        stepResults: List<StepResultEntity>,
    ) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.updateSession(
            session.copy(
                endTime     = System.currentTimeMillis(),
                score       = score,
                isCompleted = true,
            )
        )
        stepResults.forEach { sessionDao.insertStepResult(it) }
        Timber.d("SessionRepository: Completed session $sessionId with score $score")
    }

    suspend fun getAllSessions(): List<SessionEntity> =
        sessionDao.getAllSessions()

    suspend fun getSession(sessionId: String): SessionEntity? =
        sessionDao.getSession(sessionId)

    suspend fun getStepResults(sessionId: String): List<StepResultEntity> =
        sessionDao.getStepResults(sessionId)
}
