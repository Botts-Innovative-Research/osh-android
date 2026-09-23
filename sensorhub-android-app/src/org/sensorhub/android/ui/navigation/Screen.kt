package org.sensorhub.android.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard_screen")
    object Settings : Screen("settings_screen")
    object Map : Screen("map_screen")
    object Client : Screen("client_screen")
    object SystemDetail : Screen("system_detail_screen/{profileId}/{systemId}") {
        fun createRoute(profileId: String, systemId: String): String {
            return "system_detail_screen/${android.net.Uri.encode(profileId)}/${android.net.Uri.encode(systemId)}"
        }
    }
    object Sensors : Screen("sensors_screen")
    object ServerProfiles : Screen("server_profiles_screen")
    object HelpFaq : Screen("help_faq_screen")
    object AppPreferences : Screen("app_prefs_screen")
    object ServerForm : Screen("server_form_screen/{profileId}") {
        fun createRoute(profileId: String? = null): String {
            return "server_form_screen/${profileId ?: "new"}"
        }
    }
}
