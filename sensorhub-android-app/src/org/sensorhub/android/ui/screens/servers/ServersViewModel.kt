package org.sensorhub.android.ui.screens.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServersViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val _state = MutableStateFlow(ServersState())
    val state: StateFlow<ServersState> = _state.asStateFlow()

    init {
        _state.value = ServersState(
            sosEnabled = prefs.getBoolean("sos_service", true),
            csApiEnabled = prefs.getBoolean("csapi_service", true),
            discoveryEnabled = prefs.getBoolean("discovery_service", false)
        )
    }

    fun setSosEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sos_service", enabled).apply()
        _state.value = _state.value.copy(sosEnabled = enabled)
    }

    fun setCsApiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("csapi_service", enabled).apply()
        _state.value = _state.value.copy(csApiEnabled = enabled)
    }

    fun setDiscoveryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("discovery_service", enabled).apply()
        _state.value = _state.value.copy(discoveryEnabled = enabled)
    }
}
