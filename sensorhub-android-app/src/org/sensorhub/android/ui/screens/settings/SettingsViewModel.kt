package org.sensorhub.android.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sensorhub.android.data.settings.LocalServiceSettings

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val _state = MutableStateFlow(LocalServiceSettings.read(prefs))
    val state: StateFlow<LocalServiceSettings> = _state.asStateFlow()

    fun setSosEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(LocalServiceSettings.SOS_ENABLED, enabled).apply()
        _state.value = _state.value.copy(sosEnabled = enabled)
    }

    fun setCsApiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(LocalServiceSettings.CS_API_ENABLED, enabled).apply()
        _state.value = _state.value.copy(csApiEnabled = enabled)
    }

    fun setDiscoveryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(LocalServiceSettings.DISCOVERY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(discoveryEnabled = enabled)
    }

    fun setDiscoveryRulesLink(value: String) {
        prefs.edit().putString(LocalServiceSettings.RULES_LINK, value).apply()
        _state.value = _state.value.copy(rulesLink = value)
    }
}
