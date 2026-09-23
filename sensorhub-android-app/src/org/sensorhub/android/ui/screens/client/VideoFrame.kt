package org.sensorhub.android.ui.screens.client

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.sensorhub.android.data.client.RemoteControlStream

@Composable
fun VideoFrame(
    renderer: VideoStream?,
    isConnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    AndroidView(
        factory = { context ->
            TextureView(context).also { textureView = it }
        },
        update = { },
        modifier = modifier,
    )

    LaunchedEffect(renderer, textureView) {
        textureView?.let { view -> renderer?.attach(view) }
    }
    LaunchedEffect(renderer, isConnecting, textureView) {
        if (isConnecting) textureView?.let { view -> renderer?.attach(view) }
    }
}

fun RemoteControlStream.supports(command: PtzCommand): Boolean = when (command) {
    PtzCommand.PAN_LEFT, PtzCommand.PAN_RIGHT -> supportsRelativePan
    PtzCommand.TILT_UP, PtzCommand.TILT_DOWN -> supportsRelativeTilt
    PtzCommand.ZOOM_IN, PtzCommand.ZOOM_OUT -> supportsRelativeZoom
}
