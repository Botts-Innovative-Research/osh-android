package org.sensorhub.android.ui.screens.servers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.navigation.Screen
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableCardWithIcon
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun ServersScreen(
    onNavigateToAppPreferences : () -> Unit,
    onNavigateToServerProfiles : () -> Unit,
    viewModel: ServersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_servers),
                actions = {
                    IconButton(onClick = { onNavigateToAppPreferences }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = ""
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            OSHClickableCardWithIcon(
                title = stringResource(R.string.manage_servers),
                imageVector = Icons.Default.Cloud,
                contentDescription = stringResource(R.string.manage_servers),
                onClick = { onNavigateToServerProfiles },
            )

            OSHCard {
                OSHSwitchRow(
                    title = stringResource(R.string.enable_sos_service),
                    subtitle = stringResource(R.string.summary_sos),
                    checked = state.sosEnabled,
                    onCheckedChange = { viewModel.setSosEnabled(it) }
                )
                OSHSwitchRow(
                    title = stringResource(R.string.enable_csapi_service),
                    subtitle = stringResource(R.string.summary_csapi),
                    checked = state.csApiEnabled,
                    onCheckedChange = { viewModel.setCsApiEnabled(it) }
                )
                OSHSwitchRow(
                    title = stringResource(R.string.enable_discovery_service),
                    subtitle = stringResource(R.string.summary_discovery),
                    checked = state.discoveryEnabled,
                    onCheckedChange = { viewModel.setDiscoveryEnabled(it) }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServersScreenPreview() {
    OSHTheme {
        ServersScreen(
            {},
            {}
        )
    }
}
