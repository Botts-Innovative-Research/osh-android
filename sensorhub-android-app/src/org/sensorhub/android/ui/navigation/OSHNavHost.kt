package org.sensorhub.android.ui.navigation


import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.sensorhub.android.R
import org.sensorhub.android.ui.SensorHubViewModel
import org.sensorhub.android.ui.features.client.visualizations.FullscreenMap
import org.sensorhub.android.ui.features.client.OshClientScreen
import org.sensorhub.android.ui.features.client.OshClientViewModel
import org.sensorhub.android.ui.features.client.SystemDetailRoute
import org.sensorhub.android.ui.features.dashboard.DashboardRoute
import org.sensorhub.android.ui.features.settings.help.FaqItem
import org.sensorhub.android.ui.features.settings.help.HelpFaqScreen
import org.sensorhub.android.ui.features.maps.MapScreen
import org.sensorhub.android.ui.features.nodes.NodeScreen
import org.sensorhub.android.ui.features.nodes.ondevice.OnDeviceNodeScreen
import org.sensorhub.android.ui.features.sensors.SensorsScreen
import org.sensorhub.android.ui.features.servers.ServerFormScreen
import org.sensorhub.android.ui.features.servers.ServerProfilesScreen
import org.sensorhub.android.ui.features.settings.SettingsScreen

@Composable
fun OSHNavHost(
    navController: NavHostController,
    hubViewModel: SensorHubViewModel,
    clientViewModel: OshClientViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {

        // settings
        composable(Screen.Settings.route) {
            val context = LocalContext.current
            SettingsScreen(
                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) },
                onNavigateToRepo = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/botts-innovative-research/osh-android".toUri())) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.HelpFaq.route) {
            val context = LocalContext.current
            val questions = context.resources.getStringArray(R.array.faq_questions)
            val answers = context.resources.getStringArray(R.array.faq_answers)
            val categories = context.resources.getStringArray(R.array.faq_categories)

            val faqItems = buildFaqItems(questions, answers, categories)

            HelpFaqScreen(
                items = faqItems,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardRoute(
                viewModel = hubViewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Sensors.route) {
            SensorsScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                tracks = clientViewModel.tracks,
            )
        }

        composable(Screen.Node.route) {
            NodeScreen(
                onNavigateToVisualizations = { navController.navigate(Screen.Client.route) { launchSingleTop = true } },
                onNavigateToNodes = { navController.navigate(Screen.ServerProfiles.route) { launchSingleTop = true } },
            )
        }

        // visualizations
        composable(Screen.Client.route) {
            OshClientScreen(
                onNavigateToNodeManager = { navController.navigate(Screen.ServerProfiles.route) { launchSingleTop = true } },
                onOpenSystem = { profileId, systemId ->
                    navController.navigate(Screen.SystemDetail.createRoute(profileId, systemId))
                },
                viewModel = clientViewModel,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.SystemDetail.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("systemId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            SystemDetailRoute(
                profileId = backStackEntry.arguments?.getString("profileId").orEmpty(),
                systemId = backStackEntry.arguments?.getString("systemId").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenFullscreenMap = { streamId ->
                    navController.navigate(Screen.FullscreenMap.createRoute(
                        backStackEntry.arguments?.getString("profileId").orEmpty(),
                        backStackEntry.arguments?.getString("systemId").orEmpty(),
                        streamId,
                    ))
                },
                viewModel = clientViewModel,
            )
        }
        composable(
            route = Screen.FullscreenMap.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("systemId") { type = NavType.StringType },
                navArgument("streamId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            FullscreenMap(
                onBackClick = { navController.popBackStack() },
                profileId = backStackEntry.arguments?.getString("profileId").orEmpty(),
                systemId = backStackEntry.arguments?.getString("systemId").orEmpty(),
                streamId = backStackEntry.arguments?.getString("streamId").orEmpty(),
                viewModel = clientViewModel,
            )
        }


        // node manager
        composable(Screen.ServerProfiles.route) {
            ServerProfilesScreen(
                onBackClick = { navController.popBackStack() },
                onOpenThisDevice = { navController.navigate(Screen.OnDeviceNode.route) },
                navController = navController
            )
        }
        composable(Screen.OnDeviceNode.route) {
            OnDeviceNodeScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Screen.ServerForm.route,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")
            ServerFormScreen(
                onBackClick = { navController.popBackStack() },
                profileId = profileId
            )
        }
    }
}


private fun buildFaqItems(
    questions: Array<String>,
    answers: Array<String>,
    categories: Array<String>
): List<FaqItem> {
    val items = mutableListOf<FaqItem>()
    var currentCategory = ""
    for (i in questions.indices) {
        val category = categories[i]
        if (category != currentCategory) {
            items.add(FaqItem.Header(category))
            currentCategory = category
        }
        items.add(FaqItem.Entry(questions[i], answers[i]))
    }
    return items
}
