package com.skilllens.app.vision

import com.skilllens.app.taskengine.SpatialRelationship
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Geometric & Physical Interaction Analyzer.
 *
 * Evaluates:
 * 1. Hand-to-Wire grasp & holding relationships (pinch + proximity + motion).
 * 2. Wire-to-Terminal aperture insertion vs. nearness geometry ("near != connected").
 * 3. Temporal stabilization of physical connections.
 */
@Singleton
class InteractionAnalyzer @Inject constructor() {

    private val connectionHistory = mutableMapOf<String, Int>()

    fun evaluateInteractions(
        wireObservations: List<WireObservation>,
        terminalAnchors: List<TerminalAnchor>,
        handObservations: List<HandGraspObservation>,
        transformer: BoardCoordinateTransformer?,
    ): List<SpatialRelationship> {
        val relationships = mutableListOf<SpatialRelationship>()

        // 1. Hand-to-Wire Grasp Interactions
        for (hand in handObservations) {
            for (wire in wireObservations) {
                val distThumbTip = distance(hand.thumbTip.x, hand.thumbTip.y, wire.tipX, wire.tipY)
                val distIndexTip = distance(hand.indexTip.x, hand.indexTip.y, wire.tipX, wire.tipY)
                val distThumbRoot = distance(hand.thumbTip.x, hand.thumbTip.y, wire.rootX, wire.rootY)
                val distIndexRoot = distance(hand.indexTip.x, hand.indexTip.y, wire.rootX, wire.rootY)
                val distHandCenter = distance(hand.palmCenter.first, hand.palmCenter.second, wire.detectedObject.boundingBox.centerX, wire.detectedObject.boundingBox.centerY)

                val minTipDist = minOf(distThumbTip, distIndexTip, distThumbRoot, distIndexRoot)

                when {
                    // Holding: Pinch grasp active AND fingers near wire endpoint or body
                    hand.isPinching && (minTipDist < 0.12f || distHandCenter < 0.15f) -> {
                        val confidence = (0.85f + (0.12f - minTipDist).coerceAtLeast(0f)).coerceIn(0.80f, 0.98f)
                        relationships.add(
                            SpatialRelationship(
                                subject = "hand",
                                relation = "holding",
                                target = wire.label,
                                confidence = confidence,
                            )
                        )
                    }
                    // Touching: Proximity without active pinch
                    minTipDist < 0.09f || distHandCenter < 0.12f -> {
                        relationships.add(
                            SpatialRelationship(
                                subject = "hand",
                                relation = "touching",
                                target = wire.label,
                                confidence = 0.75f,
                            )
                        )
                    }
                }
            }
        }

        // 2. Wire-to-Terminal Connection & Insertion Interactions
        for (wire in wireObservations) {
            for (terminal in terminalAnchors) {
                val wireTipInAperture = wire.tipX in terminal.box.left..terminal.box.right &&
                        wire.tipY in terminal.box.top..terminal.box.bottom

                val distTipToCenter = distance(wire.tipX, wire.tipY, terminal.box.centerX, terminal.box.centerY)
                val distCenterToCenter = distance(
                    wire.detectedObject.boundingBox.centerX, wire.detectedObject.boundingBox.centerY,
                    terminal.box.centerX, terminal.box.centerY
                )

                val key = "${wire.label}_to_${terminal.id}"

                when {
                    // True insertion: Wire endpoint is contained directly within the terminal entry aperture
                    wireTipInAperture -> {
                        val historyCount = (connectionHistory[key] ?: 0) + 1
                        connectionHistory[key] = historyCount

                        // Require temporal stability (>= 2 frames in aperture) for high confidence
                        val confidence = if (historyCount >= 2) 0.92f else 0.78f

                        relationships.add(
                            SpatialRelationship(
                                subject = wire.label,
                                relation = "connected_to",
                                target = terminal.id,
                                confidence = confidence,
                            )
                        )
                    }
                    // Near / Approaching: Near terminal boundary but NOT inserted into aperture
                    distTipToCenter < 0.14f || distCenterToCenter < 0.15f -> {
                        connectionHistory[key] = 0 // Reset persistent connection count
                        relationships.add(
                            SpatialRelationship(
                                subject = wire.label,
                                relation = "near",
                                target = terminal.id,
                                confidence = 0.70f,
                            )
                        )
                    }
                    else -> {
                        connectionHistory[key] = 0
                    }
                }
            }
        }

        return relationships
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun reset() {
        connectionHistory.clear()
    }
}
