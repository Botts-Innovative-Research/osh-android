package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.Navbar
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableCard
import org.sensorhub.android.ui.components.OSHEditableRow
import org.sensorhub.android.ui.components.OSHInfoRow
import org.sensorhub.android.ui.components.OSHSwitchCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.Secondary

@Composable
fun AppPreferencesScreen() {

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
            "App Preferences",
                onBackClick = {}
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            OSHCard {
                OSHEditableRow(
                    label = "Device Name",
                    value = "MyDevice",
                    onClick = {}
                )
//                HorizontalDivider(
//                    thickness = 1.dp,
//                    color = Secondary
//                )
                OSHInfoRow(
                    label = "Device IP Address",
                    value = "192.168.1.42"
                )
//                HorizontalDivider(
//                    thickness = 1.dp,
//                    color = Secondary
//                )
                OSHInfoRow(
                    label = "Version",
                    value = "1.0.0"
                )
            }

            OSHCard {

            }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppPreferencesScreenPreview() {
    OSHTheme {
        AppPreferencesScreen()
    }
}
