package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.sensorhub.android.R
import org.sensorhub.android.data.client.PtzAxisRange
import org.sensorhub.android.data.client.RemoteControlStream
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.StatusDot
import org.sensorhub.android.ui.theme.ControlButtonBottom
import org.sensorhub.android.ui.theme.ControlButtonPressed
import org.sensorhub.android.ui.theme.ControlButtonTop
import org.sensorhub.android.ui.theme.ControlSurface
import org.sensorhub.android.ui.theme.OSHShapes
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OshDimensions

@Composable
fun PtzCommandCard(
    controlStream: RemoteControlStream,
    onPtz: (PtzCommand) -> Unit,
    onAbsolutePtz: (String, Double) -> Unit
) {
    val panRange = controlStream.absolutePanRange
    val tiltRange = controlStream.absoluteTiltRange
    val zoomRange = controlStream.absoluteZoomRange
    var pan by rememberSaveable(controlStream.controlStreamId) { mutableFloatStateOf(panRange.initialValue()) }
    var tilt by rememberSaveable(controlStream.controlStreamId) { mutableFloatStateOf(tiltRange.initialValue()) }
    var zoom by rememberSaveable(controlStream.controlStreamId) { mutableFloatStateOf(zoomRange.initialValue()) }

    OSHCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OshDimensions.screenHorizontal)
    ) {
        Column(Modifier.padding(OshDimensions.cardContent)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot("started")
                Spacer(Modifier.width(OshDimensions.titleGap))
                Text(
                    stringResource(R.string.system_detail_ptz_controls),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ControlCamera,
                    stringResource(R.string.system_detail_ptz_control)
                )
            }
            if (controlStream.supportsRelativePan || controlStream.supportsRelativeTilt || controlStream.supportsRelativeZoom) {
                RelativePtzControls(
                    controlStream,
                    onPtz
                )
            }
            if (panRange != null || tiltRange != null || zoomRange != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = OshDimensions.contentGap)
                        .clip(OSHShapes.medium)
                        .background(ControlSurface)
                ) {
                    Column(Modifier.padding(OshDimensions.compactGap)) {
                        Text(
                            stringResource(R.string.system_detail_absolute_position),
                            style = MaterialTheme.typography.titleSmall
                        ); panRange?.let {
                        AxisSlider(
                            stringResource(R.string.system_detail_pan),
                            pan,
                            it
                        ) { pan = it; onAbsolutePtz("pan", it.toDouble()) }
                    }; tiltRange?.let {
                        AxisSlider(
                            stringResource(R.string.system_detail_tilt),
                            tilt,
                            it
                        ) { tilt = it; onAbsolutePtz("tilt", it.toDouble()) }
                    }; zoomRange?.let {
                        AxisSlider(
                            stringResource(R.string.system_detail_zoom),
                            zoom,
                            it
                        ) { zoom = it; onAbsolutePtz("zoom", it.toDouble()) }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelativePtzControls(stream: RemoteControlStream, onPtz: (PtzCommand) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = OshDimensions.contentGap)
            .clip(OSHShapes.medium)
            .background(ControlSurface)
    ) {
        Column(
            Modifier.padding(OshDimensions.compactGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Spacer(Modifier.size(OshDimensions.controlButtonSize))
                PtzButton(
                    Icons.Default.KeyboardArrowUp,
                    stringResource(R.string.system_detail_tilt_up),
                    stream.supports(PtzCommand.TILT_UP),
                    size = OshDimensions.controlButtonSize
                ) { onPtz(PtzCommand.TILT_UP) }
                Spacer(Modifier.size(OshDimensions.controlButtonSize))
            }
            Spacer(Modifier.size(OshDimensions.compactGap))
            Row {
                PtzButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    stringResource(R.string.system_detail_pan_left),
                    stream.supports(PtzCommand.PAN_LEFT),
                    size = OshDimensions.controlButtonSize
                ) { onPtz(PtzCommand.PAN_LEFT) }
                Spacer(Modifier.size(OshDimensions.compactGap))
                PtzButton(
                    Icons.Default.Home,
                    stringResource(R.string.system_detail_pan_right),
                    stream.supports(PtzCommand.PAN_RIGHT),
                    size = OshDimensions.controlButtonSize
                ) { onPtz(PtzCommand.PAN_RIGHT) }
                Spacer(Modifier.size(OshDimensions.compactGap))
                PtzButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    stringResource(R.string.system_detail_pan_right),
                    stream.supports(PtzCommand.PAN_RIGHT),
                    size = OshDimensions.controlButtonSize
                ) { onPtz(PtzCommand.PAN_RIGHT) }
            }
            Spacer(Modifier.size(OshDimensions.compactGap))
            Row {
                Box(Modifier.padding(top = 10.dp)) {
                    PtzButton(
                        Icons.Default.Remove,
                        stringResource(R.string.system_detail_zoom_out),
                        stream.supports(PtzCommand.ZOOM_OUT),
                        size = OshDimensions.controlButtonSizeSmall
                    ) { onPtz(PtzCommand.ZOOM_OUT) }
                }
                Spacer(Modifier.size(OshDimensions.compactGap))
                PtzButton(
                    Icons.Default.KeyboardArrowDown,
                    stringResource(R.string.system_detail_tilt_down),
                    stream.supports(PtzCommand.TILT_DOWN),
                    size = OshDimensions.controlButtonSize
                ) { onPtz(PtzCommand.TILT_DOWN) }
                Spacer(Modifier.size(OshDimensions.compactGap))
                Box(Modifier.padding(top = 10.dp)) {
                    PtzButton(
                        Icons.Default.Add,
                        stringResource(R.string.system_detail_zoom_in),
                        stream.supports(PtzCommand.ZOOM_IN),
                        size = OshDimensions.controlButtonSizeSmall
                    ) { onPtz(PtzCommand.ZOOM_IN) }
                }
            }
        }

    }
}

@Composable
private fun AxisSlider(
    label: String,
    value: Float,
    range: PtzAxisRange,
    committed: (Float) -> Unit
) {
    var sliderValue by remember(label) { mutableFloatStateOf(value) }; Column(Modifier.padding(top = OshDimensions.contentGap)) {
        Row(
            Modifier.fillMaxWidth()
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f)
            );
             if (range.unit != null)
                Text(sliderValue.format(range.unit))
        }; Slider(
        sliderValue,
        { sliderValue = it },
        valueRange = range.minimum..range.maximum,
        onValueChangeFinished = { committed(sliderValue) })
    }
}

private fun PtzAxisRange?.initialValue() = 0f.coerceIn(this?.minimum ?: 0f, this?.maximum ?: 0f)
private fun Float.format(unit: String) =
    if (unit == "deg") "${toInt()}°" else if (unit == "1") "${toInt()}×" else "$this $unit".trim()

@Composable
private fun PtzButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = if (pressed) Brush.linearGradient(
        listOf(
            ControlButtonPressed,
            ControlButtonPressed
        )
    ) else
        Brush.verticalGradient(listOf(ControlButtonTop, ControlButtonBottom))
    Box(
        Modifier
            .size(size)
            .shadow(OshDimensions.controlButtonElevation, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, Color.White.copy(alpha = .33f), CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ), contentAlignment = Alignment.Center
    ) { Icon(icon, description) }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PtzCommandCardPreview() {
    val controlStream = RemoteControlStream(
        controlStreamId = "0134",
        supportsAbsolutePan = true,
        supportsAbsoluteTilt = true,
        supportsAbsoluteZoom = true,
        supportsRelativePan = true,
        supportsRelativeTilt = true,
        supportsRelativeZoom = true,
        supportsPresets = true,
        absolutePanRange = PtzAxisRange(-180f, 180f, "deg"),
        absoluteTiltRange = PtzAxisRange(-90f, 0f, "deg"),
        absoluteZoomRange = PtzAxisRange(1f, 9999f, "1"),
    )

    OSHTheme {
        PtzCommandCard(
            controlStream = controlStream,
            onPtz = {},
            onAbsolutePtz = { _, _ -> },
        )
    }
}
