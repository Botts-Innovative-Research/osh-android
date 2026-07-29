package org.sensorhub.android.ui.screens.appstatus

data class AppStatusState (
    val httpStatus: String = "Unknown",
    val sosStatus: String = "Unknown",
    val conSysStatus: String = "Unknown",
    val discoveryStatus: String = "Unknown",
    val sensorStatus: String = "Unknown",
    val storageStatus: String = "Unknown",
)