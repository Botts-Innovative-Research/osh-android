package org.sensorhub.android.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.data.client.RemoteNodeState
import org.sensorhub.android.data.client.RemoteSystem
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableStatusRowWithIcon
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.components.StatusDot
import org.sensorhub.android.ui.theme.Background

@Composable
fun OshClientScreen(
    onNavigateToSettings: () -> Unit,
    onOpenSystem: (profileId: String, systemId: String) -> Unit,
    viewModel: OshClientViewModel = viewModel(),
) {
    val nodes by viewModel.nodes.collectAsState()
    val visibleProfileIds by viewModel.visibleProfileIds.collectAsState()
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
                    ServerCard(
                        node = node,
                        onDiscover = { viewModel.discover(node.profileId) },
                        onOpenSystem = { system -> onOpenSystem(node.profileId, system.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    node: RemoteNodeState,
    onDiscover: () -> Unit,
    onOpenSystem: (RemoteSystem) -> Unit,
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
                SystemRow(system = system, onClick = { onOpenSystem(system) })
            }
        }
    }
}

@Composable
private fun SystemRow(
    system: RemoteSystem,
    onClick: () -> Unit,
) {
    Column {
        OSHClickableStatusRowWithIcon(
            title = system.name,
            imageVector = Icons.Filled.Sensors,
            contentDescription = "Server",
            subtitle = system.uid,
            summary =  "${system.visualizations.size} datastreams",
            status = if (system.visualizations.isEmpty()) "unknown" else "started",
            onClick = onClick,
        )
    }
}
