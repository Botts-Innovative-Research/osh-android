package org.sensorhub.android.ui.screens.dashboard

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver
import kotlin.math.max

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    init {

    }

    override fun onCleared() {
        super.onCleared()
    }

    fun stopHub() {

    }

    fun updateFabIcon() {

    }

    fun showRunNameDialog() {

    }
//    Statuses
    fun refreshStatus() {

    }
    fun displayStatus(){

    }

//    Meshtastic
    fun sendMeshtasticMessage() {

    }
    fun showMeshtasticDialog() {

    }

//    Video
    fun flipCamera() {

    }

    fun adjustZoom(direction: Int) {
//        val sensors: AndroidSensorsDriver? = provider.getAndroidSensors()
//        if (sensors == null) return
//
//        try {
//            currentZoomLevel = max(0, currentZoomLevel + direction)
//            sensors.setCameraZoom(currentZoomLevel)
//        } catch (e: Exception) {
//            Toast.makeText(requireContext(), "Zoom not supported", Toast.LENGTH_SHORT).show()
//        }
    }

    fun showVideo() {

    }

    fun hideVideo(){

    }
}