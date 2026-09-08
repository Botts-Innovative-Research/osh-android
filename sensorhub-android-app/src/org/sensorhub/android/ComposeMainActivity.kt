package org.sensorhub.android

import org.sensorhub.android.config.SensorHubConfigFactory
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.sensorhub.android.SensorHubService.LocalBinder
import org.sensorhub.android.ui.screens.profiles.ServerProfileRepository
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.api.module.IModuleConfigRepository
import org.sensorhub.impl.client.sost.SOSTClient
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver
import org.sensorhub.impl.sensor.controller.ControllerDriver
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

class ComposeMainActivity: ComponentActivity(), SensorHubServiceProvider {
    val ACTION_BROADCAST_RECEIVER: String = "org.sensorhub.android.BROADCAST_RECEIVER"
    val ANDROID_SENSORS_LAST_UPDATED: Date = Date(Instant.now().toEpochMilli())
    val log: Logger = LoggerFactory.getLogger(ComposeMainActivity::class.java)

    private var _boundService: SensorHubService? = null
    private var _sensorhubConfig: IModuleConfigRepository? = null
    private var _oshStarted: Boolean = false
    private var _showVideo: Boolean = false
    var deviceID: String? = null
    var runName: String? = null

    private var broadcastReceiver: BroadcastReceiver? = null

    var sostClients: CopyOnWriteArrayList<SOSTClient?> = CopyOnWriteArrayList<SOSTClient?>()
    var conSysClients: CopyOnWriteArrayList<ConSysApiClientModule?> = CopyOnWriteArrayList<ConSysApiClientModule?>()
    private var _androidSensors: AndroidSensorsDriver? = null


    private val sConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            _boundService = (service as LocalBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _boundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                OSHApp()
            }
        }
    }

    override fun getBoundService(): SensorHubService? {
       return _boundService
    }

    override fun isOshStarted(): Boolean {
        return _oshStarted
    }

    override fun setOshStarted(started: Boolean) {
        this._oshStarted = started
    }

    override fun getSensorhubConfig(): IModuleConfigRepository? {
        return _sensorhubConfig
    }

    override fun getSostClients(): List<SOSTClient?>? {
        return sostClients
    }

    override fun getConSysClients(): List<ConSysApiClientModule?>? {
        return conSysClients
    }

    override fun getAndroidSensors(): AndroidSensorsDriver? {
        return _androidSensors
    }

    override fun setAndroidSensors(driver: AndroidSensorsDriver?) {
        this._androidSensors = driver
    }

    override fun getShowVideo(): Boolean {
        return _showVideo
    }

    }

    override fun updateConfig(prefs: SharedPreferences?, runName: String?) {
        val configuration = configFactory.create(requireNotNull(prefs), runName)
        _sensorhubConfig = configuration.modules
        deviceID = configuration.deviceId
        _showVideo = configuration.cameraEnabled
    }

    fun shouldServe(prefs: SharedPreferences): Boolean {
        val prefMap = prefs.getAll()
        for (pref in prefMap.entries) {
            if (pref.value is java.util.HashSet<*>) {
                if ((pref.value as java.util.HashSet<*>).contains("FETCH_LOCAL")) {
                    return true
                }
            }
        }
        return false
    }

    override fun startSensorHub() {
        if (_boundService != null && _sensorhubConfig != null)
            _boundService?.startSensorHub(_sensorhubConfig, _showVideo)
    }

    override fun stopSensorHub() {
        sostClients.clear()
        conSysClients.clear()
        if (_boundService != null)
            _boundService?.stopSensorHub()
        _oshStarted = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}
