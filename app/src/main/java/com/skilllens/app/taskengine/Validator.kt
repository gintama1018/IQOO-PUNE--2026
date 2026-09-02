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
        for (errorPattern in step.errorStates) {
            when {
                // ── WRONG CONNECTION ────────────────────────────────────────────
                // Wire is physically connected to a terminal that this step doesn't require.
                errorPattern.contains("wrong_connection") -> {
                    val allTerminals = listOf("l_terminal", "n_terminal", "e_terminal", "l1_terminal", "l2_terminal", "l3_terminal", "l1", "l2", "l3")
                    val wrongConnections = relationships.filter { rel ->
                        rel.subject.lowercase().contains("wire") &&
                                rel.relation == "connected_to" &&
                                allTerminals.any { t -> rel.target.lowercase().contains(t) } &&
                                !step.requiredRelationships.any { req ->
                                    req.lowercase() == "${rel.subject}_${rel.relation}_${rel.target}".lowercase() ||
                                            (req.lowercase().contains(rel.subject.lowercase()) && req.lowercase().contains(rel.target.lowercase()))
                                }
                    }
                    if (wrongConnections.isNotEmpty()) {
                        val observedTarget = wrongConnections.first().target.replace("_", " ").uppercase()
                        return ValidatorResult(
                            proposedState = TaskState.WRONG_CONNECTION,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Wrong Connection",
                                message = "Wire is connected to $observedTarget. Check the expected terminal.",
                                hapticPattern = HapticPattern.ERROR,
                                isCorrection  = true,
                            ),
                            matchedObjects = emptyList(),
                        )
                    }
                }

                // ── WRONG COMPONENT ─────────────────────────────────────────────
                // A known physical object is detected that is not part of this step.
                // Previously dead code: old filter used label.contains("component") which
                // never matched any actual label (board/wire/terminal/hand). Fixed to use
                // the actual label vocabulary.
                errorPattern.contains("wrong_component") -> {
                    val knownPhysicalLabels = setOf(
                        "red_wire", "black_wire", "earth_wire",
                        "l_terminal", "n_terminal", "e_terminal",
                        "hand", "board", "terminal_block",
                    )
                    val unexpectedComponents = detectedLabels.filter { label ->
                        knownPhysicalLabels.any { known -> label.contains(known) } &&
                                step.requiredObjects.none { req -> label.contains(req.lowercase()) }
                    }
                    if (unexpectedComponents.isNotEmpty()) {
                        return ValidatorResult(
                            proposedState = TaskState.WRONG_COMPONENT,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Not the Right Item",
                                message = "That's not needed for this step. " +
                                        "Focus on: ${step.requiredObjects.joinToString(", ")}.",
                                hapticPattern = HapticPattern.ERROR,
                                isCorrection  = true,
                            ),
                            matchedObjects = emptyList(),
                        )
                    }
                }

                // ── OUT OF ORDER ─────────────────────────────────────────────────
                // Hand is holding a wire type that is NOT expected for the current step.
                // Gated on the holding *relationship* (not label presence), so all-wires-
                // visible-in-frame doesn't trigger false positives.
                errorPattern.contains("out_of_order") -> {
                    val heldWires = relationships
                        .filter { it.subject == "hand" && it.relation == "holding" }
                        .map { it.target.lowercase() }

                    val wrongHeldWires = heldWires.filter { wire ->
                        step.requiredObjects.none { req -> wire.contains(req.lowercase()) }
                    }
                    if (wrongHeldWires.isNotEmpty()) {
                        val wireDisplay = wrongHeldWires.first().replace("_", " ")
                        return ValidatorResult(
                            proposedState = TaskState.OUT_OF_ORDER,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Wrong Wire",
                                message = "You're holding the $wireDisplay. " +
                                        "Put it down and pick up the correct wire for this step.",
                                hapticPattern = HapticPattern.ERROR,
                                isCorrection  = true,
                            ),
                            matchedObjects = emptyList(),
                        )
                    }
                }

                // ── MISSING COMPONENT ────────────────────────────────────────────
                // A required object for this step is absent from the frame entirely.
                // Used in final verification (step 6) where all wires must be visible.
                errorPattern.contains("missing_component") -> {
                    val missingItems = step.requiredObjects.filter { required ->
                        detectedLabels.none { it.contains(required.lowercase()) }
                    }
                    if (missingItems.isNotEmpty()) {
                        return ValidatorResult(
                            proposedState = TaskState.MISSING_COMPONENT,
                            feedback = FeedbackEvent(
                                type    = FeedbackType.ERROR,
                                title   = "⚠ Missing Wire",
                                message = "Cannot verify: ${missingItems.joinToString(", ")} not visible in frame.",
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
