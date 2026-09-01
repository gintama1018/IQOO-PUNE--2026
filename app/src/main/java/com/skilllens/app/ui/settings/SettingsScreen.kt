package com.skilllens.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skilllens.app.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    var hapticEnabled by remember { mutableStateOf(true) }
    var audioEnabled by remember { mutableStateOf(false) }
    var showConfidence by remember { mutableStateOf(true) }

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
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsGroupTitle("Feedback")
            SettingsToggle("Haptic Feedback", "Vibrate on correct/incorrect actions", hapticEnabled) { hapticEnabled = it }
            SettingsToggle("Audio Feedback", "Sound effects during practice", audioEnabled) { audioEnabled = it }

            Spacer(Modifier.height(16.dp))
            SettingsGroupTitle("Display")
            SettingsToggle("Show Confidence", "Display AI confidence during practice", showConfidence) { showConfidence = it }

            Spacer(Modifier.height(16.dp))
            SettingsGroupTitle("Privacy")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, null, tint = ColorCorrect, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Privacy-first design", style = MaterialTheme.typography.labelLarge, color = ColorOnBackground)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Camera frames are processed locally and discarded. " +
                        "No images leave your device. Session results are stored locally only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextMuted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsGroupTitle("Info")
            Surface(
                onClick = onAbout,
                shape = RoundedCornerShape(12.dp),
                color = ColorSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = ColorPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("About SkillLens", style = MaterialTheme.typography.bodyMedium, color = ColorOnBackground, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = ColorTextMuted)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        title.uppercase(),
        style    = MaterialTheme.typography.labelMedium,
        color    = ColorPrimary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ColorSurface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = ColorOnBackground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ColorTextMuted)
            }
            Switch(
                checked  = checked,
                onCheckedChange = onToggle,
                colors   = SwitchDefaults.colors(
                    checkedThumbColor = ColorPrimary,
                    checkedTrackColor = ColorPrimary.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ext = SkillLensThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = ColorOnBackground) }
            Text("About", style = MaterialTheme.typography.titleLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SkillLens", style = MaterialTheme.typography.displaySmall, color = ColorOnBackground, fontWeight = FontWeight.Bold)
            Text("v1.0.0-hackathon", style = MaterialTheme.typography.bodySmall, color = ext.textMuted)
            Spacer(Modifier.height(24.dp))
            Text(
                "SkillLens turns your smartphone into a real-time practical skill examiner. " +
                "Point your camera at a physical task, and the AI observes your work, " +
                "validates each step, and provides immediate corrective feedback.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurface,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Built for iQOO Hackathon 2026",
                style = MaterialTheme.typography.labelMedium,
                color = ColorPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Track: Smart Education / Open Innovation",
                style = MaterialTheme.typography.labelSmall,
                color = ext.textMuted,
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Technology Stack", style = MaterialTheme.typography.labelLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Kotlin + Jetpack Compose",
                        "CameraX (camera pipeline)",
                        "MediaPipe (object detection + hand tracking)",
                        "Deterministic task state engine",
                        "Room (local session storage)",
                        "Hilt (dependency injection)",
                    ).forEach { tech ->
                        Text("•  $tech", style = MaterialTheme.typography.bodySmall, color = ColorOnSurface, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "This is a hackathon prototype. It is not a certified training tool.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorWarning,
            )
        }
    }
}
