package org.sensorhub.android.ui.screens.appstatus

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.sensorhub.android.SensorHubService
import org.sensorhub.api.module.ModuleConfig

class AppStatusViewModel(application: Application): AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppStatusState())
    val state: StateFlow<AppStatusState> = _state.asStateFlow()
    private var boundService: SensorHubService? = null

    private val sConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as SensorHubService.LocalBinder).service
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    init {
        val context = getApplication<Application>()
        context.bindService(
            Intent(context, SensorHubService::class.java),
            sConn,
            Context.BIND_AUTO_CREATE
        )
        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                refreshStatus()
            }
        }
    }

    private fun refreshStatus() {
        val service = boundService ?: return
        val sensorhub = service.sensorHub ?: return
        val modules = sensorhub.moduleRegistry.loadedModules

        var http = "Unknown"
        var sos = "Unknown"
        var conSys = "Unknown"
        var discovery = "Unknown"
        var sensor = "Unknown"
        var storage = "Unknown"

        for (module in modules) {
            val moduleConf = module.configuration
            if (moduleConf is ModuleConfig) {
                val status = module.currentState.name
                val moduleId = moduleConf.id

                when (moduleId) {
                    "HTTP_SERVER_0" -> http = status
                    "SOS_SERVICE" -> sos = status
                    "CON_SYS_SERVICE" -> conSys = status
                    "DISCOVERY_SERVICE" -> discovery = status
                    "ANDROID_SENSORS" -> sensor = status
                    "ANDROID_SENSORS#storage" -> storage = status
                }
            }
        }

        _state.value = AppStatusState(
            httpStatus = http,
            sosStatus = sos,
            conSysStatus = conSys,
            discoveryStatus = discovery,
            sensorStatus = sensor,
            storageStatus = storage
        )

    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(sConn)
        } catch (_: IllegalArgumentException) {

        }
        boundService = null
    }
}