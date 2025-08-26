package com.runningcoach.v2.presentation.navigation

// Screen routes based on wireframe flow
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object ConnectApps : Screen("connect_apps") 
    object PersonalizeProfile : Screen("personalize_profile")
    object SetEventGoal : Screen("set_event_goal")
    object Dashboard : Screen("dashboard")
    object AICoach : Screen("ai_coach")
    object Progress : Screen("progress")
    object Profile : Screen("profile")
    
    // Bottom navigation items
    companion object {
        fun getBottomNavItems() = listOf(
            BottomNavItem(
                route = "dashboard",
                label = "Home",
                icon = "home"
            ),
            BottomNavItem(
                route = "ai_coach",
                label = "AI Coach", 
                icon = "chat"
            ),
            BottomNavItem(
                route = "progress",
                label = "Progress",
                icon = "chart"
            ),
            BottomNavItem(
                route = "profile",
                label = "Profile",
                icon = "profile"
            )
        )
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: String
)
