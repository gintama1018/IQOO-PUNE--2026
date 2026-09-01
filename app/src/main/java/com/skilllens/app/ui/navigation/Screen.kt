package com.skilllens.app.ui.navigation

/**
 * All navigation destinations in SkillLens.
 * Single-Activity, Compose Navigation pattern.
 */
sealed class Screen(val route: String) {
    data object Home          : Screen("home")
    data object SkillSelect   : Screen("skill_select")
    data object TaskOverview  : Screen("task_overview/{skillId}") {
        fun createRoute(skillId: String) = "task_overview/$skillId"
    }
    data object CameraSetup   : Screen("camera_setup/{skillId}") {
        fun createRoute(skillId: String) = "camera_setup/$skillId"
    }
    data object LivePractice  : Screen("live_practice/{skillId}") {
        fun createRoute(skillId: String) = "live_practice/$skillId"
    }
    data object Result        : Screen("result/{sessionId}") {
        fun createRoute(sessionId: String) = "result/$sessionId"
    }
    data object History       : Screen("history")
    data object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "session_detail/$sessionId"
    }
    data object Settings      : Screen("settings")
    data object About         : Screen("about")
}
