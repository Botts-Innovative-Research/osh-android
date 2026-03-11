package org.sensorhub.android;

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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.vast.xml.XMLImplFinder;

import javax.xml.parsers.DocumentBuilderFactory;

public class SensorHubService extends Service
{
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

    public interface OnHubReadyListener {
        void onHubReady(SensorHubAndroid hub);
    }
    private OnHubReadyListener hubReadyListener;

    public void setOnHubReadyListener(OnHubReadyListener listener) {
        this.hubReadyListener = listener;
    }

    public class LocalBinder extends Binder {
        SensorHubService getService() {
            return SensorHubService.this;
        }
    }


    @Override
    public void onCreate() {

        try
        {
            SensorHubService.context = getApplicationContext();

            SensorHubService.videoTex = new SurfaceTexture(1);
            SensorHubService.videoTex.detachFromGLContext();

            XMLImplFinder.setStaxInputFactory(com.ctc.wstx.stax.WstxInputFactory.class.newInstance());
            XMLImplFinder.setStaxOutputFactory(com.ctc.wstx.stax.WstxOutputFactory.class.newInstance());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            XMLImplFinder.setDOMImplementation(dbf.newDocumentBuilder().getDOMImplementation());

            msgThread = new HandlerThread("SensorHubService", Process.THREAD_PRIORITY_BACKGROUND);
            msgThread.start();
            msgHandler = new Handler(msgThread.getLooper());

            startForegroundService();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void startForegroundService() {
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
                    this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            );
        }

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

        startForeground(NOTIFICATION_ID, notification);
    }

    public synchronized void startSensorHub(final IModuleConfigRepository config, final boolean hasVideo)
    {
        if (sensorhub != null)
            return;

        this.hasVideo = hasVideo;

        acquireWakeLocks();

        msgHandler.post(new Runnable() {
            public void run() {
                sensorhub = new SensorHubAndroid(new SensorHubConfig(), config);
                try {
                    sensorhub.initComponents();
                    if (hubReadyListener != null) {
                        hubReadyListener.onHubReady(sensorhub);
                    }
                    sensorhub.start();
                } catch (SensorHubException e) {
                    e.printStackTrace();
                    releaseWakeLocks();
                }
            }
        });
    }

    private void acquireWakeLocks() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SensorHub::DataCollection"
            );
            wakeLock.acquire();
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "SensorHub::WiFiLock"
            );
            wifiLock.acquire();
        }
    }

    private void releaseWakeLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

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