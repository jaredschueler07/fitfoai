package com.runningcoach.v2.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.runningcoach.v2.presentation.components.icons.*
import com.runningcoach.v2.presentation.navigation.Screen
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun AppBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = AppColors.Surface,
        contentColor = AppColors.OnSurface
    ) {
        // Home Tab
        NavigationBarItem(
            icon = { 
                HomeIcon(
                    tint = if (currentRoute == Screen.Dashboard.route) AppColors.Primary else AppColors.Neutral500
                )
            },
            label = { 
                Text(
                    "Home",
                    color = if (currentRoute == Screen.Dashboard.route) AppColors.Primary else AppColors.Neutral500
                ) 
            },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = {
                if (currentRoute != Screen.Dashboard.route) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppColors.Primary,
                selectedTextColor = AppColors.Primary,
                unselectedIconColor = AppColors.Neutral500,
                unselectedTextColor = AppColors.Neutral500,
                indicatorColor = AppColors.Primary.copy(alpha = 0.1f)
            )
        )

        // AI Coach Tab
        NavigationBarItem(
            icon = { 
                ChatIcon(
                    tint = if (currentRoute == Screen.AICoach.route) AppColors.Primary else AppColors.Neutral500
                )
            },
            label = { 
                Text(
                    "AI Coach",
                    color = if (currentRoute == Screen.AICoach.route) AppColors.Primary else AppColors.Neutral500
                ) 
            },
            selected = currentRoute == Screen.AICoach.route,
            onClick = {
                if (currentRoute != Screen.AICoach.route) {
                    navController.navigate(Screen.AICoach.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppColors.Primary,
                selectedTextColor = AppColors.Primary,
                unselectedIconColor = AppColors.Neutral500,
                unselectedTextColor = AppColors.Neutral500,
                indicatorColor = AppColors.Primary.copy(alpha = 0.1f)
            )
        )

        // Progress Tab
        NavigationBarItem(
            icon = { 
                ChartIcon(
                    tint = if (currentRoute == Screen.Progress.route) AppColors.Primary else AppColors.Neutral500
                )
            },
            label = { 
                Text(
                    "Progress",
                    color = if (currentRoute == Screen.Progress.route) AppColors.Primary else AppColors.Neutral500
                ) 
            },
            selected = currentRoute == Screen.Progress.route,
            onClick = {
                if (currentRoute != Screen.Progress.route) {
                    navController.navigate(Screen.Progress.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppColors.Primary,
                selectedTextColor = AppColors.Primary,
                unselectedIconColor = AppColors.Neutral500,
                unselectedTextColor = AppColors.Neutral500,
                indicatorColor = AppColors.Primary.copy(alpha = 0.1f)
            )
        )

        // Profile Tab
        NavigationBarItem(
            icon = { 
                ProfileIcon(
                    tint = if (currentRoute == Screen.Profile.route) AppColors.Primary else AppColors.Neutral500
                )
            },
            label = { 
                Text(
                    "Profile",
                    color = if (currentRoute == Screen.Profile.route) AppColors.Primary else AppColors.Neutral500
                ) 
            },
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                if (currentRoute != Screen.Profile.route) {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppColors.Primary,
                selectedTextColor = AppColors.Primary,
                unselectedIconColor = AppColors.Neutral500,
                unselectedTextColor = AppColors.Neutral500,
                indicatorColor = AppColors.Primary.copy(alpha = 0.1f)
            )
        )
    }
}
