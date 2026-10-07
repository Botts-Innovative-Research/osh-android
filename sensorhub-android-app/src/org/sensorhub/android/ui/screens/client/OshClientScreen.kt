package org.sensorhub.android.ui.screens.client

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.data.client.RemoteNodeState
import org.sensorhub.android.data.client.RemoteSystem
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableStatusRowWithIcon
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OshDimensions
import org.sensorhub.android.ui.theme.OshSpacing

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
                title = stringResource(R.string.client_title),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (nodes.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(OshSpacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.client_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = OshSpacing.md),
                )
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.padding(top = OshSpacing.md),
                ) {
                    Text(stringResource(R.string.action_settings))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = OshSpacing.sm),
            ) {
                item(key = "server-selector") {
                    OSHCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OshDimensions.screenHorizontal)
                    ) {
                        Column(Modifier.padding(OshSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.client_servers),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                IconButton(onClick = viewModel::refreshProfiles) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.content_desc_refresh_profiles)
                                    )
                                }
                            }
                            OSHDropDown(
                                summary = stringResource(R.string.client_selected_count, visibleProfileIds.size),
                                items = nodes,
                                selectedIds = visibleProfileIds,
                                itemId = { it.profileId },
                                itemLabel = { it.name },
                                itemSummary = { it.endpointUrl },
                                onSelectionChange = viewModel::setProfileVisible,
                                emptyText = stringResource(R.string.client_no_configured_nodes),
                            )

                        }
                    }
                }
                val visibleNodes = nodes.filter { it.profileId in visibleProfileIds }
                if (visibleNodes.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.client_select_node),
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
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 2.dp,
    ) {
        Column {
            OSHClickableStatusRowWithIcon(
                title = node.name,
                imageVector = Icons.Filled.Cloud,
                contentDescription = stringResource(R.string.content_desc_server),
                subtitle = node.endpointUrl,
                summary = when {
                    node.loading -> stringResource(R.string.client_discovering_systems)
                    node.error != null -> node.error
                    node.systems.isEmpty() -> stringResource(R.string.client_tap_to_discover)
                    else -> stringResource(R.string.client_systems_discovered, node.systems.size)
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
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column {
            HorizontalDivider()
            OSHClickableStatusRowWithIcon(
                title = system.name,
                imageVector = Icons.Filled.Sensors,
                contentDescription = stringResource(R.string.content_desc_system),
                subtitle = system.uid,
                summary = stringResource(R.string.client_datastream_count, system.visualizations.size),
                status = system.status,
                onClick = onClick,
            )
        }
    }
}
