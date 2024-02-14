package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.Outputs;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, ServiceConnection {
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS = 1;
    Date lastConfirmationDate = new Date(0);
    UIManager uiManager;
    HealthDataService healthDataService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiManager = new UIManager(this);
        uiManager.init();

        Intent serviceIntent = new Intent(this, HealthDataService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);

        requestOutputs();
        requestPermissions();

        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_BODY_SENSORS) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, 0);
            }
            healthDataService.startMonitoring();
        }

        uiManager.refreshUI();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.CONFIRMATION_PATH)) {
            // Hide the warning
            uiManager.setWarning(false);

            lastConfirmationDate = new Date();
        } else if (messageEvent.getPath().equals(Constants.OUTPUTS_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);

            Outputs outputs = Outputs.fromJSon(message);
            PreferencesManager.setEnableHeartRate(this, outputs.getEnableHeartRate());
            PreferencesManager.setEnableCalories(this, outputs.getEnableCalories());
            PreferencesManager.setEnableDistance(this, outputs.getEnableDistance());
            PreferencesManager.setEnableSteps(this, outputs.getEnableSteps());
            PreferencesManager.setEnableFloors(this, outputs.getEnableFloors());
            PreferencesManager.setEnableElevationGain(this, outputs.getEnableElevationGain());

            uiManager.refreshUI();

            healthDataService.startMonitoring();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        healthDataService = ((HealthDataService.LocalBinder) service).getService();
        healthDataService.startMonitoring();
        healthDataService.onDataPointReceived(onDataPointReceived);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        healthDataService.onDataPointReceived(null);
        healthDataService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    private final Consumer<HealthDataEventArgs> onDataPointReceived = eventArgs -> {
        if (eventArgs.getHeartRate() != null) {
            uiManager.setHeartRate(eventArgs.getHeartRate());
        }
        if (eventArgs.getCaloriesDaily() != null) {
            uiManager.setCalories(eventArgs.getCaloriesDaily());
        }
        if (eventArgs.getDistanceDaily() != null) {
            uiManager.setDistance(eventArgs.getDistanceDaily());
        }
        if (eventArgs.getStepsDaily() != null) {
            uiManager.setSteps(eventArgs.getStepsDaily());
        }
        if (eventArgs.getFloorsDaily() != null) {
            uiManager.setFloors(eventArgs.getFloorsDaily());
        }
        if (eventArgs.getElevationGainDaily() != null) {
            uiManager.setElevation(eventArgs.getElevationGainDaily());
        }
        if (Duration.between(lastConfirmationDate.toInstant(), Instant.now()).getSeconds() > 10) {
            uiManager.setWarning(true);
        }
    };

    private void requestOutputs() {
        Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(this).sendMessage(node.getId(), Constants.OUTPUTS_PATH, "request".getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    private void requestPermissions() {
        List<String> permissions = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BODY_SENSORS);
        }

        if (permissions.isEmpty()) {
            // Can't request both BODY_SENSORS and BODY_SENSORS_BACKGROUND at the same time
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, 0);
            }
        } else {
            requestPermissions(permissions.toArray(new String[0]), PERMISSIONS_REQUEST_BODY_SENSORS);
        }
    }
}