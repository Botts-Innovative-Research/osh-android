package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.data.client.RemoteNodeState
import org.sensorhub.android.data.client.RemoteSystem
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableStatusRowWithIcon
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.SecondaryContainer

@Composable
fun OshClientScreen(
    onNavigateToSettings: () -> Unit,
    onOpenSystem: (profileId: String, systemId: String) -> Unit,
    viewModel: OshClientViewModel = viewModel(),
) {
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val visibleProfileIds by viewModel.visibleProfileIds.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = "Client",
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
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Add and enable a server profile in Settings to use OSH Client.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    item {
                        Text(
                            "Select a server profile to view its systems.",
                            Modifier.padding(24.dp)
                        )
                    }
                }
                visibleNodes.forEach { node ->
                    item(key = "server:${node.profileId}") {
                        ServerSectionHeader(
                            node = node,
                            onDiscover = { viewModel.discover(node.profileId) },
                            hasSystems = node.systems.isNotEmpty(),
                        )
                    }
                    itemsIndexed(
                        items = node.systems,
                        key = { _, system -> "${node.profileId}:${system.id}" },
                    ) { index, system ->
                        ServerSystemRow(
                            system = system,
                            isLastInSection = index == node.systems.lastIndex,
                            onClick = { onOpenSystem(node.profileId, system.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerSectionHeader(
    node: RemoteNodeState,
    onDiscover: () -> Unit,
    hasSystems: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp)),
        shape = if (hasSystems) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        } else {
            RoundedCornerShape(12.dp)
        },
        color = SecondaryContainer,
        shadowElevation = 2.dp,
    ) {
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
            if (node.loading) CircularProgressIndicator(
                Modifier.padding(
                    start = 52.dp,
                    bottom = 16.dp
                )
            )
        }
    }
}

@Composable
private fun ServerSystemRow(
    system: RemoteSystem,
    isLastInSection: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = if (isLastInSection) 8.dp else 0.dp,
            ),
        shape = if (isLastInSection) {
            RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        } else {
            RectangleShape
        },
        color = SecondaryContainer,
    ) {
        Column {
            HorizontalDivider()
            OSHClickableStatusRowWithIcon(
                title = system.name,
                imageVector = Icons.Filled.Sensors,
                contentDescription = "System",
                subtitle = system.uid,
                summary = "${system.visualizations.size} datastreams",
                status = "unknown",
                onClick = onClick,
            )
        }
    }
}
