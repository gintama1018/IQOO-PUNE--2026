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
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// FrameAnalyzer — bridge between CameraX ImageAnalysis and VisionEngine
//
// Performance & Quality Strategy:
//   - Camera runs at 30 FPS preview
//   - Target 640x480 analysis stream with single-flight concurrency lock
//   - Comprehensive FrameQuality assessment (Luminance + Edge Gradient variance)
//   - Guaranteed immediate Bitmap memory recycling
// ─────────────────────────────────────────────────────────────────────────────

class FrameAnalyzer @Inject constructor(
    private val visionEngine: VisionEngine,
    private val onObservation: (FrameObservation) -> Unit,
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private val INFERENCE_INTERVAL_FRAMES = 4

    private var currentInterval = INFERENCE_INTERVAL_FRAMES
    private var lastFrameWasActive = false

    private val isProcessingFrame = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun analyze(image: ImageProxy) {
        frameCount++

        try {
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

    /**
     * Assesses frame quality:
     * - Checks average luminance (low light vs washed out glare)
     * - Checks edge variance (gradient contrast) to detect motion blur
     */
    private fun assessFrameQuality(bitmap: Bitmap): FrameQuality {
        val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        var totalLum = 0L
        var totalGradient = 0L

        val lumArray = IntArray(64 * 64)

        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val pixel = sample.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                lumArray[y * 64 + x] = lum
                totalLum += lum

                if (x > 0 && y > 0) {
                    val prevX = lumArray[y * 64 + (x - 1)]
                    val prevY = lumArray[(y - 1) * 64 + x]
                    val grad = abs(lum - prevX) + abs(lum - prevY)
                    totalGradient += grad
                }
            }
        }
        sample.recycle()

        val avgLum = totalLum / (64 * 64)
        val avgGrad = totalGradient / (63 * 63)

        return when {
            avgLum < 25   -> FrameQuality.LOW_LIGHT
            avgLum > 245  -> FrameQuality.FRAMING_BAD
            avgGrad < 4   -> FrameQuality.BLURRY // Very low contrast / severe blur
            else          -> FrameQuality.GOOD
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
