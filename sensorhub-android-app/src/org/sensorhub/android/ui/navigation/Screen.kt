package org.sensorhub.android.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard_screen")
    object Settings : Screen("settings_screen")
    object Map : Screen("map_screen")
    object Sensors : Screen("sensors_screen")
    object ServerProfiles : Screen("server_profiles_screen")
    object HelpFaq : Screen("help_faq_screen")
    object AppPreferences : Screen("app_prefs_screen")
    object Systems : Screen("systems_screen")
    object SystemsStreams : Screen("systems_screen/streams/{serverId}/{systemId}") {
        fun createRoute(serverId: String, systemId: String): String =
            "systems_screen/streams/${android.net.Uri.encode(serverId)}/${android.net.Uri.encode(systemId)}"
    }
    object DatastreamInfo : Screen("systems_screen/datastream/{serverId}/{datastreamId}") {
        fun createRoute(serverId: String, datastreamId: String): String =
            "systems_screen/datastream/${android.net.Uri.encode(serverId)}/${android.net.Uri.encode(datastreamId)}"
    }

    object ControlstreamInfo : Screen("systems_screen/controlstream/{serverId}/{controlstreamId}") {
        fun createRoute(serverId: String, controlstreamId: String): String =
            "systems_screen/controlstream/${android.net.Uri.encode(serverId)}/${android.net.Uri.encode(controlstreamId)}"
    }
    object ServerForm : Screen("server_form_screen/{profileId}") {
        fun createRoute(profileId: String? = null): String {
            return "server_form_screen/${profileId ?: "new"}"
        }
    }
}
