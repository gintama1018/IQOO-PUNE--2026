package com.skilllens.app.ui.results

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skilllens.app.ui.theme.*

@Composable
fun ResultScreen(
    sessionId: String,
    onRetry: (skillId: String) -> Unit,
    onGoHome: () -> Unit,
    onHistory: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = SkillLensThemeTokens.colors

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val animatedScore by animateIntAsState(
        targetValue   = uiState.score,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "score"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // Success icon
        Surface(
            shape = CircleShape,
            color = ColorCorrect.copy(alpha = 0.12f),
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.EmojiEvents, null, tint = ColorCorrect, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Practice Complete",
            style      = MaterialTheme.typography.headlineMedium,
            color      = ColorOnBackground,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            uiState.skill?.name ?: "Basic Circuit Wiring",
            style = MaterialTheme.typography.bodyMedium,
            color = ext.textMuted,
        )

        Spacer(Modifier.height(32.dp))

        // Score ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp),
        ) {
            val sweepAngle = (animatedScore / 100f) * 360f
            Canvas(modifier = Modifier.size(160.dp)) {
                drawArc(
                    color      = ColorSurfaceVariant,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    brush      = Brush.sweepGradient(listOf(ColorPrimary, ColorCorrect)),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter  = false,
                    style      = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$animatedScore",
                    style      = MaterialTheme.typography.displayLarge,
                    color      = ColorOnBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 52.sp,
                )
                Text(
                    "/ 100",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.textMuted,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard("Steps", "${uiState.correctSteps} / ${uiState.totalSteps}", ColorCorrect, Modifier.weight(1f))
            MetricCard("Corrections", "${uiState.corrections}", ColorWarning, Modifier.weight(1f))
            MetricCard("Time", formatDuration(uiState.durationSeconds), ColorPrimary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // Step breakdown
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ColorSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Step Breakdown", style = MaterialTheme.typography.titleSmall, color = ColorOnBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                uiState.skill?.steps?.forEachIndexed { idx, step ->
                    val isStepCorrect = idx != 2 || uiState.corrections == 0
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isStepCorrect) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                            null,
                            tint = if (isStepCorrect) ColorCorrect else ColorWarning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            step.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (isStepCorrect) "✓" else "Corrected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isStepCorrect) ColorCorrect else ColorWarning,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Strengths & improvement
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ColorSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ThumbUp, null, tint = ColorCorrect, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Best: Component Identification & Live Wire", style = MaterialTheme.typography.bodySmall, color = ColorCorrect)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrendingUp, null, tint = ColorWarning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Focus: Terminal Verification Precision", style = MaterialTheme.typography.bodySmall, color = ColorWarning)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Actions
        Button(
            onClick  = { onRetry(uiState.skill?.id ?: "electrical_wiring_basic") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
            shape    = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Practice Again", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick  = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
        ) {
            Text("Go Home")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorTextMuted)
        }
    }
}

@Composable
fun SessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = ColorOnBackground) }
            Text("Session Details", style = MaterialTheme.typography.titleLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Session: $sessionId\n\nCompleted on-device.", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
