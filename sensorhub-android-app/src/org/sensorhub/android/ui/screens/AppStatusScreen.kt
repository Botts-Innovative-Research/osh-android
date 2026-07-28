package org.sensorhub.android.ui.screens

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.sensorhub.android.R
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHStatusRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.api.module.ModuleConfig


data class AppStatusState (
    val httpStatus: String = "Unknown",
    val sosStatus: String = "Unknown",
    val conSysStatus: String = "Unknown",
    val discoveryStatus: String = "Unknown",
    val sensorStatus: String = "Unknown",
    val storageStatus: String = "Unknown",
)

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
@Composable
fun AppStatusScreen(
    onBackClick: () -> Unit,
    viewModel: AppStatusViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = stringResource(R.string.app_status_main_fragment),
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            OSHCard {
                OSHStatusRow(
                    title = stringResource(R.string.httpServerStatusLabel),
                    subtitle = state.httpStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.sosServiceStatusLabel),
                    subtitle = state.sosStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.conSysServiceStatusLabel),
                    subtitle = state.conSysStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.discoveryServiceStatusLabel),
                    subtitle = state.discoveryStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.sensorServiceStatusLabel),
                    subtitle = state.sensorStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.storageServiceStatusLabel),
                    subtitle = state.storageStatus
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppStatusScreenPreview() {
    OSHTheme {
        AppStatusScreen(
            onBackClick = {}
        )
    }
}