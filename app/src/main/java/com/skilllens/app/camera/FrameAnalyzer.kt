package com.skilllens.app.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.skilllens.app.taskengine.FrameObservation
import com.skilllens.app.taskengine.FrameQuality
import com.skilllens.app.vision.VisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// FrameAnalyzer — bridge between CameraX ImageAnalysis and VisionEngine
//
// Performance & Memory Strategy:
//   - Camera runs at 30 FPS preview
//   - Adaptive sampling intervals: only sample when inference is ready
//   - Guard against concurrent inference via isProcessingFrame
//   - Strictly recycle all intermediate Bitmaps to prevent OOM
// ─────────────────────────────────────────────────────────────────────────────

class FrameAnalyzer @Inject constructor(
    private val visionEngine: VisionEngine,
    private val onObservation: (FrameObservation) -> Unit,
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private val INFERENCE_INTERVAL_FRAMES = 4

    private var currentInterval = INFERENCE_INTERVAL_FRAMES
    private var lastFrameWasActive = false

    // Concurrency guard: Ensure single-flight ML inference
    private val isProcessingFrame = AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun analyze(image: ImageProxy) {
        frameCount++

        try {
            // Drop frame if cadence interval not met OR previous inference is still in-flight
            if (frameCount % currentInterval != 0 || isProcessingFrame.get()) {
                return
            }

            if (!isProcessingFrame.compareAndSet(false, true)) {
                return
            }

            val rotationDegrees = image.imageInfo.rotationDegrees
            val rawBitmap = image.toBitmap()
            val rotatedBitmap = rawBitmap.rotateIfNeeded(rotationDegrees)
            if (rotatedBitmap != rawBitmap) {
                rawBitmap.recycle()
            }

            val quality = assessFrameQuality(rotatedBitmap)

            scope.launch {
                try {
                    val observation = visionEngine.analyze(rotatedBitmap, quality)
                    onObservation(observation)
                    adaptSamplingInterval(observation)
                } catch (e: Exception) {
                    Timber.e(e, "FrameAnalyzer: Vision engine error")
                } finally {
                    rotatedBitmap.recycle()
                    isProcessingFrame.set(false)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FrameAnalyzer: Frame conversion error")
            isProcessingFrame.set(false)
        } finally {
            image.close()
        }
    }

    private fun assessFrameQuality(bitmap: Bitmap): FrameQuality {
        val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        var totalLum = 0L
        for (x in 0 until sample.width) {
            for (y in 0 until sample.height) {
                val pixel = sample.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalLum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
            }
        }
        sample.recycle()
        val avgLum = totalLum / (64 * 64)

        return when {
            avgLum < 30  -> FrameQuality.LOW_LIGHT
            avgLum > 240 -> FrameQuality.FRAMING_BAD
            else         -> FrameQuality.GOOD
        }
    }

    private fun adaptSamplingInterval(observation: FrameObservation) {
        val isActive = observation.detectedObjects.isNotEmpty()
        currentInterval = when {
            isActive && !lastFrameWasActive -> INFERENCE_INTERVAL_FRAMES - 1
            !isActive && lastFrameWasActive -> INFERENCE_INTERVAL_FRAMES + 2
            else                            -> INFERENCE_INTERVAL_FRAMES
        }.coerceIn(2, 10)
        lastFrameWasActive = isActive
    }

    fun shutdown() {
        scope.cancel()
    }
}

private fun Bitmap.rotateIfNeeded(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
