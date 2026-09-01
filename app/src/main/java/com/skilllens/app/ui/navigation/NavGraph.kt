package com.skilllens.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skilllens.app.ui.home.HomeScreen
import com.skilllens.app.ui.practice.CameraSetupScreen
import com.skilllens.app.ui.practice.LivePracticeScreen
import com.skilllens.app.ui.results.ResultScreen
import com.skilllens.app.ui.results.SessionDetailScreen
import com.skilllens.app.ui.settings.AboutScreen
import com.skilllens.app.ui.settings.SettingsScreen
import com.skilllens.app.ui.skills.HistoryScreen
import com.skilllens.app.ui.skills.SkillSelectScreen
import com.skilllens.app.ui.skills.TaskOverviewScreen

private const val ANIM_DURATION = 300

private fun enterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))

private fun exitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))

private fun popEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))

private fun popExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))

@Composable
fun SkillLensNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController   = navController,
        startDestination = Screen.Home.route,
        enterTransition  = { enterTransition() },
        exitTransition   = { exitTransition() },
        popEnterTransition  = { popEnterTransition() },
        popExitTransition   = { popExitTransition() },
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartPractice = { navController.navigate(Screen.SkillSelect.route) },
                onHistory       = { navController.navigate(Screen.History.route) },
                onSettings      = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.SkillSelect.route) {
            SkillSelectScreen(
                onSkillSelected = { skillId ->
                    navController.navigate(Screen.TaskOverview.createRoute(skillId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TaskOverview.route,
            arguments = listOf(navArgument("skillId") { type = NavType.StringType })
        ) { backStack ->
            val skillId = backStack.arguments?.getString("skillId") ?: return@composable
            TaskOverviewScreen(
                skillId = skillId,
                onStart = { navController.navigate(Screen.CameraSetup.createRoute(skillId)) },
                onBack  = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.CameraSetup.route,
            arguments = listOf(navArgument("skillId") { type = NavType.StringType })
        ) { backStack ->
            val skillId = backStack.arguments?.getString("skillId") ?: return@composable
            CameraSetupScreen(
                skillId    = skillId,
                onReady    = { navController.navigate(Screen.LivePractice.createRoute(skillId)) {
                    popUpTo(Screen.CameraSetup.createRoute(skillId)) { inclusive = true }
                }},
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.LivePractice.route,
            arguments = listOf(navArgument("skillId") { type = NavType.StringType })
        ) { backStack ->
            val skillId = backStack.arguments?.getString("skillId") ?: return@composable
            LivePracticeScreen(
                skillId = skillId,
                onSessionComplete = { sessionId ->
                    navController.navigate(Screen.Result.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
            ResultScreen(
                sessionId   = sessionId,
                onRetry     = { skillId ->
                    navController.navigate(Screen.CameraSetup.createRoute(skillId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onGoHome    = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onHistory   = { navController.navigate(Screen.History.route) },
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onSessionTap = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack    = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack  = { navController.popBackStack() },
                onAbout = { navController.navigate(Screen.About.route) },
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
