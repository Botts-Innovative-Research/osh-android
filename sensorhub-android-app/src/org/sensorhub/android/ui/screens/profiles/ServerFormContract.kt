package org.sensorhub.android.ui.screens.profiles

data class ServerFormState(
    val serverName: String = "",
    val host: String = "",
    val port: String = "",
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
    val enableOAuth: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = "",
    val clientTypeIndex: Int = 0,
    val nameError: String? = null,
    val hostError: String? = null,
    val portError: String? = null,
)
