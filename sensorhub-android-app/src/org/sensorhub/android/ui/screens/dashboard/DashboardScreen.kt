package org.sensorhub.android.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHFilterChip
import org.sensorhub.android.ui.components.OSHStatusRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.screens.sensors.ALL_SENSORS
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.api.module.ModuleEvent.ModuleState

@Composable
fun DashboardRoute(onNavigateToPreferences: () -> Unit, viewModel: DashboardViewModel = viewModel()) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by produceState(viewModel.uiState.value, viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { value = it }
        }
    }
    val context = LocalContext.current
    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)

        }.toTypedArray()
    }
    fun permissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permissionsGranted()) viewModel.startHub()
        else viewModel.reportError(context.getString(R.string.ui_grant_location_camera_microphone_and_bluetooth_permissions_to_connect_to_smart_hub))
    }
    LaunchedEffect(Unit) { if (permissionsGranted()) viewModel.connect() }
    DashboardScreen(
        state = state,
        onNavigateToPreferences = onNavigateToPreferences,
        onRunNameChange = viewModel::setRunName,
        onStart = { if (permissionsGranted()) viewModel.startHub() else launcher.launch(permissions) },
        onStop = viewModel::stopHub,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToPreferences: () -> Unit,
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
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var expandedSensorIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val categoriesById = remember { ALL_SENSORS.associate { it.id to it.category.name } }
    val filteredCards = state.sensorCards.filter { sensor ->
        when (selectedFilter) {
            "ALL" -> true
            "ATTENTION" -> sensor.status != ReadingStatus.LIVE
            else -> categoriesById[sensor.id] == selectedFilter
        }
    }

    val scrollState = rememberScrollState()

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
            IconButton(onClick = onNavigateToPreferences) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.app_preferences))
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
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        EnabledSensorCategory.entries.forEach { category ->
                            OSHFilterChip(
                                onClick = { selectedFilter = category.name },
                                text = stringResource(category.labelRes),
                                selected = selectedFilter == category.name
                            )
                        }
                    }
                }
                items(filteredCards, key = { it.id }) { sensor ->
                    SensorOutputCard(
                        sensor,
                        expanded = sensor.id in expandedSensorIds,
                        onExpandChange = { expanded ->
                            expandedSensorIds = ArrayList(if (expanded)
                                (expandedSensorIds + sensor.id).distinct()
                            else expandedSensorIds - sensor.id)
                        }
                    )
                }
                if (filteredCards.isEmpty()) {
                    item { Text(stringResource(if (state.sensorCards.isEmpty()) R.string.ui_enable_sensors_on_the_sensors_screen_to_see_their_readings else R.string.dashboard_no_matching_sensors), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp)) }
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
            DashboardUiState(
                hubStatus = ModuleState.STOPPED,
                runName = "Field survey",
            ),
            onNavigateToPreferences = {},
            onRunNameChange = {},
            onStart = {},
            onStop = {},
        )
    }
}
