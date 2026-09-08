package org.sensorhub.android

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
fun OSHApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { Navbar(navController = navController) }
    ) { padding ->
        OSHNavHost(
            navController = navController,
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
