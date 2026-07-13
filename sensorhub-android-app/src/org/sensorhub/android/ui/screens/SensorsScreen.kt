package org.sensorhub.android.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.R
import org.sensorhub.android.ui.Screen
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHFilterChip
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.OSHTheme


@Composable
fun SensorsScreen(navController: NavController = rememberNavController()) {
    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_sensors),
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
    ) { padding ->
        val filteredItems = items.filter { it.category in selectedCategories }
        val grouped = filteredItems.groupBy { it.category }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OSHFilterChip(
                    onClick = {
                    },
                    text = stringResource(R.string.category_on_device),
                    selected = SensorCategory.ON_DEVICE in selectedCategories,
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = stringResource(R.string.category_on_device)
                )
                OSHFilterChip(
                    onClick = {
                    },
                    text = stringResource(R.string.category_bluetooth),
                    selected = SensorCategory.BLUETOOTH in selectedCategories,
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = stringResource(R.string.category_bluetooth)
                )
                OSHFilterChip(
                    onClick = {
                    },
                    text = stringResource(R.string.category_others),
                    selected = SensorCategory.OTHERS in selectedCategories,
                    imageVector = Icons.Default.DevicesOther,
                    contentDescription = stringResource(R.string.category_others)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

        }

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SensorsScreenPreview() {
    OSHTheme {
        SensorsScreen()
    }
}