package org.sensorhub.android.ui.screens.sensors

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager

class SensorsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val _toggleStates = mutableStateMapOf<String, Boolean>()
    val toggleStates: Map<String, Boolean> = _toggleStates

    private val _stringStates = mutableStateMapOf<String, String>()
    val stringStates: Map<String, String> = _stringStates

    init {
        ALL_SENSORS.forEach { sensor ->
            _toggleStates[sensor.prefKey] = prefs.getBoolean(sensor.prefKey, false)
        }

        CHOICE_DIALOGS.forEach { spec ->
            _stringStates[spec.prefKey] = prefs.getString(spec.prefKey, spec.defaultValue) ?: spec.defaultValue
        }

        BT_ADDRESS_PREF_KEYS.forEach { key ->
            _stringStates[key] = prefs.getString(key, "") ?: ""
        }
    }

    fun toggleSensor(prefKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(prefKey, enabled).apply()
        _toggleStates[prefKey] = enabled
    }

    fun setStringPref(prefKey: String, value: String) {
        prefs.edit().putString(prefKey, value).apply()
        _stringStates[prefKey] = value
    }
}
