package org.sensorhub.android.config

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings.Secure
import android.util.Log
import com.botts.impl.service.discovery.DiscoveryService
import com.botts.impl.service.discovery.DiscoveryServiceConfig
import org.sensorhub.android.OkHttpClientWrapper
import org.sensorhub.android.comm.BluetoothCommProvider
import org.sensorhub.android.comm.BluetoothCommProviderConfig
import org.sensorhub.android.comm.ble.BleConfig
import org.sensorhub.android.comm.ble.BleNetwork
import org.sensorhub.android.ui.screens.profiles.ServerProfileItem
import org.sensorhub.android.ui.screens.profiles.ServerProfileRepository
import org.sensorhub.api.module.IModuleConfigRepository
import org.sensorhub.api.sensor.SensorConfig
import org.sensorhub.impl.client.sost.SOSTClientConfig
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig
import org.sensorhub.impl.module.InMemoryConfigDb
import org.sensorhub.impl.module.ModuleClassFinder
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig
import org.sensorhub.impl.sensor.android.audio.AudioEncoderConfig
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig.VideoPreset
import org.sensorhub.impl.sensor.controller.ControllerConfig
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
import org.sensorhub.impl.service.consys.client.ConSysOAuthConfig
import org.sensorhub.impl.service.sos.SOSService
import org.sensorhub.impl.service.sos.SOSServiceConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

/** Builds the legacy module configuration independently of the activity.
 *
 * Discovery downloads and legacy TLS setup still perform side effects here;
 * callers should build configurations off the main thread.
 */
class SensorHubConfigFactory(
    context: Context,
    private val serverRepository: ServerProfileRepository,
    private val sensorsLastUpdated: Date = Date()
) {
    private val context = context.applicationContext
    private val log = LoggerFactory.getLogger(SensorHubConfigFactory::class.java)

    fun create(prefs: SharedPreferences, runName: String?): SensorHubConfiguration {
        val config = InMemoryConfigDb(ModuleClassFinder())


        val isApiServiceEnabled = prefs.getBoolean("csapi_service", true)
        val isSosServiceEnabled = prefs.getBoolean("sos_service", true)
        val isDiscoveryServiceEnabled = prefs.getBoolean("discovery_service", false)

        val serverRepo = serverRepository
        val enabledServers: MutableList<ServerProfileItem> = serverRepo.getEnabled()


        //---------- SENSORS ---------------------
        val deviceID = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        var deviceName = prefs.getString("device_name", null)
        if (deviceName == null || deviceName.length < 2) deviceName = deviceID


        // Android sensors
        val sensorsConfig = AndroidSensorsConfig()
        sensorsConfig.name = "Android Sensors [" + deviceName + "]"
        sensorsConfig.id = "ANDROID_SENSORS"
        sensorsConfig.autoStart = true
        sensorsConfig.lastUpdated = sensorsLastUpdated

        sensorsConfig.activateAccelerometer = prefs.getBoolean("accel_enabled", false)
        sensorsConfig.activateGyrometer = prefs.getBoolean("gyro_enabled", false)
        sensorsConfig.activateMagnetometer = prefs.getBoolean("mag_enabled", false)
        sensorsConfig.activateOrientationQuat = prefs.getBoolean("orient_quat_enabled", false)
        sensorsConfig.activateOrientationEuler = prefs.getBoolean("orient_euler_enabled", false)
        sensorsConfig.activateGpsLocation = prefs.getBoolean("gps_enabled", false)
        sensorsConfig.activateNetworkLocation = prefs.getBoolean("netloc_enabled", false)
        sensorsConfig.enableCamera = prefs.getBoolean("cam_enabled", false)
        sensorsConfig.selectedCameraId = prefs.getString("camera_select", "0")!!.toInt()


        // video settings
        sensorsConfig.videoConfig.codec =
            prefs.getString("video_codec", VideoEncoderConfig.JPEG_CODEC)
        sensorsConfig.videoConfig.frameRate = prefs.getString("video_framerate", "30")!!.toInt()

        val resolutionStr: String = prefs.getString("video_resolution", "640x480")!!
        val resParts: Array<String?> =
            resolutionStr.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val videoPreset = VideoPreset()
        videoPreset.width = resParts[0]!!.toInt()
        videoPreset.height = resParts[1]!!.toInt()
        sensorsConfig.videoConfig.presets = arrayOf<VideoPreset>(videoPreset)
        sensorsConfig.videoConfig.selectedPreset = 0

        sensorsConfig.outputVideoRoll = prefs.getBoolean("video_roll_enabled", false)


        // audio
        sensorsConfig.activateMicAudio = prefs.getBoolean("audio_enabled", false)
        sensorsConfig.audioConfig.codec =
            prefs.getString("audio_codec", AudioEncoderConfig.AAC_CODEC)
        sensorsConfig.audioConfig.sampleRate = prefs.getString("audio_samplerate", "8000")!!.toInt()
        sensorsConfig.audioConfig.bitRate = prefs.getString("audio_bitrate", "64")!!.toInt()

        sensorsConfig.runName = runName
        sensorsConfig.uidExtension = prefs.getString("uid_extension", "0")


        // HTTP Server
        val serverConfig = HttpServerConfig()
        serverConfig.proxyBaseUrl = ""
        serverConfig.httpPort = 8585
        serverConfig.autoStart = true
        config.add(serverConfig)

        config.add(sensorsConfig)


        // TruPulse LRF
        if (prefs.getBoolean("trupulse_enabled", false)) {
            var trupulseConfig = TruPulseConfig()

            if (sensorsConfig.activateGpsLocation) {
                var gpsOutputName: String? = null
                if (context.packageManager
                        .hasSystemFeature(PackageManager.FEATURE_LOCATION)
                ) {
                    val locationManager =
                        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val locProviders = locationManager.allProviders
                    for (provName in locProviders) {
                        val locProvider = locationManager.getProvider(provName)
                        if (locProvider!!.requiresSatellite()) gpsOutputName =
                            locProvider.name.replace(" ".toRegex(), "_") + "_data"
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
            trupulseConfig.lastUpdated = sensorsLastUpdated
            trupulseConfig.serialNumber = deviceID

            val btConf = BluetoothCommProviderConfig()
            btConf.protocol.deviceName = prefs.getString("trupulse_device_address", "")
            if (prefs.getBoolean("trupulse_simu", false)) {
                btConf.moduleClass = SimulatedDataStream::class.java.canonicalName
            } else {
                btConf.moduleClass = BluetoothCommProvider::class.java.canonicalName
                trupulseConfig.connection.connectTimeout = 100000
                trupulseConfig.connection.reconnectAttempts = 10
            }
            trupulseConfig.commSettings = btConf

            config.add(trupulseConfig)
        }

        // STE Rad Pager
        if (prefs.getBoolean("ste_radpager_enabled", false)) {
            val steRadPagerConfig = STERadPagerConfig()
            steRadPagerConfig.id = "STE_RADPAGER_SENSOR"
            steRadPagerConfig.name = "STE Rad Pager [" + deviceName + "]"
            steRadPagerConfig.autoStart = true
            steRadPagerConfig.lastUpdated = sensorsLastUpdated

            config.add(steRadPagerConfig)
        }

        // Meshtastic
        if (prefs.getBoolean("meshtastic_enabled", false)) {
            val meshtasticConfig = MeshtasticConfig()
            meshtasticConfig.id = "MESHTASTIC_SENSOR"
            meshtasticConfig.name = "Meshtastic [" + deviceName + "]"
            meshtasticConfig.autoStart = true
            meshtasticConfig.lastUpdated = sensorsLastUpdated
            meshtasticConfig.device_name = prefs.getString("meshtastic_device_address", "")
            meshtasticConfig.uid_extension = prefs.getString("uid_extension", "")

            config.add(meshtasticConfig)
        }

        // Polar heart rate
        if (prefs.getBoolean("polar_enabled", false)) {
            val polarConfig = PolarConfig()
            polarConfig.id = "POLAR_HEART_SENSOR"
            polarConfig.name = "Polar Heart [" + deviceName + "]"
            polarConfig.autoStart = true
            polarConfig.lastUpdated = sensorsLastUpdated
            polarConfig.deviceId = prefs.getString("polar_device_address", "")
            polarConfig.uid_extension = prefs.getString("uid_extension", "")

            config.add(polarConfig)
        }

        // Kestrel weather
        if (prefs.getBoolean("kestrel_enabled", false)) {
            val bleConf = BleConfig()
            bleConf.id = "BLE_NETWORK"
            bleConf.moduleClass = BleNetwork::class.java.canonicalName
            bleConf.androidContext = context
            bleConf.autoStart = true
            config.add(bleConf)

            val kestrelConfig = KestrelConfig()
            kestrelConfig.id = "KESTREL_WEATHER"
            kestrelConfig.name = "Kestrel Weather [" + deviceName + "]"
            kestrelConfig.autoStart = true
            kestrelConfig.lastUpdated = sensorsLastUpdated
            kestrelConfig.networkID = bleConf.id
            kestrelConfig.deviceAddress = prefs.getString("kestrel_device_address", "")

            config.add(kestrelConfig)
        }

        // Controller
        if (prefs.getBoolean("controller_enabled", false)) {
            val controllerConfig = ControllerConfig()
            controllerConfig.id = "CONTROLLER"
            controllerConfig.name = "Controller [" + deviceName + "]"
            controllerConfig.autoStart = true
            controllerConfig.lastUpdated = sensorsLastUpdated
            controllerConfig.uid_extension = prefs.getString("uid_extension", "")

            config.add(controllerConfig)
        }

        // Wardriving
        if (prefs.getBoolean("wardriving_enabled", false)) {
            val wardrivingConfig = WardrivingConfig()
            wardrivingConfig.id = "WARDRIVING_"
            wardrivingConfig.name = "Wardriving [" + deviceName + "]"
            wardrivingConfig.autoStart = true
            wardrivingConfig.lastUpdated = sensorsLastUpdated
            wardrivingConfig.uid_extension = prefs.getString("uid_extension", "")

            config.add(wardrivingConfig)
        }

        // Template driver
        if (prefs.getBoolean("template_enabled", false)) {
            val templateConfig = TemplateConfig()
            templateConfig.id = "TEMPLATE_DRIVER_"
            templateConfig.name = "Template [" + deviceName + "]"
            templateConfig.autoStart = true
            templateConfig.lastUpdated = sensorsLastUpdated
            templateConfig.uid_extension = prefs.getString("uid_extension", "")

            config.add(templateConfig)
        }

        if (isAnySensorEnabled(prefs)) {
            for (sp in enabledServers) {
                val profileUrl = try {
                    java.net.URI(sp.endpointUrl.trim()).toURL()
                } catch (_: Exception) {
                    null
                }
                if (profileUrl == null) {
                    log.error(
                        "Skipping server profile '{}': invalid URL '{}'",
                        sp.serverName, sp.endpointUrl
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
                    addCSApiConfig(
                        config,
                        sensorsConfig,
                        sp,
                        profileUrl,
                        sp.username,
                        pwd,
                        oAuthConfig
                    )
                } else {
                    addSosTConfig(config, sensorsConfig, sp, profileUrl, sp.username, pwd)
                }
            }
        }

        if (shouldStore(prefs)) {
            val dbFile = File(context.filesDir.toString() + "/db/")
            dbFile.mkdirs()
            val basicStorageConfig = MVObsSystemDatabaseConfig()
            basicStorageConfig.moduleClass = "org.sensorhub.impl.persistence.h2.MVObsStorageImpl"
            basicStorageConfig.storagePath = dbFile.absolutePath + "/\${STORAGE_ID}.dat"
            basicStorageConfig.autoStart = true
        }

        //---------- SERVICES ---------------------
        if (isApiServiceEnabled) {
            val conSysApiService = ConSysApiServiceConfig()
            conSysApiService.moduleClass = ConSysApiService::class.java.canonicalName
            conSysApiService.id = "CON_SYS_SERVICE"
            conSysApiService.name = "Connected Systems API Service"
            conSysApiService.autoStart = true
            conSysApiService.enableTransactional = true
            conSysApiService.exposedResources = ObsSystemDatabaseViewConfig()

            config.add(conSysApiService)
        }

        if (isSosServiceEnabled) {
            val sosConfig = SOSServiceConfig()
            sosConfig.moduleClass = SOSService::class.java.canonicalName
            sosConfig.id = "SOS_SERVICE"
            sosConfig.name = "SOS Service"
            sosConfig.autoStart = true
            sosConfig.enableTransactional = true
            sosConfig.exposedResources = ObsSystemDatabaseViewConfig()

            config.add(sosConfig)
        }

        if (isDiscoveryServiceEnabled) {
            val discoveryServiceConfig = DiscoveryServiceConfig()
            discoveryServiceConfig.moduleClass = DiscoveryService::class.java.canonicalName
            discoveryServiceConfig.id = "DISCOVERY_SERVICE"
            discoveryServiceConfig.name = "Discovery Service"
            discoveryServiceConfig.autoStart = true

            val outFile = File(context.filesDir, "rules.txt")
            val rulesLink: String = prefs.getString("rules_link", "")!!
            val downloadTask = FutureTask<Void?>(Callable {
                val rulesUrl = URL(rulesLink)
                val conn = rulesUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
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
            discoveryServiceConfig.rulesFilePath = outFile.absolutePath

            config.add(discoveryServiceConfig)
        }

        return SensorHubConfiguration(config, deviceID, sensorsConfig.enableCamera)

    }

    private fun addSosTConfig(
        config: IModuleConfigRepository,
        sensorConf: SensorConfig,
        profile: ServerProfileItem,
        serverUrl: URL,
        user: String,
        pwd: String
    ) {
        val sosConfig = SOSTClientConfig()
        sosConfig.id = sensorConf.id + "_SOST_" + profile.id
        sosConfig.name =
            sensorConf.name.replace("\\[.*\\]".toRegex(), "") + " -> " + profile.serverName
        sosConfig.autoStart = true
        sosConfig.sos.remoteHost = serverUrl.host
        sosConfig.sos.remotePort = if (serverUrl.port < 0) serverUrl.defaultPort else serverUrl.port
        sosConfig.sos.resourcePath = serverUrl.path
        sosConfig.sos.enableTLS = serverUrl.protocol == "https"
        sosConfig.sos.user = user
        sosConfig.sos.password = pwd
        sosConfig.connection.connectTimeout = 10000
        sosConfig.connection.usePersistentConnection = true
        sosConfig.connection.reconnectAttempts = 9
        sosConfig.connection.maxQueueSize = 100
        sosConfig.dataSourceSelector = ObsSystemDatabaseViewConfig()
        config.add(sosConfig)
    }

    private fun addCSApiConfig(
        config: IModuleConfigRepository,
        sensorConf: SensorConfig,
        profile: ServerProfileItem,
        serverUrl: URL,
        apiUser: String,
        apiPwd: String,
        oAuthConfig: ConSysOAuthConfig
    ) {
        val consysConfig = ConSysApiClientConfig()
        consysConfig.id = sensorConf.id + "_CONSYS_" + profile.id
        consysConfig.name =
            sensorConf.name.replace("\\[.*\\]".toRegex(), "") + " -> " + profile.serverName
        consysConfig.autoStart = true
        consysConfig.conSys.remoteHost = serverUrl.host
        consysConfig.conSys.remotePort =
            if (serverUrl.port < 0) serverUrl.defaultPort else serverUrl.port
        consysConfig.conSys.resourcePath = serverUrl.path
        consysConfig.conSys.enableTLS = serverUrl.protocol == "https"
        consysConfig.conSys.user = apiUser
        consysConfig.conSys.password = apiPwd
        consysConfig.connection.connectTimeout = 10000
        consysConfig.connection.reconnectAttempts = 9
        consysConfig.httpClientImplClass = OkHttpClientWrapper::class.java.canonicalName
        consysConfig.dataSourceSelector = ObsSystemDatabaseViewConfig()
        consysConfig.conSysOAuth = oAuthConfig
        config.add(consysConfig)
    }

    private fun isAnySensorEnabled(prefs: SharedPreferences): Boolean {
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
                || prefs.getBoolean("ste_radpager_enabled", false)
    }

    private fun shouldStore(prefs: SharedPreferences): Boolean {
        val prefMap = prefs.all
        for ((_, value) in prefMap) {
            if (value is HashSet<*>) {
                if (value.contains("STORE_LOCAL")) {
                    return true
                }
            }
        }
        return false
    }

}

data class SensorHubConfiguration(
    val modules: IModuleConfigRepository,
    val deviceId: String,
    val cameraEnabled: Boolean
)
