package org.sensorhub.android.ui.screens.profiles

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sensorhub.android.R
import java.net.HttpURLConnection
import java.net.URI

class ServerFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ServerProfileRepository.getInstance(application)

    var state by mutableStateOf(ServerProfileItem())
        private set

    var nameError by mutableStateOf<String?>(null)
        private set
    var endpointUrlError by mutableStateOf<String?>(null)
        private set

    var connectionTestResult by mutableStateOf<String?>(null)
        private set
    var isTestingConnection by mutableStateOf(false)
        private set

    private var existingProfileId: String? = null
    var isEdit = false
        private set

    fun loadProfile(profileId: String?) {
        isEdit = profileId != null && profileId != "new"
        if (!isEdit) return

        val profile = repo.getById(profileId) ?: return
        existingProfileId = profile.id
        state = profile.copy(
            password = repo.getPassword(profile.id),
            clientId = repo.getOAuthClientId(profile.id),
            clientSecret = repo.getOAuthClientSecret(profile.id),
            tokenEndpoint = repo.getOAuthTokenEndpoint(profile.id),
        )
    }

    fun updateServerName(value: String) {
        state = state.copy(serverName = value)
        nameError = null
    }

    fun updateEndpointUrl(value: String) {
        state = state.copy(endpointUrl = value)
        endpointUrlError = null
    }

    fun updateUsername(value: String) {
        state = state.copy(username = value)
    }

    fun updatePassword(value: String) {
        state = state.copy(password = value)
    }

    fun updateEnableOAuth(value: Boolean) {
        state = state.copy(enableOAuth = value)
    }

    fun updateClientId(value: String) {
        state = state.copy(clientId = value)
    }

    fun updateClientSecret(value: String) {
        state = state.copy(clientSecret = value)
    }

    fun updateTokenEndpoint(value: String) {
        state = state.copy(tokenEndpoint = value)
    }

    fun validate(): Boolean {
        var valid = true
        nameError = null
        endpointUrlError = null
        val required = getApplication<Application>().getString(R.string.msg_name_host_port_required)

        if (state.serverName.isBlank()) {
            nameError = required
            valid = false
        }

        if (state.endpointUrl.isBlank()) {
            endpointUrlError = required
            valid = false
        }

        return valid
    }

    fun saveProfile(): Boolean {
        if (!validate()) return false

        val profile = state.copy(
            id = existingProfileId ?: state.id,
            serverName = state.serverName.trim(),
            endpointUrl = state.endpointUrl.trim(),
            username = if (!state.enableOAuth) state.username.trim() else "",
        )

        repo.save(profile)

        val pwd = if (!state.enableOAuth) state.password.trim() else ""
        repo.setPassword(profile.id, pwd)

        if (state.enableOAuth) {
            repo.setOAuthClientId(profile.id, state.clientId.trim())
            repo.setOAuthClientSecret(profile.id, state.clientSecret.trim())
            repo.setOAuthTokenEndpoint(profile.id, state.tokenEndpoint.trim())
        }

        return true
    }

    fun testConnection() {
        val testEp = state.endpointUrl.trim()
        if (testEp.isBlank()) {
            connectionTestResult = "Please enter a connection URL"
            return
        }

        isTestingConnection = true
        connectionTestResult = null

        viewModelScope.launch {
            connectionTestResult = withContext(Dispatchers.IO) {
                try {
                    val url = URI(testEp).toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    val user = state.username.trim()
                    val pwd = state.password.trim()
                    if (user.isNotEmpty() && pwd.isNotEmpty()) {
                        val credentials = android.util.Base64.encodeToString(
                            "$user:$pwd".toByteArray(),
                            android.util.Base64.NO_WRAP
                        )
                        conn.setRequestProperty("Authorization", "Basic $credentials")
                    }

                    try {
                        val code = conn.responseCode
                        when {
                            code in 200..299 -> "Connected"
                            code == 401 || code == 403 -> "Authentication failed"
                            else -> "Could not reach server: Server returned HTTP $code"
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    "Could not reach server: ${e.message ?: "Unable to reach the server"}"
                }
            }
            isTestingConnection = false
        }
    }
}
