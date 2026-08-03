package org.sensorhub.android.ui.screens.dashboard

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.sensorhub.android.SensorHubService
import org.sensorhub.android.ui.components.OSHIconButton
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun VideoOverlay(
    onFlipCamera: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    showFlipButton: Boolean,
    showZoomButtons: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            val videoTex = SensorHubService.getVideoTexture()
                            if (videoTex != null && !videoTex.isReleased) {
                                setSurfaceTexture(videoTex)
                            }
                        }
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            update = { textureView ->
                val videoTex = SensorHubService.getVideoTexture()
                if (videoTex != null && !videoTex.isReleased) {
                    if (textureView.surfaceTexture != videoTex) {
                        textureView.setSurfaceTexture(videoTex)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showFlipButton) {
                OSHIconButton(
                    onClick = onFlipCamera,
                    imageVector = Icons.Filled.FlipCameraAndroid
                )
            }
            if (showZoomButtons) {
                OSHIconButton(
                    onClick = onZoomIn,
                    imageVector = Icons.Filled.ZoomIn
                )
                OSHIconButton(
                    onClick = onZoomOut,
                    imageVector = Icons.Filled.ZoomOut
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun VideoOverlayPreview() {
    OSHTheme {
        VideoOverlay(
            {},
            {},
            {},
            true,
            true
        )
    }
}