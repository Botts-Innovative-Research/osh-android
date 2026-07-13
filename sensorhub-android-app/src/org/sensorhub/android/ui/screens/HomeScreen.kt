package org.sensorhub.android.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo


@Composable
fun HomeScreen(navController: NavController = rememberNavController()) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AppPreferences.route)}) {
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