package org.sensorhub.android.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard_screen")
    object Servers : Screen("servers_screen")
    object Sensors : Screen("sensors_screen")
    object ServerProfiles : Screen("server_profiles_screen")
    object AppStatus : Screen("app_status_screen")
    object HelpFaq : Screen("help_faq_screen")
}