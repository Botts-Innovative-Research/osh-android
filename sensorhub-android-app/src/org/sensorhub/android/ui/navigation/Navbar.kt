package org.sensorhub.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.ui.theme.BottomNavBg
import org.sensorhub.android.ui.theme.BottomNavSelected
import org.sensorhub.android.ui.theme.BottomNavUnselected
import org.sensorhub.android.ui.theme.AccentOrangeDim
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun Navbar(
    navController: NavController
){
    val selectedNavigationIndex = rememberSaveable { mutableIntStateOf(0) }
    val navigationItems = listOf(
        NavigationItem(
            title = "Dashboard",
            icon = Icons.Default.Home,
            route = Screen.Dashboard.route
        ),
        NavigationItem(
            title = "Sensors",
            icon = Icons.Default.Sensors,
            route = Screen.Sensors.route
        ),
        NavigationItem(
            title = "Servers",
            icon = Icons.Default.Dns,
            route = Screen.Servers.route
        )
    )

    NavigationBar(
        containerColor = BottomNavBg
    ) {
        navigationItems.forEachIndexed { index, item  ->
        NavigationBarItem(
            selected = selectedNavigationIndex.intValue == index,
            onClick = {
                selectedNavigationIndex.intValue = index
                navController.navigate(item.route)
            },
            icon = {
                Icon(imageVector = item.icon, contentDescription = item.title)
            },
            label = {
                Text(
                    item.title,
                    color = if (index == selectedNavigationIndex.intValue)
                        BottomNavSelected
                    else BottomNavUnselected
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavSelected,
                indicatorColor = AccentOrangeDim
            )

        )}
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun NavbarPreview() {
    OSHTheme {
        Navbar(navController = rememberNavController())
    }
}