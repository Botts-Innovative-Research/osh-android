package org.sensorhub.android.ui.screens.sensors

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.navigation.Screen
import org.sensorhub.android.ui.components.OSHBluetoothPickerDialog
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHExpandableSwitchCard
import org.sensorhub.android.ui.components.OSHFilterChip
import org.sensorhub.android.ui.components.OSHSensorCard
import org.sensorhub.android.ui.components.OSHSingleChoiceDialog
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme


@Composable
fun SensorsScreen(
    onNavigateToPreferences : () -> Unit,
    viewModel: SensorsViewModel = viewModel(),
) {
    var selectedCategories by remember { mutableStateOf(SensorCategory.entries.toSet()) }
    val scrollState = rememberScrollState()
    var activeDialog by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scanner = remember { BluetoothScanner(context) }
    val bluetoothDevices = scanner.foundDevices

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            scanner.startDiscovery()
        }
    }

    DisposableEffect(Unit) {
        onDispose { scanner.stopDiscovery() }
    }

    if (activeDialog != null && activeDialog in BT_ADDRESS_PREF_KEYS) {
        val addressKey = activeDialog!!
        OSHBluetoothPickerDialog(
            devices = bluetoothDevices,
            currentAddress = viewModel.stringStates[addressKey] ?: "",
            onDeviceSelected = { address, _ ->
                viewModel.setStringPref(addressKey, address)
                activeDialog = null
            },
            onDismiss = {
                activeDialog = null
                scanner.stopDiscovery()
            },
            onStartScan = { permissionLauncher.launch(requiredPermissions) },
        )
    }

    CHOICE_DIALOGS.find { it.prefKey == activeDialog }?.let { spec ->
        val current = viewModel.stringStates[spec.prefKey] ?: spec.defaultValue
        OSHSingleChoiceDialog(
            onDismissRequest = { activeDialog = null },
            dialogTitle = stringResource(spec.titleRes),
            icon = spec.icon,
            options = spec.labels,
            selectedIndex = spec.values.indexOf(current).coerceAtLeast(0),
            onOptionSelected = { index ->
                viewModel.setStringPref(spec.prefKey, spec.values[index])
            },
        )
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_sensors),
                actions = {
                    IconButton(onClick = { onNavigateToPreferences }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        val resolvedItems = ALL_SENSORS.map {
            it.copy(enabled = viewModel.toggleStates[it.prefKey] ?: false)
        }
        val filteredItems = if (selectedCategories.isEmpty()) resolvedItems
        else resolvedItems.filter { it.category in selectedCategories }
        val grouped = filteredItems.groupBy { it.category }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SensorCategory.entries.forEach { category ->
                    OSHFilterChip(
                        onClick = {
                            selectedCategories = if (category in selectedCategories)
                                selectedCategories - category
                            else
                                selectedCategories + category
                        },
                        text = stringResource(category.labelRes),
                        selected = category in selectedCategories,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                grouped.forEach { (category, sensors) ->
                    item {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(sensors, key = { it.id }) { sensor ->
                        SensorListItem(
                            sensor = sensor,
                            viewModel = viewModel,
                            onActiveDialogChange = { activeDialog = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorListItem(
    sensor: SensorItem,
    viewModel: SensorsViewModel,
    onActiveDialogChange: (String) -> Unit,
) {
    val onToggle = { checked: Boolean ->
        viewModel.toggleSensor(sensor.prefKey, checked)
    }

    when (val config = sensor.config) {
        is SensorConfig.Audio -> ConfigurableSensorCard(sensor, onToggle) {
            AudioConfig(
                codec = viewModel.stringStates["audio_codec"] ?: "AAC",
                sampleRate = viewModel.stringStates["audio_samplerate"] ?: "8000",
                bitRate = viewModel.stringStates["audio_bitrate"] ?: "64",
                onCodecClick = { onActiveDialogChange("audio_codec") },
                onSampleRateClick = { onActiveDialogChange("audio_samplerate") },
                onBitRateClick = { onActiveDialogChange("audio_bitrate") },
            )
        }

        is SensorConfig.Video -> ConfigurableSensorCard(sensor, onToggle) {
            VideoConfig(
                codec = viewModel.stringStates["video_codec"] ?: "JPEG",
                frameRate = viewModel.stringStates["video_framerate"] ?: "30",
                resolution = viewModel.stringStates["video_resolution"] ?: "640x480",
                camera = viewModel.stringStates["camera_select"] ?: "0",
                onCodecClick = { onActiveDialogChange("video_codec") },
                onFrameRateClick = { onActiveDialogChange("video_framerate") },
                onResolutionClick = { onActiveDialogChange("video_resolution") },
                onCameraClick = { onActiveDialogChange("camera_select") },
            )
        }

        is SensorConfig.BluetoothDevice -> ConfigurableSensorCard(sensor, onToggle) {
            BluetoothDeviceConfig(
                selectLabelRes = config.selectLabelRes,
                currentAddress = viewModel.stringStates[config.addressPrefKey] ?: "",
                onSelect = { onActiveDialogChange(config.addressPrefKey) },
            )
        }

        is SensorConfig.TruPulse -> {
            val dsSpec = CHOICE_DIALOGS.first { it.prefKey == "trupulse_datasource" }
            val dsIndex = dsSpec.values.indexOf(
                viewModel.stringStates["trupulse_datasource"] ?: "STREAM",
            ).coerceAtLeast(0)

            ConfigurableSensorCard(sensor, onToggle) {
                TruPulseConfig(
                    datasource = dsSpec.labels[dsIndex],
                    deviceAddress = viewModel.stringStates["trupulse_device_address"] ?: "",
                    onDatasourceClick = { onActiveDialogChange("trupulse_datasource") },
                    onDeviceClick = { onActiveDialogChange("trupulse_device_address") },
                )
            }
        }

        null -> {
            OSHSensorCard {
                OSHSwitchRow(
                    title = stringResource(sensor.nameRes),
                    subtitle = stringResource(sensor.summaryRes),
                    checked = sensor.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

@Composable
private fun ConfigurableSensorCard(
    sensor: SensorItem,
    onToggle: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    OSHExpandableSwitchCard(
        title = stringResource(sensor.nameRes),
        subtitle = stringResource(sensor.summaryRes),
        checked = sensor.enabled,
        onCheckedChange = onToggle,
        configHint = stringResource(R.string.hint_tap_to_configure),
        expandedContent = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            expandedContent()
        },
    )
}

@Composable
private fun AudioConfig(
    codec: String,
    sampleRate: String,
    bitRate: String,
    onCodecClick: () -> Unit,
    onSampleRateClick: () -> Unit,
    onBitRateClick: () -> Unit,
) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_codec),
        subtitle = codec,
        imageVector = Icons.Default.Audiotrack,
        contentDescription = stringResource(R.string.title_codec),
        onClick = onCodecClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_sample_rate),
        subtitle = "$sampleRate Hz",
        imageVector = Icons.Default.Audiotrack,
        contentDescription = stringResource(R.string.title_sample_rate),
        onClick = onSampleRateClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_bitrate),
        subtitle = "$bitRate kbps",
        imageVector = Icons.Default.Audiotrack,
        contentDescription = stringResource(R.string.title_bitrate),
        onClick = onBitRateClick,
    )
}

@Composable
private fun VideoConfig(
    codec: String,
    frameRate: String,
    resolution: String,
    camera: String,
    onCodecClick: () -> Unit,
    onFrameRateClick: () -> Unit,
    onResolutionClick: () -> Unit,
    onCameraClick: () -> Unit,
) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_camera),
        subtitle = "Camera $camera",
        imageVector = Icons.Default.Videocam,
        contentDescription = stringResource(R.string.title_camera),
        onClick = onCameraClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_codec),
        subtitle = codec,
        imageVector = Icons.Default.Videocam,
        contentDescription = stringResource(R.string.title_codec),
        onClick = onCodecClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_resolution),
        subtitle = resolution,
        imageVector = Icons.Default.Videocam,
        contentDescription = stringResource(R.string.title_resolution),
        onClick = onResolutionClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_frame_rate),
        subtitle = "$frameRate fps",
        imageVector = Icons.Default.Videocam,
        contentDescription = stringResource(R.string.title_frame_rate),
        onClick = onFrameRateClick,
    )
}

@Composable
private fun BluetoothDeviceConfig(
    @StringRes selectLabelRes: Int,
    currentAddress: String,
    onSelect: () -> Unit,
) {
    OSHClickableRowWithIcon(
        title = stringResource(selectLabelRes),
        subtitle = currentAddress.ifEmpty { stringResource(R.string.summary_select_device) },
        imageVector = Icons.Default.Bluetooth,
        contentDescription = stringResource(selectLabelRes),
        onClick = onSelect,
    )
}

@Composable
private fun TruPulseConfig(
    datasource: String,
    deviceAddress: String,
    onDatasourceClick: () -> Unit,
    onDeviceClick: () -> Unit,
) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_trupulse_datasource),
        subtitle = datasource,
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_trupulse_datasource),
        onClick = onDatasourceClick,
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_select_trupulse),
        subtitle = deviceAddress.ifEmpty { stringResource(R.string.summary_select_device) },
        imageVector = Icons.Default.Bluetooth,
        contentDescription = stringResource(R.string.title_select_trupulse),
        onClick = onDeviceClick,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SensorsScreenPreview() {
    OSHTheme {
        SensorsScreen(
            {}
        )
    }
}
