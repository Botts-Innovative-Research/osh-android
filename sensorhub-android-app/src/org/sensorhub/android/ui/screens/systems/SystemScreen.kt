package org.sensorhub.android.ui.screens.systems

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHDropDown
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme
@Composable
fun SystemsScreen(onNavigateToSettings: () -> Unit, onOpenSystem: (String, String) -> Unit, viewModel: SystemsViewModel = viewModel()) {
}
@Composable
private fun SystemsContent(
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

            item(key = "server-selector") {
                OSHDropDown(
                    title = stringResource(R.string.client_servers_title),
                    summary = stringResource(R.string.client_selected_servers, selectedIds.size),
                    emptyText = stringResource(R.string.client_no_saved_servers),
                )
            }
}
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SystemsScreenPreview() {
}
