package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.components.OSHClickableCard
import org.sensorhub.android.ui.components.OSHSwitchCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme
import androidx.compose.ui.res.stringResource
import org.sensorhub.android.R
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHSwitchRow

@Composable
fun ServersScreen(navController: NavController = rememberNavController()) {
    var sosEnabled by remember { mutableStateOf(false) }
    var csApiEnabled by remember { mutableStateOf(false) }
    var discoveryEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_servers),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AppPreferences.route)}) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = ""
                        )
                    }
                },
            ) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            OSHCard {
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.manage_servers),
//                    subtitle = "0 of 0 server(s) enabled",
                    imageVector = Icons.Default.Cloud,
                    contentDescription = stringResource(R.string.manage_servers),
                    onClick = { navController.navigate(Screen.ServerProfiles.route)}
                )
            }

            OSHCard {
                OSHSwitchRow(
                    title = stringResource(R.string.enable_sos_service),
                    subtitle = stringResource(R.string.summary_sos),
                    checked = sosEnabled,
                    onCheckedChange = { sosEnabled = it }
                )
                OSHSwitchRow(
                    title = stringResource(R.string.enable_csapi_service),
                    subtitle = stringResource(R.string.summary_csapi),
                    checked = csApiEnabled,
                    onCheckedChange = { csApiEnabled = it }
                )
                OSHSwitchRow(
                    title = stringResource(R.string.enable_discovery_service),
                    subtitle = stringResource(R.string.summary_discovery),
                    checked = discoveryEnabled,
                    onCheckedChange = { discoveryEnabled = it }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServersScreenPreview() {
    OSHTheme {
        ServersScreen()
    }
}
