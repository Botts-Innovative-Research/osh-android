package org.sensorhub.android.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background

@Composable
fun SystemDetailScreen(
    profileId: String,
    systemId: String,
    onBack: () -> Unit,
    viewModel: OshClientViewModel,
) {
    val nodes by viewModel.nodes.collectAsState()
    val enabledLocations by viewModel.enabledLocations.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val system = nodes.firstOrNull { it.profileId == profileId }
        ?.systems
        ?.firstOrNull { it.id == systemId }

    if (selectedVideo != null) {
        val video = selectedVideo!!
        BackHandler(onBack = viewModel::disconnectVideo)
        VideoScreen(
            videoSurface = { modifier ->
                VideoFrame(
                    renderer = viewModel.videoRenderer(video.visualization.dataStreamId),
                    onSurfaceReady = viewModel::startSelectedVideo,
                    modifier = modifier,
                )
            },
            onExit = viewModel::disconnectVideo,
            onPtz = { command ->
                video.system.ptz?.takeIf { it.supports(command) }?.let { ptz ->
                    viewModel.sendPtzCommand(
                        video.profileId,
                        ptz,
                        mapOf(command.item to command.delta),
                    )
                }
            },
        )
        return
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = system?.name ?: "System",
                onBackClick = onBack,
            )
        },
        containerColor = Background,
    ) { padding ->
        if (system == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("This system is no longer available.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item(key = "datastreams") {
                    OSHCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("Datastreams", style = MaterialTheme.typography.titleMedium)
                            system.visualizations.forEach { visualization ->
                                StreamRow(
                                    visualization = visualization,
                                    enabled = visualization.dataStreamId in enabledLocations,
                                    onLocationEnabled = { enabled ->
                                        viewModel.setLocationEnabled(profileId, system, visualization, enabled)
                                    },
                                    onOpenVideo = {
                                        viewModel.openVideo(profileId, system, visualization)
                                    },
                                )
                            }
                            if (system.visualizations.isEmpty()) {
                                Text(
                                    "No supported video or location streams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
                system.ptz?.let {
                    item(key = "ptz") {
                        OSHCard {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.ControlCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PTZ controls available in video view",
                                    style = MaterialTheme.typography.bodySmall,
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
private fun StreamRow(
    visualization: RemoteVisualization,
    enabled: Boolean,
    onLocationEnabled: (Boolean) -> Unit,
    onOpenVideo: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (visualization.kind == RemoteVisualization.Kind.VIDEO) {
                Icons.Filled.Videocam
            } else {
                Icons.Filled.LocationOn
            },
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(visualization.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                visualization.dataStreamId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        when (visualization.kind) {
            RemoteVisualization.Kind.LOCATION -> Switch(
                checked = enabled,
                onCheckedChange = onLocationEnabled,
            )
            RemoteVisualization.Kind.VIDEO -> IconButton(onClick = onOpenVideo) {
                Icon(Icons.Filled.Videocam, contentDescription = "Open video")
            }
        }
    }
}
