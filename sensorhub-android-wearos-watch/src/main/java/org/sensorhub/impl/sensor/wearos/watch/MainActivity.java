package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.PassiveListenerConfig;

import java.util.Collections;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request android.permission.BODY_SENSORS
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        Log.d(TAG, " permissionCheck: " + permissionCheck);

        if (permissionCheck == -1)
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 1);

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND);
        Log.d(TAG, " permissionCheck: " + permissionCheck);

        if (permissionCheck == -1)
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, 1);


        HealthServicesClient healthServicesClient = HealthServices.getClient(this);
        PassiveMonitoringClient passiveMonitoringClient = healthServicesClient.getPassiveMonitoringClient();

        PassiveListenerConfig passiveListenerConfig = new PassiveListenerConfig.Builder()
                .setDataTypes(Collections.singleton(DataType.HEART_RATE_BPM)).build();

        HeartRateListener heartRateListener = new HeartRateListener(this);
        passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, heartRateListener);
    }
}