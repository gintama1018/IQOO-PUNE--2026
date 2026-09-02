package com.skilllens.app.vision

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.skilllens.app.taskengine.BoundingBox
import com.skilllens.app.taskengine.DetectedObject
import com.skilllens.app.taskengine.FrameObservation
import com.skilllens.app.taskengine.FrameQuality
import com.skilllens.app.taskengine.SpatialRelationship
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// VisionEngine — On-Device Computer Vision & Physical Task Perception Engine
//
// Modular Architecture:
//   1. PersonDetector: Trainee presence, isolation, and user count.
//   2. BoardLocalizer: Image-evidence boundary scan & normalized board coordinate space.
//   3. HandLandmarkAnalyzer: MediaPipe 21 3D landmarks, pinch grasp detection & occlusion.
//   4. WirePerceptor: HSV chromatic segmentation + aspect ratio shape filtering.
//   5. ObjectTracker: Multi-frame temporal tracking & velocity estimation.
//   6. InteractionAnalyzer: Aperture containment vs. nearness geometry & connection stability.
// ─────────────────────────────────────────────────────────────────────────────

enum class VisionMode {
    MODEL_ACTIVE,
    CALIBRATED_BENCHMARK,
}

@Singleton
class VisionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personDetector: PersonDetector,
    private val boardLocalizer: BoardLocalizer,
    private val handLandmarkAnalyzer: HandLandmarkAnalyzer,
    private val wirePerceptor: WirePerceptor,
    private val objectTracker: ObjectTracker,
    private val interactionAnalyzer: InteractionAnalyzer,
) {

    private val _visionMode = MutableStateFlow(VisionMode.CALIBRATED_BENCHMARK)
    val visionMode: StateFlow<VisionMode> = _visionMode.asStateFlow()

    private var isInitialized = false

    // Movement-delta fallback state for wire tip motion tracking
    private var prevRedTipX: Float? = null
    private var prevRedTipY: Float? = null

    companion object {
        private const val MOVEMENT_THRESHOLD = 0.04f
    }

    fun initialize() {
        if (isInitialized) return

        val handLoaded = handLandmarkAnalyzer.initialize()
        _visionMode.value = if (handLoaded) VisionMode.MODEL_ACTIVE else VisionMode.CALIBRATED_BENCHMARK

        isInitialized = true
        Timber.i("VisionEngine: Initialized in mode ${_visionMode.value}")
    }

    fun analyze(bitmap: Bitmap, quality: FrameQuality): FrameObservation {
        val timestamp = System.currentTimeMillis()

        if (quality == FrameQuality.LOW_LIGHT || quality == FrameQuality.BLURRY) {
            return FrameObservation(
                detectedObjects = emptyList(),
                relationships   = emptyList(),
                handLandmarks   = null,
                frameTimestamp  = timestamp,
                confidence      = 0f,
                frameQuality    = quality,
                boardROI        = null,
            )
        }

        // 1. Evidence-Based Board Localization & Coordinate System
        val boardResult = boardLocalizer.localizeBoard(bitmap)
        if (boardResult == null) {
            Timber.d("VisionEngine: Board ROI not detected — uniform scene (wall/table/ceiling)")
            val personObs = personDetector.detectPerson(bitmap, hasActiveHands = false)
            return FrameObservation(
                detectedObjects    = emptyList(),
                relationships      = emptyList(),
                handLandmarks      = null,
                frameTimestamp     = timestamp,
                confidence         = 0.2f, // Low confidence -> TASK_OUT_OF_FRAME
                frameQuality       = FrameQuality.FRAMING_BAD,
                personObservation  = personObs,
                boardROI           = null,
            )
        }

        val (boardBox, transformer) = boardResult
        val detectedObjects = mutableListOf<DetectedObject>()

        // 2. Dynamic Board and Proportional Terminal Anchors
        val boardConfidence = (boardBox.width * boardBox.height * 4f).coerceIn(0.65f, 0.96f)
        detectedObjects.add(
            DetectedObject(
                label       = "board",
                confidence  = boardConfidence,
                boundingBox = boardBox,
            )
        )

        detectedObjects.add(
            DetectedObject(
                label       = "terminal_block",
                confidence  = 0.88f,
                boundingBox = transformer.computeTerminalBlockBox(),
            )
        )

        val dynamicAnchors = transformer.computeDynamicTerminalAnchors()
        for (anchor in dynamicAnchors) {
            detectedObjects.add(
                DetectedObject(
                    label       = anchor.id,
                    confidence  = 0.86f,
                    boundingBox = anchor.box,
                )
            )
        }

        // 3. Multi-Signal Chromatic & Shape-Filtered Wire Extraction
        val wireSegments = wirePerceptor.extractWireSegments(bitmap, transformer)
        for (wire in wireSegments) {
            detectedObjects.add(wire.detectedObject)
        }

        // 4. MediaPipe 21-Landmark Hand & Grasp Detection
        val mpImage = BitmapImageBuilder(bitmap).build()
        val handObservations = handLandmarkAnalyzer.analyzeHands(mpImage, dynamicAnchors)

        var isOccluded = false
        val allHandLandmarks = mutableListOf<com.skilllens.app.taskengine.HandLandmark>()

        for (hand in handObservations) {
            detectedObjects.add(
                DetectedObject(
                    label       = "hand",
                    confidence  = 0.95f,
                    boundingBox = hand.handBox,
                )
            )
            allHandLandmarks.addAll(hand.landmarks)
            if (hand.isOccludingTerminals) {
                isOccluded = true
            }
        }

        // 5. Person Presence & Participant Isolation Check
        val personObs = personDetector.detectPerson(bitmap, hasActiveHands = handObservations.isNotEmpty())
        if (personObs.personCount > 0 && personObs.primaryPersonBox != null) {
            detectedObjects.add(
                DetectedObject(
                    label       = "person",
                    confidence  = 0.90f,
                    boundingBox = personObs.primaryPersonBox,
                )
            )
        }

        // 6. Kinematic Object & Motion Tracking
        val trackedEntities = objectTracker.trackObjects(
            detectedObjects  = detectedObjects,
            handObservations = handObservations,
            terminalAnchors  = dynamicAnchors,
            timestamp        = timestamp,
        )

        // 7. Physical Interaction & Connection Reasoning (Aperture Entry + Grasping)
        val relationships = mutableListOf<SpatialRelationship>()
        relationships.addAll(
            interactionAnalyzer.evaluateInteractions(
                wireObservations = wireSegments,
                terminalAnchors  = dynamicAnchors,
                handObservations = handObservations,
                transformer      = transformer,
            )
        )

        // 8. Fallback wire motion inference when hand landmarks are unavailable
        if (handObservations.isEmpty()) {
            inferHoldFromMovement(wireSegments, relationships)
        }

        val effectiveQuality = when {
            isOccluded -> FrameQuality.OCCLUDED
            !personObs.isParticipantIsolated -> FrameQuality.FRAMING_BAD
            else -> quality
        }

        val avgConfidence = if (detectedObjects.isEmpty()) 0.2f
        else detectedObjects.map { it.confidence }.average().toFloat()

        return FrameObservation(
            detectedObjects   = detectedObjects,
            relationships     = relationships,
            handLandmarks     = if (allHandLandmarks.isNotEmpty()) allHandLandmarks else null,
            frameTimestamp    = timestamp,
            confidence        = if (isOccluded) 0.35f else avgConfidence,
            frameQuality      = effectiveQuality,
            personObservation = personObs,
            trackedEntities   = trackedEntities,
            boardROI          = boardBox,
        )
    }

    private fun inferHoldFromMovement(
        wireSegments: List<WireObservation>,
        relationships: MutableList<SpatialRelationship>,
    ) {
        for (wire in wireSegments) {
            if (wire.label == "red_wire") {
                val prevX = prevRedTipX
                val prevY = prevRedTipY
                val dx = wire.tipX - (prevX ?: wire.tipX)
                val dy = wire.tipY - (prevY ?: wire.tipY)
                val moved = prevX != null && prevY != null && sqrt((dx * dx + dy * dy).toDouble()) > MOVEMENT_THRESHOLD

                if (moved) {
                    relationships.add(
                        SpatialRelationship(
                            subject    = "hand",
                            relation   = "holding",
                            target     = "red_wire",
                            confidence = 0.65f,
                        )
                    )
                }
                prevRedTipX = wire.tipX
                prevRedTipY = wire.tipY
                break
            }
        }
    }

    fun release() {
        handLandmarkAnalyzer.release()
        objectTracker.reset()
        interactionAnalyzer.reset()
        isInitialized = false
        prevRedTipX = null
        prevRedTipY = null
        Timber.d("VisionEngine: Released")
    }
}
