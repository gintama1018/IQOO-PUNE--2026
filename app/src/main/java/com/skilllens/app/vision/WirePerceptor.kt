package com.skilllens.app.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.skilllens.app.taskengine.BoundingBox
import com.skilllens.app.taskengine.DetectedObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Observed wire segment with geometric features and board-relative coordinates.
 */
data class WireObservation(
    val label: String,
    val detectedObject: DetectedObject,
    val tipX: Float,
    val tipY: Float,
    val rootX: Float,
    val rootY: Float,
    val boardU: Float,
    val boardV: Float,
    val length: Float,
    val aspectRatio: Float,
)

/**
 * Multi-Signal Chromatic and Morphological Wire Perceptor.
 *
 * Combines HSV color segmentation with geometric elongation / aspect-ratio filtering
 * to isolate genuine wires and reject background false positives (e.g. red mugs, clothing).
 */
@Singleton
class WirePerceptor @Inject constructor() {

    fun extractWireSegments(
        bitmap: Bitmap,
        transformer: BoardCoordinateTransformer?,
    ): List<WireObservation> {
        val w = 96
        val h = 96
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)

        val redPixels = mutableListOf<Pair<Int, Int>>()
        val blackPixels = mutableListOf<Pair<Int, Int>>()
        val earthPixels = mutableListOf<Pair<Int, Int>>()

        val hsv = FloatArray(3)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = scaled.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                when {
                    // Live Wire: Red (Hue in [0, 14] or [168, 360], high saturation, sufficient value)
                    (hue in 0f..14f || hue in 345f..360f) && sat >= 0.42f && value >= 0.28f -> {
                        redPixels.add(Pair(x, y))
                    }
                    // Neutral Wire: Black (Very low brightness, low saturation)
                    value <= 0.20f && sat <= 0.35f -> {
                        blackPixels.add(Pair(x, y))
                    }
                    // Earth Wire: Green-Yellow (Hue in [35, 88], saturated)
                    hue in 35f..88f && sat >= 0.35f && value >= 0.25f -> {
                        earthPixels.add(Pair(x, y))
                    }
                }
            }
        }
        scaled.recycle()

        val results = mutableListOf<WireObservation>()

        processCluster(redPixels, "red_wire", w, h, transformer)?.let { results.add(it) }
        processCluster(blackPixels, "black_wire", w, h, transformer)?.let { results.add(it) }
        processCluster(earthPixels, "earth_wire", w, h, transformer)?.let { results.add(it) }

        return results
    }

    private fun processCluster(
        pixels: List<Pair<Int, Int>>,
        label: String,
        w: Int,
        h: Int,
        transformer: BoardCoordinateTransformer?,
    ): WireObservation? {
        // Minimum pixel mass threshold: ignore tiny noise flecks
        val minPixels = 16
        if (pixels.size < minPixels) return null

        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0

        for ((x, y) in pixels) {
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        val boxW = (maxX - minX + 1).toFloat()
        val boxH = (maxY - minY + 1).toFloat()

        // Geometric shape & aspect ratio filter:
        // Wires are elongated structures. Reject compact circular/square masses (mugs, shirts)
        val majorAxis = max(boxW, boxH)
        val minorAxis = min(boxW, boxH).coerceAtLeast(1f)
        val elongation = majorAxis / minorAxis

        // Density check: if pixel mass completely fills the bounding box like a solid square, it's not a thin wire
        val boundingArea = boxW * boxH
        val fillDensity = pixels.size.toFloat() / boundingArea

        // Reject solid compact blocks (like a red mug/block) — wires have lower density or high elongation
        if (fillDensity > 0.75f && elongation < 1.8f) {
            return null
        }

        val normLeft = (minX.toFloat() / w).coerceIn(0f, 1f)
        val normTop = (minY.toFloat() / h).coerceIn(0f, 1f)
        val normRight = (maxX.toFloat() / w).coerceIn(0f, 1f)
        val normBottom = (maxY.toFloat() / h).coerceIn(0f, 1f)
        val box = BoundingBox(normLeft, normTop, normRight, normBottom)

        // Find insertion tip (closest to top-middle where terminals typically reside) and root
        val tipPixel = pixels.minByOrNull { it.second } ?: pixels.first()
        val rootPixel = pixels.maxByOrNull { it.second } ?: pixels.last()

        val tipX = (tipPixel.first.toFloat() / w).coerceIn(0f, 1f)
        val tipY = (tipPixel.second.toFloat() / h).coerceIn(0f, 1f)
        val rootX = (rootPixel.first.toFloat() / w).coerceIn(0f, 1f)
        val rootY = (rootPixel.second.toFloat() / h).coerceIn(0f, 1f)

        val dx = tipX - rootX
        val dy = tipY - rootY
        val length = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Project tip into board coordinates if transformer available
        val boardU = transformer?.cameraToBoardU(tipX) ?: 0.5f
        val boardV = transformer?.cameraToBoardV(tipY) ?: 0.5f

        val confidence = (0.70f + (elongation * 0.05f).coerceAtMost(0.22f)).coerceIn(0.65f, 0.94f)

        return WireObservation(
            label = label,
            detectedObject = DetectedObject(
                label = label,
                confidence = confidence,
                boundingBox = box,
            ),
            tipX = tipX,
            tipY = tipY,
            rootX = rootX,
            rootY = rootY,
            boardU = boardU,
            boardV = boardV,
            length = length,
            aspectRatio = elongation,
        )
    }
}
