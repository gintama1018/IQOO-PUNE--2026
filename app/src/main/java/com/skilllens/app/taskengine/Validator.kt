package com.skilllens.app.taskengine

import timber.log.Timber
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Validator — deterministic rule evaluation
//
// Takes a TaskStep definition and a FrameObservation,
// returns a proposed TaskState transition and an optional FeedbackEvent.
//
// All rules here are explicit and auditable.
// No generative model decides task correctness.
// ─────────────────────────────────────────────────────────────────────────────

data class ValidatorResult(
    val proposedState: TaskState,
    val feedback: FeedbackEvent?,
    val matchedObjects: List<String>,
)

class Validator @Inject constructor() {

    /**
     * Evaluate a step's validation rules against the current observation.
     *
     * Priority order:
     * 1. Error conditions (wrong connection, wrong component, out of order)
     * 2. Success condition
     * 3. In-progress / awaiting
     */
    fun validate(
        step: TaskStep,
        observation: FrameObservation,
        currentState: TaskState,
    ): ValidatorResult {
        val detectedLabels = observation.detectedObjects
            .filter { it.confidence >= step.minConfidence }
            .map { it.label.lowercase() }

        val detectedRelationships = observation.relationships
            .filter { it.confidence >= step.minConfidence }

        Timber.v("Validator: Step=${step.id} detected=$detectedLabels")

        // ── Check for known error conditions ─────────────────────────────────
        val errorResult = checkErrorConditions(step, detectedLabels, detectedRelationships)
        if (errorResult != null) return errorResult

        // ── Check success condition ───────────────────────────────────────────
        val allRequiredPresent = step.requiredObjects.all { required ->
            detectedLabels.any { it.contains(required.lowercase()) }
        }
        val allRelationshipsPresent = step.requiredRelationships.all { requiredRel ->
            detectedRelationships.any { rel ->
                "${rel.subject}_${rel.relation}_${rel.target}".lowercase()
                    .contains(requiredRel.lowercase())
            }
        }

        return if (allRequiredPresent && allRelationshipsPresent) {
            val nextState = try {
                TaskState.valueOf(step.successState)
            } catch (e: Exception) {
                TaskState.COMPLETED
            }

            ValidatorResult(
                proposedState  = nextState,
                feedback = FeedbackEvent(
                    type          = if (nextState == TaskState.COMPLETED) FeedbackType.COMPLETION else FeedbackType.CORRECT,
                    title         = "✓ ${step.title}",
                    message       = if (nextState == TaskState.COMPLETED) "All connections verified! Task complete." else "Step verified. Continue to next.",
                    hapticPattern = if (nextState == TaskState.COMPLETED) HapticPattern.COMPLETION else HapticPattern.SUCCESS,
                ),
                matchedObjects = step.requiredObjects,
            )
        } else {
            // Partial or missing — stay in current step, give nudge
            val missing = step.requiredObjects.filter { required ->
                !detectedLabels.any { it.contains(required.lowercase()) }
            }
            ValidatorResult(
                proposedState = currentState,
                feedback = FeedbackEvent(
                    type    = FeedbackType.INFO,
                    title   = step.title,
                    message = if (missing.isNotEmpty())
                        "Make sure ${missing.joinToString(", ")} is visible."
                    else step.instruction,
                    hapticPattern = HapticPattern.NONE,
                ),
                matchedObjects = emptyList(),
            )
        }
    }

    private fun checkErrorConditions(
        step: TaskStep,
        detectedLabels: List<String>,
        relationships: List<SpatialRelationship>,
    ): ValidatorResult? {
        // Check for wrong-terminal connections
        for (errorPattern in step.errorStates) {
            when {
                errorPattern.contains("wrong_connection") -> {
                    val wrongTerminals = listOf("l1", "l2", "l3").filter { terminal ->
                        relationships.any { rel ->
                            rel.subject.lowercase().contains("wire") &&
                                    rel.relation == "connected_to" &&
                                    rel.target.lowercase().contains(terminal) &&
                                    !step.requiredRelationships.any { req ->
                                        req.lowercase().contains(terminal)
                                    }
                        }
                    }
                    if (wrongTerminals.isNotEmpty()) {
                        return ValidatorResult(
                            proposedState = TaskState.WRONG_CONNECTION,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Wrong Connection",
                                message = "Wire is connected to ${wrongTerminals.joinToString()}. " +
                                        "Check the expected terminal.",
                                hapticPattern = HapticPattern.ERROR,
                                isCorrection  = true,
                            ),
                            matchedObjects = emptyList(),
                        )
                    }
                }

                errorPattern.contains("wrong_component") -> {
                    val unexpectedComponents = detectedLabels.filter { label ->
                        label.contains("component") &&
                                step.requiredObjects.none { req -> label.contains(req.lowercase()) }
                    }
                    if (unexpectedComponents.isNotEmpty()) {
                        return ValidatorResult(
                            proposedState = TaskState.WRONG_COMPONENT,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Wrong Component",
                                message = "That is not the correct component for this step.",
                                hapticPattern = HapticPattern.ERROR,
                                isCorrection  = true,
                            ),
                            matchedObjects = emptyList(),
                        )
                    }
                }
            }
        }
        return null
    }
}
