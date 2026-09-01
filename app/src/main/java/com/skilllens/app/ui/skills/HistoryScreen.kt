package com.skilllens.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skilllens.app.ui.theme.*

@Composable
fun HistoryScreen(
    onSessionTap: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ext = SkillLensThemeTokens.colors

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
            Text("Practice History", style = MaterialTheme.typography.titleLarge, color = ColorOnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        // Empty state for MVP
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Filled.History, null, tint = ext.textDisabled, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("No sessions yet", style = MaterialTheme.typography.headlineSmall, color = ColorOnBackground)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Complete a practice session to see your history here.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = ext.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
