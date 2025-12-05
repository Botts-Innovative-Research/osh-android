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

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.graphics.SurfaceTexture;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import net.opengis.swe.v20.DataBlock;

import org.sensorhub.android.comm.BluetoothCommProvider;
import org.sensorhub.android.comm.BluetoothCommProviderConfig;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClient.StreamInfo;
import org.sensorhub.impl.client.sost.SOSTClientConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig;
import org.sensorhub.impl.event.EventBus;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.module.ModuleClassFinder;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import org.sensorhub.impl.sensor.android.audio.AudioEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig.VideoPreset;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticSensor;
import org.sensorhub.impl.sensor.meshtastic.control.TextMessageControl;
import org.sensorhub.impl.sensor.kestrel.KestrelConfig;
import org.sensorhub.impl.sensor.polar.PolarConfig;
import org.sensorhub.impl.sensor.ste.STERadPagerConfig;
import org.sensorhub.impl.sensor.trupulse.TruPulseConfig;
import org.sensorhub.impl.sensor.trupulse.TruPulseWithGeolocConfig;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.consys.ConSysApiService;
import org.sensorhub.impl.service.consys.ConSysApiServiceConfig;
import org.sensorhub.impl.service.consys.client.ConSysApiClientConfig;
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule;
import org.sensorhub.impl.service.consys.client.ConSysOAuthConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.impl.service.sos.SOSServiceConfig;
import org.sensorhub.impl.sensor.trupulse.SimulatedDataStream;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Flow;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, Flow.Subscriber<Event>
{
    public static final String ACTION_BROADCAST_RECEIVER = "org.sensorhub.android.BROADCAST_RECEIVER";
    public static final String ANDROID_SENSORS_MODULE_ID = "ANDROID_SENSORS";
    public static final Date ANDROID_SENSORS_LAST_UPDATED = new Date(Instant.now().toEpochMilli());
    public static final Date TRUPULSE_SENSOR_LAST_UPDATED = ANDROID_SENSORS_LAST_UPDATED;
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    TextView mainInfoArea;
    TextView videoInfoArea;
    SensorHubService boundService;
    IModuleConfigRepository sensorhubConfig;
    Handler displayHandler;
    Runnable displayCallback;
    StringBuffer mainInfoText = new StringBuffer();
    StringBuffer videoInfoText = new StringBuffer();
    boolean oshStarted = false;
    ArrayList<SOSTClient> sostClients = new ArrayList<>();
    ArrayList<ConSysApiClientModule> conSysClients = new ArrayList<>();

    URL url;
    AndroidSensorsDriver androidSensors;
    boolean showVideo;
    URI clientUri = null;
    URL clientURL = null;

    String deviceID;
    String runName;

    private Flow.Subscription subscription;
    Flow.Subscriber mainActivity = this;

    // Request codes for permissions
    final int FINE_LOC_RC = 101;
    final int CAMERA_RC = 102;
    final int AUDIO_RC = 103;

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
        KestrelBallistics
    }


    private final ServiceConnection sConn = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            boundService = ((SensorHubService.LocalBinder) service).getService();
//            boundService.initSensorhub();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            boundService = null;
        }
    };



    protected void updateConfig(SharedPreferences prefs, String runName)
    {
        deviceID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        sensorhubConfig = new InMemoryConfigDb(new ModuleClassFinder());

        //get ip, port, user, password
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

//        String sensorhubEndpoint = "/sensorhub";

        String newUrl = (isTLSEnabled ? "https://" : "http://") + host + ":" + port + endpointPath;

        try {
            clientUri = new URI(newUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            clientURL = clientUri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // disable SSL check if requested
        boolean disableSslCheck = prefs.getBoolean("sos_disable_ssl_check", false);
        if (disableSslCheck)
        {
            // Create a trust manager that does not validate certificate chains
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

            // Install the all-trusting trust manager
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



        // get device name
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
        /*if (sensorsConfig.activateBackCamera || sensorsConfig.activateFrontCamera)
            showVideo = true;*/
        if (sensorsConfig.enableCamera)
            showVideo = true;

        // video settings
        sensorsConfig.videoConfig.codec = prefs.getString("video_codec", VideoEncoderConfig.JPEG_CODEC);
        sensorsConfig.videoConfig.frameRate = Integer.parseInt(prefs.getString("video_framerate", "30"));

        // selected preset or AUTO mode
        String selectedPreset = prefs.getString("video_preset", "0");
        if ("AUTO".equals(selectedPreset)) {
            sensorsConfig.videoConfig.autoPreset = true;
            sensorsConfig.videoConfig.selectedPreset = 0;
        }
        else {
            sensorsConfig.videoConfig.autoPreset = false;
            sensorsConfig.videoConfig.selectedPreset = Integer.parseInt(selectedPreset);
        }

        // video preset list
        int resIdx = 1;
        ArrayList<VideoPreset> presetList = new ArrayList<>();
        while (prefs.contains("video_size" + resIdx))
        {
            String resString = prefs.getString("video_size" + resIdx, "Disabled");
            String[] tokens = resString.split("x");
            VideoPreset preset = new VideoPreset();
            preset.width = Integer.parseInt(tokens[0]);
            preset.height = Integer.parseInt(tokens[1]);
            preset.minBitrate = Integer.parseInt(prefs.getString("video_min_bitrate" + resIdx, "3000"));
            preset.maxBitrate = Integer.parseInt(prefs.getString("video_max_bitrate" + resIdx, "3000"));
            preset.selectedBitrate = preset.maxBitrate;
            presetList.add(preset);
            resIdx++;
        }
        sensorsConfig.videoConfig.presets = presetList.toArray(new VideoPreset[0]);

        sensorsConfig.outputVideoRoll = prefs.getBoolean("video_roll_enabled", false);

        // audio
        sensorsConfig.activateMicAudio = prefs.getBoolean("audio_enabled", false);
        sensorsConfig.audioConfig.codec = prefs.getString("audio_codec", AudioEncoderConfig.AAC_CODEC);
        sensorsConfig.audioConfig.sampleRate = Integer.parseInt(prefs.getString("audio_samplerate", "8000"));
        sensorsConfig.audioConfig.bitRate = Integer.parseInt(prefs.getString("audio_bitrate", "64"));

        sensorsConfig.runName = runName;


        // START SOS Config ************************************************************************
        // Setup HTTPServerConfig for enabling more complete node functionality
        HttpServerConfig serverConfig = new HttpServerConfig();
        serverConfig.proxyBaseUrl = "";
        serverConfig.httpPort = 8585;
        serverConfig.autoStart = true;
        sensorhubConfig.add(serverConfig);

        // We don't need android context unless we're doing IPC things
        SOSServiceConfig sosConfig = new SOSServiceConfig();
        sosConfig.moduleClass = SOSService.class.getCanonicalName();
        sosConfig.id = "SOS_SERVICE";
        sosConfig.name = "SOS Service";
        sosConfig.autoStart = true;
        sosConfig.enableTransactional = true;
        sosConfig.exposedResources = new ObsSystemDatabaseViewConfig();


        //Connected systems service
        ConSysApiServiceConfig conSysApiService = new ConSysApiServiceConfig();
        conSysApiService.moduleClass = ConSysApiService.class.getCanonicalName();
        conSysApiService.id = "CON_SYS_SERVICE";
        conSysApiService.name= "Connected Systems API Service";
        conSysApiService.autoStart = true;
        conSysApiService.enableTransactional = true;
        conSysApiService.exposedResources = new ObsSystemDatabaseViewConfig();

        ConSysOAuthConfig conSysOAuthConfig = new ConSysOAuthConfig();

        if (isOAuthEnabled && !clientId.isEmpty() && !tokenEndpoint.isEmpty() && !clientSecret.isEmpty()) {
            conSysOAuthConfig.oAuthEnabled = true;
            conSysOAuthConfig.tokenEndpoint = tokenEndpoint;
            conSysOAuthConfig.clientID = clientId;
            conSysOAuthConfig.clientSecret = clientSecret;
        }



        // Push Sensors Config
        sensorhubConfig.add(sensorsConfig);

        if (isPushingSensor(Sensors.Android)) {
            if (isClientEnabled) {
                System.out.println("Connected Systems Client enabled");
                if (isOAuthEnabled)
                    addCSApiConfig(sensorsConfig, user, password, conSysOAuthConfig);
                else
                    addCSApiConfig(sensorsConfig, user, password, null);

            } else {
                System.out.println("SOST Client enabled");
                addSosTConfig(sensorsConfig, user, password);
            }

        }

        //Storage Configuration
//        if(prefs.getBoolean("hub_enable", true) && prefs.getBoolean("hub_enable_local_storage", true)) {
        if(shouldStore(prefs)) {
            File dbFile = new File(getApplicationContext().getFilesDir() + "/db/");
            dbFile.mkdirs();
            MVObsSystemDatabaseConfig basicStorageConfig = new MVObsSystemDatabaseConfig();
            basicStorageConfig.moduleClass = "org.sensorhub.impl.persistence.h2.MVObsStorageImpl";
            basicStorageConfig.storagePath = dbFile.getAbsolutePath() + "/${STORAGE_ID}.dat";
            basicStorageConfig.autoStart = true;

//            sosConfig.newStorageConfig = basicStorageConfig;

//            StreamStorageConfig androidStreamStorageConfig = createStreamStorageConfig(androidSensorsConfig);
//            addStorageConfig(androidSensorsConfig, androidStreamStorageConfig);

           /* File dbFile = new File(getApplicationContext().getFilesDir() + "/db/");
            dbFile.mkdirs();

            MVStorageConfig storageConfig = new MVStorageConfig();
            storageConfig.setStorageIdentifier("OSH_CONNECT_OBS");*/
        }

//        SensorDataProviderConfig androidDataProviderConfig = createDataProviderConfig(androidSensorsConfig);
//        addSosServerConfig(sosConfig, androidDataProviderConfig);
        // END SOS CONFIG **************************************************************************

        // TruPulse sensor
        boolean enabled = prefs.getBoolean("trupulse_enabled", false);
        if (enabled)
        {
            TruPulseConfig trupulseConfig = new TruPulseConfig();

            // add target geolocation processing if GPS is enabled
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
                ((TruPulseWithGeolocConfig)trupulseConfig).locationSourceUID = AndroidSensorsConfig.getAndroidSensorsUid();
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
            else{
                btConf.moduleClass = BluetoothCommProvider.class.getCanonicalName();
                trupulseConfig.connection.connectTimeout = 100000;
                trupulseConfig.connection.reconnectAttempts = 10;
            }
            trupulseConfig.commSettings = btConf;


            sensorhubConfig.add(trupulseConfig);
        }

        // STE Rad Pager sensor
        enabled = prefs.getBoolean("ste_radpager_enabled", false);
        if(enabled){
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

            sensorhubConfig.add(meshtasticConfig);
        }

        // polar heart Sensor
        enabled = prefs.getBoolean("polar_enabled", false);
        if (enabled) {
            PolarConfig polarConfig = new PolarConfig();
          polarConfig.id = "POLAR_HEART_SENSOR";
          polarConfig.name = "Polar Heart [" + deviceName + "]";
          polarConfig.autoStart = true;
          polarConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
          polarConfig.device_name = prefs.getString("polar_device_address", "");

            sensorhubConfig.add(polarConfig);
        }

        // Kestrel Ballistics Weather
        enabled = prefs.getBoolean("kestrel_enabled", false);
        if (enabled) {
            KestrelConfig kestrelConfig = new KestrelConfig();
            kestrelConfig.id = "KESTREL_WEATHER";
            kestrelConfig.name = "Kestrel Weather [" + deviceName + "]";
            kestrelConfig.autoStart = true;
            kestrelConfig.lastUpdated = ANDROID_SENSORS_LAST_UPDATED;
//            kestrelConfig.device_name = "FE:BB:D9:8B:53:23";

            kestrelConfig.serialNumber = prefs.getString("kestrel_serial", null);
//                    prefs.getString("kestrel_device_address", "");

            sensorhubConfig.add(kestrelConfig);
        }


        // AngelSensor
        /*enabled = prefs.getBoolean("angel_enabled", false);
        if (enabled)
        {
            BleConfig bleConf = new BleConfig();
            bleConf.id = "BLE";
            bleConf.moduleClass = BleNetwork.class.getCanonicalName();
            bleConf.androidContext = this.getApplicationContext();
            bleConf.autoStart = true;
            sensorhubConfig.add(bleConf);

            AngelSensorConfig angelConfig = new AngelSensorConfig();
            angelConfig.id = "ANGEL_SENSOR";
            angelConfig.name = "Angel Sensor [" + deviceName + "]";
            angelConfig.autoStart = true;
            angelConfig.networkID = bleConf.id;
            //angelConfig.btAddress = "00:07:80:79:04:AF"; // mike
            //angelConfig.btAddress = "00:07:80:03:0E:0A"; // alex
            angelConfig.btAddress = prefs.getString("angel_address", null);
            sensorhubConfig.add(angelConfig);
            addSosTConfig(angelConfig, sosUser, sosPwd);
        }

        // FLIR One sensor
        enabled = prefs.getBoolean("flirone_enabled", false);
        if (enabled)
        {
            FlirOneCameraConfig flironeConfig = new FlirOneCameraConfig();
            flironeConfig.id = "FLIRONE_SENSOR";
            flironeConfig.name = "FLIR One Camera [" + deviceName + "]";
            flironeConfig.autoStart = true;
            flironeConfig.androidContext = this.getApplicationContext();
            flironeConfig.camPreviewTexture = boundService.getVideoTexture();
            showVideo = true;
            sensorhubConfig.add(flironeConfig);
            addSosTConfig(flironeConfig, sosUser, sosPwd);
        }*/

        // DJI Drone
        /*enabled = prefs.getBoolean("dji_enabled", false);
        if (enabled)
        {
            DjiConfig djiConfig = new DjiConfig();
            djiConfig.id = "DJI_DRONE";
            djiConfig.name = "DJI Aircraft [" + deviceName + "]";
            djiConfig.autoStart = true;
            djiConfig.androidContext = this.getApplicationContext();
            djiConfig.camPreviewTexture = boundService.getVideoTexture();
            showVideo = true;
            sensorhubConfig.add(djiConfig);
            addSosTConfig(djiConfig, sosUser, sosPwd);
        }*/

        if(isApiServiceEnabled){
            //add connected sys service
            System.out.println("Connected Systems Service enabled");
            sensorhubConfig.add(conSysApiService);
        }
        if(isSosServiceEnabled){
            //if off add sos service
            System.out.println("SOS Service enabled");
            sensorhubConfig.add(sosConfig);
        }
    }

    protected void addSosTConfig(SensorConfig sensorConf, String user, String pwd)
    {


        if (clientURL == null)
            return;

//        URL sosUrl = null;
//        try {
//            sosUrl = clientUri.toURL();
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }


//        try {
////            sosUrl = clientUri.resolve("/sensorhub/sos").toURL();
//            sosUrl = clientUri.toURL();
//            System.out.println("SOS URL"+ sosUrl);
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }

        SOSTClientConfig sosConfig = new SOSTClientConfig();
        sosConfig.id = sensorConf.id + "_SOST";
        sosConfig.name = sensorConf.name.replaceAll("\\[.*\\]", "");// + "SOS-T Client";
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
//        URL apiUrl;
//
//        if (clientUri == null)
//            return;
//
//        try {
//            apiUrl = clientUri.resolve("/sensorhub/api/").toURL();
//            System.out.println("API URL"+ apiUrl);
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }

        System.out.println("client URL: " + clientURL);
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

        consysConfig.dataSourceSelector = new ObsSystemDatabaseViewConfig();

        if (oAuthConfig != null)
            consysConfig.conSysOAuth = oAuthConfig;


        sensorhubConfig.add(consysConfig);
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
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainInfoArea =  findViewById(R.id.main_info);
        videoInfoArea = findViewById(R.id.video_info);

        // listen to texture view lifecycle
        TextureView textureView = findViewById(R.id.video);
        textureView.setSurfaceTextureListener(this);

        // bind to SensorHub service
        Intent intent = new Intent(this, SensorHubService.class);
        startService(intent);  // ADD THIS LINE
        bindService(intent, sConn, Context.BIND_AUTO_CREATE);

        // handler to refresh sensor status in UI
        displayHandler = new Handler(Looper.getMainLooper());

        setupBroadcastReceivers();
        checkForPermissions();
        requestBatteryOptimizationExemption();

        // Due to changes with OSH, it may be best to create and start the hub immediately
        // This allows us access to the module registry created by default
//        boundService.initSensorhub();
    }


    Menu optionsMenu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        optionsMenu = menu;

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, UserSettingsActivity.class));
            return true;
        }
        else if (id == R.id.action_start)
        {
            if (boundService != null && boundService.getSensorHub() == null)
                showRunNamePopup();
            return true;
        }
        else if (id == R.id.action_stop)
        {
            stopListeningForEvents();
            stopRefreshingStatus();
            sostClients.clear();
            conSysClients.clear();
            if (boundService != null)
                boundService.stopSensorHub();
            mainInfoArea.setBackgroundColor(0xFFFFFFFF);
            oshStarted = false;
            newStatusMessage("SensorHub Stopped");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return true;
        }
        else if (id == R.id.action_about)
        {
            showAboutPopup();
        }
        else if (id == R.id.action_meshtastic) {

            showMeshtasticDialog();
        }
//        else if (id == R.id.action_restart){
//            stopListeningForEvents();
//            stopRefreshingStatus();
//
//            if (boundService != null)
//              boundService.stopSensorHub();
//
//            restartSensorHub();
//
//        }
        else if(id == R.id.action_status)
        {
            Intent statusIntent = new Intent(this, AppStatusActivity.class);
            if(boundService.sensorhub != null) {
                ModuleRegistry moduleRegistry = boundService.sensorhub.getModuleRegistry();
                Collection<IModule<?>> modules = moduleRegistry.getLoadedModules();

                for (IModule module : modules) {
                    var moduleConf = module.getConfiguration();
                    String status = module.getCurrentState().name();

                    switch (((ModuleConfig) moduleConf).id) {
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
            else {
                statusIntent.putExtra("sosService", "N/A");
                statusIntent.putExtra("conSysService", "N/A");
                statusIntent.putExtra("httpStatus", "N/A");
                statusIntent.putExtra("androidSensorStatus", "N/A");
                statusIntent.putExtra("sensorStorageStatus", "N/A");
            }

//            statusIntent.putExtra("boundService", boundService);


            startActivity(statusIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void showMeshtasticDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_meshtastic, null);

        EditText messageInput = dialogView.findViewById(R.id.msg_input);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Send Meshtastic Message");
        builder.setView(dialogView);

        EditText destinationIdText = dialogView.findViewById(R.id.destination_nodeId);


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
        log.debug("sending mesh node");
        // todo think ab how we want to send the commands
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

    protected synchronized void showRunNamePopup() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Run Name");
        alert.setMessage("Please enter the name for this run");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.getText().append("Run-");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        input.getText().append(formatter.format(new Date()));
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                String runName = input.getText().toString();


                updateConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), runName);

                AndroidSensorsConfig androidSensorConfig = (AndroidSensorsConfig) sensorhubConfig.get("ANDROID_SENSORS");
                VideoEncoderConfig videoConfig = androidSensorConfig.videoConfig;

                boolean cameraInUse = (androidSensorConfig.activateBackCamera || androidSensorConfig.activateFrontCamera);
                boolean improperVideoSettings = (videoConfig.selectedPreset < 0 || videoConfig.selectedPreset >= videoConfig.presets.length);

                if (cameraInUse && improperVideoSettings) {
                    showVideoConfigErrorPopup();
                    newStatusMessage("Video Config Error: Check Settings");
                } else {
                    newStatusMessage("Starting SensorHub...");
                    sostClients.clear();
                    conSysClients.clear();
                    boundService.startSensorHub(sensorhubConfig, showVideo);

                    if (boundService.hasVideo())
                        mainInfoArea.setBackgroundColor(0x80FFFFFF);

                    while(boundService.getSensorHub() == null){
                        System.out.println("Waiting for BoundService Hub to start...");
                    }
                    System.out.println("BoundService SensorHub Started...");
                    while(boundService.getSensorHub().getEventBus() == null){
                        System.out.println("Waiting for BoundService Hub EventBus to start...");
                    }
                    System.out.println("BoundService SensorHub EventBus Started...");
                    EventBus shEvtBus = (EventBus) boundService.getSensorHub().getEventBus();

                    shEvtBus.newSubscription()
                            .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
                            .subscribe(mainActivity);
                }

            }
        });

        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
        });

        alert.show();
    }


    protected void showAboutPopup() {
        String version = "?";

        try
        {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
        }

        String message = "A software platform for building smart sensor networks and the Internet of Things\n\n";
        message += "Version: " + version + "\n";

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("OpenSensorHub");
        alert.setMessage(message);
        alert.setIcon(R.drawable.ic_launcher);
        alert.show();
    }

    protected void showVideoConfigErrorPopup() {
        String message = "Check Video Settings and ensure the resolution for the selected preset has been set.";

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("OpenSensorHub");
        alert.setMessage(message);
        alert.setPositiveButton("OK", (dialog, id) -> {
            // user accepted
        });
        alert.show();
    }


    protected void startRefreshingStatus() {
        if (displayCallback != null)
            return;

        // handler to display async messages in UI
        displayCallback = new Runnable()
        {
            public void run()
            {
                displayStatus();
                mainInfoArea.setText(Html.fromHtml(mainInfoText.toString()));
                videoInfoArea.setText(Html.fromHtml(videoInfoText.toString()));
                displayHandler.postDelayed(this, 1000);
            }
        };

        displayHandler.post(displayCallback);
    }


    protected void stopRefreshingStatus()
    {
        if (displayCallback != null)
        {
            displayHandler.removeCallbacks(displayCallback);
            displayCallback = null;
        }
    }


    protected synchronized void displayStatus() {

        boolean needsRestart = false;

        mainInfoText.setLength(0);

        // first display error messages if any
        for (SOSTClient client: sostClients)
        {
            Map<String, StreamInfo> dataStreams = client.getDataStreams();
            boolean showError = (client.getCurrentError() != null);
            boolean showMsg = (dataStreams.isEmpty()) && (client.getStatusMessage() != null);

            if (showError || showMsg)
            {
                mainInfoText.append("<p>" + client.getName() + ":<br/>");
                if (showMsg)
                    mainInfoText.append(client.getStatusMessage() + "<br/>");
                if (showError)
                {
                    Throwable errorObj = client.getCurrentError();
                    String errorMsg = errorObj.getMessage().trim();
                    if (!errorMsg.endsWith("."))
                        errorMsg += ". ";
                    if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                        errorMsg += errorObj.getCause().getMessage();
                    mainInfoText.append("<font color='red'>" + errorMsg + "</font>");
                }
                mainInfoText.append("</p>");

            }

//            log.debug("[SENSOR RESTART]", client.isConnected());

//            if(!client.isConnected()){
//                mainInfoText.setLength(0);
//                mainInfoText.append("Attempting to restart SensorHub");
//                needsRestart = true;
//            }
        }


        for (ConSysApiClientModule client: conSysClients)
        {
            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            boolean showError = (client.getCurrentError() != null);
            boolean showMsg = (dataStreams.isEmpty()) && (client.getStatusMessage() != null);

            if (showError || showMsg)
            {
                mainInfoText.append("<p>" + client.getName() + ":<br/>");
                if (showMsg)
                    mainInfoText.append(client.getStatusMessage() + "<br/>");
                if (showError)
                {
                    Throwable errorObj = client.getCurrentError();
                    String errorMsg = errorObj.getMessage().trim();
                    if (!errorMsg.endsWith("."))
                        errorMsg += ". ";
                    if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                        errorMsg += errorObj.getCause().getMessage();
                    mainInfoText.append("<font color='red'>" + errorMsg + "</font>");
                }
                mainInfoText.append("</p>");
            }

            log.debug("[CONSYS CLIENT CONNECTION]", client.isConnected());
        }

        // then display streams status
        mainInfoText.append("<p>");
        for (SOSTClient client: sostClients)
        {
            mainInfoText.append("SOS-T Client");
            mainInfoText.append("<p>");

            Map<String, StreamInfo> dataStreams = client.getDataStreams();
            long now = System.currentTimeMillis();

            for (Entry<String, StreamInfo> stream : dataStreams.entrySet())
            {
                mainInfoText.append("<b>" + stream.getKey() + " : </b>");

                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE)
                    mainInfoText.append("<font color='red'>NO OBS</font>");
                else if (dt > stream.getValue().measPeriodMs)
                    mainInfoText.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                else
                    mainInfoText.append("<font color='green'>OK (" + dt + "ms ago)</font>");

                if (stream.getValue().errorCount > 0)
                {
                    mainInfoText.append("<font color='red'> (");
                    mainInfoText.append(stream.getValue().errorCount);
                    mainInfoText.append(")</font>");
                }

                mainInfoText.append("<br/>");
            }

        }

        for (ConSysApiClientModule client: conSysClients)
        {
            mainInfoText.append("ConSysApi Client");
            mainInfoText.append("<p>");

            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            long now = System.currentTimeMillis();

            for (Entry<String, ConSysApiClientModule.StreamInfo> stream : dataStreams.entrySet())
            {
                mainInfoText.append("<b>" + stream.getKey() + " : </b>");

                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE)
                    mainInfoText.append("<font color='red'>NO OBS</font>");
                else if (dt > stream.getValue().measPeriodMs)
                    mainInfoText.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                else
                    mainInfoText.append("<font color='green'>OK (" + dt + "ms ago)</font>");

                if (stream.getValue().errorCount > 0)
                {
                    mainInfoText.append("<font color='red'> (");
                    mainInfoText.append(stream.getValue().errorCount);
                    mainInfoText.append(")</font>");
                }

                mainInfoText.append("<br/>");
            }
        }
        mainInfoText.append("<p>");

        if (mainInfoText.length() > 5)
            mainInfoText.setLength(mainInfoText.length()-5); // remove last </br>
        mainInfoText.append("</p>");

        // Notify we are running when no data is being pushed
        boolean serveOrStore = shouldServe(PreferenceManager.getDefaultSharedPreferences(MainActivity.this)) || shouldStore(PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
        if(sostClients.isEmpty() && serveOrStore){
            mainInfoText.append("No Sensors Set to Push Remotely");
        }

        if(conSysClients.isEmpty() && serveOrStore){
            mainInfoText.append("No Sensors Set to Push Remotely");
        }

        // show video info
        if (androidSensors != null && boundService.hasVideo())
        {
//             TODO: Fix crash resulting from this (620)
            try {
                VideoEncoderConfig config = androidSensors.getConfiguration().videoConfig;
                VideoPreset preset = config.presets[config.selectedPreset];
                videoInfoText.setLength(0);
                videoInfoText.append("")
                        .append(config.codec).append(", ")
                        .append(preset.width).append("x").append(preset.height).append(", ")
                        .append(config.frameRate).append(" fps, ")
                        .append(preset.selectedBitrate).append(" kbits/s")
                        .append("");
            }catch (Exception e){
                log.error("Exception thrown trying to disaply video", e.getMessage());
            }
        }

//        if(needsRestart){
//
//            mainInfoArea.clearComposingText();
//
//           restartService();
//        }

    }

//    private void restartService(){
//        displayHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                restartSensorHubService();
//            }
//        }, 5000);
//    }
//
//    private void restartSensorHubService() {
//        try {
//            mainInfoText.append("Restarting OSH Service");
//
//            stopListeningForEvents();
//            stopRefreshingStatus();
//
//            if (boundService != null) {
//                boundService.stopSensorHub();
//            }
//
//            Thread.sleep(10000);
//
//            updateConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), runName);
//
//            AndroidSensorsConfig androidSensorConfig = (AndroidSensorsConfig) sensorhubConfig.get("ANDROID_SENSORS");
//            VideoEncoderConfig videoConfig = androidSensorConfig.videoConfig;
//
//            boolean cameraInUse = (androidSensorConfig.activateBackCamera || androidSensorConfig.activateFrontCamera);
//            boolean improperVideoSettings = (videoConfig.selectedPreset < 0 || videoConfig.selectedPreset >= videoConfig.presets.length);
//
//            if (cameraInUse && improperVideoSettings) {
//                showVideoConfigErrorPopup();
//                newStatusMessage("Video Config Error: Check Settings");
//                return;
//            }
//
//            newStatusMessage("Starting SensorHub...");
//
//
//            boundService.startSensorHub(sensorhubConfig, showVideo);
//
//            if (boundService.hasVideo()) {
//                mainInfoArea.setBackgroundColor(0x80FFFFFF);
//            }
//
//            while(boundService.getSensorHub() == null) {
//                newStatusMessage("Waiting for BoundService Hub to start...");
//                Thread.sleep(1000);
//            }
//
//            if (boundService.getSensorHub() == null) {
//                appendStatusMessage("Failed to start SensorHub after restart");
//                return;
//            }
//
//            while(boundService.getSensorHub().getEventBus() == null) {
//                newStatusMessage("Waiting for BoundService Hub EventBus to start...");
//                Thread.sleep(1000);
//            }
//
//            if (boundService.getSensorHub().getEventBus() != null) {
//                EventBus shEvtBus = (EventBus) boundService.getSensorHub().getEventBus();
//                shEvtBus.newSubscription()
//                        .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
//                        .subscribe(mainActivity);
//
//                newStatusMessage("SensorHub restarted successfully");
//            }
//
//        } catch (InterruptedException e) {
//            newStatusMessage("Restart interrupted: " + e.getMessage());
//            Thread.currentThread().interrupt();
//        } catch (Exception e) {
//            newStatusMessage("Restart failed: " + e.getMessage());
//        }
//    }


    protected synchronized void newStatusMessage(String msg)
    {
        mainInfoText.setLength(0);
        appendStatusMessage(msg);
    }


    protected synchronized void appendStatusMessage(String msg)
    {
        mainInfoText.append(msg);

        displayHandler.post(new Runnable()
        {
            public void run()
            {
                mainInfoArea.setText(mainInfoText.toString());
            }
        });
    }


    protected void startListeningForEvents() {
        if (boundService == null || boundService.getSensorHub() == null){

        }

        // TODO: Implement a listener that can sub to the status of the hub
//        boundService.getSensorHub().getModuleRegistry().registerListener(this);

    }


    protected void stopListeningForEvents()
    {
        if (boundService == null || boundService.getSensorHub() == null){

        }

        // TODO: Unsub the listener here
//        boundService.getSensorHub().getModuleRegistry().unregisterListener(this);
    }



    protected void showVideo()
    {
        if (boundService.getVideoTexture() != null)
        {
            TextureView textureView = (TextureView) findViewById(R.id.video);
            if (textureView.getSurfaceTexture() != boundService.getVideoTexture())
                textureView.setSurfaceTexture(boundService.getVideoTexture());
        }
    }


    protected void hideVideo()
    {
    }

    private boolean isPushingSensor(Sensors sensor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

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
            if(prefs.getBoolean("audio_enabled", false)
                    && prefs.getStringSet("audio_options", Collections.emptySet()).contains("PUSH_REMOTE"))
                return true;
        } else if (Sensors.TruPulse.equals(sensor) || Sensors.TruPulseSim.equals(sensor)) {
            return prefs.getBoolean("trupulse_enabled", false)
                    && prefs.getStringSet("trupulse_options", Collections.emptySet()).contains("PUSH_REMOTE");
        } else if(Sensors.BLELocation.equals(sensor)){
            return prefs.getBoolean("ble_enable", false) && prefs.getStringSet("ble_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }
        else if (Sensors.Meshtastic.equals(sensor)) {
            return prefs.getBoolean("meshtastic_enabled", false)
                    && prefs.getStringSet("meshtastic_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }
        else if (Sensors.PolarHRMonitor.equals(sensor)) {
            return prefs.getBoolean("polar_enabled", false)
                    && prefs.getStringSet("polar_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }
        else if (Sensors.KestrelBallistics.equals(sensor)) {
            return prefs.getBoolean("kestrel_enabled", false)
                    && prefs.getStringSet("kestrel_options", Collections.emptySet()).contains("PUSH_REMOTE");
        }

        return false;
    }


    private void setupBroadcastReceivers() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
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

                   /* ProxySensorConfig proxySensorConfig = (ProxySensorConfig) createSensorConfig(Sensors.ProxySensor);
                    proxySensorConfig.androidContext = getApplicationContext();
                    proxySensorConfig.sosEndpointUrl = sosEndpointUrl;
                    proxySensorConfig.name = name;
                    proxySensorConfig.id = sensorId;
                    proxySensorConfig.sensorUID = sensorId;
                    proxySensorConfig.observedProperties.addAll(properties);
                    proxySensorConfig.sosUseWebsockets = true;
                    proxySensorConfig.autoStart = true;
                    proxySensorConfigs.add(proxySensorConfig);*/

                    // register and "start" new sensor, data stream doesn't begin until someone requests data;
                    try {
                        boundService.stopSensorHub();
                        Thread.sleep(2000);
                        Log.d("OSHApp", "Starting SensorHub Again");
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        updateConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), runName);
                        sostClients.clear();
                        boundService.startSensorHub(sensorhubConfig, showVideo);
                        if (boundService.hasVideo())
                            mainInfoArea.setBackgroundColor(0x80FFFFFF);

                        EventBus shEventBus = (EventBus) boundService.getSensorHub().getEventBus();
//                        shEventBus.newSubscription()
//                                .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
//                                .subscribe();
                    } catch (InterruptedException e) {
                        Log.e("OSHApp", "Error Loading Proxy Sensor", e);
                    }

                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BROADCAST_RECEIVER);

        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        TextureView textureView = (TextureView) findViewById(R.id.video);
        textureView.setSurfaceTextureListener(this);

        if (oshStarted)
        {
            startListeningForEvents();
            startRefreshingStatus();

            if (boundService.hasVideo())
                mainInfoArea.setBackgroundColor(0x80FFFFFF);
        }
    }


    @Override
    protected void onPause()
    {
        stopListeningForEvents();
        stopRefreshingStatus();
        hideVideo();
        super.onPause();
    }


    @Override
    protected void onStop()
    {
        stopListeningForEvents();
        stopRefreshingStatus();
        super.onStop();
    }


    @Override
    protected void onDestroy()
    {
//        stopService(new Intent(this, SensorHubService.class));
//        super.onDestroy();

        // this should stop it from stopping sensorhub and allow it to stay connected when the app closes/ phone shuts off
        if (boundService != null) {
            unbindService(sConn);
            boundService = null;
        }
        super.onDestroy();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1)
    {
        showVideo();
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1)
    {
    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
    {
        return false;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
    {
    }

    private boolean shouldServe(SharedPreferences prefs){
        Map<String, ?> prefMap = prefs.getAll();
        for(Map.Entry<String,?> pref : prefMap.entrySet()){
            if(pref.getValue() instanceof HashSet) {
                if(((HashSet) pref.getValue()).contains("FETCH_LOCAL")) {
                    Log.d(TAG, "shouldServe: TRUE");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldStore(SharedPreferences prefs){
        Map<String, ?> prefMap = prefs.getAll();
        for(Map.Entry<String,?> pref : prefMap.entrySet()){
            if(pref.getValue() instanceof HashSet) {
                if(((HashSet) pref.getValue()).contains("STORE_LOCAL")) {
                    Log.d(TAG, "shouldStore: TRUE");
                    return true;}
            }
        }
        return false;
    }

    private void checkForPermissions(){
        List<String> permissions = new ArrayList<>();

        // Check for necessary permissions
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (checkSelfPermission(Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND);
        }
        if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        if (checkSelfPermission(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.WAKE_LOCK);
        }
        if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.INTERNET);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        // Does app actually need storage permissions now?
        String[] permARR = new String[permissions.size()];
        permARR = permissions.toArray(permARR);
        if(permARR.length >0) {
            requestPermissions(permARR, 100);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println("MainActivity Subscribed...");
        subscription.request(10);
    }

    @Override
    public void onNext(Event e) {
        System.out.println("Event of : " + e);

        System.out.println(e.getSource());
        if (e instanceof ModuleEvent)
        {
            // start refreshing status on first module loaded
            if (!oshStarted && ((ModuleEvent) e).getType() == ModuleEvent.Type.LOADED)
            {
                oshStarted = true;
                startRefreshingStatus();
                return;
            }

            // detect when Android sensor driver is started
            else if (e.getSource() instanceof AndroidSensorsDriver)
            {
                this.androidSensors = (AndroidSensorsDriver)e.getSource();
            }

            // detect when SOS-T modules are connected
            else if (e.getSource() instanceof SOSTClient && ((ModuleEvent)e).getType() == ModuleEvent.Type.STATE_CHANGED)
            {
                switch (((ModuleEvent)e).getNewState())
                {
                    case INITIALIZING:
                        sostClients.add((SOSTClient)e.getSource());
                        break;
                }
            }
            else if (e.getSource() instanceof ConSysApiClientModule && ((ModuleEvent)e).getType() == ModuleEvent.Type.STATE_CHANGED)
            {
                switch (((ModuleEvent)e).getNewState())
                {
                    case INITIALIZING:
                        conSysClients.add((ConSysApiClientModule)e.getSource());
                        break;
                }
            }
        }

        subscription.request(10);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}