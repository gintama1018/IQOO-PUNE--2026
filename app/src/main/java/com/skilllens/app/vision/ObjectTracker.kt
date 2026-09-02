package com.skilllens.app.vision

import com.skilllens.app.taskengine.BoundingBox
import com.skilllens.app.taskengine.DetectedObject
import com.skilllens.app.taskengine.MotionState
import com.skilllens.app.taskengine.TrackedEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Multi-Frame Object & Kinematic Motion Tracker.
 *
 * Maintains temporal identity of observed objects, computes instantaneous velocities,
 * and categorizes motion states (STATIONARY, MOVING, APPROACHING, HELD, STABILIZING).
 */
@Singleton
class ObjectTracker @Inject constructor() {

    private data class TrackRecord(
        val id: Int,
        val label: String,
        var lastBox: BoundingBox,
        var lastTimestamp: Long,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var speed: Float = 0f,
        var stationaryFrames: Int = 0,
        var activeFrames: Int = 0,
    )

    private var nextTrackId = 1
    private val activeTracks = mutableMapOf<String, TrackRecord>()

    fun trackObjects(
        detectedObjects: List<DetectedObject>,
        handObservations: List<HandGraspObservation>,
        terminalAnchors: List<TerminalAnchor>,
        timestamp: Long,
    ): List<TrackedEntity> {
        val results = mutableListOf<TrackedEntity>()

        for (obj in detectedObjects) {
            val key = obj.label
            val existing = activeTracks[key]

            if (existing == null) {
                val newId = nextTrackId++
                val track = TrackRecord(
                    id = newId,
                    label = obj.label,
                    lastBox = obj.boundingBox,
                    lastTimestamp = timestamp,
                    stationaryFrames = 1,
                    activeFrames = 1,
                )
                activeTracks[key] = track
                results.add(
                    TrackedEntity(
                        trackingId = newId,
                        label = obj.label,
                        boundingBox = obj.boundingBox,
                        velocityX = 0f,
                        velocityY = 0f,
                        speed = 0f,
                        motionState = MotionState.STATIONARY,
                    )
                )
            } else {
                val dt = (timestamp - existing.lastTimestamp).coerceAtLeast(16L).toFloat() / 1000f
                val dx = obj.boundingBox.centerX - existing.lastBox.centerX
                val dy = obj.boundingBox.centerY - existing.lastBox.centerY
                val vx = dx / dt
                val vy = dy / dt
                val speed = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                existing.vx = vx
                existing.vy = vy
                existing.speed = speed
                existing.lastBox = obj.boundingBox
                existing.lastTimestamp = timestamp
                existing.activeFrames++

                if (speed < 0.02f) {
                    existing.stationaryFrames++
                } else {
                    existing.stationaryFrames = 0
                }

                // Determine motion state
                val motionState = determineMotionState(obj, existing, handObservations, terminalAnchors)

                results.add(
                    TrackedEntity(
                        trackingId = existing.id,
                        label = obj.label,
                        boundingBox = obj.boundingBox,
                        velocityX = vx,
                        velocityY = vy,
                        speed = speed,
                        motionState = motionState,
                    )
                )
            }
        }

        return results
    }

    private fun determineMotionState(
        obj: DetectedObject,
        track: TrackRecord,
        hands: List<HandGraspObservation>,
        terminals: List<TerminalAnchor>,
    ): MotionState {
        // Check if held by hand
        val isHeld = hands.any { hand ->
            val distThumb = distance(hand.thumbTip.x, hand.thumbTip.y, obj.boundingBox.centerX, obj.boundingBox.centerY)
            val distIndex = distance(hand.indexTip.x, hand.indexTip.y, obj.boundingBox.centerX, obj.boundingBox.centerY)
            hand.isPinching && (distThumb < 0.12f || distIndex < 0.12f)
        }

        if (isHeld) {
            return MotionState.HELD_BY_HAND
        }

        // Check if inside any terminal aperture
        val insideAperture = terminals.any { t ->
            obj.boundingBox.centerX in t.box.left..t.box.right &&
                    obj.boundingBox.centerY in t.box.top..t.box.bottom
        }

        return when {
            insideAperture && track.stationaryFrames >= 3 -> MotionState.STABILIZING
            insideAperture -> MotionState.ENTERING_APERTURE
            track.speed >= 0.025f -> MotionState.MOVING
            else -> MotionState.STATIONARY
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun reset() {
        activeTracks.clear()
        nextTrackId = 1
    }
}
