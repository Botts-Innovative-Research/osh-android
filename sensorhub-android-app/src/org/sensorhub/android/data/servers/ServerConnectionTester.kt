package org.sensorhub.android.data.servers

import org.sensorhub.android.R
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Base64

internal fun serverUrlError(value: String): Int? {
    val uri = try { URI(value.trim()) } catch (_: Exception) { return R.string.ui_enter_a_valid_http_or_https_url }
    return when {
        value.isBlank() -> R.string.ui_enter_a_valid_url
        uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrBlank() -> R.string.ui_enter_a_valid_http_or_https_url
        uri.port != -1 && uri.port !in 1..65535 -> R.string.ui_port_must_be_between_1_and_65535
        uri.rawAuthority?.endsWith(":") == true -> R.string.ui_enter_a_valid_port
        else -> null
    }
}

internal data class ConnectionTestResult(val message: String, val successful: Boolean)

internal class ServerConnectionTester(private val text: (Int, Array<out Any>) -> String) {
    private fun message(id: Int, vararg args: Any) = text(id, args)

    fun testResult(profile: ServerProfileItem): ConnectionTestResult {
        successful = false
        val result = test(profile)
        return ConnectionTestResult(result, successful)
    }

    private var successful = false
    private fun test(profile: ServerProfileItem): String {
        var stage = message(R.string.ui_server)
        return try {
            val authorization = if (profile.enableOAuth) {
                stage = message(R.string.ui_oauth_token_endpoint)
                "Bearer ${requestToken(profile)}"
            } else if (profile.username.isNotBlank()) {
                val encoded = Base64.getEncoder().encodeToString("${profile.username.trim()}:${profile.password}".toByteArray(Charsets.UTF_8))
                "Basic $encoded"
            } else null
            stage = message(R.string.ui_server)

            val url = URL(profile.endpointUrl.trim())
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = false
            }
            try {
                authorization?.let { connection.setRequestProperty("Authorization", it) }
                when (val code = connection.responseCode) {
                    in 200..299 -> { successful = true; message(R.string.ui_connected) }
                    401 -> message(R.string.ui_authentication_401)
                    403 -> message(R.string.ui_access_denied_403)
                    in 300..399 -> message(R.string.ui_server_redirected, code)
                    else -> message(R.string.ui_server_returned, code)
                }
            } finally { connection.disconnect() }
        } catch (e: TokenFailure) {
            e.message ?: message(R.string.ui_unable_to_obtain_oauth_token)
        } catch (_: SocketTimeoutException) {
            message(R.string.ui_connection_timed_out, stage)
        } catch (_: Exception) {
            message(R.string.ui_unable_to_connect, stage)
        }
    }


    private fun requestToken(profile: ServerProfileItem): String {
        val url = URL(profile.tokenEndpoint.trim())
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            instanceFollowRedirects = false
        }
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Accept", "application/json")
            val body = listOf("grant_type" to "client_credentials", "client_id" to profile.clientId.trim(), "client_secret" to profile.clientSecret)
                .joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) throw TokenFailure(message(R.string.ui_oauth_token_request_failed, code))
            return try {
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    val buffer = CharArray(65537)
                    var length = 0
                    while (length < buffer.size) {
                        val count = it.read(buffer, length, buffer.size - length)
                        if (count < 0) break
                        length += count
                    }
                    val text = String(buffer, 0, length)
                    if (text.length > 65536) throw IllegalArgumentException()
                    JsonParser.parseString(text).asJsonObject
                }
                val token = response.get("access_token")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                val type = response.get("token_type")?.asString
                if (token.isNullOrBlank() || token.any { it.isWhitespace() || it.code < 32 || it.code == 127 } ||
                    (type != null && !type.equals("Bearer", true))) throw IllegalArgumentException()
                token
            } catch (_: Exception) { throw TokenFailure(message(R.string.ui_oauth_invalid_bearer)) }
        } finally { connection.disconnect() }
    }

    private class TokenFailure(message: String) : Exception(message)
}
