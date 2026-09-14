package org.sensorhub.android

import org.sensorhub.android.ui.SensorHubViewModel

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.navigation.Navbar
import org.sensorhub.android.ui.navigation.OSHNavHost
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun OSHApp(hubViewModel: SensorHubViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { Navbar(navController = navController) }
    ) { padding ->
        OSHNavHost(
            navController = navController,
            hubViewModel = hubViewModel,
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun MainScreenPreview() {
    OSHTheme {
        OSHApp()
    }
}
