/*************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.location.LocationManager;
import android.location.LocationProvider;

import com.botts.impl.service.discovery.DiscoveryService;
import com.botts.impl.service.discovery.DiscoveryServiceConfig;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.sensorhub.android.comm.BluetoothCommProvider;
import org.sensorhub.android.comm.BluetoothCommProviderConfig;
import org.sensorhub.android.comm.ble.BleConfig;
import org.sensorhub.android.comm.ble.BleNetwork;
import org.sensorhub.android.server.ServerProfile;
import org.sensorhub.android.server.ServerProfileRepository;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClientConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.module.ModuleClassFinder;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import org.sensorhub.impl.sensor.android.audio.AudioEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig.VideoPreset;
import org.sensorhub.impl.sensor.controller.ControllerConfig;
import org.sensorhub.impl.sensor.controller.ControllerDriver;
import org.sensorhub.impl.sensor.kestrel.KestrelConfig;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticConfig;
import org.sensorhub.impl.sensor.polar.PolarConfig;
import org.sensorhub.impl.sensor.ste.STERadPagerConfig;
import org.sensorhub.impl.sensor.template.TemplateConfig;
import org.sensorhub.impl.sensor.trupulse.SimulatedDataStream;
import org.sensorhub.impl.sensor.trupulse.TruPulseConfig;
import org.sensorhub.impl.sensor.trupulse.TruPulseWithGeolocConfig;
import org.sensorhub.impl.sensor.wardriving.WardrivingConfig;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.consys.ConSysApiService;
import org.sensorhub.impl.service.consys.ConSysApiServiceConfig;
import org.sensorhub.impl.service.consys.client.ConSysApiClientConfig;
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule;
import org.sensorhub.impl.service.consys.client.ConSysOAuthConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.impl.service.sos.SOSServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends AppCompatActivity implements SensorHubServiceProvider
{
    public static final String ACTION_BROADCAST_RECEIVER = "org.sensorhub.android.BROADCAST_RECEIVER";
    public static final String ANDROID_SENSORS_MODULE_ID = "ANDROID_SENSORS";
    public static final Date ANDROID_SENSORS_LAST_UPDATED = new Date(Instant.now().toEpochMilli());
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    SensorHubService boundService;
    IModuleConfigRepository sensorhubConfig;
    boolean oshStarted = false;
    CopyOnWriteArrayList<SOSTClient> sostClients = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<ConSysApiClientModule> conSysClients = new CopyOnWriteArrayList<>();

    AndroidSensorsDriver androidSensors;
    boolean showVideo;

    String deviceID;
    String runName;

    private Fragment activeFragment;
    private TextView toolbarTitle;
    private BroadcastReceiver broadcastReceiver;

    private final ServiceConnection sConn = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            boundService = ((SensorHubService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            boundService = null;
        }
    };

    // ==================== SensorHubServiceProvider ====================

    @Override
    public SensorHubService getBoundService() { return boundService; }

    @Override
    public boolean isOshStarted() { return oshStarted; }

    @Override
    public void setOshStarted(boolean started) { this.oshStarted = started; }

    @Override
    public IModuleConfigRepository getSensorhubConfig() { return sensorhubConfig; }

    @Override
    public List<SOSTClient> getSostClients() { return sostClients; }

    @Override
    public List<ConSysApiClientModule> getConSysClients() { return conSysClients; }

    @Override
    public AndroidSensorsDriver getAndroidSensors() { return androidSensors; }

    @Override
    public void setAndroidSensors(AndroidSensorsDriver driver) { this.androidSensors = driver; }

    @Override
    public boolean getShowVideo() { return showVideo; }

    @Override
    public void startSensorHub() {
        if (boundService != null && sensorhubConfig != null) {
            boundService.startSensorHub(sensorhubConfig, showVideo);
        }
    }

    @Override
    public void stopSensorHub() {
        sostClients.clear();
        conSysClients.clear();
        if (boundService != null)
            boundService.stopSensorHub();
        oshStarted = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // ==================== Config ====================

    @Override
    public void updateConfig(SharedPreferences prefs, String runName)
    {
        deviceID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        sensorhubConfig = new InMemoryConfigDb(new ModuleClassFinder());

        Boolean isApiServiceEnabled = prefs.getBoolean("csapi_service", true);
        Boolean isSosServiceEnabled = prefs.getBoolean("sos_service", true);
        Boolean isDiscoveryServiceEnabled = prefs.getBoolean("discovery_service", false);

        ServerProfileRepository serverRepo = ServerProfileRepository.getInstance(this);
        List<ServerProfile> enabledServers = serverRepo.getEnabled();

        boolean disableSslCheck = false;
        for (ServerProfile sp : enabledServers) {
            if (sp.disableSslCheck) {
                disableSslCheck = true;
                break;
            }
        }
        if (disableSslCheck)
        {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                            return myTrustedAnchors;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }


        String deviceID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        String deviceName = prefs.getString("device_name", null);
        if (deviceName == null || deviceName.length() < 2)
            deviceName = deviceID;

        // Android sensors
        AndroidSensorsConfig sensorsConfig = new AndroidSensorsConfig();
        sensorsConfig.name = "Android Sensors [" + deviceName + "]";
        sensorsConfig.id = "ANDROID_SENSORS";
        sensorsConfig.autoStart = true;
        sensorsConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;

        sensorsConfig.activateAccelerometer = prefs.getBoolean("accel_enabled", false);
        sensorsConfig.activateGyrometer = prefs.getBoolean("gyro_enabled", false);
        sensorsConfig.activateMagnetometer = prefs.getBoolean("mag_enabled", false);
        sensorsConfig.activateOrientationQuat = prefs.getBoolean("orient_quat_enabled", false);
        sensorsConfig.activateOrientationEuler = prefs.getBoolean("orient_euler_enabled", false);
        sensorsConfig.activateGpsLocation = prefs.getBoolean("gps_enabled", false);
        sensorsConfig.activateNetworkLocation = prefs.getBoolean("netloc_enabled", false);
        sensorsConfig.enableCamera = prefs.getBoolean("cam_enabled", false);
        sensorsConfig.selectedCameraId = Integer.parseInt(prefs.getString("camera_select", "0"));
        if (sensorsConfig.enableCamera)
            showVideo = true;

        // video settings
        sensorsConfig.videoConfig.codec = prefs.getString("video_codec", VideoEncoderConfig.JPEG_CODEC);
        sensorsConfig.videoConfig.frameRate = Integer.parseInt(prefs.getString("video_framerate", "30"));

        String resolutionStr = prefs.getString("video_resolution", "640x480");
        String[] resParts = resolutionStr.split("x");
        VideoPreset videoPreset = new VideoPreset();
        videoPreset.width = Integer.parseInt(resParts[0]);
        videoPreset.height = Integer.parseInt(resParts[1]);
        sensorsConfig.videoConfig.presets = new VideoPreset[]{videoPreset};
        sensorsConfig.videoConfig.selectedPreset = 0;

        sensorsConfig.outputVideoRoll = prefs.getBoolean("video_roll_enabled", false);

        // audio
        sensorsConfig.activateMicAudio = prefs.getBoolean("audio_enabled", false);
        sensorsConfig.audioConfig.codec = prefs.getString("audio_codec", AudioEncoderConfig.AAC_CODEC);
        sensorsConfig.audioConfig.sampleRate = Integer.parseInt(prefs.getString("audio_samplerate", "8000"));
        sensorsConfig.audioConfig.bitRate = Integer.parseInt(prefs.getString("audio_bitrate", "64"));

        sensorsConfig.runName = runName;
        sensorsConfig.uidExtension = prefs.getString("uid_extension", "0");

        // HTTP Server
        HttpServerConfig serverConfig = new HttpServerConfig();
        serverConfig.proxyBaseUrl = "";
        serverConfig.httpPort = 8585;
        serverConfig.autoStart = true;
        sensorhubConfig.add(serverConfig);

        // SOS Service
        SOSServiceConfig sosConfig = new SOSServiceConfig();
        sosConfig.moduleClass = SOSService.class.getCanonicalName();
        sosConfig.id = "SOS_SERVICE";
        sosConfig.name = "SOS Service";
        sosConfig.autoStart = true;
        sosConfig.enableTransactional = true;
        sosConfig.exposedResources = new ObsSystemDatabaseViewConfig();

        // Connected Systems Service
        ConSysApiServiceConfig conSysApiService = new ConSysApiServiceConfig();
        conSysApiService.moduleClass = ConSysApiService.class.getCanonicalName();
        conSysApiService.id = "CON_SYS_SERVICE";
        conSysApiService.name = "Connected Systems API Service";
        conSysApiService.autoStart = true;
        conSysApiService.enableTransactional = true;
        conSysApiService.exposedResources = new ObsSystemDatabaseViewConfig();

        // Discovery Service
        DiscoveryServiceConfig discoveryServiceConfig = new DiscoveryServiceConfig();
        discoveryServiceConfig.moduleClass = DiscoveryService.class.getCanonicalName();
        discoveryServiceConfig.id = "DISCOVERY_SERVICE";
        discoveryServiceConfig.name= "Discovery Service";
        discoveryServiceConfig.autoStart = true;

        File outFile = new File(getApplicationContext().getFilesDir(), "rules.txt");
        String rulesLink = prefs.getString("rules_link", "");
        FutureTask<Void> downloadTask = new java.util.concurrent.FutureTask<>(() -> {
            URL rulesUrl = new URL(rulesLink);
            HttpURLConnection conn = (HttpURLConnection) rulesUrl.openConnection();
            conn.setInstanceFollowRedirects(true);
            try (InputStream in = conn.getInputStream();
                 OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            } finally {
                conn.disconnect();
            }
            return null;
        });
        new Thread(downloadTask).start();
        try {
            downloadTask.get();
        } catch (Exception e) {
            Log.e("OSH - Discovery", "Failed to download rules file", e);
        }
        discoveryServiceConfig.rulesFilePath = outFile.getAbsolutePath();

        sensorhubConfig.add(sensorsConfig);

        // TruPulse sensor
        if (prefs.getBoolean("trupulse_enabled", false)) {
            TruPulseConfig trupulseConfig = new TruPulseConfig();

            if (sensorsConfig.activateGpsLocation) {
                String gpsOutputName = null;
                if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION))
                {
                    LocationManager locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    List<String> locProviders = locationManager.getAllProviders();
                    for (String provName: locProviders)
                    {
                        LocationProvider locProvider = locationManager.getProvider(provName);
                        if (locProvider.requiresSatellite())
                            gpsOutputName = locProvider.getName().replaceAll(" ", "_") + "_data";
                    }
                }

                trupulseConfig = new TruPulseWithGeolocConfig();
                ((TruPulseWithGeolocConfig)trupulseConfig).locationSourceUID = "urn:osh:android" + sensorsConfig.getAndroidSensorsUidWithExt();
                ((TruPulseWithGeolocConfig)trupulseConfig).locationOutputName = gpsOutputName;
            }

            trupulseConfig.id = "TRUPULSE_SENSOR";
            trupulseConfig.name = "TruPulse Range Finder [" + deviceName + "]";
            trupulseConfig.autoStart = true;
            trupulseConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            trupulseConfig.serialNumber = deviceID;

            BluetoothCommProviderConfig btConf = new BluetoothCommProviderConfig();
            btConf.protocol.deviceName = prefs.getString("trupulse_device_address", "");
            if (prefs.getBoolean("trupulse_simu", false)) {
                btConf.moduleClass = SimulatedDataStream.class.getCanonicalName();
            } else {
                btConf.moduleClass = BluetoothCommProvider.class.getCanonicalName();
                trupulseConfig.connection.connectTimeout = 100000;
                trupulseConfig.connection.reconnectAttempts = 10;
            }
            trupulseConfig.commSettings = btConf;

            sensorhubConfig.add(trupulseConfig);
        }

        // STE Rad Pager sensor
        if (prefs.getBoolean("ste_radpager_enabled", false)) {
            STERadPagerConfig steRadPagerConfig = new STERadPagerConfig();
            steRadPagerConfig.id = "STE_RADPAGER_SENSOR";
            steRadPagerConfig.name = "STE Rad Pager [" + deviceName + "]";
            steRadPagerConfig.autoStart = true;
            steRadPagerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;

            sensorhubConfig.add(steRadPagerConfig);
        }

        // Meshtastic sensor
        if (prefs.getBoolean("meshtastic_enabled", false)) {
            MeshtasticConfig meshtasticConfig = new MeshtasticConfig();
            meshtasticConfig.id = "MESHTASTIC_SENSOR";
            meshtasticConfig.name = "Meshtastic [" + deviceName + "]";
            meshtasticConfig.autoStart = true;
            meshtasticConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            meshtasticConfig.device_name = prefs.getString("meshtastic_device_address", "");
            meshtasticConfig.uid_extension = prefs.getString("uid_extension", "");

            sensorhubConfig.add(meshtasticConfig);
        }

        // Polar heart rate sensor
        if (prefs.getBoolean("polar_enabled", false)) {
            PolarConfig polarConfig = new PolarConfig();
            polarConfig.id = "POLAR_HEART_SENSOR";
            polarConfig.name = "Polar Heart [" + deviceName + "]";
            polarConfig.autoStart = true;
            polarConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            polarConfig.deviceId = prefs.getString("polar_device_address", "");
            polarConfig.uid_extension = prefs.getString("uid_extension", "");

            sensorhubConfig.add(polarConfig);
        }

        // Kestrel weather sensor
        if (prefs.getBoolean("kestrel_enabled", false)) {
            BleConfig bleConf = new BleConfig();
            bleConf.id = "BLE_NETWORK";
            bleConf.moduleClass = BleNetwork.class.getCanonicalName();
            bleConf.androidContext = this.getApplicationContext();
            bleConf.autoStart = true;
            sensorhubConfig.add(bleConf);

            KestrelConfig kestrelConfig = new KestrelConfig();
            kestrelConfig.id = "KESTREL_WEATHER";
            kestrelConfig.name = "Kestrel Weather [" + deviceName + "]";
            kestrelConfig.autoStart = true;
            kestrelConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            kestrelConfig.networkID = bleConf.id;
            kestrelConfig.deviceAddress = prefs.getString("kestrel_device_address", "");

            sensorhubConfig.add(kestrelConfig);
        }

        // Controller
        if (prefs.getBoolean("controller_enabled", false)) {
            ControllerConfig controllerConfig = new ControllerConfig();
            controllerConfig.id = "CONTROLLER";
            controllerConfig.name = "Controller [" + deviceName + "]";
            controllerConfig.autoStart = true;
            controllerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            controllerConfig.uid_extension = prefs.getString("uid_extension", "");

            sensorhubConfig.add(controllerConfig);
        }

        // Wardriving sensor
        if (prefs.getBoolean("wardriving_enabled", false)) {
            WardrivingConfig wardrivingConfig = new WardrivingConfig();
            wardrivingConfig.id = "WARDRIVING_";
            wardrivingConfig.name = "Wardriving [" + deviceName + "]";
            wardrivingConfig.autoStart = true;
            wardrivingConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            wardrivingConfig.uid_extension = prefs.getString("uid_extension", "");

            sensorhubConfig.add(wardrivingConfig);
        }

        // Template driver
        if (prefs.getBoolean("template_enabled", false)) {
            TemplateConfig templateConfig = new TemplateConfig();
            templateConfig.id = "TEMPLATE_DRIVER_";
            templateConfig.name = "Template [" + deviceName + "]";
            templateConfig.autoStart = true;
            templateConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            templateConfig.uid_extension = prefs.getString("uid_extension", "");

            sensorhubConfig.add(templateConfig);
        }

        if (isAnySensorEnabled(prefs)) {
            for (ServerProfile sp : enabledServers) {
                URL profileUrl = sp.buildClientUrl();
                if (profileUrl == null) {
                    log.error("Skipping server profile '{}': invalid URL", sp.name);
                    continue;
                }

                String pwd = serverRepo.getPassword(sp.id);

                if (sp.useConSysClient) {
                    ConSysOAuthConfig oAuthConfig = new ConSysOAuthConfig();
                    oAuthConfig.oAuthEnabled = sp.oAuthEnabled;
                    oAuthConfig.tokenEndpoint = serverRepo.getOAuthTokenEndpoint(sp.id);
                    oAuthConfig.clientID = serverRepo.getOAuthClientId(sp.id);
                    oAuthConfig.clientSecret = serverRepo.getOAuthClientSecret(sp.id);
                    addCSApiConfig(sensorsConfig, sp, profileUrl, sp.username, pwd, oAuthConfig);
                } else {
                    addSosTConfig(sensorsConfig, sp, profileUrl, sp.username, pwd);
                }
            }
        }

        if (shouldStore(prefs)) {
            File dbFile = new File(getApplicationContext().getFilesDir() + "/db/");
            dbFile.mkdirs();
            MVObsSystemDatabaseConfig basicStorageConfig = new MVObsSystemDatabaseConfig();
            basicStorageConfig.moduleClass = "org.sensorhub.impl.persistence.h2.MVObsStorageImpl";
            basicStorageConfig.storagePath = dbFile.getAbsolutePath() + "/${STORAGE_ID}.dat";
            basicStorageConfig.autoStart = true;
        }

        //---------- SERVICES ---------------------
        if (isApiServiceEnabled) {
            sensorhubConfig.add(conSysApiService);
        }
        if (isSosServiceEnabled) {
            sensorhubConfig.add(sosConfig);
        }
        if (isDiscoveryServiceEnabled) {
            sensorhubConfig.add(discoveryServiceConfig);
        }
    }

    protected void addSosTConfig(SensorConfig sensorConf, ServerProfile profile, URL serverUrl, String user, String pwd)
    {
        SOSTClientConfig sosConfig = new SOSTClientConfig();
        sosConfig.id = sensorConf.id + "_SOST_" + profile.id;
        sosConfig.name = sensorConf.name.replaceAll("\\[.*\\]", "") + " -> " + profile.name;
        sosConfig.autoStart = true;
        sosConfig.sos.remoteHost = serverUrl.getHost();
        sosConfig.sos.remotePort = serverUrl.getPort() < 0 ? serverUrl.getDefaultPort() : serverUrl.getPort();
        sosConfig.sos.resourcePath = serverUrl.getPath();
        sosConfig.sos.enableTLS = serverUrl.getProtocol().equals("https");
        sosConfig.sos.user = user;
        sosConfig.sos.password = pwd;
        sosConfig.connection.connectTimeout = 10000;
        sosConfig.connection.usePersistentConnection = true;
        sosConfig.connection.reconnectAttempts = 9;
        sosConfig.connection.maxQueueSize = 100;
        sosConfig.dataSourceSelector = new ObsSystemDatabaseViewConfig();
        sensorhubConfig.add(sosConfig);
    }

    protected void addCSApiConfig(SensorConfig sensorConf, ServerProfile profile, URL serverUrl, String apiUser, String apiPwd, ConSysOAuthConfig oAuthConfig)
    {
        ConSysApiClientConfig consysConfig = new ConSysApiClientConfig();
        consysConfig.id = sensorConf.id + "_CONSYS_" + profile.id;
        consysConfig.name = sensorConf.name.replaceAll("\\[.*\\]", "") + " -> " + profile.name;
        consysConfig.autoStart = true;
        consysConfig.conSys.remoteHost = serverUrl.getHost();
        consysConfig.conSys.remotePort = serverUrl.getPort() < 0 ? serverUrl.getDefaultPort() : serverUrl.getPort();
        consysConfig.conSys.resourcePath = serverUrl.getPath();
        consysConfig.conSys.enableTLS = serverUrl.getProtocol().equals("https");
        consysConfig.conSys.user = apiUser;
        consysConfig.conSys.password = apiPwd;
        consysConfig.connection.connectTimeout = 10000;
        consysConfig.connection.reconnectAttempts = 9;
        consysConfig.httpClientImplClass = OkHttpClientWrapper.class.getCanonicalName();
        consysConfig.dataSourceSelector = new ObsSystemDatabaseViewConfig();
        consysConfig.conSysOAuth = oAuthConfig;
        sensorhubConfig.add(consysConfig);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);

        findViewById(R.id.btn_app_preferences).setOnClickListener(v ->
                startActivity(new Intent(this, AppPreferencesActivity.class)));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        Fragment homeFragment;
        Fragment sensorsFragment;
        Fragment settingsFragment;

        if (savedInstanceState == null) {
            homeFragment = new DashboardFragment();
            sensorsFragment = new SensorsFragment();
            settingsFragment = new SettingsFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.flFragment, homeFragment, "dashboard")
                    .add(R.id.flFragment, sensorsFragment, "sensors")
                    .add(R.id.flFragment, settingsFragment, "settings")
                    .hide(sensorsFragment)
                    .hide(settingsFragment)
                    .commit();

            activeFragment = homeFragment;
        } else {
            homeFragment = getSupportFragmentManager().findFragmentByTag("dashboard");
            sensorsFragment = getSupportFragmentManager().findFragmentByTag("sensors");
            settingsFragment = getSupportFragmentManager().findFragmentByTag("settings");

            if (settingsFragment != null && !settingsFragment.isHidden())
                activeFragment = settingsFragment;
            else if (sensorsFragment != null && !sensorsFragment.isHidden())
                activeFragment = sensorsFragment;
            else
                activeFragment = homeFragment;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.dashboard:
                    switchFragment(homeFragment, getString(R.string.app_name));
                    break;
                case R.id.sensors:
                    switchFragment(sensorsFragment, getString(R.string.tab_sensors));
                    break;
                case R.id.settings:
                    switchFragment(settingsFragment, getString(R.string.tab_settings));
                    break;
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.dashboard);
        }

        hasBluetoothPermissions();
        checkForPermissions();

        // bind to SensorHub service
        Intent intent = new Intent(this, SensorHubService.class);
        startService(intent);
        bindService(intent, sConn, Context.BIND_AUTO_CREATE);

        setupBroadcastReceivers();
        requestBatteryOptimizationExemption();
    }

    private void switchFragment(Fragment fragment, String title) {
        if (fragment == activeFragment) return;
        getSupportFragmentManager()
                .beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }


    @Override
    protected void onDestroy()
    {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

        if (boundService != null) {
            unbindService(sConn);
            boundService = null;
        }
        super.onDestroy();
    }

    private ControllerDriver getControllerDriver() {
        if (boundService == null || boundService.sensorhub == null)
            return null;
        try {
            return boundService.sensorhub.getModuleRegistry().getModuleByType(ControllerDriver.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ControllerDriver controller = getControllerDriver();
        if (controller != null && controller.onKeyEvent(event))
            return true;
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        ControllerDriver controller = getControllerDriver();
        if (controller != null && controller.onMotionEvent(event))
            return true;
        return super.dispatchGenericMotionEvent(event);
    }



    boolean isAnySensorEnabled(SharedPreferences prefs) {
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
                || prefs.getBoolean("template_enabled", false);
    }

    boolean shouldServe(SharedPreferences prefs) {
        Map<String, ?> prefMap = prefs.getAll();
        for (Map.Entry<String, ?> pref : prefMap.entrySet()) {
            if (pref.getValue() instanceof HashSet) {
                if (((HashSet) pref.getValue()).contains("FETCH_LOCAL")) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean shouldStore(SharedPreferences prefs) {
        Map<String, ?> prefMap = prefs.getAll();
        for (Map.Entry<String, ?> pref : prefMap.entrySet()) {
            if (pref.getValue() instanceof HashSet) {
                if (((HashSet) pref.getValue()).contains("STORE_LOCAL")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkForPermissions() {
        List<String> permissions = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.CAMERA);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.RECORD_AUDIO);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.BLUETOOTH);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        if (checkSelfPermission(Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND);
        if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_DENIED)
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        if (checkSelfPermission(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.WAKE_LOCK);
        if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.INTERNET);
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED)
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

        String[] permARR = new String[permissions.size()];
        permARR = permissions.toArray(permARR);
        if (permARR.length > 0) {
            requestPermissions(permARR, 100);
        }
    }

    private void setupBroadcastReceivers() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String origin = intent.getStringExtra("src");
                if (!context.getPackageName().equalsIgnoreCase(origin)) {
                    String sosEndpointUrl = intent.getStringExtra("sosEndpointUrl");
                    String name = intent.getStringExtra("name");
                    String sensorId = intent.getStringExtra("sensorId");
                    ArrayList<String> properties = intent.getStringArrayListExtra("properties");

                    if (sosEndpointUrl == null || name == null || sensorId == null || properties == null || properties.size() == 0) {
                        return;
                    }

                    try {
                        boundService.stopSensorHub();
                        Thread.sleep(2000);
                        Log.d("OSHApp", "Starting SensorHub Again");
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        updateConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), runName);
                        sostClients.clear();
                        boundService.startSensorHub(sensorhubConfig, showVideo);
                    } catch (InterruptedException e) {
                        Log.e("OSHApp", "Error Loading Proxy Sensor", e);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BROADCAST_RECEIVER);
        registerReceiver(broadcastReceiver, filter);
    }
}
