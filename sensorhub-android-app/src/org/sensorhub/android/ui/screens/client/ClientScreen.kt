package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme
@Composable
fun ClientScreen(onNavigateToPreferences: () -> Unit, viewModel: ClientViewModel = viewModel()) {
}

@Composable
private fun ClientContent(servers: List<ClientServerState>, onRefresh: (String) -> Unit, onPreferences: () -> Unit) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(title = stringResource(R.string.tab_client), actions = {
                IconButton(onClick = onPreferences) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.app_preferences))
                }
            })
        }, containerColor = Background
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    }
}

