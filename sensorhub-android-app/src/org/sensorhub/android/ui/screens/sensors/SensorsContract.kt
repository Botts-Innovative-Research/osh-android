package org.sensorhub.android.ui.screens.sensors

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import org.sensorhub.android.R

enum class SensorCategory(@StringRes val labelRes: Int) {
    ON_DEVICE(R.string.category_on_device),
    BLUETOOTH(R.string.category_bluetooth),
    USB(R.string.category_usb),
    OTHERS(R.string.category_others),
}

sealed class SensorConfig {
    data class BluetoothDevice(
        @StringRes val selectLabelRes: Int,
        val addressPrefKey: String,
    ) : SensorConfig()

    data object Video : SensorConfig()
    data object Audio : SensorConfig()
    data object TruPulse : SensorConfig()
}

data class SensorItem(
    val id: String,
    val prefKey: String,
    @StringRes val nameRes: Int,
    @StringRes val summaryRes: Int,
    val category: SensorCategory,
    val enabled: Boolean = false,
    val config: SensorConfig? = null,
)

data class ChoiceDialogSpec(
    val prefKey: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val values: List<String>,
    val labels: List<String> = values,
    val defaultValue: String = values.first(),
)

val CHOICE_DIALOGS = listOf(
    ChoiceDialogSpec(
        "audio_codec", R.string.title_codec, Icons.Default.Audiotrack,
        listOf("AAC", "AMR-NB", "AMR-WB", "FLAC", "VORBIS")
    ),
    ChoiceDialogSpec(
        "audio_samplerate", R.string.title_sample_rate, Icons.Default.Audiotrack,
        listOf("8000", "11025", "22050", "44100", "48000"),
        listOf("8000 Hz", "11025 Hz", "22050 Hz", "44100 Hz", "48000 Hz")
    ),
    ChoiceDialogSpec(
        "audio_bitrate", R.string.title_bitrate, Icons.Default.Audiotrack,
        listOf("32", "64", "96", "128", "160", "192"),
        listOf("32 kbps", "64 kbps", "96 kbps", "128 kbps", "160 kbps", "192 kbps"),
        defaultValue = "64"
    ),
    ChoiceDialogSpec(
        "video_codec", R.string.title_codec, Icons.Default.Videocam,
        listOf("JPEG", "H264", "H265", "VP9", "VP8")
    ),
    ChoiceDialogSpec(
        "video_framerate", R.string.title_frame_rate, Icons.Default.Videocam,
        listOf("10", "15", "24", "30", "60", "120"),
        listOf("10 fps", "15 fps", "24 fps", "30 fps", "60 fps", "120 fps"),
        defaultValue = "30"
    ),
    ChoiceDialogSpec(
        "video_resolution", R.string.title_resolution, Icons.Default.Videocam,
        listOf("320x240", "640x480", "1280x720", "1920x1080"),
        defaultValue = "640x480"
    ),
    ChoiceDialogSpec(
        "camera_select", R.string.title_camera, Icons.Default.Videocam,
        listOf("0", "1"),
        listOf("Camera 0", "Camera 1")
    ),
    ChoiceDialogSpec(
        "trupulse_datasource", R.string.title_trupulse_datasource, Icons.Default.DevicesOther,
        listOf("STREAM", "SIMULATED"),
        listOf("Streaming Physical Device", "Simulate Virtual Device")
    ),
)

val ALL_SENSORS: List<SensorItem> = listOf(
    // On-device sensors (no config)
    SensorItem(
        "accelerometer",
        "accel_enabled",
        R.string.sensor_accelerometer,
        R.string.summary_accel,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "gyroscope",
        "gyro_enabled",
        R.string.sensor_gyroscope,
        R.string.summary_gyro,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "magnetometer",
        "mag_enabled",
        R.string.sensor_magnetometer,
        R.string.summary_mag,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "orient_q",
        "orient_quat_enabled",
        R.string.sensor_orientation_quat,
        R.string.summary_orientation,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "orient_e",
        "orient_euler_enabled",
        R.string.sensor_orientation_euler,
        R.string.summary_orientation,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "gps",
        "gps_enabled",
        R.string.sensor_gps,
        R.string.summary_gps,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "network",
        "netloc_enabled",
        R.string.sensor_network_location,
        R.string.summary_netloc,
        SensorCategory.ON_DEVICE
    ),
    SensorItem(
        "video_roll",
        "video_roll_enabled",
        R.string.sensor_video_roll,
        R.string.summary_video_roll,
        SensorCategory.ON_DEVICE
    ),

    // On-device sensors (with config)
    SensorItem(
        "camera",
        "cam_enabled",
        R.string.sensor_video,
        R.string.summary_video,
        SensorCategory.ON_DEVICE,
        config = SensorConfig.Video
    ),
    SensorItem(
        "audio",
        "audio_enabled",
        R.string.sensor_audio,
        R.string.summary_audio,
        SensorCategory.ON_DEVICE,
        config = SensorConfig.Audio
    ),

    // Bluetooth sensors
    SensorItem(
        "meshtastic",
        "meshtastic_enabled",
        R.string.sensor_meshtastic,
        R.string.summary_meshtastic,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(
            R.string.title_select_meshtastic,
            "meshtastic_device_address"
        )
    ),
    SensorItem(
        "polar",
        "polar_enabled",
        R.string.sensor_polar,
        R.string.summary_polar,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(R.string.title_select_polar, "polar_device_address")
    ),
    SensorItem(
        "kestrel",
        "kestrel_enabled",
        R.string.sensor_kestrel,
        R.string.summary_kestrel,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(
            R.string.title_select_kestrel,
            "kestrel_device_address"
        )
    ),
    SensorItem(
        "garmin",
        "garmin_enabled",
        R.string.sensor_garmin,
        R.string.summary_garmin,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(R.string.title_select_garmin, "garmin_device_address")
    ),
    SensorItem(
        "ste",
        "ste_radpager_enabled",
        R.string.sensor_ste,
        R.string.summary_ste,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(R.string.title_select_device, "ste_device_address")
    ),
    SensorItem(
        "trupulse",
        "trupulse_enabled",
        R.string.sensor_trupulse,
        R.string.summary_trupulse,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.TruPulse
    ),
    SensorItem(
        "angel",
        "angel_enabled",
        R.string.sensor_angel,
        R.string.summary_angel,
        SensorCategory.BLUETOOTH,
        config = SensorConfig.BluetoothDevice(R.string.title_select_device, "angel_address")
    ),

    // USB sensors
    SensorItem(
        "controller",
        "controller_enabled",
        R.string.sensor_controller,
        R.string.summary_controller,
        SensorCategory.USB
    ),
    SensorItem(
        "flirone",
        "flirone_enabled",
        R.string.sensor_flirone,
        R.string.summary_flirone,
        SensorCategory.USB
    ),

    // Others
    SensorItem(
        "wardriving",
        "wardriving_enabled",
        R.string.sensor_wardriving,
        R.string.summary_wardriving,
        SensorCategory.OTHERS
    ),
    SensorItem(
        "template",
        "template_enabled",
        R.string.sensor_template,
        R.string.summary_template,
        SensorCategory.OTHERS,
        config = SensorConfig.BluetoothDevice(
            R.string.title_select_device,
            "template_device_address"
        )
    ),
)

val BT_ADDRESS_PREF_KEYS = listOf(
    "meshtastic_device_address",
    "polar_device_address",
    "kestrel_device_address",
    "garmin_device_address",
    "ste_device_address",
    "trupulse_device_address",
    "angel_address",
    "template_device_address",
)
