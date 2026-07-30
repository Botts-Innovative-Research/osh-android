package org.sensorhub.android.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.sensorhub.android.ui.components.OSHIconButton
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun CameraPreviewScreen(
    viewModel: CameraPreviewViewModel,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val cameraPermission = true;
    val context = LocalContext.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    if (cameraPermission) {
        Box (modifier = Modifier.fillMaxSize()) {

            // buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OSHIconButton(
                    onClick = { },
                    imageVector = Icons.Filled.FlipCameraAndroid
                )

                OSHIconButton(
                    onClick = { },
                    imageVector = Icons.Filled.ZoomIn
                )

                OSHIconButton(
                    onClick = { },
                    imageVector = Icons.Filled.ZoomOut
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {  }) {
                Text("Grant Camera Permission")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CameraPreviewScreenPreview() {
    OSHTheme {
//        CameraPreviewScreen()
    }
}