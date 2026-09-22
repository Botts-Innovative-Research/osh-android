package org.sensorhub.android.ui.screens.systems

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHClickableStatusRowWithIcon
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHExpandableCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun SystemsScreen(onNavigateToSettings: () -> Unit, onOpenSystem: (String, String) -> Unit, viewModel: SystemsViewModel = viewModel()) {
    val selectedServerStates by viewModel.selectedServerStates.collectAsState()
    val savedServerOptions by viewModel.savedServerOptions.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    LaunchedEffect(Unit) { viewModel.open() }
    SystemsContent(selectedServerStates, onNavigateToSettings, savedServerOptions, selectedIds, viewModel::setSelected, onOpenSystem)
}

@Composable
private fun SystemsContent(
    selectedServerStates: List<SystemsServerState>,
    onSettings: () -> Unit,
    savedServerOptions: List<SystemsServerOption> = emptyList(),
    selectedIds: Set<String> = emptySet(),
    onSelected: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSystem: (String, String) -> Unit = { _, _ -> }
) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(title = stringResource(R.string.tab_system), actions = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                }
            })
        }, containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (selectedServerStates.isEmpty()) item {
                Text(stringResource(R.string.client_switch_on_server), modifier = Modifier.padding(16.dp))
            }

            item(key = "node-selector") {
                OSHDropDown(
                    title = stringResource(R.string.client_servers_title),
                    summary = stringResource(R.string.client_selected_servers, selectedIds.size),
                    items = savedServerOptions,
                    selectedIds = selectedIds,
                    itemId = { it.id },
                    itemLabel = { it.name },
                    itemSummary = { it.endpointUrl },
                    onSelectionChange = onSelected,
                    emptyText = stringResource(R.string.client_no_saved_servers),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            selectedServerStates.forEach { source ->
                item(key = "server:${source.id}") {
                    val state = source.collections.getValue(ResourceKind.SYSTEMS)
                    OSHExpandableCard(
                        title = source.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        collapsedContent = {
                            Text(
                                text = source.endpointUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CollectionStatus(
                                state,
                                state.resources.isEmpty(),
                                R.string.client_no_systems
                            )
                        },
                        expandedContent = {
                            val datastreamState = source.collections.getValue(ResourceKind.DATASTREAMS)
                            val datastreamCounts = datastreamState.resources.groupingBy { it.systemId }.eachCount()
                            val controlstreamState = source.collections.getValue(ResourceKind.CONTROLSTREAMS)
                            val controlstreamCounts = controlstreamState.resources.groupingBy { it.systemId }.eachCount()
                            state.resources.forEach { entry ->
                                val datastreamCount = datastreamCounts[entry.id] ?: 0
                                val datastreamCountLabel = when {
                                    datastreamState.loading -> stringResource(R.string.client_datastreams_loading)
                                    datastreamState.error != null -> stringResource(R.string.client_datastreams_unavailable)
                                    else -> androidx.compose.ui.res.pluralStringResource(
                                        R.plurals.client_loaded_datastream_count, datastreamCount, datastreamCount
                                    )
                                }

                                val controlStreamCount = controlstreamCounts[entry.id] ?: 0
                                val controlStreamCountLabel = when {
                                    controlstreamState.loading -> stringResource(R.string.client_controlstreams_loading)
                                    controlstreamState.error != null -> stringResource(R.string.client_controlstreams_unavailable)
                                    else -> androidx.compose.ui.res.pluralStringResource(
                                        R.plurals.client_loaded_controlstream_count, controlStreamCount, controlStreamCount
                                    )
                                }
                                HorizontalDivider()

                                OSHClickableStatusRowWithIcon(
                                    title = entry.name,
                                    subtitle = "${entry.systemUid ?: entry.id}",
                                    summary = "$datastreamCountLabel - $controlStreamCountLabel",
                                    imageVector = Icons.Filled.Sensors,
                                    contentDescription = entry.name,
                                    status = "ok",
                                    onClick = {
                                        onOpenSystem(source.id, entry.id)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CollectionStatus(state: CollectionState, empty: Boolean, emptyMessage: Int) {
    when {
        state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        state.error != null -> Text(state.error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        empty -> Text(stringResource(emptyMessage), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SystemsScreenPreview() {
    val system = SystemsResource("041g", "Weather station", null, JsonObject(), "urn:osh:sensor:georobotix:kestrel:1394934")
    val system2 = SystemsResource("041f", "Axis Camera", null, JsonObject(), "urn:osh:sensor:georobotix:kestrel:1394934")
    val system3 = SystemsResource("040g", "Tempest station", null, JsonObject(), "urn:osh:sensor:georobotix:kestrel:1394934")
    val system4 = SystemsResource("042g", "LRF", null, JsonObject(), "urn:osh:sensor:georobotix:kestrel:1394934")

    val server = SystemsServerState("preview", "Local server", mapOf(
        ResourceKind.SYSTEMS to CollectionState(listOf(system, system2, system3, system4), loading = false),
        ResourceKind.DATASTREAMS to CollectionState(loading = false),
        ResourceKind.CONTROLSTREAMS to CollectionState(loading = false)
    ), "http://localhost.dev")
    val server2 = SystemsServerState("preview2", "Digital Ocean server", mapOf(
        ResourceKind.SYSTEMS to CollectionState(listOf(system, system4), loading = false),
        ResourceKind.DATASTREAMS to CollectionState(loading = false),
        ResourceKind.CONTROLSTREAMS to CollectionState(loading = false)
    ), "http://digital.ocean")

    OSHTheme { SystemsContent(listOf(server, server2), {}) }
}
