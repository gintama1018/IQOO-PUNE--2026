package com.skilllens.app.ui.skills

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
import com.skilllens.app.taskengine.SkillRepository
import com.skilllens.app.ui.theme.*

@Composable
fun TaskOverviewScreen(
    skillId: String,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val skill = remember { SkillRepository.getSkillById(skillId) }
    val ext = SkillLensThemeTokens.colors

    if (skill == null) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            Text("Skill not found", color = ColorError)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = ColorOnBackground)
            }
            Text(skill.name, style = MaterialTheme.typography.titleLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Description
            Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface)

            Spacer(Modifier.height(16.dp))

            // Safety warning
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorWarning.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, null, tint = ColorWarning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(skill.safetyNote, style = MaterialTheme.typography.bodySmall, color = ColorWarning)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Info row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(icon = Icons.Outlined.Timer, label = "~${skill.estimatedDurationMin} min", modifier = Modifier.weight(1f))
                InfoChip(icon = Icons.Outlined.FormatListNumbered, label = "${skill.steps.size} steps", modifier = Modifier.weight(1f))
                InfoChip(icon = Icons.Outlined.OfflineBolt, label = "Offline", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Equipment
            Text("Required Equipment", style = MaterialTheme.typography.titleSmall, color = ColorOnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            skill.requiredEquipment.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = ColorCorrect, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall, color = ColorOnSurface)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Steps preview
            Text("Task Steps", style = MaterialTheme.typography.titleSmall, color = ColorOnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            skill.steps.forEachIndexed { index, step ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ColorSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ColorPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = ColorPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(step.title, style = MaterialTheme.typography.labelLarge, color = ColorOnBackground)
                            Text(step.instruction, style = MaterialTheme.typography.bodySmall, color = ext.textMuted, maxLines = 2)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Start button
        Button(
            onClick  = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .height(56.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
            shape    = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Begin Practice", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ColorSurfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = ColorPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorOnSurface)
        }
    }
}
