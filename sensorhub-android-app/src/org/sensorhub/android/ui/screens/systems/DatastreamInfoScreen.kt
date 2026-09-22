package org.sensorhub.android.ui.screens.systems

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background

class DatastreamInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NodeRepository.getInstance(application)
    var resource by mutableStateOf<JsonObject?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    private var current: Triple<String, String, ResourceKind>? = null
    private var request: Job? = null

    fun load(serverId: String, streamId: String, kind: ResourceKind, retry: Boolean = false) {
        val key = Triple(serverId, streamId, kind)
        if (current == key && !retry) return
        current = key
        request?.cancel()
        request = viewModelScope.launch {
            loading = true
            failed = false
            resource = null
            try {
                resource = when (kind) {
                    ResourceKind.CONTROLSTREAMS -> repository.getControlstream(serverId, streamId)
                    else -> repository.getDatastream(serverId, streamId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun DatastreamInfoScreen(
    serverId: String,
    datastreamId: String,
    kind: ResourceKind = ResourceKind.DATASTREAMS,
    onBackClick: () -> Unit,
    viewModel: DatastreamInfoViewModel = viewModel()
) {
    LaunchedEffect(serverId, datastreamId, kind) { viewModel.load(serverId, datastreamId, kind) }
    Scaffold(
        topBar = { OSHTopAppBarWithBack((viewModel.resource)?.text("name").toString(), onBackClick) },
        containerColor = Background
    ) { padding ->
        val resource = viewModel.resource
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            when {
                viewModel.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
                viewModel.failed -> item {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.client_info_failed), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.load(serverId, datastreamId, kind, retry = true) }) {
                            Text(stringResource(R.string.client_info_retry))
                        }
                    }
                }
                resource != null -> {
                    item {
                        OSHCard {
                            InfoField(R.string.client_info_name, resource.text("name"))
                            HorizontalDivider()
                            InfoField(R.string.client_info_id, resource.text("id"))
                            HorizontalDivider()
                            if (kind == ResourceKind.CONTROLSTREAMS) {
                                InfoField(R.string.client_info_input, resource.text("inputName"))
                            } else {
                                InfoField(R.string.client_info_output, resource.text("outputName"))
                            }
                            HorizontalDivider()
                            InfoField(R.string.client_info_system, resource.text("system@id"))
                            HorizontalDivider()
                            InfoField(R.string.client_info_valid_time, resource.timeRange("validTime"))
                            HorizontalDivider()
                            if (kind == ResourceKind.CONTROLSTREAMS) {
                                InfoField(R.string.client_info_issue_time, resource.timeRange("issueTime"))
                                HorizontalDivider()
                                InfoField(R.string.client_info_execution_time, resource.timeRange("executionTime"))
                            } else {
                                InfoField(R.string.client_info_phenomenon_time, resource.timeRange("phenomenonTime"))
                            }
                            HorizontalDivider()
                            val formats = resource.get("formats")?.takeIf { it.isJsonArray }?.asJsonArray
                                ?.mapNotNull { it.takeIf { it.isJsonPrimitive }?.asString }.orEmpty()
                            InfoField(R.string.client_info_formats, formats.joinToString(", "))
                        }
                    }
                }
            }
        }
    }
}

private fun JsonObject.text(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
private fun JsonObject.timeRange(key: String): String? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray
    ?.joinToString(" → ") { if (it.isJsonNull) "…" else if (it.isJsonPrimitive) it.asString else "…" }
    ?.takeIf { it.isNotBlank() }

@Composable
private fun InfoField(label: Int, value: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.client_info_missing),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
