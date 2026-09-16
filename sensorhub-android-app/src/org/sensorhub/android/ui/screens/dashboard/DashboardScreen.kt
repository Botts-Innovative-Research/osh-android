package org.sensorhub.android.ui.screens.dashboard

import org.sensorhub.android.ui.HubUiState

import org.sensorhub.android.ui.SensorHubViewModel

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.sensorhub.android.data.sensors.SensorRegistry
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import org.sensorhub.android.R
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHExpandableCard
import org.sensorhub.android.ui.components.OSHStatusRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Error
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OnSurfaceVariant
import org.sensorhub.android.ui.theme.Success
import org.sensorhub.api.module.ModuleEvent.ModuleState

@Composable
fun DashboardRoute(onNavigateToSettings: () -> Unit, viewModel: SensorHubViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by produceState(viewModel.state.value, viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { value = it }
        }
    }
    val context = LocalContext.current
    // Re-evaluate on each state update so changes made on the Sensors screen are
    // reflected when the user returns to Dashboard.
    val permissions = permissionsForEnabledSensors(context)
    fun permissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permissionsGranted()) viewModel.startHub()
        else viewModel.reportError(context.getString(R.string.ui_grant_required_sensor_permissions))
    }
    LaunchedEffect(Unit) {
        if (permissionsGranted()) viewModel.connect()
    }
    DashboardScreen(
        state = state,
        onNavigateToSettings = onNavigateToSettings,
        onRunNameChange = viewModel::setRunName,
        onStart = { if (permissionsGranted()) viewModel.startHub() else launcher.launch(permissions) },
        onStop = viewModel::stopHub,
    )
}

/** Returns only runtime permissions needed by the sensors enabled for the next run. */
private fun permissionsForEnabledSensors(context: android.content.Context): Array<String> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    return SensorRegistry.requiredPermissions(prefs, Build.VERSION.SDK_INT)
}

@Composable
fun DashboardScreen(
    state: HubUiState,
    onNavigateToSettings: () -> Unit,
    onRunNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    var showRunNameDialog by rememberSaveable { mutableStateOf(false) }
    var runNameDraft by rememberSaveable { mutableStateOf("") }
    val running = state.hubStatus == ModuleState.STARTED
    val busy = state.connecting || state.hubStatus in listOf(ModuleState.STARTING, ModuleState.STOPPING)
    val buttonText = when {
        state.connecting -> stringResource(R.string.ui_connecting)
        state.hubStatus == ModuleState.STARTING -> stringResource(R.string.ui_starting)
        state.hubStatus == ModuleState.STOPPING -> stringResource(R.string.ui_stopping)
        running -> stringResource(R.string.ui_stop_streaming)
        else -> stringResource(R.string.ui_start_streaming)
    }
    if (showRunNameDialog) {
        AlertDialog(
            onDismissRequest = { showRunNameDialog = false },
            title = { Text(stringResource(R.string.ui_start_streaming_title)) },
            text = {
                org.sensorhub.android.ui.components.OSHInputField(
                    value = runNameDraft,
                    onValueChange = { runNameDraft = it },
                    label = stringResource(R.string.ui_run_name)
                )
            },
            confirmButton = {
                OSHButton(text = stringResource(R.string.ui_start_streaming), enabled = runNameDraft.isNotBlank() && !busy, onClick = {
                    onRunNameChange(runNameDraft.trim())
                    showRunNameDialog = false
                    onStart()
                })
            },
            dismissButton = { TextButton(onClick = { showRunNameDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
    Scaffold(topBar = {
        OSHTopAppBarWithLogo(title = stringResource(R.string.app_name), actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
            }
        })
    },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OSHCard(modifier = Modifier.fillMaxWidth()) {
                    OSHStatusRow(
                        title = stringResource(R.string.ui_smarthub),
                        subtitle = state.runName.takeIf { running }.orEmpty(),
                        status = if (state.error != null) "error" else state.hubStatus.name,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OSHButton(
                    onClick = {
                        if (running) onStop() else {
                            runNameDraft = state.runName
                            showRunNameDialog = true
                        }
                    },
                    text = buttonText,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            if (running) {
                if (state.serverStatuses.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dashboard_server_status),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    items(state.serverStatuses, key = { it.clientId }) { server ->
                        ServerStatusCard(server)
                    }
                } else if (state.sensorCards.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dashboard_no_remote_clients),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard(server: ServerStatusUi) {
    val hasError = server.errorText != null
    val overallStatus = when {
        hasError -> "error"
        server.allOk -> "ok"
        else -> "nok"
    }
    val summary = stringResource(when {
        hasError -> R.string.destination_error
        server.moduleState != "STARTED" -> R.string.destination_not_started
        server.sensorGroups.isEmpty() -> R.string.destination_waiting
        server.allOk -> R.string.destination_active
        else -> R.string.destination_attention
    })
    OSHExpandableCard(
        title = server.serverName,
        status = overallStatus,
        modifier = Modifier.padding(horizontal = 12.dp),
        collapsedContent = {
            Text(
                text = "${server.clientMode}: $summary",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        },
        expandedContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                server.errorText?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                }
                server.statusMsg?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                server.sensorGroups.forEach { group ->
                    Text(
                        text = group.sensorName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (group.allOk) Success else Error
                    )
                    group.streams.forEach { stream ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stream.outputName,
                                Modifier.weight(1f),
                                color = OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stream.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stream.isOk) Success else Error
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CameraPreview() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        val shared = SensorHubService.getVideoTexture()
                        if (shared != null && !shared.isReleased && surfaceTexture !== shared) {
                            setSurfaceTexture(shared)
                            surface.release()
                        }
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                        surface !== SensorHubService.getVideoTexture()
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    OSHTheme {
        DashboardScreen(
            HubUiState(
                hubStatus = ModuleState.STOPPED,
                runName = "Field survey",
            ),
            onNavigateToSettings = {},
            onRunNameChange = {},
            onStart = {},
            onStop = {},
        )
    }
}
