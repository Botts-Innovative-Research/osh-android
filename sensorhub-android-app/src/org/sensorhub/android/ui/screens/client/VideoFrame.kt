package org.sensorhub.android.ui.screens.client

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.sensorhub.android.data.client.RemoteControlStream

@Composable
fun VideoFrame(
    renderer: VideoStream?,
    onSurfaceReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            TextureView(context).also { view ->
                renderer?.attach(view)
                onSurfaceReady()
            }
        },
        update = { },
        modifier = modifier,
    )
}

fun RemoteControlStream.supports(command: PtzCommand): Boolean = when (command) {
    PtzCommand.PAN_LEFT, PtzCommand.PAN_RIGHT -> supportsRelativePan
    PtzCommand.TILT_UP, PtzCommand.TILT_DOWN -> supportsRelativeTilt
    PtzCommand.ZOOM_IN, PtzCommand.ZOOM_OUT -> supportsRelativeZoom
}
