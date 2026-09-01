package com.skilllens.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// CameraController — manages CameraX lifecycle, Preview + ImageAnalysis
//
// Design decisions:
// - Back camera only (task board is in front of the user, phone pointed at it)
// - Preview at screen resolution; analysis explicitly targeted at 640×480
// - Single-thread executor for ImageAnalysis (prevents frame backlog)
// - STRATEGY_KEEP_ONLY_LATEST: discard frames when analyzer is busy
// ─────────────────────────────────────────────────────────────────────────────

enum class CameraStatus {
    IDLE,
    STARTING,
    RUNNING,
    ERROR,
    PERMISSION_DENIED,
}

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _status = MutableStateFlow(CameraStatus.IDLE)
    val status: StateFlow<CameraStatus> = _status.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Bind CameraX to a lifecycle with a PreviewView surface and an analyzer.
     *
     * @param lifecycleOwner  Activity or Fragment lifecycle
     * @param previewView     Surface for live preview
     * @param analyzer        Frame processing callback (runs on analysisExecutor)
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
    ) {
        _status.value = CameraStatus.STARTING

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // ── Preview use-case ──────────────────────────────────────────
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // ── Analysis use-case: Explicit 640×480 target ────────────────
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }

                // ── Bind ──────────────────────────────────────────────────────
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )

                _status.value = CameraStatus.RUNNING
                Timber.d("CameraController: Camera bound successfully at target 640x480 analysis")

            } catch (e: Exception) {
                Timber.e(e, "CameraController: Failed to bind camera")
                _status.value = CameraStatus.ERROR
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Unbind all use-cases. Call when leaving practice screen.
     * This releases camera hardware immediately — critical for battery & privacy.
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        _status.value = CameraStatus.IDLE
        Timber.d("CameraController: Camera released")
    }

    /**
     * Shut down the analysis thread pool on application termination.
     */
    fun shutdown() {
        stopCamera()
        analysisExecutor.shutdown()
    }
}
