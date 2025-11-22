package com.example.droidploy.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.droidploy.ui.screens.DashboardScreen
import com.example.droidploy.ui.screens.Screen
import com.example.droidploy.ui.screens.SettingsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                paddingValues = paddingValues,
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                paddingValues = paddingValues,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
