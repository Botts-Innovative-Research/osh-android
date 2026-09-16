package org.sensorhub.android.ui.screens.settings


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableCardWithIcon
import org.sensorhub.android.ui.components.OSHInputField
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.components.OSHClickableRowWithIcon
import org.sensorhub.android.ui.components.OSHAlertDialogWithoutDismiss
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun SettingsScreen(
    onNavigateToPreferences : () -> Unit,
    onNavigateToProfiles : () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToHelpFaq: () -> Unit,
    onNavigateToRepo: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAbout by rememberSaveable { mutableStateOf(false) }
    if (showAbout) {
        OSHAlertDialogWithoutDismiss(
            onConfirmation = { showAbout = false },
            dialogText = stringResource(R.string.about_description),
            dialogTitle = stringResource(R.string.app_name),
            icon = Icons.Filled.Info
        )
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = stringResource(R.string.action_settings),
                onBackClick = onBackClick
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            OSHClickableCardWithIcon(
                title = stringResource(R.string.manage_servers),
                imageVector = Icons.Default.Cloud,
                contentDescription = stringResource(R.string.manage_servers),
                onClick = onNavigateToProfiles,
            )

            Text(stringResource(R.string.services), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium)
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

                if (state.discoveryEnabled) {
                    OSHInputField(
                        value = state.rulesLink,
                        onValueChange = { viewModel.setDiscoveryRulesLink(it) },
                        label = stringResource(R.string.title_discovery_rules),
                    )
                }
            }
            OSHClickableCardWithIcon(
                title = stringResource(R.string.app_preferences),
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.app_preferences),
                onClick = onNavigateToPreferences
            )
            Text(stringResource(R.string.settings_help_about), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium)
            OSHCard {
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_help_faq),
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = stringResource(R.string.title_help_faq),
                    onClick = onNavigateToHelpFaq
                )
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.title_about),
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.title_about),
                    onClick = { showAbout = true }
                )
                OSHClickableRowWithIcon(
                    title = stringResource(R.string.github_repo),
                    imageVector = Icons.Default.Link,
                    contentDescription = stringResource(R.string.github_repo),
                    onClick = onNavigateToRepo
                )
            }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SettingsScreenPreview() {
    OSHTheme {
        SettingsScreen({}, {}, {}, {}, {})
    }
}
