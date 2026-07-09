package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.Navbar
import org.sensorhub.android.ui.components.OSHAlertDialogWithoutDismiss
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableCard
import org.sensorhub.android.ui.components.OSHClickableCardWithIcon
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHEditableRow
import org.sensorhub.android.ui.components.OSHInfoRow
import org.sensorhub.android.ui.components.OSHRowWithIcon
import org.sensorhub.android.ui.components.OSHSingleChoiceDialog
import org.sensorhub.android.ui.components.OSHSwitchCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.Secondary

@Composable
fun AppPreferencesScreen(
    onBackClick: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceHost by remember { mutableStateOf("") }
    var appVersion by remember { mutableStateOf("") }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showDeviceNameEdit by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }

    if(showLanguageDialog) {
        OSHSingleChoiceDialog(
            onDismissRequest = { showLanguageDialog = false },
            dialogTitle = stringResource(R.string.title_language_dialog),
            icon = Icons.Filled.Language,
            options = listOf("English", "中文 (台灣)", "Español", "Français", "Deutsch"),
            selectedIndex = 0,
            onOptionSelected = {}
        )
    }

    if (showNotificationDialog) {

    }

    if (showAboutAppDialog) {
        OSHAlertDialogWithoutDismiss(
            onConfirmation = { showLanguageDialog = false },
            dialogText = stringResource(R.string.about_description),
            dialogTitle = stringResource(R.string.app_name),
            icon = Icons.Filled.Info
        )
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
            stringResource(R.string.app_preferences),
                onBackClick = {

                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            OSHCard {
                OSHEditableRow(
                    label = stringResource(R.string.device_name),
                    value = deviceName.ifEmpty { "MyDevice" },
                    onClick = { showDeviceNameEdit = true }
                )
                OSHInfoRow(
                    label = stringResource(R.string.device_ip_address),
                    value = deviceHost
                )
                OSHInfoRow(
                    label = stringResource(R.string.title_version),
                    value = appVersion
                )
            }

            OSHCard {
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_notifications),
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.title_notifications),
                    onClick = { showNotificationDialog = true }
                )
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_language),
                    imageVector = Icons.Default.Language,
                    contentDescription = stringResource(R.string.title_language),
                    onClick = { showLanguageDialog = true }
                )
            }
            OSHCard {
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_about),
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.title_about),
                    onClick = { showAboutAppDialog = true }
                )
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_help_faq),
                    imageVector = Icons.Default.Help,
                    contentDescription = stringResource(R.string.title_help_faq),
                    onClick = { /* navigate to help/faq screen */ }
                )
            }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppPreferencesScreenPreview() {
    OSHTheme {
        AppPreferencesScreen({})
    }
}
