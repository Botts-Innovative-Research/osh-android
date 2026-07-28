package org.sensorhub.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.sensorhub.android.ui.screens.AppStatusScreen
import org.sensorhub.android.ui.theme.OSHTheme

class AppStatusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                AppStatusScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}
