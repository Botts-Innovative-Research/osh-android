package org.sensorhub.android.ui.screens.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sensorhub.android.server.ServerProfileRepository

class ServerProfilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServerProfileRepository.getInstance(application)
    private val _profiles = MutableStateFlow<List<ServerProfileItem>>(emptyList())
    val profiles: StateFlow<List<ServerProfileItem>> = _profiles.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _profiles.value = repo.all.map { p ->
            ServerProfileItem(
                id = p.id,
                name = p.name,
                host = p.host,
                port = p.port,
                endpointPath = p.endpointPath,
                username = p.username,
                enableTls = p.enableTls,
                disableSslCheck = p.disableSslCheck,
                useConSysClient = p.useConSysClient,
                oAuthEnabled = p.oAuthEnabled,
                enabled = p.enabled
            )
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        repo.setEnabled(id, enabled)
        refresh()
    }

    fun delete(id: String) {
        repo.delete(id)
        refresh()
    }
}
