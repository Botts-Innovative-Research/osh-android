package org.sensorhub.android.ui.screens.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHAlertDialogWithoutDismiss
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHInfoRow
import org.sensorhub.android.ui.components.OSHSingleChoiceDialog
import org.sensorhub.android.ui.components.OSHTextInputDialog
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun AppPreferencesScreen(
    onBackClick: () -> Unit,
    onNavigateToAppStatus: () -> Unit,
    onNavigateToHelpFaq: () -> Unit,
    viewModel: AppPreferencesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showDeviceNameEdit by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        OSHSingleChoiceDialog(
            onDismissRequest = { showLanguageDialog = false },
            dialogTitle = stringResource(R.string.title_language_dialog),
            icon = Icons.Filled.Language,
            options = LANGUAGE_LABELS,
            selectedIndex = state.selectedLanguageIndex,
            onOptionSelected = { index -> viewModel.selectLanguage(index) }
        )
    }

    if (showNotificationDialog) {
        OSHAlertDialogWithoutDismiss(
            onConfirmation = { showNotificationDialog = false },
            dialogText = "Not yet implemented",
            dialogTitle = stringResource(R.string.title_notifications),
            icon = Icons.Filled.Info
        )
    }

    if (showDeviceNameEdit) {
        OSHTextInputDialog(
            onDismissRequest = { showDeviceNameEdit = false },
            onConfirmation = { newName ->
                viewModel.updateDeviceName(newName)
                showDeviceNameEdit = false
            },
            dialogTitle = stringResource(R.string.device_name),
            dialogText = stringResource(R.string.device_name),
            icon = Icons.Filled.Info,
            initialValue = state.deviceName
        )
    }

    if (showAboutAppDialog) {
        OSHAlertDialogWithoutDismiss(
            onConfirmation = { showAboutAppDialog = false },
            dialogText = stringResource(R.string.about_description),
            dialogTitle = stringResource(R.string.app_name),
            icon = Icons.Filled.Info
        )
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                stringResource(R.string.app_preferences),
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OSHCard {
                OSHInfoRow(
                    label = stringResource(R.string.device_name),
                    value = state.deviceName,
                    onClick = { showDeviceNameEdit = true }
                )
                OSHInfoRow(
                    label = stringResource(R.string.device_ip_address),
                    value = state.deviceIpAddress
                )
                OSHInfoRow(
                    label = stringResource(R.string.title_version),
                    value = state.appVersion
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
                    title = stringResource(R.string.app_status_main_fragment),
                    imageVector = Icons.Default.Cloud,
                    contentDescription = stringResource(R.string.app_status_main_fragment),
                    onClick = { onNavigateToAppStatus}
                )

                OSHClickableRowWithIcon(
                    title = stringResource(R.string.send_feedback),
                    imageVector = Icons.Default.Send,
                    contentDescription = stringResource(R.string.send_feedback_desc),
                    onClick = { }
                )

                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_help_faq),
                    imageVector = Icons.Default.Help,
                    contentDescription = stringResource(R.string.title_help_faq),
                    onClick = { onNavigateToHelpFaq }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppPreferencesScreenPreview() {
    OSHTheme {
        AppPreferencesScreen({}, {}, {})
    }
}
