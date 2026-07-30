package org.sensorhub.android.ui.screens.profiles

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class ServerFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ServerProfileRepository.getInstance(application)

    var state by mutableStateOf(ServerProfileItem())
        private set

    var portText by mutableStateOf("")
        private set

    var nameError by mutableStateOf<String?>(null)
        private set
    var hostError by mutableStateOf<String?>(null)
        private set
    var portError by mutableStateOf<String?>(null)
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
        portText = profile.port.toString()
    }

    fun updateServerName(value: String) {
        state = state.copy(serverName = value)
        nameError = null
    }

    fun updateHost(value: String) {
        state = state.copy(host = value)
        hostError = null
    }

    fun updatePort(value: String) {
        portText = value
        portError = null
    }

    fun updateEndpointPath(value: String) {
        state = state.copy(endpointPath = value)
    }

    fun updateUsername(value: String) {
        state = state.copy(username = value)
    }

    fun updatePassword(value: String) {
        state = state.copy(password = value)
    }

    fun updateEnableTls(value: Boolean) {
        state = state.copy(enableTls = value, disableSslCheck = if (!value) false else state.disableSslCheck)
    }

    fun updateDisableSslCheck(value: Boolean) {
        state = state.copy(disableSslCheck = value)
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

    fun updateUseConSysClient(useConSys: Boolean) {
        val path = if (useConSys) "/sensorhub/api" else "/sensorhub/sos"
        state = state.copy(useConSysClient = useConSys, endpointPath = path)
    }

    fun validate(): Boolean {
        var valid = true

        if (state.serverName.isBlank()) { valid = false; } else null

        if (state.host.isBlank()) {
            valid = false;
        } else if (state.host.contains(" ") || state.host.contains("://")) {
            valid = false;
        } else null

        if (portText.isBlank()) {
            valid = false;
        } else {
            val portNum = portText.toIntOrNull()
            if (portNum == null) {
                valid = false;
            }
            else if (portNum < 1 || portNum > 65535) {
                valid = false;
            }
            else null
        }

        return valid
    }

    fun saveProfile(): Boolean {
        if (!validate()) return false

        var ep = state.endpointPath.trim()
        if (ep.isNotEmpty() && !ep.startsWith("/")) ep = "/$ep"

        val profile = state.copy(
            id = existingProfileId ?: state.id,
            serverName = state.serverName.trim(),
            host = state.host.trim(),
            port = portText.toInt(),
            endpointPath = ep,
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
}
