package org.sensorhub.android.ui.screens

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHExpandableSwitchCard
import org.sensorhub.android.ui.components.OSHFilterChip
import org.sensorhub.android.ui.components.OSHSensorCard
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme

enum class SensorCategory(@StringRes val labelRes: Int) {
    ON_DEVICE(R.string.category_on_device),
    BLUETOOTH(R.string.category_bluetooth),
    USB(R.string.category_usb),
    OTHERS(R.string.category_others),
}

sealed class SensorConfig {
    data class BluetoothDevice(@StringRes val selectLabelRes: Int) : SensorConfig()

    data object Video : SensorConfig()

    data object Audio : SensorConfig()

    data object TruPulse : SensorConfig()
}

data class SensorItem(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val summaryRes: Int,
    val category: SensorCategory,
    val enabled: Boolean = false,
    val config: SensorConfig? = null,
)

val ALL_SENSORS: List<SensorItem> = listOf(
    // On device sensors
    SensorItem("accelerometer",  R.string.sensor_accelerometer,       R.string.summary_accel,       SensorCategory.ON_DEVICE),
    SensorItem("gyroscope",      R.string.sensor_gyroscope,           R.string.summary_gyro,        SensorCategory.ON_DEVICE),
    SensorItem("magnetometer",   R.string.sensor_magnetometer,        R.string.summary_mag,         SensorCategory.ON_DEVICE),
    SensorItem("orientation_q",  R.string.sensor_orientation_quat,    R.string.summary_orientation, SensorCategory.ON_DEVICE),
    SensorItem("orientation_e",  R.string.sensor_orientation_euler,   R.string.summary_orientation, SensorCategory.ON_DEVICE),
    SensorItem("gps",            R.string.sensor_gps,                 R.string.summary_gps,         SensorCategory.ON_DEVICE),
    SensorItem("network_loc",    R.string.sensor_network_location,    R.string.summary_netloc,      SensorCategory.ON_DEVICE),

    // On device sensors (have additional config)
    SensorItem("video",          R.string.sensor_video,               R.string.summary_video,       SensorCategory.ON_DEVICE, config = SensorConfig.Video),
    SensorItem("video_roll",     R.string.sensor_video_roll,          R.string.summary_video_roll,  SensorCategory.ON_DEVICE),
    SensorItem("audio",          R.string.sensor_audio,               R.string.summary_audio,       SensorCategory.ON_DEVICE, config = SensorConfig.Audio),

    // Bluetooth sensors (have additional config)
    SensorItem("meshtastic",     R.string.sensor_meshtastic,          R.string.summary_meshtastic,  SensorCategory.BLUETOOTH, config = SensorConfig.BluetoothDevice(R.string.title_select_meshtastic)),
    SensorItem("polar",          R.string.sensor_polar,               R.string.summary_polar,       SensorCategory.BLUETOOTH, config = SensorConfig.BluetoothDevice(R.string.title_select_polar)),
    SensorItem("kestrel",        R.string.sensor_kestrel,             R.string.summary_kestrel,     SensorCategory.BLUETOOTH, config = SensorConfig.BluetoothDevice(R.string.title_select_kestrel)),
    SensorItem("garmin",         R.string.sensor_garmin,              R.string.summary_garmin,      SensorCategory.BLUETOOTH, config = SensorConfig.BluetoothDevice(R.string.title_select_garmin)),
    SensorItem("ste",            R.string.sensor_ste,                 R.string.summary_ste,         SensorCategory.BLUETOOTH, config = SensorConfig.BluetoothDevice(R.string.title_select_device)),
    SensorItem("trupulse",       R.string.sensor_trupulse,            R.string.summary_trupulse,    SensorCategory.BLUETOOTH, config = SensorConfig.TruPulse),

    // USB sensors
    SensorItem("controller",     R.string.sensor_controller,          R.string.summary_controller,  SensorCategory.USB),
    SensorItem("flirone",        R.string.sensor_flirone,             R.string.summary_flirone,     SensorCategory.USB),

    // Others
    SensorItem("wardriving",     R.string.sensor_wardriving,          R.string.summary_wardriving,  SensorCategory.OTHERS),
)

@Composable
fun SensorsScreen(
    items: List<SensorItem> = ALL_SENSORS,
    onSensorToggled: (SensorItem, Boolean) -> Unit = { _, _ -> },
    onConfigAction: (SensorItem) -> Unit = {},
    navController: NavController = rememberNavController()
) {
    var selectedCategories by remember {
        mutableStateOf(SensorCategory.entries.toSet())
    }
    var sensorStates by remember {
        mutableStateOf(items.associate { it.id to it.enabled })
    }

    val scrollState = rememberScrollState()


    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_sensors),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AppPreferences.route) }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = ""
                        )
                    }
                },
            )
        },
    ) { padding ->
        val resolvedItems = items.map { it.copy(enabled = sensorStates[it.id] ?: it.enabled) }
        val filteredItems = if (selectedCategories.isEmpty()) resolvedItems
            else resolvedItems.filter { it.category in selectedCategories }
        val grouped = filteredItems.groupBy { it.category }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OSHFilterChip(
                    onClick = {
                        selectedCategories = if (SensorCategory.ON_DEVICE in selectedCategories)
                            selectedCategories - SensorCategory.ON_DEVICE
                        else
                            selectedCategories + SensorCategory.ON_DEVICE
                    },
                    text = stringResource(R.string.category_on_device),
                    selected = SensorCategory.ON_DEVICE in selectedCategories,
                )
                OSHFilterChip(
                    onClick = {
                        selectedCategories = if (SensorCategory.BLUETOOTH in selectedCategories)
                            selectedCategories - SensorCategory.BLUETOOTH
                        else
                            selectedCategories + SensorCategory.BLUETOOTH
                    },
                    text = stringResource(R.string.category_bluetooth),
                    selected = SensorCategory.BLUETOOTH in selectedCategories,
                )
                OSHFilterChip(
                    onClick = {
                        selectedCategories = if (SensorCategory.USB in selectedCategories)
                            selectedCategories - SensorCategory.USB
                        else
                            selectedCategories + SensorCategory.USB
                    },
                    text = stringResource(R.string.category_usb),
                    selected = SensorCategory.USB in selectedCategories,
                )
                OSHFilterChip(
                    onClick = {
                        selectedCategories = if (SensorCategory.OTHERS in selectedCategories)
                            selectedCategories - SensorCategory.OTHERS
                        else
                            selectedCategories + SensorCategory.OTHERS
                    },
                    text = stringResource(R.string.category_others),
                    selected = SensorCategory.OTHERS in selectedCategories,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                grouped.forEach { (category, sensors) ->
                    item {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(sensors, key = { it.id }) { sensor ->
                        val toggleSensor = { checked: Boolean ->
                            sensorStates = sensorStates + (sensor.id to checked)
                            onSensorToggled(sensor, checked)
                        }
                        if (sensor.config != null) {
                            SensorExpandableCard(
                                sensor = sensor,
                                onCheckedChange = toggleSensor,
                                onConfigAction = { onConfigAction(sensor) }
                            )
                        } else {
                            OSHSensorCard {
                                OSHSwitchRow(
                                    title = stringResource(sensor.nameRes),
                                    subtitle = stringResource(sensor.summaryRes),
                                    checked = sensor.enabled,
                                    onCheckedChange = toggleSensor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorExpandableCard(
    sensor: SensorItem,
    onCheckedChange: (Boolean) -> Unit,
    onConfigAction: () -> Unit
) {
    OSHExpandableSwitchCard(
        title = stringResource(sensor.nameRes),
        subtitle = stringResource(sensor.summaryRes),
        checked = sensor.enabled,
        onCheckedChange = onCheckedChange,
        configHint = stringResource(R.string.hint_tap_to_configure),
        expandedContent = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when (sensor.config) {
                is SensorConfig.BluetoothDevice -> {
                    BluetoothDeviceConfig(
                        selectLabelRes = sensor.config.selectLabelRes,
                        onSelect = onConfigAction
                    )
                }
                is SensorConfig.Video -> VideoConfig(onConfigAction)
                is SensorConfig.Audio -> AudioConfig(onConfigAction)
                is SensorConfig.TruPulse -> TruPulseConfig(onConfigAction)
                null -> {}
            }
        }
    )
}

@Composable
private fun BluetoothDeviceConfig(
    @StringRes selectLabelRes: Int,
    onSelect: () -> Unit
) {
    OSHClickableRowWithIcon(
        title = stringResource(selectLabelRes),
        subtitle = stringResource(R.string.summary_select_device),
        imageVector = Icons.Default.Bluetooth,
        contentDescription = stringResource(selectLabelRes),
        onClick = onSelect
    )
}

@Composable
private fun VideoConfig(onConfigAction: () -> Unit) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_codec),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_codec),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_resolution),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_resolution),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_frame_rate),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_frame_rate),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_camera),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_camera),
        onClick = onConfigAction
    )
}

@Composable
private fun AudioConfig(onConfigAction: () -> Unit) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_codec),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_codec),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_sample_rate),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_sample_rate),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_bitrate),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_bitrate),
        onClick = onConfigAction
    )
}

@Composable
private fun TruPulseConfig(onConfigAction: () -> Unit) {
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_trupulse_datasource),
        imageVector = Icons.Default.DevicesOther,
        contentDescription = stringResource(R.string.title_trupulse_datasource),
        onClick = onConfigAction
    )
    OSHClickableRowWithIcon(
        title = stringResource(R.string.title_select_trupulse),
        subtitle = stringResource(R.string.summary_select_device),
        imageVector = Icons.Default.Bluetooth,
        contentDescription = stringResource(R.string.title_select_trupulse),
        onClick = onConfigAction
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SensorsScreenPreview() {
    OSHTheme {
        SensorsScreen()
    }
}