package com.skilllens.app.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.skilllens.app.taskengine.SkillRepository
import com.skilllens.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// CameraSetupScreen — Calibration / framing guide before practice begins
//
// The user positions the phone camera to clearly frame the task board.
// The screen shows a guide overlay and validates framing before allowing start.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CameraSetupScreen(
    skillId: String,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val skill = remember { SkillRepository.getSkillById(skillId) }
    val ext = SkillLensThemeTokens.colors

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
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = ColorOnBackground)
                }
                Text(
                    "Camera Setup",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorOnBackground,
                    modifier = Modifier.weight(1f),
                )
            }

            // Camera area with guide frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorSurfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Camera not available", color = ext.textMuted)
                    }
                }

                // Animated guide frame
                val dashPhase by rememberInfiniteTransition(label = "dash").animateFloat(
                    initialValue = 0f, targetValue = 20f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                    label = "dash"
                )
                Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), dashPhase)
                    drawRoundRect(
                        color       = ColorPrimary,
                        style       = androidx.compose.ui.graphics.drawscope.Stroke(
                            width      = 2.dp.toPx(),
                            pathEffect = dashEffect,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    )
                }

                // Guide text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ColorOverlayDark,
                    ) {
                        Text(
                            text     = "Position the task board\ninside this frame",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = ColorOnBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                skill?.let { s ->
                    Text(
                        s.safetyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    "Tips:",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorOnBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                listOf(
                    "Ensure good lighting on the board",
                    "Hold phone 20–30 cm above the board",
                    "Keep hands out of the frame during setup",
                    "Avoid shadows and glare",
                ).forEach { tip ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("•  ", color = ColorPrimary, style = MaterialTheme.typography.bodySmall)
                        Text(tip, color = ColorOnSurface, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Start button
            Button(
                onClick  = onReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .height(56.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape    = RoundedCornerShape(16.dp),
                enabled  = hasCameraPermission,
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Start Practice",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
