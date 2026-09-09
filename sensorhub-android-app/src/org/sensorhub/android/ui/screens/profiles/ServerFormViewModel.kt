package org.sensorhub.android.ui.screens.profiles

import org.sensorhub.android.R
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class ServerFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ServerProfileRepository.getInstance(application)

    var state by mutableStateOf(ServerProfileItem())
        private set

    var nameError by mutableStateOf<String?>(null)
        private set
    var endpointUrlError by mutableStateOf<String?>(null)
        private set

    var tokenEndpointError by mutableStateOf<String?>(null)
        private set
    var clientIdError by mutableStateOf<String?>(null)
        private set
    var clientSecretError by mutableStateOf<String?>(null)
        private set
    var usernameError by mutableStateOf<String?>(null)
        private set

    var connectionTestResult by mutableStateOf<String?>(null)
        private set
    var connectionTestSuccessful by mutableStateOf(false)
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
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateEndpointUrl(value: String) {
        state = state.copy(endpointUrl = value)
        endpointUrlError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateUsername(value: String) {
        state = state.copy(username = value)
        usernameError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updatePassword(value: String) {
        state = state.copy(password = value)
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateEnableOAuth(value: Boolean) {
        state = state.copy(enableOAuth = value)
        tokenEndpointError = null
        clientIdError = null
        clientSecretError = null
        usernameError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateClientId(value: String) {
        state = state.copy(clientId = value)
        clientIdError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateClientSecret(value: String) {
        state = state.copy(clientSecret = value)
        clientSecretError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun updateTokenEndpoint(value: String) {
        state = state.copy(tokenEndpoint = value)
        tokenEndpointError = null
        connectionTestResult = null
        connectionTestSuccessful = false
    }

    fun validate(requireName: Boolean = true): Boolean {
        nameError = if (requireName && state.serverName.isBlank()) getApplication<Application>().getString(R.string.ui_enter_a_server_name) else null
        endpointUrlError = serverUrlError(state.endpointUrl)?.let { getApplication<Application>().getString(it) }
        tokenEndpointError = if (state.enableOAuth) serverUrlError(state.tokenEndpoint)?.let { getApplication<Application>().getString(it) } else null
        clientIdError = if (state.enableOAuth && state.clientId.isBlank()) getApplication<Application>().getString(R.string.ui_enter_a_client_id) else null
        clientSecretError = if (state.enableOAuth && state.clientSecret.isBlank()) getApplication<Application>().getString(R.string.ui_enter_a_client_secret) else null
        usernameError = if (!state.enableOAuth && (state.username.contains(':') ||
            (state.username.isBlank() && state.password.isNotEmpty()))) getApplication<Application>().getString(R.string.ui_enter_a_username_without_a_colon) else null
        return listOf(nameError, endpointUrlError, tokenEndpointError, clientIdError, clientSecretError, usernameError).all { it == null }
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

        val pwd = if (!state.enableOAuth) state.password else ""
        repo.setPassword(profile.id, pwd)

        if (state.enableOAuth) {
            repo.setOAuthClientId(profile.id, state.clientId.trim())
            repo.setOAuthClientSecret(profile.id, state.clientSecret)
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
