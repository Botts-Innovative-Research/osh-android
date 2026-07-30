package org.sensorhub.android.ui.screens.servers

data class ServersState(
    val sosEnabled: Boolean = true,
    val csApiEnabled: Boolean = true,
    val discoveryEnabled: Boolean = false
)