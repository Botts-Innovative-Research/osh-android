package org.sensorhub.android.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.sensorhub.android.ui.screens.AppPreferencesScreen
import org.sensorhub.android.ui.theme.OSHTheme

class AppPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                AppPreferencesScreen(onBackClick = { finish() })
            }
        }
    }
}