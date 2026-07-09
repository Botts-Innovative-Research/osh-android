package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import org.sensorhub.android.ui.Navbar
import org.sensorhub.android.ui.components.OSHClickableCard
import org.sensorhub.android.ui.components.OSHSwitchCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun ServersScreen(navController: NavController = rememberNavController()) {
    var sosEnabled by remember { mutableStateOf(false) }
    var csApiEnabled by remember { mutableStateOf(false) }
    var discoveryEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
            "Servers",
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "App preferences"
                        )
                    }
                },
            ) },
        bottomBar = { Navbar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            OSHClickableCard(
                onCardClick = {},
                title = "Manage Servers",
                subtitle = "0 of 0 server(s) enabled"
            )
            OSHSwitchCard(
                title = "SOS-T Service",
                subtitle = "OGC SOS Transactional service",
                checked = sosEnabled,
                onCheckedChange = { sosEnabled = it }
            )
            OSHSwitchCard(
                title = "Connected Systems Service",
                subtitle = "OGC Connected Systems API service",
                checked = csApiEnabled,
                onCheckedChange = { csApiEnabled = it }
            )
            OSHSwitchCard(
                title = "Discovery Service",
                subtitle = "Sensor discovery based on rulesets",
                checked = discoveryEnabled,
                onCheckedChange = { discoveryEnabled = it }
            )
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
