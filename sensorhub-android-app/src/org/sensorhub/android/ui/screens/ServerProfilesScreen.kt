package org.sensorhub.android.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sensorhub.android.R
import org.sensorhub.android.server.ServerProfileRepository
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHActionCard
import org.sensorhub.android.ui.components.OSHAddFAB
import org.sensorhub.android.ui.components.OSHAlertDialog
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

data class ServerProfileItem(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 8181,
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
    val useConSysClient: Boolean = false,
    val oAuthEnabled: Boolean = false,
    val enabled: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = ""
)

class ServerProfilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServerProfileRepository.getInstance(application)
    private val _profiles = MutableStateFlow<List<ServerProfileItem>>(emptyList())
    val profiles: StateFlow<List<ServerProfileItem>> = _profiles.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _profiles.value = repo.all.map { p ->
            ServerProfileItem(
                id = p.id,
                name = p.name,
                host = p.host,
                port = p.port,
                endpointPath = p.endpointPath,
                username = p.username,
                enableTls = p.enableTls,
                disableSslCheck = p.disableSslCheck,
                useConSysClient = p.useConSysClient,
                oAuthEnabled = p.oAuthEnabled,
                enabled = p.enabled
            )
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        repo.setEnabled(id, enabled)
        refresh()
    }

    fun delete(id: String) {
        repo.delete(id)
        refresh()
    }
}

@Composable
fun ServerProfilesScreen(
    onBackClick: () -> Unit,
    navController: NavController = rememberNavController(),
    viewModel: ServerProfilesViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var profileToDelete by remember { mutableStateOf<ServerProfileItem?>(null) }
    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = "Server Profiles",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            OSHAddFAB(
                onClick = { navController.navigate(Screen.ServerForm.createRoute()) }
            )
        },
        containerColor = Background
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_server_profiles),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(profiles, key = { it.id }) { item ->
                    OSHActionCard(
                        title = item.name,
                        subtitle = "${if (item.enableTls) "https" else "http"}://${item.host}:${item.port}${item.endpointPath}",
                        checked = item.enabled,
                        onCheckedChange = { enabled -> viewModel.setEnabled(item.id, enabled) },
                        onClick = { navController.navigate(Screen.ServerForm.createRoute(item.id)) },
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit ${item.name}"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerProfilesScreenPreview() {
    OSHTheme {
        ServerProfilesScreen(
            onBackClick = {}
        )
    }
}
