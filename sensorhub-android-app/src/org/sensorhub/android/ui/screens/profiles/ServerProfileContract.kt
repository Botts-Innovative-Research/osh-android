package org.sensorhub.android.ui.screens.profiles

data class ServerProfileItem(
    val id: String = "",
    val serverName: String = "",
    val host: String = "",
    val port: String = "",
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
    val useConSysClient: Boolean = false,
    val enableOAuth: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = "",
    val enabled: Boolean = false
)
