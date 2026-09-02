package com.skilllens.app.vision

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.skilllens.app.taskengine.BoundingBox
import com.skilllens.app.taskengine.HandLandmark
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Detailed grasp and spatial metrics for an observed hand.
 */
data class HandGraspObservation(
    val handBox: BoundingBox,
    val landmarks: List<HandLandmark>,
    val thumbTip: HandLandmark,
    val indexTip: HandLandmark,
    val wrist: HandLandmark,
    val palmCenter: Pair<Float, Float>,
    val pinchDistance: Float,
    val isPinching: Boolean,
    val isOccludingTerminals: Boolean,
)

/**
 * On-Device 21-Landmark 3D Hand Tracking & Grasp Analyzer.
 * Wraps MediaPipe HandLandmarker.
 */
@Singleton
class HandLandmarkAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var handLandmarker: HandLandmarker? = null
    private val MODEL_PATH = "models/hand_landmarker.task"

    companion object {
        const val PINCH_THRESHOLD = 0.085f // Normalized screen distance threshold for pinch
    }

    fun initialize(): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.40f)
                .setMinHandPresenceConfidence(0.40f)
                .setMinTrackingConfidence(0.40f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Timber.i("HandLandmarkAnalyzer: MediaPipe 3D Hand Landmarker initialized.")
            true
        } catch (e: Exception) {
            Timber.w("HandLandmarkAnalyzer: Initialization failed: ${e.message}")
            handLandmarker = null
            false
        }
    }

    fun isReady(): Boolean = handLandmarker != null

    fun analyzeHands(
        mpImage: MPImage,
        terminalAnchors: List<TerminalAnchor>,
    ): List<HandGraspObservation> {
        val landmarker = handLandmarker ?: return emptyList()

        return try {
            val result: HandLandmarkerResult = landmarker.detect(mpImage)
            val handLandmarkLists = result.landmarks()

            if (handLandmarkLists.isEmpty()) return emptyList()

            handLandmarkLists.mapNotNull { normalizedLandmarks ->
                if (normalizedLandmarks.size < 21) return@mapNotNull null

                val converted = normalizedLandmarks.mapIndexed { index, lm ->
                    HandLandmark(lm.x(), lm.y(), lm.z(), index)
                }

                val minX = converted.minOf { it.x }.coerceIn(0f, 1f)
                val maxX = converted.maxOf { it.x }.coerceIn(0f, 1f)
                val minY = converted.minOf { it.y }.coerceIn(0f, 1f)
                val maxY = converted.maxOf { it.y }.coerceIn(0f, 1f)
                val handBox = BoundingBox(minX, minY, maxX, maxY)

                val thumbTip = converted[4]
                val indexTip = converted[8]
                val wrist = converted[0]

                // Compute pinch distance between thumb tip (4) and index tip (8)
                val dx = thumbTip.x - indexTip.x
                val dy = thumbTip.y - indexTip.y
                val pinchDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val isPinching = pinchDist < PINCH_THRESHOLD

                // Compute palm center (average of wrist 0, MCP joints 5, 9, 13, 17)
                val palmX = (converted[0].x + converted[5].x + converted[9].x + converted[13].x + converted[17].x) / 5f
                val palmY = (converted[0].y + converted[5].y + converted[9].y + converted[13].y + converted[17].y) / 5f

                // Occlusion check: does the hand bounding box cover >= 3 terminal centers?
                val coveredTerminals = terminalAnchors.count { anchor ->
                    anchor.box.centerX in handBox.left..handBox.right &&
                            anchor.box.centerY in handBox.top..handBox.bottom
                }
                val isOccluding = coveredTerminals >= 3

                HandGraspObservation(
                    handBox = handBox,
                    landmarks = converted,
                    thumbTip = thumbTip,
                    indexTip = indexTip,
                    wrist = wrist,
                    palmCenter = Pair(palmX, palmY),
                    pinchDistance = pinchDist,
                    isPinching = isPinching,
                    isOccludingTerminals = isOccluding,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "HandLandmarkAnalyzer: Error during inference")
            emptyList()
        }
    }

    fun release() {
        handLandmarker?.close()
        handLandmarker = null
        Timber.d("HandLandmarkAnalyzer: Released")
    }
}
