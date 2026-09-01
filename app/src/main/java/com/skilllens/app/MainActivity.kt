package com.skilllens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skilllens.app.ui.navigation.SkillLensNavGraph
import com.skilllens.app.ui.theme.SkillLensTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity entry-point for SkillLens.
 * All screens are Composable destinations managed by the NavGraph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before setContent
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SkillLensTheme {
                SkillLensNavGraph()
            }
        }
    }
}
