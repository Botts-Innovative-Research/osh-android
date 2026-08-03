package org.sensorhub.android.ui.screens.dashboard

import android.app.Application
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import org.sensorhub.android.SensorHubServiceProvider
import org.sensorhub.api.command.CommandData
import org.sensorhub.api.command.IStreamingControlInterface
import org.sensorhub.api.common.BigId
import org.sensorhub.impl.module.ModuleRegistry
import org.sensorhub.impl.sensor.meshtastic.MeshtasticSensor
import org.sensorhub.impl.sensor.meshtastic.control.TextMessageControl
import kotlin.math.max

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private var provider: SensorHubServiceProvider? = null
    var isRunning by mutableStateOf(false)
        private set
    var isStarting by mutableStateOf(false)
        private set
    var hasVideo by mutableStateOf(false)
        private set
    var videoPreviewVisible by mutableStateOf(false)
        private set
    var showFlipButton by mutableStateOf(false)
        private set
    var showZoomButtons by mutableStateOf(true)
        private set

    var hasMeshtastic by mutableStateOf(false)
        private set
    private var currentZoomLevel = 0

    init {

    }

    fun syncState(provider: SensorHubServiceProvider) {
        isRunning = provider.isOshStarted
        val service = provider.boundService
        hasVideo = service != null && service.hasVideo()
    }

    fun toggleVideoPreview() {
        videoPreviewVisible = !videoPreviewVisible
    }

    @Suppress("deprecation")
    fun flipCamera(provider: SensorHubServiceProvider) {
        val sensors = provider.androidSensors ?: return
        try {
            val currentId = sensors.configuration.selectedCameraId
            val info = android.hardware.Camera.CameraInfo()
            android.hardware.Camera.getCameraInfo(currentId, info)

            val targetFacing = if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)
                android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
            else
                android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK

            for (i in 0 until android.hardware.Camera.getNumberOfCameras()) {
                val camInfo = android.hardware.Camera.CameraInfo()
                android.hardware.Camera.getCameraInfo(i, camInfo)
                if (camInfo.facing == targetFacing) {
                    sensors.switchCamera(i)
                    currentZoomLevel = 0
                    updateCameraControls(provider)
                    return
                }
            }
        } catch (_: Exception) {
            Toast.makeText(getApplication(), "Failed to switch camera", Toast.LENGTH_SHORT).show()
        }
    }

    fun adjustZoom(direction: Int, provider: SensorHubServiceProvider) {
        val sensors = provider.androidSensors ?: return
        try {
            currentZoomLevel = max(0, currentZoomLevel + direction)
            sensors.setCameraZoom(currentZoomLevel)
        } catch (_: Exception) {
            Toast.makeText(getApplication(), "Zoom not supported", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("deprecation")
    fun updateCameraControls(provider: SensorHubServiceProvider) {
        showFlipButton = android.hardware.Camera.getNumberOfCameras() > 1
        val sensors = provider.androidSensors
        if (sensors != null) {
            try {
                val cameraId = sensors.configuration.selectedCameraId
                val info = android.hardware.Camera.CameraInfo()
                android.hardware.Camera.getCameraInfo(cameraId, info)
                showZoomButtons = info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
            } catch (_: Exception) {
                showZoomButtons = true
            }
        }
    }

    fun onHubStarted(provider: SensorHubServiceProvider) {
        isRunning = true
        isStarting = false
        currentZoomLevel = 0
        hasVideo = provider.boundService?.hasVideo() == true
        if (hasVideo) updateCameraControls(provider)
    }

    fun onHubStopped() {
        isRunning = false
        isStarting = false
        hasVideo = false
        videoPreviewVisible = false
        currentZoomLevel = 0
    }

    fun sendMeshtasticMessage(provider: SensorHubServiceProvider, nodeId: String, message: String){
        val service = provider.getBoundService()

        val reg = service.getSensorHub().getModuleRegistry() as ModuleRegistry

        val meshtastic = reg.getModuleByType(MeshtasticSensor::class.java)
        val textMessageControl: IStreamingControlInterface? =
            meshtastic.getCommandInputs().get(TextMessageControl.NAME)

        val cmdData = textMessageControl!!.getCommandDescription().createDataBlock()
        cmdData.setStringValue(0, message)
        cmdData.setIntValue(1, nodeId.toInt())

        val deviceID = Settings.Secure.getString(
            requireContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        )

        val cmd = CommandData.Builder()
            .withCommandStream(BigId.NONE)
            .withSender(deviceID)
            .withParams(cmdData)
            .build()

        textMessageControl.submitCommand(cmd)
    }

}
