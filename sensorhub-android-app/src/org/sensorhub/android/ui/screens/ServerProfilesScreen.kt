package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHActionCard
import org.sensorhub.android.ui.components.OSHAddFAB
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun ServerProfilesScreen(
    items: List<ServerProfileItem>,
    onBackClick: () -> Unit,
    navController: NavController = rememberNavController()
) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = "Server Profiles",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            OSHAddFAB(
                onClick = { navController.navigate(Screen.ServerForm.route)}
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(items) { item ->
                var enabled by remember { mutableStateOf(item.enabled) }

                OSHActionCard(
                    title = item.name,
                    subtitle = "${item.host}:${item.port}${item.endpointPath}",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    onClick = { navController.navigate(Screen.ServerForm.route)},
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${item.name}"
                )
            }
        }
    }
}

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

private fun buildSampleServerProfilesItems(): List<ServerProfileItem> = listOf(
    ServerProfileItem(
        id = "1",
        name = "Local Server",
        host = "127.0.0.1",
        port = 8181,
        endpointPath = "/sensorhub/api",
        useConSysClient = true,
        enabled = true
    ),
    ServerProfileItem(
        id = "2",
        name = "Cloud Server",
        host = "osh.example.com",
        port = 443,
        endpointPath = "/sensorhub/api",
        enableTls = true,
        useConSysClient = true,
        enabled = false
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerProfilesScreenPreview() {
    OSHTheme {
        ServerProfilesScreen(
            onBackClick = {},
            items = buildSampleServerProfilesItems()
        )
    }
}
