package org.sensorhub.android.ui

sealed class Screen(val rout: String) {
    object Dashboard: Screen("dashboard_screen")
    object Settings: Screen("settings_screen")
    object Sensors: Screen("sensors_screen")
}