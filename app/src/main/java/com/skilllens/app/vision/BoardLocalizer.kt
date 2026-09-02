package com.skilllens.app.vision

import android.graphics.Bitmap
import com.skilllens.app.taskengine.BoundingBox
import com.skilllens.app.taskengine.DetectedObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Data representation for localized board terminal anchors.
 */
data class TerminalAnchor(
    val id: String,
    val name: String,
    val box: BoundingBox,
    val boardU: Float,
    val boardV: Float,
)

/**
 * Bidirectional coordinate transformer between Normalized Board Space (u, v) in [0, 1]^2
 * and Camera Frame Coordinates (x, y) in [0, 1]^2.
 */
class BoardCoordinateTransformer(val boardBox: BoundingBox) {

    fun boardToCameraX(u: Float): Float =
        (boardBox.left + u * boardBox.width).coerceIn(0f, 1f)

    fun boardToCameraY(v: Float): Float =
        (boardBox.top + v * boardBox.height).coerceIn(0f, 1f)

    fun cameraToBoardU(x: Float): Float =
        if (boardBox.width > 0f) ((x - boardBox.left) / boardBox.width) else 0.5f

    fun cameraToBoardV(y: Float): Float =
        if (boardBox.height > 0f) ((y - boardBox.top) / boardBox.height) else 0.5f

    fun createApertureBox(boardU: Float, boardV: Float, radiusU: Float = 0.08f, radiusV: Float = 0.08f): BoundingBox {
        val left = boardToCameraX(boardU - radiusU)
        val top = boardToCameraY(boardV - radiusV)
        val right = boardToCameraX(boardU + radiusU)
        val bottom = boardToCameraY(boardV + radiusV)
        return BoundingBox(left, top, right, bottom)
    }

    /**
     * Compute dynamic physical terminal anchors projected from normalized board space.
     */
    fun computeDynamicTerminalAnchors(): List<TerminalAnchor> {
        return listOf(
            TerminalAnchor("l_terminal", "L (Live Main)", createApertureBox(0.22f, 0.48f), 0.22f, 0.48f),
            TerminalAnchor("n_terminal", "N (Neutral Main)", createApertureBox(0.50f, 0.48f), 0.50f, 0.48f),
            TerminalAnchor("e_terminal", "E (Earth Ground)", createApertureBox(0.78f, 0.48f), 0.78f, 0.48f),
            TerminalAnchor("l1_terminal", "L1 (Switched Live 1)", createApertureBox(0.22f, 0.68f), 0.22f, 0.68f),
            TerminalAnchor("l2_terminal", "L2 (Switched Live 2)", createApertureBox(0.50f, 0.68f), 0.50f, 0.68f),
            TerminalAnchor("l3_terminal", "L3 (Switched Live 3)", createApertureBox(0.78f, 0.68f), 0.78f, 0.68f),
        )
    }

    fun computeTerminalBlockBox(): BoundingBox {
        return BoundingBox(
            left = boardToCameraX(0.08f),
            top = boardToCameraY(0.35f),
            right = boardToCameraX(0.92f),
            bottom = boardToCameraY(0.92f),
        )
    }
}

/**
 * Image-Evidence Based Board Localizer.
 *
 * Scans camera frames for texture variance and geometric contours of the physical board.
 * Returns null if camera is pointed at blank surfaces (wall, table, ceiling) -> TASK_OUT_OF_FRAME.
 */
@Singleton
class BoardLocalizer @Inject constructor() {

    fun localizeBoard(bitmap: Bitmap): Pair<BoundingBox, BoardCoordinateTransformer>? {
        val w = 64
        val h = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)

        val luma = Array(h) { y ->
            FloatArray(w) { x ->
                val px = scaled.getPixel(x, y)
                val r = (px shr 16 and 0xFF) / 255f
                val g = (px shr 8 and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                0.299f * r + 0.587f * g + 0.114f * b
            }
        }
        scaled.recycle()

        var sumLuma = 0f
        for (y in 0 until h) {
            for (x in 0 until w) {
                sumLuma += luma[y][x]
            }
        }
        val meanLuma = sumLuma / (w * h)

        val varianceThreshold = 0.08f
        val minInterestingPixels = w * h / 6 // Minimum ~17% non-uniform surface area

        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        var interestingCount = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (abs(luma[y][x] - meanLuma) > varianceThreshold) {
                    interestingCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (interestingCount < minInterestingPixels || maxX <= minX || maxY <= minY) {
            // Scene is uniform / non-board (blank wall, floor, ceiling)
            return null
        }

        val margin = 0.04f
        val boardBox = BoundingBox(
            left = (minX.toFloat() / w - margin).coerceIn(0f, 1f),
            top = (minY.toFloat() / h - margin).coerceIn(0f, 1f),
            right = (maxX.toFloat() / w + margin).coerceIn(0f, 1f),
            bottom = (maxY.toFloat() / h + margin).coerceIn(0f, 1f),
        )

        // Ensure plausible board aspect ratio (reject razor-thin artifacts)
        if (boardBox.width < 0.25f || boardBox.height < 0.25f) {
            return null
        }

        val transformer = BoardCoordinateTransformer(boardBox)
        return Pair(boardBox, transformer)
    }
}
