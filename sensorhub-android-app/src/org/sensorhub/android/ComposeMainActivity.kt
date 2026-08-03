package org.sensorhub.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.botts.impl.service.discovery.DiscoveryService
import com.botts.impl.service.discovery.DiscoveryServiceConfig
import org.sensorhub.android.SensorHubService.LocalBinder
import org.sensorhub.android.comm.BluetoothCommProvider
import org.sensorhub.android.comm.BluetoothCommProviderConfig
import org.sensorhub.android.comm.ble.BleConfig
import org.sensorhub.android.comm.ble.BleNetwork
import org.sensorhub.android.ui.navigation.Navbar
import org.sensorhub.android.ui.navigation.OSHNavHost
import org.sensorhub.android.ui.screens.profiles.ServerProfileItem
import org.sensorhub.android.ui.screens.profiles.ServerProfileRepository
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.api.module.IModuleConfigRepository
import org.sensorhub.api.sensor.SensorConfig
import org.sensorhub.impl.client.sost.SOSTClient
import org.sensorhub.impl.client.sost.SOSTClientConfig
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig
import org.sensorhub.impl.module.InMemoryConfigDb
import org.sensorhub.impl.module.ModuleClassFinder
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver
import org.sensorhub.impl.sensor.android.audio.AudioEncoderConfig
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig.VideoPreset
import org.sensorhub.impl.sensor.controller.ControllerConfig
import org.sensorhub.impl.sensor.controller.ControllerDriver
import org.sensorhub.impl.sensor.kestrel.KestrelConfig
import org.sensorhub.impl.sensor.meshtastic.MeshtasticConfig
import org.sensorhub.impl.sensor.polar.PolarConfig
import org.sensorhub.impl.sensor.ste.STERadPagerConfig
import org.sensorhub.impl.sensor.template.TemplateConfig
import org.sensorhub.impl.sensor.trupulse.SimulatedDataStream
import org.sensorhub.impl.sensor.trupulse.TruPulseConfig
import org.sensorhub.impl.sensor.trupulse.TruPulseWithGeolocConfig
import org.sensorhub.impl.sensor.wardriving.WardrivingConfig
import org.sensorhub.impl.service.HttpServerConfig
import org.sensorhub.impl.service.consys.ConSysApiService
import org.sensorhub.impl.service.consys.ConSysApiServiceConfig
import org.sensorhub.impl.service.consys.client.ConSysApiClientConfig
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule
import org.sensorhub.impl.service.consys.client.ConSysOAuthConfig
import org.sensorhub.impl.service.sos.SOSService
import org.sensorhub.impl.service.sos.SOSServiceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.FutureTask
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ComposeMainActivity: AppCompatActivity(), SensorHubServiceProvider {
    val ACTION_BROADCAST_RECEIVER: String = "org.sensorhub.android.BROADCAST_RECEIVER"
    val ANDROID_SENSORS_MODULE_ID: String = "ANDROID_SENSORS"
    val ANDROID_SENSORS_LAST_UPDATED: Date = Date(Instant.now().toEpochMilli())
    val log: Logger = LoggerFactory.getLogger(ComposeMainActivity::class.java)

    private var _boundService: SensorHubService? = null
    private var _sensorhubConfig: IModuleConfigRepository? = null
    private var _oshStarted: Boolean = false
    private var _showVideo: Boolean = false
    var deviceID: String? = null
    var runName: String? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
        }
    }

    private var _sostClients: CopyOnWriteArrayList<SOSTClient?> = CopyOnWriteArrayList<SOSTClient?>()
    private var _conSysClients: CopyOnWriteArrayList<ConSysApiClientModule?> = CopyOnWriteArrayList<ConSysApiClientModule?>()
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
                MainScreen()
            }
        }

        hasBluetoothPermissions()
        checkAndRequestPermissions()

        val serviceIntent = Intent(this, SensorHubService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, sConn, Context.BIND_AUTO_CREATE)

        setupBroadcastReceivers()

        requestBatteryOptimizationExemption()
    }

    override fun getBoundService(): SensorHubService? = _boundService
    override fun isOshStarted(): Boolean = _oshStarted
    override fun setOshStarted(started: Boolean) { _oshStarted = started }
    override fun getSensorhubConfig(): IModuleConfigRepository? = _sensorhubConfig
    override fun getSostClients(): List<SOSTClient?> = _sostClients
    override fun getConSysClients(): List<ConSysApiClientModule?> = _conSysClients
    override fun getAndroidSensors(): AndroidSensorsDriver? = _androidSensors
    override fun setAndroidSensors(driver: AndroidSensorsDriver?) { _androidSensors = driver }
    override fun getShowVideo(): Boolean = _showVideo

    override fun updateConfig(prefs: SharedPreferences?, runName: String?) {
        deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        _sensorhubConfig = InMemoryConfigDb(ModuleClassFinder())

        val isApiServiceEnabled = prefs!!.getBoolean("csapi_service", true)
        val isSosServiceEnabled = prefs.getBoolean("sos_service", true)
        val isDiscoveryServiceEnabled = prefs.getBoolean("discovery_service", false)

        val serverRepo = ServerProfileRepository.getInstance(this)
        val enabledServers = serverRepo.getEnabled()

        var disableSslCheck = false
        for (sp in enabledServers) {
            if (sp.disableSslCheck) {
                disableSslCheck = true
                break
            }
        }
        if (disableSslCheck) {
            val trustAllCerts: Array<TrustManager?> =
                arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate?> {
                        val myTrustedAnchors = arrayOfNulls<X509Certificate>(0)
                        return myTrustedAnchors
                    }

                    override fun checkClientTrusted(
                        certs: Array<X509Certificate?>?, authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        certs: Array<X509Certificate?>?, authType: String?
                    ) {
                    }
                }
                ) as Array<TrustManager?>

            try {
                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
                HttpsURLConnection.setDefaultHostnameVerifier(object : HostnameVerifier {
                    override fun verify(arg0: String?, arg1: SSLSession?): Boolean {
                        return true
                    }
                })
            } catch (e: Exception) {
                log.error(e.message)
            }
        }

        var deviceName = prefs!!.getString("device_name", null)
        if (deviceName == null || deviceName.length < 2) deviceName = deviceID

        val sensorsConfig = AndroidSensorsConfig()
        sensorsConfig.name = "Android Sensors [" + deviceName + "]"
        sensorsConfig.id = "ANDROID_SENSORS"
        sensorsConfig.autoStart = true
        sensorsConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED

        sensorsConfig.activateAccelerometer = prefs!!.getBoolean("accel_enabled", false)
        sensorsConfig.activateGyrometer = prefs.getBoolean("gyro_enabled", false)
        sensorsConfig.activateMagnetometer = prefs.getBoolean("mag_enabled", false)
        sensorsConfig.activateOrientationQuat = prefs.getBoolean("orient_quat_enabled", false)
        sensorsConfig.activateOrientationEuler = prefs.getBoolean("orient_euler_enabled", false)
        sensorsConfig.activateGpsLocation = prefs.getBoolean("gps_enabled", false)
        sensorsConfig.activateNetworkLocation = prefs.getBoolean("netloc_enabled", false)
        sensorsConfig.enableCamera = prefs.getBoolean("cam_enabled", false)
        sensorsConfig.selectedCameraId = prefs.getString("camera_select", "0")!!.toInt()
        if (sensorsConfig.enableCamera) _showVideo = true


        // video settings
        sensorsConfig.videoConfig.codec = prefs.getString("video_codec", VideoEncoderConfig.JPEG_CODEC)
        sensorsConfig.videoConfig.frameRate = prefs.getString("video_framerate", "30")!!.toInt()

        val resolutionStr: String = prefs.getString("video_resolution", "640x480")!!
        val resParts: Array<String?> = resolutionStr.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val videoPreset = VideoPreset()
        videoPreset.width = resParts[0]!!.toInt()
        videoPreset.height = resParts[1]!!.toInt()
        sensorsConfig.videoConfig.presets = arrayOf<VideoPreset>(videoPreset)
        sensorsConfig.videoConfig.selectedPreset = 0

        sensorsConfig.outputVideoRoll = prefs.getBoolean("video_roll_enabled", false)


        // audio
        sensorsConfig.activateMicAudio = prefs.getBoolean("audio_enabled", false)
        sensorsConfig.audioConfig.codec = prefs.getString("audio_codec", AudioEncoderConfig.AAC_CODEC)
        sensorsConfig.audioConfig.sampleRate = prefs.getString("audio_samplerate", "8000")!!.toInt()
        sensorsConfig.audioConfig.bitRate = prefs.getString("audio_bitrate", "64")!!.toInt()

        sensorsConfig.runName = runName
        sensorsConfig.uidExtension = prefs.getString("uid_extension", "0")


        // HTTP Server
        val serverConfig = HttpServerConfig()
        serverConfig.proxyBaseUrl = ""
        serverConfig.httpPort = 8585
        serverConfig.autoStart = true
        _sensorhubConfig!!.add(serverConfig)

        // Sensors Config
        _sensorhubConfig!!.add(sensorsConfig)

        // TruPulse
        if (prefs.getBoolean("trupulse_enabled", false)) {
            var trupulseConfig = TruPulseConfig()

            if (sensorsConfig.activateGpsLocation) {
                var gpsOutputName: String? = null
                if (getApplicationContext().getPackageManager()
                        .hasSystemFeature(PackageManager.FEATURE_LOCATION)
                ) {
                    val locationManager =
                        getApplicationContext().getSystemService(LOCATION_SERVICE) as LocationManager
                    val locProviders = locationManager.getAllProviders()
                    for (provName in locProviders) {
                        val locProvider = locationManager.getProvider(provName)
                        if (locProvider!!.requiresSatellite()) gpsOutputName =
                            locProvider.getName().replace(" ".toRegex(), "_") + "_data"
                    }
                }

                trupulseConfig = TruPulseWithGeolocConfig()
                trupulseConfig.locationSourceUID =
                    "urn:osh:android" + sensorsConfig.getAndroidSensorsUidWithExt()
                trupulseConfig.locationOutputName = gpsOutputName
            }

            trupulseConfig.id = "TRUPULSE_SENSOR"
            trupulseConfig.name = "TruPulse Range Finder [" + deviceName + "]"
            trupulseConfig.autoStart = true
            trupulseConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            trupulseConfig.serialNumber = deviceID

            val btConf = BluetoothCommProviderConfig()
            btConf.protocol.deviceName = prefs.getString("trupulse_device_address", "")
            if (prefs.getBoolean("trupulse_simu", false)) {
                btConf.moduleClass = SimulatedDataStream::class.java.getCanonicalName()
            } else {
                btConf.moduleClass = BluetoothCommProvider::class.java.getCanonicalName()
                trupulseConfig.connection.connectTimeout = 100000
                trupulseConfig.connection.reconnectAttempts = 10
            }
            trupulseConfig.commSettings = btConf

            _sensorhubConfig!!.add(trupulseConfig)
        }

        // STE Rad Pager
        if (prefs.getBoolean("ste_radpager_enabled", false)) {
            val steRadPagerConfig = STERadPagerConfig()
            steRadPagerConfig.id = "STE_RADPAGER_SENSOR"
            steRadPagerConfig.name = "STE Rad Pager [" + deviceName + "]"
            steRadPagerConfig.autoStart = true
            steRadPagerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            _sensorhubConfig!!.add(steRadPagerConfig)
        }

        // Meshtastic
        if (prefs.getBoolean("meshtastic_enabled", false)) {
            val meshtasticConfig = MeshtasticConfig()
            meshtasticConfig.id = "MESHTASTIC_SENSOR"
            meshtasticConfig.name = "Meshtastic [" + deviceName + "]"
            meshtasticConfig.autoStart = true
            meshtasticConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            meshtasticConfig.device_name = prefs.getString("meshtastic_device_address", "")
            meshtasticConfig.uid_extension = prefs.getString("uid_extension", "")
            _sensorhubConfig!!.add(meshtasticConfig)
        }

        // Polar HR
        if (prefs.getBoolean("polar_enabled", false)) {
            val polarConfig = PolarConfig()
            polarConfig.id = "POLAR_HEART_SENSOR"
            polarConfig.name = "Polar Heart [" + deviceName + "]"
            polarConfig.autoStart = true
            polarConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            polarConfig.deviceId = prefs.getString("polar_device_address", "")
            polarConfig.uid_extension = prefs.getString("uid_extension", "")
            _sensorhubConfig!!.add(polarConfig)
        }

        // Kestrel Weather
        if (prefs.getBoolean("kestrel_enabled", false)) {
            val bleConf = BleConfig()
            bleConf.id = "BLE_NETWORK"
            bleConf.moduleClass = BleNetwork::class.java.getCanonicalName()
            bleConf.androidContext = this.getApplicationContext()
            bleConf.autoStart = true
            _sensorhubConfig!!.add(bleConf)

            val kestrelConfig = KestrelConfig()
            kestrelConfig.id = "KESTREL_WEATHER"
            kestrelConfig.name = "Kestrel Weather [" + deviceName + "]"
            kestrelConfig.autoStart = true
            kestrelConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            kestrelConfig.networkID = bleConf.id
            kestrelConfig.deviceAddress = prefs.getString("kestrel_device_address", "")
            _sensorhubConfig!!.add(kestrelConfig)
        }

        // Controller
        if (prefs.getBoolean("controller_enabled", false)) {
            val controllerConfig = ControllerConfig()
            controllerConfig.id = "CONTROLLER"
            controllerConfig.name = "Controller [" + deviceName + "]"
            controllerConfig.autoStart = true
            controllerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            controllerConfig.uid_extension = prefs.getString("uid_extension", "")
            _sensorhubConfig!!.add(controllerConfig)
        }

        // Wardriving
        if (prefs.getBoolean("wardriving_enabled", false)) {
            val wardrivingConfig = WardrivingConfig()
            wardrivingConfig.id = "WARDRIVING_"
            wardrivingConfig.name = "Wardriving [" + deviceName + "]"
            wardrivingConfig.autoStart = true
            wardrivingConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            wardrivingConfig.uid_extension = prefs.getString("uid_extension", "")
            _sensorhubConfig!!.add(wardrivingConfig)
        }

        // Template driver
        if (prefs.getBoolean("template_enabled", false)) {
            val templateConfig = TemplateConfig()
            templateConfig.id = "TEMPLATE_DRIVER_"
            templateConfig.name = "Template [" + deviceName + "]"
            templateConfig.autoStart = true
            templateConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED
            templateConfig.uid_extension = prefs.getString("uid_extension", "")
            _sensorhubConfig!!.add(templateConfig)
        }

        if (isAnySensorEnabled(prefs)) {
            for (sp in enabledServers) {
                val profileUrl = sp.buildClientUrl()
                if (profileUrl == null) {
                    log.error(
                        "Skipping server profile '{}': invalid URL",
                        sp.serverName
                    )
                    continue
                }

                val pwd = serverRepo.getPassword(sp.id)

                if (sp.useConSysClient) {
                    val oAuthConfig = ConSysOAuthConfig()
                    oAuthConfig.oAuthEnabled = sp.enableOAuth
                    oAuthConfig.tokenEndpoint = serverRepo.getOAuthTokenEndpoint(sp.id)
                    oAuthConfig.clientID = serverRepo.getOAuthClientId(sp.id)
                    oAuthConfig.clientSecret = serverRepo.getOAuthClientSecret(sp.id)
                    addCSApiConfig(sensorsConfig, sp, profileUrl, sp.username, pwd, oAuthConfig)
                } else {
                    addSosTConfig(sensorsConfig, sp, profileUrl, sp.username, pwd)
                }
            }
        }

        if (shouldStore(prefs)) {
            val dbFile = File(getApplicationContext().getFilesDir().toString() + "/db/")
            dbFile.mkdirs()
            val basicStorageConfig = MVObsSystemDatabaseConfig()
            basicStorageConfig.moduleClass = "org.sensorhub.impl.persistence.h2.MVObsStorageImpl"
            basicStorageConfig.storagePath = dbFile.getAbsolutePath() + "/\${STORAGE_ID}.dat"
            basicStorageConfig.autoStart = true
        }

        // Connected Systems Service
        if (isApiServiceEnabled) {
            val conSysApiService = ConSysApiServiceConfig()
            conSysApiService.moduleClass = ConSysApiService::class.java.getCanonicalName()
            conSysApiService.id = "CON_SYS_SERVICE"
            conSysApiService.name = "Connected Systems API Service"
            conSysApiService.autoStart = true
            conSysApiService.enableTransactional = true
            conSysApiService.exposedResources = ObsSystemDatabaseViewConfig()
            _sensorhubConfig!!.add(conSysApiService)
        }
        // SOS Service
        if (isSosServiceEnabled) {
            val sosConfig = SOSServiceConfig()
            sosConfig.moduleClass = SOSService::class.java.getCanonicalName()
            sosConfig.id = "SOS_SERVICE"
            sosConfig.name = "SOS Service"
            sosConfig.autoStart = true
            sosConfig.enableTransactional = true
            sosConfig.exposedResources = ObsSystemDatabaseViewConfig()
            _sensorhubConfig!!.add(sosConfig)
        }
        // Discovery Service
        if (isDiscoveryServiceEnabled) {
            val discoveryServiceConfig = DiscoveryServiceConfig()
            discoveryServiceConfig.moduleClass = DiscoveryService::class.java.getCanonicalName()
            discoveryServiceConfig.id = "DISCOVERY_SERVICE"
            discoveryServiceConfig.name = "Discovery Service"
            discoveryServiceConfig.autoStart = true

            val outFile = File(getApplicationContext().getFilesDir(), "rules.txt")
            val rulesLink: String = prefs.getString("rules_link", "")!!
            val downloadTask = FutureTask<Void?>(Callable {
                val rulesUrl = URL(rulesLink)
                val conn = rulesUrl.openConnection() as HttpURLConnection
                conn.setInstanceFollowRedirects(true)
                try {
                    conn.getInputStream().use { `in` ->
                        FileOutputStream(outFile).use { out ->
                            val buffer = ByteArray(1024)
                            var len: Int
                            while ((`in`.read(buffer).also { len = it }) > 0) {
                                out.write(buffer, 0, len)
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                null
            })
            Thread(downloadTask).start()
            try {
                downloadTask.get()
            } catch (e: java.lang.Exception) {
                Log.e("OSH - Discovery", "Failed to download rules file", e)
            }
            discoveryServiceConfig.rulesFilePath = outFile.getAbsolutePath()
            _sensorhubConfig!!.add(discoveryServiceConfig)
        }
    }

    override fun startSensorHub() {
        if (_boundService != null && _sensorhubConfig != null)
            _boundService?.startSensorHub(_sensorhubConfig, _showVideo)
    }

    override fun stopSensorHub() {
        _sostClients.clear()
        _conSysClients.clear()
        if (_boundService != null)
            _boundService?.stopSensorHub()
        _oshStarted = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }

        if (_boundService != null) {
            unbindService(sConn)
            _boundService = null
        }
        super.onDestroy()
    }

    protected fun addSosTConfig(
        sensorConf: SensorConfig,
        profile: ServerProfileItem,
        serverUrl: URL,
        user: String?,
        pwd: String?
    ) {
        val sosConfig = SOSTClientConfig()
        sosConfig.id = sensorConf.id + "_SOST_" + profile.id
        sosConfig.name =
            sensorConf.name.replace("\\[.*\\]".toRegex(), "") + " -> " + profile.serverName
        sosConfig.autoStart = true
        sosConfig.sos.remoteHost = serverUrl.getHost()
        sosConfig.sos.remotePort =
            if (serverUrl.getPort() < 0) serverUrl.getDefaultPort() else serverUrl.getPort()
        sosConfig.sos.resourcePath = serverUrl.getPath()
        sosConfig.sos.enableTLS = serverUrl.getProtocol() == "https"
        sosConfig.sos.user = user
        sosConfig.sos.password = pwd
        sosConfig.connection.connectTimeout = 10000
        sosConfig.connection.usePersistentConnection = true
        sosConfig.connection.reconnectAttempts = 9
        sosConfig.connection.maxQueueSize = 100
        sosConfig.dataSourceSelector = ObsSystemDatabaseViewConfig()
        _sensorhubConfig!!.add(sosConfig)
    }

    protected fun addCSApiConfig(
        sensorConf: SensorConfig,
        profile: ServerProfileItem,
        serverUrl: URL,
        apiUser: String?,
        apiPwd: String?,
        oAuthConfig: ConSysOAuthConfig?
    ) {
        val consysConfig = ConSysApiClientConfig()
        consysConfig.id = sensorConf.id + "_CONSYS_" + profile.id
        consysConfig.name =
            sensorConf.name.replace("\\[.*\\]".toRegex(), "") + " -> " + profile.serverName
        consysConfig.autoStart = true
        consysConfig.conSys.remoteHost = serverUrl.getHost()
        consysConfig.conSys.remotePort =
            if (serverUrl.getPort() < 0) serverUrl.getDefaultPort() else serverUrl.getPort()
        consysConfig.conSys.resourcePath = serverUrl.getPath()
        consysConfig.conSys.enableTLS = serverUrl.getProtocol() == "https"
        consysConfig.conSys.user = apiUser
        consysConfig.conSys.password = apiPwd
        consysConfig.connection.connectTimeout = 10000
        consysConfig.connection.reconnectAttempts = 9
        consysConfig.httpClientImplClass = OkHttpClientWrapper::class.java.getCanonicalName()
        consysConfig.dataSourceSelector = ObsSystemDatabaseViewConfig()
        consysConfig.conSysOAuth = oAuthConfig
        _sensorhubConfig!!.add(consysConfig)
    }

    fun isAnySensorEnabled(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean("accel_enabled", false)
                || prefs.getBoolean("gyro_enabled", false)
                || prefs.getBoolean("mag_enabled", false)
                || prefs.getBoolean("orient_quat_enabled", false)
                || prefs.getBoolean("orient_euler_enabled", false)
                || prefs.getBoolean("gps_enabled", false)
                || prefs.getBoolean("netloc_enabled", false)
                || prefs.getBoolean("cam_enabled", false)
                || prefs.getBoolean("audio_enabled", false)
                || prefs.getBoolean("trupulse_enabled", false)
                || prefs.getBoolean("ble_enabled", false)
                || prefs.getBoolean("meshtastic_enabled", false)
                || prefs.getBoolean("polar_enabled", false)
                || prefs.getBoolean("kestrel_enabled", false)
                || prefs.getBoolean("wardriving_enabled", false)
                || prefs.getBoolean("controller_enabled", false)
                || prefs.getBoolean("template_enabled", false)
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

    fun shouldStore(prefs: SharedPreferences): Boolean {
        val prefMap = prefs.getAll()
        for (pref in prefMap.entries) {
            if (pref.value is HashSet<*>) {
                if ((pref.value as HashSet<*>).contains("STORE_LOCAL")) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setData(Uri.parse("package:" + getPackageName()))
                startActivity(intent)
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun checkAndRequestPermissions() {
        val allPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val denied = allPermissions.filter {
            checkSelfPermission(it) == PackageManager.PERMISSION_DENIED
        }

        if (denied.isNotEmpty()) {
            permissionLauncher.launch(denied.toTypedArray())
        }
    }


    // Controller Driver
    private fun getControllerDriver(): ControllerDriver? {
        if (_boundService == null || _boundService!!.sensorhub == null) return null
        try {
            return _boundService!!.sensorhub.getModuleRegistry()
                .getModuleByType<ControllerDriver?>(ControllerDriver::class.java)
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val controller = getControllerDriver()
        if (controller != null && controller.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val controller = getControllerDriver()
        if (controller != null && controller.onMotionEvent(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

    private fun setupBroadcastReceivers() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val origin = intent.getStringExtra("src")
                if (!context.getPackageName().equals(origin, ignoreCase = true)) {
                    val sosEndpointUrl = intent.getStringExtra("sosEndpointUrl")
                    val name = intent.getStringExtra("name")
                    val sensorId = intent.getStringExtra("sensorId")
                    val properties = intent.getStringArrayListExtra("properties")

                    if (sosEndpointUrl == null || name == null || sensorId == null || properties == null || properties.size == 0) {
                        return
                    }

                    try {
                        _boundService!!.stopSensorHub()
                        Thread.sleep(2000)
                        Log.d("OSHApp", "Starting SensorHub Again")
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        updateConfig(
                            PreferenceManager.getDefaultSharedPreferences(context),
                            runName
                        )
                        _sostClients.clear()
                        _boundService!!.startSensorHub(_sensorhubConfig, _showVideo)
                    } catch (e: InterruptedException) {
                        Log.e("OSHApp", "Error Loading Proxy Sensor", e)
                    }
                }
            }
        }
        val filter = IntentFilter()
        filter.addAction(ACTION_BROADCAST_RECEIVER)
        registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
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
