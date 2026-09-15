package org.sensorhub.android.ui

import org.sensorhub.android.ui.screens.dashboard.SensorCardReader
import org.sensorhub.android.ui.screens.dashboard.ServerStatusReader
import org.sensorhub.android.ui.screens.dashboard.SensorCardUi
import org.sensorhub.android.ui.screens.dashboard.ServerStatusUi
import org.sensorhub.android.data.sensors.SensorRegistry
import org.sensorhub.android.data.sensors.SensorUiEntry
import org.sensorhub.android.data.settings.LocalServiceSettings
import org.sensorhub.android.config.DiscoveryRulesDownloader
import org.sensorhub.android.config.PreferenceSnapshot
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.sensorhub.android.data.sensors.SensorBinding
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig
import org.sensorhub.impl.sensor.controller.ControllerDriver

import org.sensorhub.android.R
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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

class SensorHubViewModel(private val application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val profiles = ServerProfileRepository.getInstance(application)
    private val sensorCardReader = SensorCardReader(application)
    private val serverStatusReader = ServerStatusReader(application)
    private val _uiState = MutableStateFlow(HubUiState(runName = application.getString(
        R.string.ui_run_1_s, SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    )))
    val state = _uiState.asStateFlow()
    private var activeSensors: List<SensorUiEntry> = emptyList()
    private var pendingRunName: String? = null
    private var foreground = false
    private var service: SensorHubService? = null
    private var bound = false
    private var preparing = false
    private var startWhenConnected = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as SensorHubService.LocalBinder).service
            _uiState.update { it.copy(serviceConnected = true, connecting = false, error = null) }
            if (activeSensors.isEmpty()) {
                val modules = service?.sensorHub?.moduleRegistry?.loadedModules.orEmpty()
                activeSensors = SensorRegistry.entries.filter { sensor ->
                    val module = modules.firstOrNull { it.localID == sensor.runtimeModuleId }
                    when (val runtime = sensor.runtime) {
                        is SensorBinding.Android -> (module?.configuration as? AndroidSensorsConfig)
                            ?.let(runtime.isEnabled) ?: false
                        is SensorBinding.Dedicated -> module != null
                    }
                }
            }
            refresh()
            if (startWhenConnected) {
                startWhenConnected = false
                startHub(pendingRunName.orEmpty())
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _uiState.update { it.copy(serviceConnected = false, connecting = false) }
            reportError(application.getString(R.string.ui_connection_to_smart_hub_was_lost))
        }
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                if (foreground && _uiState.subscriptionCount.value > 0) {
                    try {
                        refresh()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = application.getString(R.string.ui_unable_to_read_current_sensor_status)) }
                    }
                }
                delay(1000)
            }
        }
    }

    fun attach() {
        foreground = true
        connect()
    }

    fun detach() {
        foreground = false
        if (!preparing) disconnect()
    }

    private fun disconnect() {
        startWhenConnected = false
        if (bound) application.unbindService(connection)
        bound = false
        service = null
        _uiState.update { it.copy(serviceConnected = false, connecting = false) }
    }

    fun controller(): ControllerDriver? = try {
        service?.sensorHub?.moduleRegistry?.getModuleByType(ControllerDriver::class.java)
    } catch (_: Exception) { null }

    fun connect() {
        if (service != null) {
            _uiState.update { it.copy(error = null) }
            refresh()
            return
        }
        if (bound) return
        _uiState.update { it.copy(connecting = true, error = null) }
        try {
            val app = application
            bound = app.bindService(Intent(app, SensorHubService::class.java), connection, Context.BIND_AUTO_CREATE)
            if (!bound) reportError(application.getString(R.string.ui_unable_to_bind_to_smart_hub))
        } catch (e: Exception) {
            reportError(e.message ?: application.getString(R.string.ui_unable_to_connect_to_smart_hub))
        }
    }

    fun reportError(message: String) {
        startWhenConnected = false
        _uiState.update { it.copy(error = message, connecting = false) }
    }

    fun setRunName(value: String) { _uiState.update { it.copy(runName = value) } }

    fun startHub(runName: String = state.value.runName) {
        pendingRunName = runName
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
        val snapshot = PreferenceSnapshot(prefs)
        viewModelScope.launch {
            try {
                val config = withContext(Dispatchers.IO) {
                    val settings = LocalServiceSettings.read(snapshot)
                    val rules = if (settings.discoveryEnabled)
                        DiscoveryRulesDownloader().download(settings.rulesLink, application.filesDir) else null
                    SensorHubConfigFactory(application, profiles).create(snapshot, runName.trim(), rules?.absolutePath)
                }
                val app = application
                app.startService(Intent(app, SensorHubService::class.java))
                activeSensors = SensorRegistry.enabledSensorEntries(snapshot).toList()
                current.startSensorHub(config.modules, config.cameraEnabled)
            } catch (e: Exception) {
                reportError(e.message ?: application.getString(R.string.ui_unable_to_start_smart_hub))
            } finally {
                preparing = false
                refresh()
                if (!foreground) disconnect()
            }
        }
    }

    fun stopHub() {
        if (state.value.hubStatus != ModuleState.STARTED) return
        _uiState.update { it.copy(hubStatus = ModuleState.STOPPING) }
        try {
            service?.stopSensorHub()
        } catch (e: Exception) {
            reportError(e.message ?: application.getString(R.string.ui_unable_to_stop_smart_hub))
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
        val cards = sensorCardReader.read(current, activeSensors)
        val servers = serverStatusReader.read(current)
        _uiState.update {
            it.copy(
                sensorCards = cards,
                serverStatuses = servers,
                hubStatus = status,
                error = it.error ?: if (current?.hubState == SensorHubService.HubState.ERROR)
                    application.getString(R.string.ui_unable_to_start_sensor_settings) else null
            )
        }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}

data class HubUiState(
    val runName: String = "",
    val hubStatus: ModuleState = ModuleState.LOADED,
    val sensorCards: List<SensorCardUi> = emptyList(),
    val serverStatuses: List<ServerStatusUi> = emptyList(),
    val connecting: Boolean = false,
    val serviceConnected: Boolean = false,
    val error: String? = null,
)
