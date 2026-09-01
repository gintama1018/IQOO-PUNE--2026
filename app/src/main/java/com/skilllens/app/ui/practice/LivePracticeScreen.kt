package com.skilllens.app.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skilllens.app.taskengine.DetectedObject
import com.skilllens.app.taskengine.FeedbackType
import com.skilllens.app.taskengine.TaskState
import com.skilllens.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// LivePracticeScreen — The core product screen
//
// Layout (top to bottom):
//   1. Status bar (skill name, timer, state)
//   2. Camera preview with HUD overlay
//   3. Progress bar
//   4. Feedback card
//   5. Action buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LivePracticeScreen(
    skillId: String,
    onSessionComplete: (sessionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LivePracticeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = SkillLensThemeTokens.colors

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.loadSkill(skillId)
    }

    // Completion handler
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            kotlinx.coroutines.delay(2000)
            onSessionComplete(skillId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        if (!hasCameraPermission) {
            PermissionDeniedContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onBack = onBack,
            )
        } else if (uiState.isLoading) {
            LoadingContent()
        } else if (uiState.error != null) {
            ErrorContent(message = uiState.error!!, onBack = onBack)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── 1. Status Bar ────────────────────────────────────────────
                PracticeTopBar(
                    skillName       = uiState.skill?.name ?: "",
                    state           = uiState.currentState,
                    durationSec     = uiState.sessionDurationSec,
                    confidence      = uiState.confidence,
                    onPause         = { viewModel.pause() },
                    onBack          = onBack,
                )

                // ── 2. Camera Preview + HUD ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraPreviewWithOverlay(
                        lifecycleOwner  = lifecycleOwner,
                        viewModel       = viewModel,
                        detectedObjects = uiState.detectedObjects,
                        currentState    = uiState.currentState,
                    )

                    // Corner brackets overlay
                    HudCornerBrackets(modifier = Modifier.fillMaxSize())
                }

                // ── 3. Progress ──────────────────────────────────────────────
                StepProgressBar(
                    currentStep = uiState.currentStepIndex,
                    totalSteps  = uiState.totalSteps,
                    state       = uiState.currentState,
                )

                // ── 4. Feedback Card ─────────────────────────────────────────
                AnimatedFeedbackCard(
                    feedback     = uiState.feedback,
                    currentState = uiState.currentState,
                    stepTitle    = viewModel.uiState.value.skill?.steps
                        ?.getOrNull(uiState.currentStepIndex)?.instruction ?: "",
                )

                // ── 5. Action Bar ────────────────────────────────────────────
                PracticeActionBar(
                    isPaused    = uiState.isPaused,
                    isCompleted = uiState.isCompleted,
                    onPause     = { viewModel.pause() },
                    onResume    = { viewModel.resume() },
                    onReset     = { viewModel.resetSession() },
                )
            }
        }

        // ── Pause Overlay ────────────────────────────────────────────────────
        if (uiState.isPaused) {
            PausedOverlay(
                onResume = { viewModel.resume() },
                onQuit   = onBack,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PracticeTopBar(
    skillName: String,
    state: TaskState,
    durationSec: Int,
    confidence: Float,
    onPause: () -> Unit,
    onBack: () -> Unit,
) {
    val ext = SkillLensThemeTokens.colors
    val stateColor = when (state) {
        TaskState.COMPLETED                                      -> ext.correct
        TaskState.WRONG_CONNECTION, TaskState.WRONG_COMPONENT,
        TaskState.OUT_OF_ORDER, TaskState.MISSING_COMPONENT      -> ext.error
        TaskState.LOW_CONFIDENCE, TaskState.POOR_FRAMING,
        TaskState.OCCLUDED                                       -> ext.warning
        else                                                     -> ColorPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ColorBackground, Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = ColorOnBackground)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = skillName,
                style = MaterialTheme.typography.titleSmall,
                color = ColorOnBackground,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing live indicator
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(800), repeatMode = RepeatMode.Reverse
                    ), label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = pulse))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = state.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = stateColor,
                )
            }
        }

        // Timer
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ColorSurfaceVariant,
        ) {
            Text(
                text     = formatTime(durationSec),
                style    = MaterialTheme.typography.labelLarge,
                color    = ColorOnBackground,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(onClick = onPause) {
            Icon(Icons.Filled.Pause, "Pause", tint = ColorOnBackground)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Preview with Detection Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraPreviewWithOverlay(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    viewModel: LivePracticeViewModel,
    detectedObjects: List<DetectedObject>,
    currentState: TaskState,
) {
    val ext = SkillLensThemeTokens.colors

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { preview ->
                    viewModel.startCamera(lifecycleOwner, preview)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Detection bounding boxes overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (obj in detectedObjects) {
                val bb = obj.boundingBox
                val left   = bb.left * size.width
                val top    = bb.top * size.height
                val right  = bb.right * size.width
                val bottom = bb.bottom * size.height

                val color = when {
                    currentState == TaskState.WRONG_CONNECTION ||
                    currentState == TaskState.WRONG_COMPONENT -> ColorBoundingBoxError
                    obj.confidence > 0.7f                     -> ColorBoundingBoxActive
                    else                                      -> ColorBoundingBoxIdle
                }

                // Bounding box
                drawRoundRect(
                    color        = color,
                    topLeft      = Offset(left, top),
                    size         = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style        = Stroke(width = 2.dp.toPx()),
                )

                // Label background
                drawRoundRect(
                    color        = color.copy(alpha = 0.85f),
                    topLeft      = Offset(left, top - 24.dp.toPx()),
                    size         = Size(
                        (obj.label.length * 8 + 16).dp.toPx().coerceAtMost(right - left),
                        22.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUD Corner Brackets — engineering feel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HudCornerBrackets(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(24.dp)) {
        val cornerLen = 40.dp.toPx()
        val stroke    = Stroke(width = 2.dp.toPx())
        val color     = ColorHudCorner

        // Top-left
        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), stroke.width)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), stroke.width)
        // Top-right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), stroke.width)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), stroke.width)
        // Bottom-left
        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), stroke.width)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), stroke.width)
        // Bottom-right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), stroke.width)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), stroke.width)

        // Center crosshair
        val cx = size.width / 2; val cy = size.height / 2; val ch = 12.dp.toPx()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        drawLine(ColorHudLine, Offset(cx - ch, cy), Offset(cx + ch, cy), 1.dp.toPx(), pathEffect = dashEffect)
        drawLine(ColorHudLine, Offset(cx, cy - ch), Offset(cx, cy + ch), 1.dp.toPx(), pathEffect = dashEffect)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step Progress Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    state: TaskState,
) {
    val ext = SkillLensThemeTokens.colors
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "STEP ${currentStep + 1} / $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = ColorPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = ext.textMuted,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ColorSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ColorPrimary, ColorSecondary)
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Feedback Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedFeedbackCard(
    feedback: com.skilllens.app.taskengine.FeedbackEvent?,
    currentState: TaskState,
    stepTitle: String,
) {
    val ext = SkillLensThemeTokens.colors

    AnimatedContent(
        targetState = feedback,
        transitionSpec = {
            fadeIn(tween(300)) + slideInVertically { it / 2 } togetherWith
                    fadeOut(tween(200))
        },
        label = "feedback"
    ) { fb ->
        val (bgColor, borderColor, icon) = when (fb?.type) {
            FeedbackType.CORRECT    -> Triple(ext.correctGlow, ext.correct, Icons.Filled.CheckCircle)
            FeedbackType.ERROR      -> Triple(ext.errorGlow, ext.error, Icons.Filled.Error)
            FeedbackType.WARNING    -> Triple(ext.warningGlow, ext.warning, Icons.Filled.Warning)
            FeedbackType.COMPLETION -> Triple(ext.correctGlow, ext.correct, Icons.Filled.EmojiEvents)
            else                    -> Triple(ColorSurfaceVariant, ColorBorder, Icons.Outlined.Info)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = borderColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text      = fb?.title ?: "Waiting...",
                        style     = MaterialTheme.typography.titleSmall,
                        color     = ColorOnBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    if (fb?.message?.isNotBlank() == true) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = fb.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface,
                        )
                    }
                    if (fb == null && stepTitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = stepTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.textMuted,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PracticeActionBar(
    isPaused: Boolean,
    isCompleted: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onReset,
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reset")
        }

        if (!isCompleted) {
            Button(
                onClick = if (isPaused) onResume else onPause,
                colors  = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
            ) {
                Icon(
                    if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isPaused) "Resume" else "Pause")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Paused Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PausedOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorOverlayDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Pause, null,
                tint     = ColorPrimary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Practice Paused",
                style = MaterialTheme.typography.headlineMedium,
                color = ColorOnBackground,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onResume,
                colors  = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                modifier = Modifier.width(200.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Resume")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onQuit,
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                modifier = Modifier.width(200.dp),
            ) {
                Text("Quit Session")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.CameraAlt, null, tint = ColorPrimary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall, color = ColorOnBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("SkillLens needs camera access to observe your physical task and provide real-time feedback.", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)) {
                Text("Grant Camera Access")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = ColorTextMuted) }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ColorPrimary)
            Spacer(Modifier.height(16.dp))
            Text("Loading models...", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.ErrorOutline, null, tint = ColorError, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Something went wrong", style = MaterialTheme.typography.headlineSmall, color = ColorOnBackground)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
