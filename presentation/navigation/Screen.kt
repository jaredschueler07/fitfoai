package com.runningcoach.v2.presentation.navigation

sealed class Screen(val route: String) {
    object WelcomeScreen : Screen("welcome_screen")
    object ConnectAppsScreen : Screen("connect_apps_screen")
    object PersonalizeProfileScreen : Screen("personalize_profile_screen")
    object SetEventGoalScreen : Screen("set_event_goal_screen")
    object DashboardScreen : Screen("dashboard_screen")
    object AICoachScreen : Screen("ai_coach_screen")
    object ProgressScreen : Screen("progress_screen")
    object ProfileScreen : Screen("profile_screen")
    object RunTrackingScreen : Screen("run_tracking_screen") // Added based on existing files
}