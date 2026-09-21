package org.sensorhub.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.let {
        it
    }
    val navigationItems = listOf(
        NavigationItem(
            title = "Dashboard",
            icon = Icons.Default.Home,
            route = Screen.Dashboard.route
        ),
        NavigationItem(
            title = "Map",
            icon = Icons.Default.Map,
            route = Screen.Map.route
        ),
        NavigationItem(
            title = "Sensors",
            icon = Icons.Default.Sensors,
            route = Screen.Sensors.route
        ),
    )

    NavigationBar(
        containerColor = BottomNavBg
    ) {
        navigationItems.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route,
            onClick = {
                navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(imageVector = item.icon, contentDescription = item.title)
            },
            label = {
                Text(
                    item.title,
                    color = if (currentRoute == item.route)
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
