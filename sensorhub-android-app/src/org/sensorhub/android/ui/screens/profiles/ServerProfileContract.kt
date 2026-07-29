package org.sensorhub.android.ui.screens.profiles

data class ServerProfileItem(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 8181,
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
    val useConSysClient: Boolean = false,
    val oAuthEnabled: Boolean = false,
    val enabled: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = ""
)
