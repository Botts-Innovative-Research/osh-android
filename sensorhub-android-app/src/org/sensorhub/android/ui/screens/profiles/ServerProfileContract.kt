package org.sensorhub.android.ui.screens.profiles

import org.json.JSONObject
import java.util.UUID

data class ServerProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val serverName: String = "New Local Server",
    val endpointUrl: String = "",
    val username: String = "",
    val password: String = "",
    val useConSysClient: Boolean = true,
    val enableOAuth: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = "",
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("serverName", serverName)
        put("endpointUrl", endpointUrl)
        put("username", username)
        put("useConSysClient", useConSysClient)
        put("enableOAuth", enableOAuth)
        put("enabled", enabled)
    }

    fun withEnabled(value: Boolean): ServerProfileItem = copy(enabled = value)

    companion object {
        fun fromJson(obj: JSONObject): ServerProfileItem = ServerProfileItem(
            id = obj.getString("id"),
            serverName = obj.optString("serverName", ""),
            endpointUrl = obj.optString("endpointUrl", ""),
            username = obj.optString("username", ""),
            useConSysClient = obj.optBoolean("useConSysClient", true),
            enableOAuth = obj.optBoolean("enableOAuth", false),
            enabled = obj.optBoolean("enabled", true),
        )
    }
}
