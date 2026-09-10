package org.sensorhub.android.ui.screens.dashboard

import org.sensorhub.android.R
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.sensorhub.api.module.ModuleEvent.ModuleState
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.config.SensorHubConfigFactory
import org.sensorhub.android.data.servers.ServerProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val profiles = ServerProfileRepository.getInstance(application)
    private val sensorCardReader = SensorCardReader(application, prefs)
    private val factory = SensorHubConfigFactory(application, profiles)
    private val _uiState = MutableStateFlow(DashboardUiState(
        runName = getApplication<Application>().getString(R.string.ui_run_1_s, SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()))
    ))
    val uiState = _uiState.asStateFlow()
    private var service: SensorHubService? = null
    private var bound = false
    private var preparing = false
    private var startWhenConnected = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as SensorHubService.LocalBinder).service
            _uiState.update { it.copy(serviceConnected = true, connecting = false, error = null) }
            refresh()
            if (startWhenConnected) {
                startWhenConnected = false
                startHub()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _uiState.update { it.copy(serviceConnected = false, connecting = false) }
            reportError(getApplication<Application>().getString(R.string.ui_connection_to_smart_hub_was_lost))
        }
    }

    init {
        // Binding is deferred until runtime permissions have been granted.
        viewModelScope.launch {
            while (isActive) {
                try {
                    refresh()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.ui_unable_to_read_current_sensor_status)) }
                }
                delay(250)
            }
        }
    }

    fun connect() {
        if (service != null) {
            _uiState.update { it.copy(error = null) }
            refresh()
            return
        }
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        _uiState.update { it.copy(connecting = true, error = null) }
        try {
            val app = getApplication<Application>()
            bound = app.bindService(Intent(app, SensorHubService::class.java), connection, Context.BIND_AUTO_CREATE)
            if (!bound) reportError(getApplication<Application>().getString(R.string.ui_unable_to_bind_to_smart_hub))
        } catch (e: Exception) {
            reportError(e.message ?: getApplication<Application>().getString(R.string.ui_unable_to_connect_to_smart_hub))
        }
    }

    fun setRunName(value: String) { _uiState.update { it.copy(runName = value) } }

    fun reportError(message: String) {
        startWhenConnected = false
        _uiState.update { it.copy(error = message, connecting = false) }
    }

    fun startHub() {
        val current = service ?: run {
            if (!startWhenConnected) {
                startWhenConnected = true
                connect()
            }
            return
        }
        if (preparing || current.hubState !in listOf(SensorHubService.HubState.STOPPED, SensorHubService.HubState.ERROR)) return
        preparing = true
        _uiState.update { it.copy(hubStatus = ModuleState.STARTING, error = null) }
        val runName = uiState.value.runName.trim()
        viewModelScope.launch {
            try {
                val config = withContext(Dispatchers.IO) { factory.create(prefs, runName) }
                val app = getApplication<Application>()
                app.startService(Intent(app, SensorHubService::class.java))
                current.startSensorHub(config.modules, config.cameraEnabled)
            } catch (e: Exception) {
                reportError(e.message ?: getApplication<Application>().getString(R.string.ui_unable_to_start_smart_hub))
            } finally {
                preparing = false
                refresh()
            }
        }
    }

    fun stopHub() {
        if (uiState.value.hubStatus != ModuleState.STARTED) return
        _uiState.update { it.copy(hubStatus = ModuleState.STOPPING) }
        try {
            service?.stopSensorHub()
        } catch (e: Exception) {
            reportError(e.message ?: getApplication<Application>().getString(R.string.ui_unable_to_stop_smart_hub))
            refresh()
        }
    }

    private fun refresh() {
        val current = service
        val status = if (preparing) ModuleState.STARTING else when (current?.hubState) {
            SensorHubService.HubState.STOPPED -> ModuleState.STOPPED
            SensorHubService.HubState.STARTING -> ModuleState.STARTING
            SensorHubService.HubState.RUNNING -> ModuleState.STARTED
            SensorHubService.HubState.STOPPING -> ModuleState.STOPPING
            SensorHubService.HubState.ERROR -> ModuleState.STOPPED
            else -> ModuleState.LOADED
        }
        val cards = sensorCardReader.read(current)
        _uiState.update {
            it.copy(
                sensorCards = cards,
                hubStatus = status,
                error = it.error ?: if (current?.hubState == SensorHubService.HubState.ERROR)
                    getApplication<Application>().getString(R.string.ui_unable_to_start_sensor_settings) else null
            )
        }
    }

    override fun onCleared() {
        if (bound) getApplication<Application>().unbindService(connection)
        super.onCleared()
    }
}
