package org.sensorhub.android.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard_screen")
    object Servers : Screen("servers_screen")
    object Sensors : Screen("sensors_screen")
}