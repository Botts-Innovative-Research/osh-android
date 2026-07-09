package org.sensorhub.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.sensorhub.android.ui.screens.HomeScreen
import org.sensorhub.android.ui.screens.SensorsScreen
import org.sensorhub.android.ui.screens.ServersScreen

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
            HomeScreen(navController = navController)
        }
        composable(Screen.Sensors.route) {
            SensorsScreen(navController = navController)
        }
        composable(Screen.Servers.route) {
            ServersScreen(navController = navController)
        }
    }
}

