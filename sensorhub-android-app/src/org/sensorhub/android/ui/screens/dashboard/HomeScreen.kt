package org.sensorhub.android.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.SensorHubServiceProvider
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHClickableCardWithIcon
import org.sensorhub.android.ui.components.OSHFAB
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.Primary


@Composable
fun HomeScreen(
    onNavigateToPreferences: () -> Unit,
    viewModel: HomeViewModel = viewModel(),

) {
    val context = LocalContext.current
    val provider = context as? SensorHubServiceProvider


    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onNavigateToPreferences) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "App Preferences"
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            OSHFAB(
                onClick = { } // start smart hub / stop hub
            )

        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Video section
            if (viewModel.hasVideo) {
                VideoStatusCard(
                    isPreviewVisible = viewModel.videoPreviewVisible,
                    onTogglePreview = { viewModel.toggleVideoPreview() },
                )

                AnimatedVisibility(visible = viewModel.videoPreviewVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        VideoOverlay(
                            onFlipCamera = { provider?.let { viewModel.flipCamera(it) } },
                            onZoomIn = { provider?.let { viewModel.adjustZoom(1, it) } },
                            onZoomOut = { provider?.let { viewModel.adjustZoom(-1, it) } },
                            showFlipButton = viewModel.showFlipButton,
                            showZoomButtons = viewModel.showZoomButtons,
                        )
                    }
                }
            }

            // Meshtastic section
            if (viewModel.hasMeshtastic) {
            }

        }
    }
}

@Composable
private fun MeshtasticDialog(

) {
    var nodeId: String = ""
    var msg: String = ""
    var meshtasticDialog = false
    AlertDialog(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Message, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.title_send_meshtastic),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Column {
                Text(text = stringResource(R.string.meshtastic_nodeId))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nodeId,
                    onValueChange = { nodeId = it },
                    placeholder = { Text(stringResource(R.string.meshtastic_nodeId)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    )
                )

                Text(text = stringResource(R.string.meshtastic_msg))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    placeholder = { Text(stringResource(R.string.meshtastic_msg)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    )
                )
            }
        },
        onDismissRequest = { meshtasticDialog = false },
        confirmButton = {
            OSHButton(
                onClick = { sendMeshtasticMessage(nodeId, msg) },
                text = "OK"
            )
        },
        dismissButton = {
            TextButton(onClick = { meshtasticDialog = false }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MeshtasticCard(

) {
    OSHClickableCardWithIcon(
        title = stringResource(R.string.meshtastic_msg),
        imageVector = Icons.Default.Cloud,
        contentDescription = stringResource(R.string.manage_servers),
        onClick = { },
    )
}
@Composable
private fun VideoStatusCard(
    isPreviewVisible: Boolean,
    onTogglePreview: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Camera",
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
                onClick = onTogglePreview,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(if (isPreviewVisible) stringResource(R.string.btn_hide) else stringResource(R.string.btn_show))
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun HomeScreenPreview() {
    OSHTheme {
        HomeScreen(
            onNavigateToPreferences = {},
        )
    }
}
