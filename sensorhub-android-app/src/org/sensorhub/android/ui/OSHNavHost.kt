package org.sensorhub.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.sensorhub.android.ui.screens.HomeScreen
import org.sensorhub.android.ui.screens.SensorsScreen
import org.sensorhub.android.ui.screens.SettingsScreen

@Composable
fun OSHNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            HomeScreen()
        }
        composable(Screen.Sensors.route) {
            SensorsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}

