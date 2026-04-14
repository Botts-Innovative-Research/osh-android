package org.sensorhub.android;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.xml.XMLImplFinder;

import javax.xml.parsers.DocumentBuilderFactory;

public class SensorHubService extends Service
{
    private static final Logger log = LoggerFactory.getLogger(SensorHubService.class);
    final IBinder binder = new LocalBinder();
    private HandlerThread msgThread;
    private Handler msgHandler;
    SensorHubAndroid sensorhub;
    boolean hasVideo;
    static Context context;
    static SurfaceTexture videoTex;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private static final String CHANNEL_ID = "sensorhub_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private LocationManager locationManager;
    private LocationListener locationListener;

    public class LocalBinder extends Binder {
        SensorHubService getService() {
            return SensorHubService.this;
        }
    }


    @Override
    public void onCreate() {

        try
        {
            // keep handle to Android context so it can be retrieved by OSH components
            SensorHubService.context = getApplicationContext();

            // create video surface texture here so it's not destroyed when pausing the app
            SensorHubService.videoTex = new SurfaceTexture(1);
            SensorHubService.videoTex.detachFromGLContext();

            // load external dex file containing stax API
            //Dexter.loadFromAssets(this.getApplicationContext(), "stax-api-1.0-2.dex");

            // set default StAX implementation
            XMLImplFinder.setStaxInputFactory(WstxInputFactory.class.newInstance());
            XMLImplFinder.setStaxOutputFactory(WstxOutputFactory.class.newInstance());

            // set default DOM implementation
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            XMLImplFinder.setDOMImplementation(dbf.newDocumentBuilder().getDOMImplementation());

            // start handler thread
            msgThread = new HandlerThread("SensorHubService", Process.THREAD_PRIORITY_BACKGROUND);
            msgThread.start();
            msgHandler = new Handler(msgThread.getLooper());

            // Start as foreground service with notification
            startForegroundService();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

//    private void startLocationManager() {
//        LocationRequest request = new LocationRequest.Builder(
//                Priority.PRIORITY_HIGH_ACCURACY,
//                5000 // update interval (ms)
//        ).build();
//
//    }
    private void startForegroundService() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SensorHub Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps sensor data collection running");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent();
        notificationIntent.setClassName(
                getApplicationContext(),
                "org.sensorhub.android.MainActivity"
        );

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );
        }

        // Create notification
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("SensorHub Running")
                    .setContentText("Collecting and transmitting sensor data")
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("SensorHub Running")
                    .setContentText("Collecting and transmitting sensor data")
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        }

        // Start foreground
        startForeground(NOTIFICATION_ID, notification);
    }

    public synchronized void startSensorHub(final IModuleConfigRepository config, final boolean hasVideo)
    {
        if (sensorhub != null)
            return;

        this.hasVideo = hasVideo;

        // Acquire wake locks BEFORE starting the hub
        acquireWakeLocks();

        msgHandler.post(new Runnable() {
            public void run() {
                // create and start sensorhub instance
                sensorhub = new SensorHubAndroid(new SensorHubConfig(), config);
                try {
                    sensorhub.start();
                } catch (SensorHubException e) {
                    log.error("Error starting SensorHub: "+ e.getMessage());
                    // Release locks if startup fails
                    releaseWakeLocks();
                }
            }
        });
    }

    private void acquireWakeLocks() {
        // Acquire partial wake lock to keep CPU active
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SensorHub::DataCollection"
            );
            wakeLock.acquire();
        }

        // Acquire WiFi lock to keep WiFi active
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                    "SensorHub::WiFiLock"
            );
            wifiLock.acquire();
        }
    }

    private void releaseWakeLocks() {
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // Release WiFi lock
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
        }
    }

    public synchronized void stopSensorHub()
    {
        if (sensorhub == null)
            return;

        this.hasVideo = false;

        final SensorHubAndroid hubToStop = sensorhub;
        sensorhub = null;

        final java.util.concurrent.CountDownLatch stopLatch = new java.util.concurrent.CountDownLatch(1);

        msgHandler.post(new Runnable() {
            public void run() {
                try {
                    hubToStop.stop();
                } finally {
                    stopLatch.countDown();
                }
            }
        });

        try {
            stopLatch.await(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        releaseWakeLocks();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }


    @Override
    public void onDestroy()
    {
        stopSensorHub();
        msgThread.quitSafely();
        if (SensorHubService.videoTex != null) {
            SensorHubService.videoTex.release();
            SensorHubService.videoTex = null;
        }
        SensorHubService.context = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        log.info("Task removed, scheduling restart");
        Intent restartIntent = new Intent(getApplicationContext(), SensorHubService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000, pendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }


    public SensorHub getSensorHub()
    {
        return sensorhub;
    }


    public boolean hasVideo()
    {
        return hasVideo;
    }


    public static SurfaceTexture getVideoTexture()
    {
        return videoTex;
    }


    public static Context getContext()
    {
        return context;
    }
}