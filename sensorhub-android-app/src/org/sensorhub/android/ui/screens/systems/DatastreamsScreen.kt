package org.sensorhub.android.ui.screens.systems

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.data.VIDEO_DEF
import org.sensorhub.android.ui.components.OSHStreamCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background

@Composable
fun DataStreamsScreen(
    serverId: String,
    systemId: String,
    onBackClick: () -> Unit,
    viewModel: SystemsViewModel = viewModel(),
    handleSchemaInfo: (SystemsResource, ResourceKind) -> Unit
) {
    val selectedServerStates by viewModel.selectedServerStates.collectAsState()
    LaunchedEffect(Unit) { viewModel.open() }
    val server = selectedServerStates.firstOrNull { it.id == serverId }
    val systemState = server?.collections?.get(ResourceKind.SYSTEMS)
    val system = systemState?.resources?.firstOrNull { it.id == systemId }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(title = system?.name.toString(), onBackClick = onBackClick)
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (server != null && system != null) {
                val dsState = server.collections.getValue(ResourceKind.DATASTREAMS)
                val datastreams = dsState.resources.filter { it.systemId == system.id }
                items(datastreams, key = { "data:${it.id}" }) { ds ->
                    OSHStreamCard(
                        title = ds.name,
                        onClick = { handleSchemaInfo(ds, ResourceKind.DATASTREAMS) },
                        status = "ok",
                        isVideo = ds.hasResource(true, VIDEO_DEF),
                    )
                }

                val csState = server.collections.getValue(ResourceKind.CONTROLSTREAMS)
                val controlstreams = csState.resources.filter { it.systemId == system.id }
                items(controlstreams, key = { "control:${it.id}" }) { cs ->
                    OSHStreamCard(
                        title = cs.name,
                        onClick = { handleSchemaInfo(cs, ResourceKind.CONTROLSTREAMS) },
                        status = "ok",
                        isVideo = false
                    )
                }

            }
        }
    }
}
