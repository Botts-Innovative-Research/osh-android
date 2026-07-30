package org.sensorhub.android.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.MainScreen
import org.sensorhub.android.R
import org.sensorhub.android.ui.navigation.Screen
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme


@Composable
fun HomeScreen(
    onNavigateToAppPreferences : () -> Unit,
) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = { onNavigateToAppPreferences }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = ""
                        )
                    }
                },
            ) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

        }

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun HomeScreenPreview() {
    OSHTheme {
        HomeScreen(
            {}
        )
    }
}