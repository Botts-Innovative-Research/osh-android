package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.Navbar
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo


@Composable
fun HomeScreen(navController: NavController = rememberNavController()) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_dashboard),
                actions = {
                    IconButton(onClick = { /* open app preferences */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = ""
                        )
                    }
                },
            ) },
        bottomBar = { Navbar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

        }

    }
}