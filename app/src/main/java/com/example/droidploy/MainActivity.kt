package com.example.droidploy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.droidploy.ui.screens.DashboardScreen
import com.example.droidploy.ui.screens.Screen
import com.example.droidploy.ui.screens.SettingsScreen
import com.example.droidploy.ui.theme.DroidployTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroidployTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val navController = rememberNavController();

                    NavHost(navController = navController, startDestination = Screen.Dashboard.route){

                        composable(route = Screen.Dashboard.route){
                            DashboardScreen(paddingValues = innerPadding,
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                }
                                )
                        }

                        composable(route = Screen.Settings.route){
                            SettingsScreen(paddingValues = innerPadding)
                        }

                    }

                }
            }
        }
    }
}