package org.sensorhub.android

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import org.sensorhub.android.SensorHubService.LocalBinder
import org.sensorhub.android.ui.navigation.Navbar
import org.sensorhub.android.ui.navigation.OSHNavHost
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.api.module.IModuleConfigRepository
import org.sensorhub.impl.client.sost.SOSTClient
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

class ComposeMainActivity: ComponentActivity(), SensorHubServiceProvider {
    val ACTION_BROADCAST_RECEIVER: String = "org.sensorhub.android.BROADCAST_RECEIVER"
    val ANDROID_SENSORS_MODULE_ID: String = "ANDROID_SENSORS"
    val ANDROID_SENSORS_LAST_UPDATED: Date = Date(Instant.now().toEpochMilli())
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)

    var boundService: SensorHubService? = null;
    var sensorhubConfig: IModuleConfigRepository? = null
    var oshStarted: Boolean = false
    var showVideo: Boolean = false
    var deviceID: String? = null
    var runName: String? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    var sostClients: CopyOnWriteArrayList<SOSTClient?> = CopyOnWriteArrayList<SOSTClient?>()
    var conSysClients: CopyOnWriteArrayList<ConSysApiClientModule?> = CopyOnWriteArrayList<ConSysApiClientModule?>()
    var androidSensors: AndroidSensorsDriver? = null


    private val sConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as LocalBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                MainScreen()
            }
        }
    }

    override fun getBoundService(): SensorHubService? {
       return boundService
    }

    override fun isOshStarted(): Boolean {
        return oshStarted
    }

    override fun setOshStarted(started: Boolean) {
        this.oshStarted = started
    }

    override fun getSensorhubConfig(): IModuleConfigRepository? {
        return sensorhubConfig
    }

    override fun getSostClients(): List<SOSTClient?>? {
        return sostClients
    }

    override fun getConSysClients(): List<ConSysApiClientModule?>? {
        return conSysClients
    }

    override fun getAndroidSensors(): AndroidSensorsDriver? {
        return androidSensors
    }

    override fun setAndroidSensors(driver: AndroidSensorsDriver?) {
        this.androidSensors = driver
    }

    override fun getShowVideo(): Boolean {
        return showVideo
    }

    override fun updateConfig(prefs: SharedPreferences?, runName: String?) {
        deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

    }

    override fun startSensorHub() {
        if (boundService != null && sensorhubConfig != null)
            boundService?.startSensorHub(sensorhubConfig, showVideo)
    }

    override fun stopSensorHub() {
        sostClients.clear()
        conSysClients.clear()
        if (boundService != null)
            boundService?.stopSensorHub()
        oshStarted = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { Navbar(navController = navController) }
    ) { padding ->
        OSHNavHost(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun MainScreenPreview() {
    OSHTheme {
        MainScreen()
    }
}
