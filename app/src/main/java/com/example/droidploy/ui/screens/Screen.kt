package com.example.droidploy.ui.screens

sealed class Screen(val route: String) {

    data object Dashboard: Screen("dashboard_screen")

    data object Settings: Screen("settings_screen")
}