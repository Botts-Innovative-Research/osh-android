package org.sensorhub.android.ui.screens

import android.app.Application
import android.preference.PreferenceManager
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sensorhub.android.R
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHClickableCardWithIcon
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme

data class ServersState(
    val sosEnabled: Boolean = true,
    val csApiEnabled: Boolean = true,
    val discoveryEnabled: Boolean = false
)

class ServersViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val _state = MutableStateFlow(ServersState())
    val state: StateFlow<ServersState> = _state.asStateFlow()

    init {
        _state.value = ServersState(
            sosEnabled = prefs.getBoolean("sos_service", true),
            csApiEnabled = prefs.getBoolean("csapi_service", true),
            discoveryEnabled = prefs.getBoolean("discovery_service", false)
        )
    }

    fun setSosEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sos_service", enabled).apply()
        _state.value = _state.value.copy(sosEnabled = enabled)
    }

    fun setCsApiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("csapi_service", enabled).apply()
        _state.value = _state.value.copy(csApiEnabled = enabled)
    }

    fun setDiscoveryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("discovery_service", enabled).apply()
        _state.value = _state.value.copy(discoveryEnabled = enabled)
    }
}

@Composable
fun ServersScreen(
    navController: NavController = rememberNavController(),
    viewModel: ServersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_servers),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AppPreferences.route) }) {
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
                onClick = { navController.navigate(Screen.ServerProfiles.route) },
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
        ServersScreen()
    }
}
