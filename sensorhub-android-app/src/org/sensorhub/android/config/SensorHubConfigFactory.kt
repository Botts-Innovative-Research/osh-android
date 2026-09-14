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
import org.sensorhub.android.data.sensors.SensorRegistry
import org.sensorhub.android.data.sensors.SensorRuntimeConfiguration
import org.sensorhub.android.data.servers.ServerProfileItem
import org.sensorhub.android.data.servers.ServerProfileRepository
import org.sensorhub.api.module.IModuleConfigRepository
import org.sensorhub.api.sensor.SensorConfig
import org.sensorhub.impl.client.sost.SOSTClientConfig
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig
import org.sensorhub.impl.module.InMemoryConfigDb
import org.sensorhub.impl.module.ModuleClassFinder
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


        // HTTP Server
        val serverConfig = HttpServerConfig()
        serverConfig.proxyBaseUrl = ""
        serverConfig.httpPort = 8585
        serverConfig.autoStart = true
        config.add(serverConfig)


        val sensorsConfig = SensorRuntimeConfiguration.create(
            context, prefs, config, deviceID, deviceName, runName, sensorsLastUpdated
        )

        if (SensorRegistry.hasEnabledSensor(prefs)) {
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

    private fun shouldStore(prefs: SharedPreferences): Boolean {
        val prefMap = prefs.all
        for ((_, value) in prefMap) {
            if (value is Set<*>) {
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
