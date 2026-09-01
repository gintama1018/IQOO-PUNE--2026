package com.skilllens.app.data.database

import androidx.room.*

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val skillId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val score: Int? = null,
    val isCompleted: Boolean = false,
    val totalSteps: Int = 0,
    val correctSteps: Int = 0,
    val corrections: Int = 0,
)

@Entity(
    tableName = "step_results",
    foreignKeys = [ForeignKey(
        entity       = SessionEntity::class,
        parentColumns = ["id"],
        childColumns  = ["sessionId"],
        onDelete     = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class StepResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val stepId: String,
    val expectedState: String,
    val observedState: String,
    val isCorrect: Boolean,
    val confidence: Float,
    val timestamp: Long,
    val feedback: String? = null,
)

class Converters {
    // Add type converters here as needed
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepResult(result: StepResultEntity)

    @Query("SELECT * FROM step_results WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getStepResults(sessionId: String): List<StepResultEntity>

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}
