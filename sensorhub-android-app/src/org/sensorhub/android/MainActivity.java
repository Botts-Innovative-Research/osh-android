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
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.os.PowerManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.opengis.swe.v20.DataBlock;

import org.sensorhub.android.comm.BluetoothCommProvider;
import org.sensorhub.android.comm.BluetoothCommProviderConfig;
import org.sensorhub.android.comm.ble.BleConfig;
import org.sensorhub.android.comm.ble.BleNetwork;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClientConfig;
import org.sensorhub.impl.module.ModuleRegistry;
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

import android.view.KeyEvent;
import android.view.MotionEvent;
import org.sensorhub.impl.sensor.kestrel.KestrelConfig;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticConfig;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticSensor;
import org.sensorhub.impl.sensor.meshtastic.control.TextMessageControl;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    public static final Date TRUPULSE_SENSOR_LAST_UPDATED = ANDROID_SENSORS_LAST_UPDATED;
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    SensorHubService boundService;
    IModuleConfigRepository sensorhubConfig;
    boolean oshStarted = false;
    ArrayList<SOSTClient> sostClients = new ArrayList<>();
    ArrayList<ConSysApiClientModule> conSysClients = new ArrayList<>();

    URL url;
    AndroidSensorsDriver androidSensors;
    boolean showVideo;
    URL clientURL = null;

    String deviceID;
    String runName;

    private BroadcastReceiver broadcastReceiver;

    enum Sensors {
        Android,
        TruPulse,
        TruPulseSim,
        Angel,
        FlirOne,
        DJIDrone,
        ProxySensor,
        BLELocation,
        Meshtastic,
        PolarHRMonitor,
        Kestrel,
        Wardriving,
        Controller,
        Template
    }

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
    public ArrayList<SOSTClient> getSostClients() { return sostClients; }

    @Override
    public ArrayList<ConSysApiClientModule> getConSysClients() { return conSysClients; }

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

        String host = prefs.getString("ip_address", "").trim();
        String port = prefs.getString("port", "").trim();
        String user = prefs.getString("username", null);
        String password = prefs.getString("password", null);
        String endpointPath = prefs.getString("endpoint_path", null);

        Boolean isApiServiceEnabled = prefs.getBoolean("api_service", true);
        Boolean isSosServiceEnabled = prefs.getBoolean("sos_service", true);
        Boolean isClientEnabled = prefs.getBoolean("enable_client", true);
        Boolean isTLSEnabled = prefs.getBoolean("enable_tls", false);

        if (host.isEmpty())
            host = "127.0.0.1";
        if (port.isEmpty())
            port = "8585";
        
        String url = (isTLSEnabled ? "https://" : "http://") + host + ":" + port + endpointPath;

        try {
            clientURL = new URI(url).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            log.error("Error: Client URL is invalid");
        }


        boolean disableSslCheck = prefs.getBoolean("sos_disable_ssl_check", false);
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

        // OAuth
        Boolean isOAuthEnabled = prefs.getBoolean("o_auth_enabled", false);
        String clientId = prefs.getString("client_id", "").trim();
        String tokenEndpoint = prefs.getString("token_endpoint", "").trim();
        String clientSecret = prefs.getString("client_secret", "").trim();


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

        ConSysOAuthConfig conSysOAuthConfig = new ConSysOAuthConfig();
        conSysOAuthConfig.oAuthEnabled = isOAuthEnabled;
        conSysOAuthConfig.tokenEndpoint = tokenEndpoint;
        conSysOAuthConfig.clientID = clientId;
        conSysOAuthConfig.clientSecret = clientSecret;

        sensorhubConfig.add(sensorsConfig);

        if (isPushingSensor(Sensors.Android)) {
            if (isClientEnabled) {
                addCSApiConfig(sensorsConfig, user, password, conSysOAuthConfig);
            } else {
                addSosTConfig(sensorsConfig, user, password);
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

        // TruPulse sensor
        boolean enabled = prefs.getBoolean("trupulse_enabled", false);
        if (enabled)
        {
            TruPulseConfig trupulseConfig = new TruPulseConfig();

            if (sensorsConfig.activateGpsLocation)
            {
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
            trupulseConfig.lastUpdated = TRUPULSE_SENSOR_LAST_UPDATED;
            trupulseConfig.serialNumber = deviceID;
            BluetoothCommProviderConfig btConf = new BluetoothCommProviderConfig();
            btConf.protocol.deviceName = prefs.getString("trupulse_device_address", "");
            if (prefs.getBoolean("trupulse_simu", false))
                btConf.moduleClass = SimulatedDataStream.class.getCanonicalName();
            else {
                btConf.moduleClass = BluetoothCommProvider.class.getCanonicalName();
                trupulseConfig.connection.connectTimeout = 100000;
                trupulseConfig.connection.reconnectAttempts = 10;
            }
            trupulseConfig.commSettings = btConf;
            sensorhubConfig.add(trupulseConfig);
        }

        // STE Rad Pager sensor
        enabled = prefs.getBoolean("ste_radpager_enabled", false);
        if (enabled) {
            STERadPagerConfig steRadPagerConfig = new STERadPagerConfig();
            steRadPagerConfig.id = "STE_RADPAGER_SENSOR";
            steRadPagerConfig.name = "STE Rad Pager [" + deviceName + "]";
            steRadPagerConfig.autoStart = true;
            steRadPagerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            sensorhubConfig.add(steRadPagerConfig);
        }

        // Meshtastic Sensor
        enabled = prefs.getBoolean("meshtastic_enabled", false);
        if (enabled)
        {
            MeshtasticConfig meshtasticConfig = new MeshtasticConfig();
            meshtasticConfig.id = "MESHTASTIC_SENSOR";
            meshtasticConfig.name = "Meshtastic [" + deviceName + "]";
            meshtasticConfig.autoStart = true;
            meshtasticConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            meshtasticConfig.device_name = prefs.getString("meshtastic_device_address", "");
            meshtasticConfig.uid_extension = prefs.getString("uid_extension", "");
            sensorhubConfig.add(meshtasticConfig);
        }

        // Polar heart Sensor
        enabled = prefs.getBoolean("polar_enabled", false);
        if (enabled) {
            PolarConfig polarConfig = new PolarConfig();
            polarConfig.id = "POLAR_HEART_SENSOR";
            polarConfig.name = "Polar Heart [" + deviceName + "]";
            polarConfig.autoStart = true;
            polarConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            polarConfig.device_name = prefs.getString("polar_device_address", "");
            polarConfig.uid_extension = prefs.getString("uid_extension", "");
            sensorhubConfig.add(polarConfig);
        }

        // Kestrel Weather
        enabled = prefs.getBoolean("kestrel_enabled", false);
        if (enabled) {
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

        // Wardriving
        enabled = prefs.getBoolean("wardriving_enabled", false);
        if (enabled) {
            WardrivingConfig wardrivingConfig = new WardrivingConfig();
            wardrivingConfig.id = "WARDRIVING_";
            wardrivingConfig.name = "Wardriving [" + deviceName + "]";
            wardrivingConfig.autoStart = true;
            wardrivingConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            wardrivingConfig.uid_extension = prefs.getString("uid_extension", "");
            sensorhubConfig.add(wardrivingConfig);
        }

        // USB Controller
        enabled = prefs.getBoolean("controller_enabled", false);
        if (enabled) {
            ControllerConfig controllerConfig = new ControllerConfig();
            controllerConfig.id = "CONTROLLER";
            controllerConfig.name = "Controller [" + deviceName + "]";
            controllerConfig.autoStart = true;
            controllerConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            controllerConfig.uid_extension = prefs.getString("uid_extension", "");
            sensorhubConfig.add(controllerConfig);
        }

        if (isApiServiceEnabled) {
            sensorhubConfig.add(conSysApiService);
        }
        if (isSosServiceEnabled) {
            sensorhubConfig.add(sosConfig);
        }

        // Template Driver
        enabled = prefs.getBoolean("template_enabled", false);
        if (enabled) {
            TemplateConfig templateConfig = new TemplateConfig();
            templateConfig.id = "TEMPLATE_DRIVER_";
            templateConfig.name = "Template [" + deviceName + "]";
            templateConfig.autoStart = true;
            templateConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
            templateConfig.uid_extension = prefs.getString("uid_extension", "");
            sensorhubConfig.add(templateConfig);
        }

    }

    protected void addSosTConfig(SensorConfig sensorConf, String user, String pwd)
    {
        if (clientURL == null)
            return;
        SOSTClientConfig sosConfig = new SOSTClientConfig();
        sosConfig.id = sensorConf.id + "_SOST";
        sosConfig.name = sensorConf.name.replaceAll("\\[.*\\]", "");
        sosConfig.autoStart = true;
        sosConfig.sos.remoteHost = clientURL.getHost();
        sosConfig.sos.remotePort = clientURL.getPort() < 0 ? clientURL.getDefaultPort() : clientURL.getPort();
        sosConfig.sos.resourcePath = clientURL.getPath();
        sosConfig.sos.enableTLS = clientURL.getProtocol().equals("https");
        sosConfig.sos.user = user;
        sosConfig.sos.password = pwd;
        sosConfig.connection.connectTimeout = 10000;
        sosConfig.connection.usePersistentConnection = true;
        sosConfig.connection.reconnectAttempts = 9;
        sosConfig.connection.maxQueueSize = 100;
        sosConfig.dataSourceSelector = new ObsSystemDatabaseViewConfig();
        sensorhubConfig.add(sosConfig);
    }

    protected void addCSApiConfig(SensorConfig sensorConf, String apiUser, String apiPwd, ConSysOAuthConfig oAuthConfig)
    {
        if (clientURL == null)
            return;

        ConSysApiClientConfig consysConfig = new ConSysApiClientConfig();
        consysConfig.id = sensorConf.id + "_CONSYS";
        consysConfig.name = sensorConf.name.replaceAll("\\[.*\\]", "");
        consysConfig.autoStart = true;
        consysConfig.conSys.remoteHost = clientURL.getHost();
        consysConfig.conSys.remotePort = clientURL.getPort() < 0 ? clientURL.getDefaultPort() : clientURL.getPort();
        consysConfig.conSys.resourcePath = clientURL.getPath();
        consysConfig.conSys.enableTLS = clientURL.getProtocol().equals("https");
        consysConfig.conSys.user = apiUser;
        consysConfig.conSys.password = apiPwd;
        consysConfig.connection.connectTimeout = 10000;
        consysConfig.connection.reconnectAttempts = 9;
        consysConfig.httpClientImplClass = OkHttpClientWrapper.class.getCanonicalName();
        consysConfig.dataSourceSelector = new ObsSystemDatabaseViewConfig();
        consysConfig.conSysOAuth = oAuthConfig;
        sensorhubConfig.add(consysConfig);
    }


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        Fragment homeFragment = new DashboardFragment();
        Fragment sensorsFragment = new SensorsFragment();
        Fragment settingsFragment = new SettingsFragment();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.dashboard:
                    setCurrentFragment(homeFragment);
                    break;
                case R.id.sensors:
                    setCurrentFragment(sensorsFragment);
                    break;
                case R.id.settings:
                    setCurrentFragment(settingsFragment);
                    break;
            }
            return true;
        });

        setCurrentFragment(homeFragment);
        bottomNav.setSelectedItemId(R.id.dashboard);

        hasBluetoothPermissions();
        checkForPermissions();

        // bind to SensorHub service
        Intent intent = new Intent(this, SensorHubService.class);
        startService(intent);
        bindService(intent, sConn, Context.BIND_AUTO_CREATE);

        setupBroadcastReceivers();
        requestBatteryOptimizationExemption();
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_about)
        {
            showAboutPopup();
            return true;
        }
        else if (id == R.id.action_meshtastic)
        {
            showMeshtasticDialog();
            return true;
        }
        else if(id == R.id.action_status) {
            Intent statusIntent = new Intent(this, AppStatusActivity.class);

            if (boundService != null && boundService.sensorhub != null) {
                ModuleRegistry moduleRegistry = boundService.sensorhub.getModuleRegistry();
                Collection<IModule<?>> modules = moduleRegistry.getLoadedModules();

                for (IModule module : modules) {
                    var moduleConf = module.getConfiguration();

                    if (moduleConf instanceof ModuleConfig) {
                        String status = module.getCurrentState().name();
                        String moduleId = ((ModuleConfig) moduleConf).id;

                        switch (moduleId) {
                            case "HTTP_SERVER_0":
                                statusIntent.putExtra("httpStatus", status);
                                break;
                            case "SOS_SERVICE":
                                statusIntent.putExtra("sosService", status);
                                break;
                            case "CON_SYS_SERVICE":
                                statusIntent.putExtra("conSysService", status);
                                break;
                            case "ANDROID_SENSORS":
                                statusIntent.putExtra("androidSensorStatus", status);
                                break;
                            case "ANDROID_SENSORS#storage":
                                statusIntent.putExtra("sensorStorageStatus", status);
                                break;
                        }
                    }
                }
            } else {
                statusIntent.putExtra("sosService", "N/A");
                statusIntent.putExtra("conSysService", "N/A");
                statusIntent.putExtra("httpStatus", "N/A");
                statusIntent.putExtra("androidSensorStatus", "N/A");
                statusIntent.putExtra("sensorStorageStatus", "N/A");
            }

            startActivity(statusIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    // ==================== Controller Event Forwarding ====================

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

    // ==================== Dialogs ====================

    protected void showMeshtasticDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_meshtastic, null);

        EditText messageInput = dialogView.findViewById(R.id.msg_input);
        EditText destinationIdText = dialogView.findViewById(R.id.destination_nodeId);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Send Meshtastic Message");
        builder.setView(dialogView);

        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String msg = messageInput.getText().toString();
                String destinationId = destinationIdText.getText().toString();
                try {
                    sendMeshtasticMessage(msg, destinationId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendMeshtasticMessage(String message, String nodeId) throws IOException {
        ModuleRegistry reg = boundService.getSensorHub().getModuleRegistry();
        MeshtasticSensor meshy = reg.getModuleByType(MeshtasticSensor.class);

        IStreamingControlInterface textMessageControl = meshy.getCommandInputs().get(TextMessageControl.NAME);

        DataBlock cmdData = textMessageControl.getCommandDescription().createDataBlock();
        cmdData.setStringValue(0, message);
        cmdData.setIntValue(1, Integer.parseInt(nodeId));

        var cmd = new CommandData.Builder()
                .withCommandStream(BigId.NONE)
                .withSender(deviceID)
                .withParams(cmdData)
                .build();

        textMessageControl.submitCommand(cmd);
    }

    protected void showAboutPopup() {
        String version = "?";

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }

        String message = "A software platform for building smart sensor networks and the Internet of Things\n\n";
        message += "Version: " + version + "\n";

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("OpenSensorHub");
        alert.setMessage(message);
        alert.setIcon(R.drawable.ic_launcher);
        alert.show();
    }

    // ========================================

    boolean isPushingSensor(Sensors sensor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Sensors.Android.equals(sensor)) {
            if (prefs.getBoolean("accel_enabled", false)
                    && prefs.getStringSet("accel_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("gyro_enabled", false)
                    && prefs.getStringSet("gyro_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("mag_enabled", false)
                    && prefs.getStringSet("mag_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("orient_quat_enabled", false)
                    && prefs.getStringSet("orient_quat_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("orient_euler_enabled", false)
                    && prefs.getStringSet("orient_euler_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("gps_enabled", false)
                    && prefs.getStringSet("gps_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("netloc_enabled", false)
                    && prefs.getStringSet("netloc_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("cam_enabled", false)
                    && prefs.getStringSet("cam_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
            if (prefs.getBoolean("audio_enabled", false)
                    && prefs.getStringSet("audio_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
        } else if (Sensors.TruPulse.equals(sensor) || Sensors.TruPulseSim.equals(sensor)) {
            return prefs.getBoolean("trupulse_enabled", false)
                    && prefs.getStringSet("trupulse_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.BLELocation.equals(sensor)) {
            return prefs.getBoolean("ble_enable", false) && prefs.getStringSet("ble_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.Meshtastic.equals(sensor)) {
            return prefs.getBoolean("meshtastic_enabled", false)
                    && prefs.getStringSet("meshtastic_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.PolarHRMonitor.equals(sensor)) {
            return prefs.getBoolean("polar_enabled", false)
                    && prefs.getStringSet("polar_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.Kestrel.equals(sensor)) {
            return prefs.getBoolean("kestrel_enabled", false)
                    && prefs.getStringSet("kestrel_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.Wardriving.equals(sensor)) {
            return prefs.getBoolean("wardriving_enabled", false)
                    && prefs.getStringSet("wardriving_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if (Sensors.Controller.equals(sensor)) {
            return prefs.getBoolean("controller_enabled", false)
                    && prefs.getStringSet("controller_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }  else if (Sensors.Template.equals(sensor)) {
            return prefs.getBoolean("template_enabled", false)
                    && prefs.getStringSet("template_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }

        return false;
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

                    if (sosEndpointUrl == null || name == null || sensorId == null || properties.size() == 0) {
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
