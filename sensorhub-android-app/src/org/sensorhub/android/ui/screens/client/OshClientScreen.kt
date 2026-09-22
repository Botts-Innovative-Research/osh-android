package org.sensorhub.android.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.data.client.RemoteNodeState
import org.sensorhub.android.data.client.RemoteSystem
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableStatusRowWithIcon
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHExpandableCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background

@Composable
fun OshClientScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: OshClientViewModel = viewModel(),
) {
    val nodes by viewModel.nodes.collectAsState()
    val visibleProfileIds by viewModel.visibleProfileIds.collectAsState()
    val enabledLocations by viewModel.enabledLocations.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()

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
            OSHTopAppBarWithLogo(
                title = "Systems",
                actions = {
                    IconButton(onClick = viewModel::refreshProfiles) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh profiles")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        containerColor = Background,
    ) { padding ->
        if (nodes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Add and enable a server profile in Settings to use OSH Client.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item(key = "server-selector") {
                    OSHCard {
                        Column(Modifier.padding(16.dp)) {
                            OSHDropDown(
                                title = "Servers",
                                summary = "${visibleProfileIds.size} selected",
                                items = nodes,
                                selectedIds = visibleProfileIds,
                                itemId = { it.profileId },
                                itemLabel = { it.name },
                                itemSummary = { it.endpointUrl },
                                onSelectionChange = viewModel::setProfileVisible,
                                emptyText = "No enabled server profiles",
                            )
                        }
                    }
                }
                val visibleNodes = nodes.filter { it.profileId in visibleProfileIds }
                if (visibleNodes.isEmpty()) {
                    item { Text("Select a server profile to view its systems.", Modifier.padding(24.dp)) }
                }
                items(visibleNodes, key = { it.profileId }) { node ->
                    ServerCard(node, onDiscover = { viewModel.discover(node.profileId) }) { system ->
                        SystemCard(
                            system = system,
                            locationEnabled = { visualization -> visualization.dataStreamId in enabledLocations },
                            onLocationEnabled = { visualization, enabled ->
                                viewModel.setLocationEnabled(node.profileId, system, visualization, enabled)
                            },
                            onOpenVideo = { visualization ->
                                viewModel.openVideo(node.profileId, system, visualization)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    node: RemoteNodeState,
    onDiscover: () -> Unit,
    systems: @Composable (RemoteSystem) -> Unit,
) {
    OSHCard {
        Column {
            OSHClickableStatusRowWithIcon(
                title = node.name,
                imageVector = Icons.Filled.Cloud,
                contentDescription = "Server",
                subtitle = node.endpointUrl,
                summary = when {
                    node.loading -> "Discovering systems"
                    node.error != null -> node.error
                    node.systems.isEmpty() -> "Tap to discover systems"
                    else -> "${node.systems.size} systems discovered"
                },
                status = when {
                    node.loading -> "starting"
                    node.error != null -> "error"
                    node.systems.isNotEmpty() -> "started"
                    else -> "unknown"
                },
                onClick = if (node.loading) null else onDiscover,
            )
            if (node.loading) CircularProgressIndicator(Modifier.padding(start = 52.dp, bottom = 16.dp))
            node.systems.forEach { system ->
                HorizontalDivider()
                systems(system)
            }
        }
    }
}

@Composable
private fun SystemCard(
    system: RemoteSystem,
    locationEnabled: (RemoteVisualization) -> Boolean,
    onLocationEnabled: (RemoteVisualization, Boolean) -> Unit,
    onOpenVideo: (RemoteVisualization) -> Unit,
) {
    OSHExpandableCard(
        title = system.name,
        status = if (system.visualizations.isEmpty()) "unknown" else "started",
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        collapsedContent = {
            if (system.description.isNotBlank()) {
                Text(
                    system.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
    ) {
        system.visualizations.forEach { visualization ->
            StreamRow(
                visualization = visualization,
                enabled = locationEnabled(visualization),
                onLocationEnabled = { onLocationEnabled(visualization, it) },
                onOpenVideo = { onOpenVideo(visualization) },
            )
        }
        system.ptz?.let { ptz ->
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ControlCamera, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    "PTZ controls available in video view",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (system.visualizations.isEmpty()) {
            Text("No supported video or location streams.", style = MaterialTheme.typography.bodySmall)
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
            imageVector = if (visualization.kind == RemoteVisualization.Kind.VIDEO) Icons.Filled.Videocam else Icons.Filled.LocationOn,
            contentDescription = null,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                visualization.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                visualization.dataStreamId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        when (visualization.kind) {
            RemoteVisualization.Kind.LOCATION -> Switch(checked = enabled, onCheckedChange = onLocationEnabled)
            RemoteVisualization.Kind.VIDEO -> IconButton(onClick = onOpenVideo) {
                Icon(Icons.Filled.Videocam, contentDescription = "Open video")
            }
        }
    }
}
