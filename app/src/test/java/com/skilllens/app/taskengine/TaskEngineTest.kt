package com.skilllens.app.taskengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskEngineTest {

    private lateinit var validator: Validator
    private lateinit var stateMachine: StateMachine
    private lateinit var skill: SkillDefinition

    @Before
    fun setUp() {
        validator = Validator()
        stateMachine = StateMachine(validator)
        val loadedSkill = SkillRepository.getSkillById("electrical_switch_wiring")
        assertNotNull("Skill definition should exist", loadedSkill)
        skill = loadedSkill!!
        stateMachine.loadSkill(skill)
    }

    @Test
    fun testStep1_IdentifyComponents_Success() {
        val step = skill.steps[0]
        val observation = FrameObservation(
            detectedObjects = listOf(
                DetectedObject("board", 0.90f, BoundingBox(0.1f, 0.2f, 0.9f, 0.9f)),
                DetectedObject("terminal_block", 0.88f, BoundingBox(0.2f, 0.4f, 0.8f, 0.8f)),
                DetectedObject("l_terminal", 0.85f, BoundingBox(0.25f, 0.45f, 0.35f, 0.55f)),
            ),
            relationships = emptyList(),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.88f,
            frameQuality = FrameQuality.GOOD,
            boardROI = BoundingBox(0.1f, 0.2f, 0.9f, 0.9f),
            personObservation = PersonObservation(1, BoundingBox(0f, 0f, 1f, 1f), true),
        )

        val result = validator.validate(step, observation, TaskState.STEP_1_IDENTIFY)
        assertEquals(TaskState.STEP_2_PICK_WIRE, result.proposedState)
        assertEquals(FeedbackType.CORRECT, result.feedback?.type)
    }

    @Test
    fun testStep2_PickWire_GraspSuccess() {
        val step = skill.steps[1]
        val observation = FrameObservation(
            detectedObjects = listOf(
                DetectedObject("red_wire", 0.85f, BoundingBox(0.3f, 0.3f, 0.6f, 0.7f)),
                DetectedObject("hand", 0.95f, BoundingBox(0.25f, 0.25f, 0.65f, 0.75f)),
            ),
            relationships = listOf(
                SpatialRelationship("hand", "holding", "red_wire", 0.90f)
            ),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.88f,
            frameQuality = FrameQuality.GOOD,
            boardROI = BoundingBox(0.1f, 0.2f, 0.9f, 0.9f),
            personObservation = PersonObservation(1, BoundingBox(0f, 0f, 1f, 1f), true),
        )

        val result = validator.validate(step, observation, TaskState.STEP_2_PICK_WIRE)
        assertEquals(TaskState.STEP_3_CONNECT_L, result.proposedState)
    }

    @Test
    fun testStep3_WrongConnection_CatchesMistake() {
        val step = skill.steps[2] // Expects red_wire connected to l_terminal
        val observation = FrameObservation(
            detectedObjects = listOf(
                DetectedObject("red_wire", 0.85f, BoundingBox(0.3f, 0.3f, 0.6f, 0.7f)),
                DetectedObject("l1_terminal", 0.85f, BoundingBox(0.2f, 0.6f, 0.3f, 0.7f)),
            ),
            relationships = listOf(
                SpatialRelationship("red_wire", "connected_to", "l1_terminal", 0.90f)
            ),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.88f,
            frameQuality = FrameQuality.GOOD,
            boardROI = BoundingBox(0.1f, 0.2f, 0.9f, 0.9f),
            personObservation = PersonObservation(1, BoundingBox(0f, 0f, 1f, 1f), true),
        )

        val result = validator.validate(step, observation, TaskState.STEP_3_CONNECT_L)
        assertEquals(TaskState.WRONG_CONNECTION, result.proposedState)
        assertEquals(FeedbackType.ERROR, result.feedback?.type)
        assertTrue(result.feedback?.message?.contains("L1") == true)
    }

    @Test
    fun testStep2_OutOfOrder_HoldingBlackWire() {
        val step = skill.steps[1] // Expects red_wire
        val observation = FrameObservation(
            detectedObjects = listOf(
                DetectedObject("black_wire", 0.85f, BoundingBox(0.3f, 0.3f, 0.6f, 0.7f)),
                DetectedObject("hand", 0.95f, BoundingBox(0.25f, 0.25f, 0.65f, 0.75f)),
            ),
            relationships = listOf(
                SpatialRelationship("hand", "holding", "black_wire", 0.90f)
            ),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.88f,
            frameQuality = FrameQuality.GOOD,
            boardROI = BoundingBox(0.1f, 0.2f, 0.9f, 0.9f),
            personObservation = PersonObservation(1, BoundingBox(0f, 0f, 1f, 1f), true),
        )

        val result = validator.validate(step, observation, TaskState.STEP_2_PICK_WIRE)
        assertEquals(TaskState.OUT_OF_ORDER, result.proposedState)
        assertEquals(FeedbackType.ERROR, result.feedback?.type)
    }

    @Test
    fun testBoardOutOfView_TriggersTaskOutOfFrame() {
        val observation = FrameObservation(
            detectedObjects = emptyList(),
            relationships = emptyList(),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.2f,
            frameQuality = FrameQuality.FRAMING_BAD,
            boardROI = null,
            personObservation = PersonObservation(1, BoundingBox(0f, 0f, 1f, 1f), true),
        )

        stateMachine.processObservation(observation)
        val result = stateMachine.validationResult.value
        assertNotNull(result)
        assertEquals(TaskState.TASK_OUT_OF_FRAME, result?.newState)
    }

    @Test
    fun testMultipleUsers_TriggersIsolationWarning() {
        val observation = FrameObservation(
            detectedObjects = emptyList(),
            relationships = emptyList(),
            handLandmarks = null,
            frameTimestamp = System.currentTimeMillis(),
            confidence = 0.85f,
            frameQuality = FrameQuality.GOOD,
            boardROI = BoundingBox(0.1f, 0.2f, 0.9f, 0.9f),
            personObservation = PersonObservation(2, BoundingBox(0f, 0f, 1f, 1f), false),
        )

        stateMachine.processObservation(observation)
        val result = stateMachine.validationResult.value
        assertNotNull(result)
        assertEquals(TaskState.MULTIPLE_USERS_DETECTED, result?.newState)
    }
}
