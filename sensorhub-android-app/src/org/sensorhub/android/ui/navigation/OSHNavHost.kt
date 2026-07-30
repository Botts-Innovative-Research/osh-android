package org.sensorhub.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.sensorhub.android.ui.screens.preferences.AppPreferencesScreen
import org.sensorhub.android.ui.screens.appstatus.AppStatusScreen
import org.sensorhub.android.R
import org.sensorhub.android.ui.screens.help.FaqItem
import org.sensorhub.android.ui.screens.help.HelpFaqScreen
import org.sensorhub.android.ui.screens.dashboard.HomeScreen
import org.sensorhub.android.ui.screens.sensors.SensorsScreen
import org.sensorhub.android.ui.screens.profiles.ServerFormScreen
import org.sensorhub.android.ui.screens.servers.ServersScreen
import org.sensorhub.android.ui.screens.profiles.ServerProfilesScreen

@Composable
fun OSHNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            HomeScreen(
                onNavigateToAppPreferences = { navController.navigate(Screen.AppPreferences.route) }
            )
        }
        composable(Screen.Sensors.route) {
            SensorsScreen(
                onNavigateToAppPreferences = { navController.navigate(Screen.AppPreferences.route) }
            )
        }
        composable(Screen.Servers.route) {
            ServersScreen(
                onNavigateToAppPreferences = { navController.navigate(Screen.AppPreferences.route) },
                onNavigateToServerProfiles = { navController.navigate(Screen.ServerProfiles.route) }
            )
        }
        composable(Screen.AppStatus.route) {
            AppStatusScreen(
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

        composable(Screen.AppPreferences.route) {
            AppPreferencesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToAppStatus = { navController.navigate(Screen.AppStatus.route) },
                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) }
            )
        }
        composable(Screen.ServerProfiles.route) {
            ServerProfilesScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
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
