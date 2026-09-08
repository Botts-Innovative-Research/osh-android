package org.sensorhub.android.ui.screens.settings

data class ServersState(
    val sosEnabled: Boolean = true,
    val csApiEnabled: Boolean = true,
    val discoveryEnabled: Boolean = false,
    val rulesLink: String = ""
)