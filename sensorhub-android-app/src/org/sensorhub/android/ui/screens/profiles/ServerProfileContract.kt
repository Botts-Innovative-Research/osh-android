package org.sensorhub.android.ui.screens.profiles

import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.UUID

data class ServerProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val serverName: String = "Local Server",
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
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
        put("host", host)
        put("port", port)
        put("endpointPath", endpointPath)
        put("username", username)
        put("enableTls", enableTls)
        put("disableSslCheck", disableSslCheck)
        put("useConSysClient", useConSysClient)
        put("enableOAuth", enableOAuth)
        put("enabled", enabled)
    }

    fun buildClientUrl(): URL? {
        var cleanHost = host.replace("http://", "").replace("https://", "").trim()
        if (cleanHost.isEmpty()) cleanHost = "127.0.0.1"

        var path = endpointPath.trim()
        if (path.isNotEmpty() && !path.startsWith("/")) path = "/$path"

        val urlStr = "${if (enableTls) "https" else "http"}://$cleanHost:$port$path"
        return try { URI(urlStr).toURL() } catch (_: Exception) { null }
    }

    fun getDisplaySummary(): String =
        "${if (enableTls) "https" else "http"}://$host:$port$endpointPath"

    fun getClientModeLabel(): String =
        if (useConSysClient) "Connected Systems" else "SOS-T"

    fun withEnabled(value: Boolean): ServerProfileItem = copy(enabled = value)

    companion object {
        fun fromJson(obj: JSONObject): ServerProfileItem = ServerProfileItem(
            id = obj.getString("id"),
            serverName = obj.optString("serverName", ""),
            host = obj.optString("host", "127.0.0.1"),
            port = obj.optInt("port", 8080),
            endpointPath = obj.optString("endpointPath", "/sensorhub/api"),
            username = obj.optString("username", ""),
            enableTls = obj.optBoolean("enableTls", false),
            disableSslCheck = obj.optBoolean("disableSslCheck", false),
            useConSysClient = obj.optBoolean("useConSysClient", true),
            enableOAuth = obj.optBoolean("enableOAuth", false),
            enabled = obj.optBoolean("enabled", true),
        )
    }
}
