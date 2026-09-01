package com.skilllens.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skilllens.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — Hero landing screen
//
// Premium dark design with:
// - Animated glow orb
// - Product tagline
// - CTA to start practice
// - Quick access to History and Settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val ext = SkillLensThemeTokens.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Ambient glow background
        AnimatedGlowOrb()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo / Brand ─────────────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = ColorPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "SkillLens",
                        tint     = ColorPrimary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Title ────────────────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = ColorOnBackground)) { append("Skill") }
                    withStyle(SpanStyle(color = ColorPrimary)) { append("Lens") }
                },
                style    = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Your phone watches you learn.",
                style     = MaterialTheme.typography.bodyLarge,
                color     = ColorOnSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text      = "Real-time practical skill verification for Android.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = ext.textMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // ── Primary CTA ──────────────────────────────────────────────────
            Button(
                onClick  = onStartPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape    = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Start Practice",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Feature cards row ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.CameraAlt,
                    title    = "Camera AI",
                    subtitle = "Real-time observation",
                    color    = ColorPrimary,
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.OfflineBolt,
                    title    = "Offline",
                    subtitle = "Works without internet",
                    color    = ColorSecondary,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.CheckCircle,
                    title    = "Verify",
                    subtitle = "Step-by-step validation",
                    color    = ColorCorrect,
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Speed,
                    title    = "Instant",
                    subtitle = "Immediate feedback",
                    color    = ColorWarning,
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── How it works ─────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "How It Works",
                        style      = MaterialTheme.typography.titleSmall,
                        color      = ColorOnBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "PERFORM" to "Do the task in front of your camera",
                        "OBSERVE"  to "AI detects components and actions",
                        "VERIFY"   to "Engine validates against expected steps",
                        "CORRECT"  to "Get immediate corrective feedback",
                    ).forEachIndexed { idx, (step, desc) ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ColorPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ColorPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(step, style = MaterialTheme.typography.labelLarge, color = ColorPrimary)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = ext.textMuted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Quick links ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick  = onHistory,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                ) {
                    Icon(Icons.Outlined.History, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("History")
                }
                OutlinedButton(
                    onClick  = onSettings,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                ) {
                    Icon(Icons.Outlined.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settings")
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Footer ───────────────────────────────────────────────────────
            Text(
                "Built for iQOO Hackathon 2026",
                style = MaterialTheme.typography.labelSmall,
                color = ext.textDisabled,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = ColorSurface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ColorTextMuted)
        }
    }
}

@Composable
private fun AnimatedGlowOrb() {
    val transition = rememberInfiniteTransition(label = "glow")
    val offsetX by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "x"
    )
    val offsetY by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            ColorPrimary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * offsetX, size.height * offsetY),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * offsetX, size.height * offsetY),
                )
            }
    )
}
