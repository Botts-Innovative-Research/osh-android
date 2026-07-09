package org.sensorhub.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.sensorhub.android.ui.screens.AppStatusScreen
import org.sensorhub.android.R
import org.sensorhub.android.ui.screens.FaqItem
import org.sensorhub.android.ui.screens.HelpFaqScreen
import org.sensorhub.android.ui.screens.HomeScreen
import org.sensorhub.android.ui.screens.SensorsScreen
import org.sensorhub.android.ui.screens.ServersScreen
import org.sensorhub.android.ui.screens.ServerProfilesScreen
import org.sensorhub.android.ui.screens.SettingsScreen

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
            HomeScreen(navController = navController)
        }
        composable(Screen.Sensors.route) {
            SensorsScreen(navController = navController)
        }
        composable(Screen.Servers.route) {
            ServersScreen(navController = navController)
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

        composable(Screen.ServerProfiles.route) {
            ServerProfilesScreen(
                onBackClick = { navController.popBackStack() }
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
