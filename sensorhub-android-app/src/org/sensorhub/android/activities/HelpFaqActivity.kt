package org.sensorhub.android.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.sensorhub.android.R
import org.sensorhub.android.ui.screens.FaqItem
import org.sensorhub.android.ui.screens.HelpFaqScreen
import org.sensorhub.android.ui.theme.OSHTheme

class HelpFaqActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val questions = resources.getStringArray(R.array.faq_questions)
        val answers = resources.getStringArray(R.array.faq_answers)
        val categories = resources.getStringArray(R.array.faq_categories)

        val items = buildList {
            var currentCategory = ""
            for (i in questions.indices) {
                val category = categories[i]
                if (category != currentCategory) {
                    add(FaqItem.Header(category))
                    currentCategory = category
                }
                add(FaqItem.Entry(questions[i], answers[i]))
            }
        }

        setContent {
            OSHTheme {
                HelpFaqScreen(
                    items = items,
                    onBackClick = { finish() }
                )
            }
        }
    }
}