package org.sensorhub.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.Navbar
import org.sensorhub.android.ui.OSHNavHost
import org.sensorhub.android.ui.theme.OSHTheme

class ComposeMainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { Navbar(navController = navController) }
    ) { padding ->
        OSHNavHost(navController = navController, modifier = Modifier.padding(padding))
    }
}