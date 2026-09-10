package org.sensorhub.android.ui.screens.dashboard

import org.sensorhub.android.R
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.opengis.swe.v20.*
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.ui.components.OSHExpandableCard
import org.sensorhub.android.ui.screens.sensors.ALL_SENSORS
import org.sensorhub.android.ui.theme.Error
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OnSurfaceVariant
import org.sensorhub.android.ui.theme.Success
import org.sensorhub.android.ui.theme.Surface
import org.sensorhub.android.ui.theme.SurfaceVariant
import org.sensorhub.android.ui.theme.Warning
import org.sensorhub.api.data.IDataProducer
import org.sensorhub.api.data.IStreamingDataInterface
import org.sensorhub.impl.sensor.android.*
import org.sensorhub.impl.sensor.android.audio.AndroidAudioOutput
import org.sensorhub.impl.sensor.android.video.AndroidCameraOutput
import java.util.Locale

enum class ReadingStatus { LIVE, WAITING, STALE, UNAVAILABLE }
data class MeasurementUi(val label: String, val value: String = "—")
data class SensorCardUi(
    val id: String,
    val title: String,
    val status: ReadingStatus,
    val message: String,
    val measurements: List<MeasurementUi> = emptyList()
)

internal class SensorCardReader(private val context: Context, private val prefs: SharedPreferences) {
    private val moduleIds = mapOf(
        "meshtastic" to "MESHTASTIC_SENSOR",
        "polar" to "POLAR_HEART_SENSOR",
        "kestrel" to "KESTREL_WEATHER",
        "ste" to "STE_RADPAGER_SENSOR",
        "trupulse" to "TRUPULSE_SENSOR",
        "controller" to "CONTROLLER",
        "wardriving" to "WARDRIVING_",
        "template" to "TEMPLATE_DRIVER_",
    )
    private val placeholders = mapOf(
        "accelerometer" to listOf(context.getString(R.string.ui_x_acceleration), context.getString(R.string.ui_y_acceleration), context.getString(R.string.ui_z_acceleration)),
        "gyroscope" to listOf(context.getString(R.string.ui_x_angular_velocity), context.getString(R.string.ui_y_angular_velocity), context.getString(R.string.ui_z_angular_velocity)),
        "magnetometer" to listOf(context.getString(R.string.ui_x_magnetic_field), context.getString(R.string.ui_y_magnetic_field), context.getString(R.string.ui_z_magnetic_field)),
        "orient_e" to listOf(context.getString(R.string.ui_heading_angle), context.getString(R.string.ui_pitch_angle), context.getString(R.string.ui_roll_angle)),
        "orient_q" to listOf(context.getString(R.string.ui_quaternion_x), context.getString(R.string.ui_quaternion_y), context.getString(R.string.ui_quaternion_z), context.getString(R.string.ui_quaternion_w)),
        "gps" to listOf(context.getString(R.string.ui_latitude), context.getString(R.string.ui_longitude), context.getString(R.string.ui_altitude)),
        "network" to listOf(context.getString(R.string.ui_latitude), context.getString(R.string.ui_longitude), context.getString(R.string.ui_altitude)),
        "video_roll" to listOf(context.getString(R.string.ui_roll_angle)),
        "audio" to listOf(context.getString(R.string.title_sample_rate), context.getString(R.string.ui_num_samples), context.getString(R.string.ui_samples))
    )

    fun read(service: SensorHubService?): List<SensorCardUi> {
        val modules = service?.sensorHub?.moduleRegistry?.loadedModules?.toList().orEmpty()
        val running = service?.hubState == SensorHubService.HubState.RUNNING
        return ALL_SENSORS.filter { prefs.getBoolean(it.prefKey, false) }.map { sensor ->
            val title = context.getString(sensor.nameRes)
            val empty = placeholders[sensor.id].orEmpty().map { MeasurementUi(it) }
            try {
                val module = modules.firstOrNull { it.localID == (moduleIds[sensor.id] ?: "ANDROID_SENSORS") }
                val outputs = (module as? IDataProducer)?.outputs?.values?.toList().orEmpty().filter { output ->
                    when (sensor.id) {
                        "accelerometer" -> output is AndroidAcceleroOutput
                        "gyroscope" -> output is AndroidGyroOutput
                        "magnetometer" -> output is AndroidMagnetoOutput
                        "orient_e" -> output is AndroidOrientationEulerOutput
                        "orient_q" -> output is AndroidOrientationQuatOutput
                        "gps" -> output is AndroidLocationOutput && output.name == "gps_data"
                        "network" -> output is AndroidLocationOutput && output.name == "network_data"
                        "camera" -> output is AndroidCameraOutput || output is AndroidCamera2Output
                        "video_roll" -> (output is AndroidCameraOutput || output is AndroidCamera2Output) && output.recordDescription.getComponent("videoRoll") != null
                        "audio" -> output is AndroidAudioOutput
                        else -> moduleIds.containsKey(sensor.id)
                    }
                }
                val status = when {
                    !running -> ReadingStatus.UNAVAILABLE
                    outputs.isEmpty() -> ReadingStatus.UNAVAILABLE
                    outputs.any { it.latestRecord == null } -> ReadingStatus.WAITING
                    outputs.any { output ->
                        val period = output.averageSamplingPeriod
                        val threshold = if (period.isFinite() && period > 0) maxOf(10000.0, period * 3000) else Double.POSITIVE_INFINITY
                        output.latestRecord != null && System.currentTimeMillis() - output.latestRecordTime > threshold
                    } -> ReadingStatus.STALE
                    else -> ReadingStatus.LIVE
                }
                val message = when (status) {
                    ReadingStatus.UNAVAILABLE -> if (sensor.id == "video_roll") context.getString(R.string.ui_camera_with_roll_output_is_required) else context.getString(R.string.ui_sensor_or_driver_not_available)
                    ReadingStatus.WAITING -> context.getString(R.string.ui_waiting_for_data)
                    ReadingStatus.STALE -> context.getString(R.string.ui_no_recent_data_showing_last_reading)
                    ReadingStatus.LIVE -> context.getString(R.string.ui_receiving_data)
                }
                val rows = if (!running) empty else outputs.flatMap { output ->
                    measurements(output, sensor.id, outputs.size > 1)
                }.ifEmpty { empty }
                SensorCardUi(sensor.id, title, status, message, rows)
            } catch (_: Exception) {
                SensorCardUi(sensor.id, title, ReadingStatus.UNAVAILABLE, context.getString(R.string.ui_unable_to_read_sensor_output), empty)
            }
        }
    }

    private fun measurements(output: IStreamingDataInterface, sensorId: String, includeOutput: Boolean): List<MeasurementUi> {
        // Never attach data to the driver's shared schema. Arrays stay collapsed.
        val schema = output.recordDescription.copy()
        output.latestRecord?.let { schema.data = it }
        val rows = mutableListOf<MeasurementUi>()
        fun visit(component: DataComponent, prefix: String) {
            if (component is Time) return
            val label = component.label?.takeIf { it.isNotBlank() } ?: component.name.orEmpty().replace('_', ' ')
            if (component is DataArray) {
                if (sensorId != "video_roll") rows += MeasurementUi(label, if (output.latestRecord != null) context.getString(R.string.ui_available) else "—")
                return
            }
            if (component.componentCount > 0) {
                for (i in 0 until component.componentCount) visit(component.getComponent(i), prefix)
                return
            }
            if (sensorId == "video_roll" && !component.name.orEmpty().contains("roll", true)) return
            val unit = (component as? Quantity)?.uom?.code.orEmpty()
            val value = if (!component.hasData()) "—" else when (component) {
                is Quantity -> format(component.data.doubleValue, if (component.name in listOf("lat", "lon")) 6 else 3) + if (unit.isBlank() or unit.equals("1")) "" else " $unit"
                is Count -> component.data.intValue.toString()
                is net.opengis.swe.v20.Boolean -> component.data.booleanValue.toString()
                is Text -> component.data.stringValue.orEmpty().take(120)
                else -> "—"
            }
            rows += MeasurementUi(prefix + label, value)
        }
        visit(schema, if (includeOutput) output.name.replace('_', ' ') + " · " else "")
        return rows
    }

    private fun format(value: Double, decimals: Int = 3): String = when {
        value == Double.NEGATIVE_INFINITY -> "−∞"
        !value.isFinite() -> "—"
        else -> String.format(Locale.getDefault(), "%.${decimals}f", value)
    }
}

@Composable
internal fun SensorOutputCard(sensor: SensorCardUi) {
    val status = when (sensor.status) {
        ReadingStatus.LIVE -> "ok"
        ReadingStatus.WAITING, ReadingStatus.STALE -> "nok"
        ReadingStatus.UNAVAILABLE -> "error"
    }
    OSHExpandableCard(
        title = sensor.title,
        status = status,
        expandedContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sensor.measurements.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(row.label, Modifier.weight(1f), color = OnSurfaceVariant)
                        Text(row.value, modifier = Modifier.widthIn(max = 160.dp))
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SensorCardsPreview() {
    OSHTheme {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SensorOutputCard(
                SensorCardUi(
                    "orient_e",
                    "Euler Orientation",
                    ReadingStatus.LIVE,
                    "Receiving data",
                    listOf(
                    MeasurementUi(
                        "Heading angle",
                        "125.400 deg"
                    ),
                    MeasurementUi(
                        "Pitch angle",
                        "2.100 deg"
                    ),
                    MeasurementUi(
                        "Roll angle",
                        "−0.300 deg"
                    )
                )))
        }
    }
}
