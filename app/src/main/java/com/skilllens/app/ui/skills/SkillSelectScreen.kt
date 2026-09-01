package com.skilllens.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skilllens.app.taskengine.SkillDefinition
import com.skilllens.app.taskengine.SkillRepository
import com.skilllens.app.ui.theme.*

@Composable
fun SkillSelectScreen(
    onSkillSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val skills = remember { SkillRepository.getAllSkills() }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ColorOnBackground)
            }
            Text(
                "Choose Skill",
                style      = MaterialTheme.typography.titleLarge,
                color      = ColorOnBackground,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f),
            )
        }

        Text(
            "Select a practical skill to practice. The AI will observe and validate your work in real time.",
            style    = MaterialTheme.typography.bodySmall,
            color    = ColorTextMuted,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(skills) { skill ->
                SkillCard(skill = skill, onClick = { onSkillSelected(skill.id) })
            }

            // Future skills — clearly labelled as planned
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "COMING SOON",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = ColorTextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            items(futureSkills) { (name, icon) ->
                FutureSkillCard(name = name, iconDesc = icon)
            }
        }
    }
}

@Composable
private fun SkillCard(skill: SkillDefinition, onClick: () -> Unit) {
    val ext = SkillLensThemeTokens.colors
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = ColorSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ElectricalServices, null, tint = ColorPrimary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleMedium, color = ColorOnBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(skill.description, style = MaterialTheme.typography.bodySmall, color = ext.textMuted, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = ColorCorrect.copy(alpha = 0.15f)) {
                        Text(
                            " ${skill.steps.size} steps ",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorCorrect,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = ColorPrimary.copy(alpha = 0.15f)) {
                        Text(
                            " ~${skill.estimatedDurationMin} min ",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = ext.textMuted)
        }
    }
}

@Composable
private fun FutureSkillCard(name: String, iconDesc: String) {
    val ext = SkillLensThemeTokens.colors
    Card(
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = ColorSurfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ext.textDisabled.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, null, tint = ext.textDisabled, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = ext.textDisabled)
                Text("Future expansion", style = MaterialTheme.typography.labelSmall, color = ext.textDisabled)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = ext.textDisabled.copy(alpha = 0.15f)) {
                Text(
                    " PLANNED ",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textDisabled,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private val futureSkills = listOf(
    "Plumbing – Pipe Fitting" to "plumbing",
    "HVAC – Thermostat Wiring" to "hvac",
    "Automotive – Spark Plug" to "auto",
    "Manufacturing – Assembly" to "mfg",
)
