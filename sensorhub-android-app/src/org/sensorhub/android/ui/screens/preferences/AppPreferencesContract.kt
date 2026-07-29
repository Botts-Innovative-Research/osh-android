package org.sensorhub.android.ui.screens.preferences

public val LANGUAGE_LABELS = listOf("English", "中文 (台灣)", "Español", "Français", "Deutsch", "Italiano", "Português")
public val LANGUAGE_VALUES = listOf("en", "zh-TW", "es", "fr", "de", "it", "pt")

data class AppPreferencesState(
    val deviceName: String = "MyDevice",
    val deviceIpAddress: String = "",
    val appVersion: String = "",
    val selectedLanguageIndex: Int = 0
)
