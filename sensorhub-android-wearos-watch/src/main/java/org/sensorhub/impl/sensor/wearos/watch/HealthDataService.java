package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.PassiveListenerConfig;
import androidx.health.services.client.data.PassiveMonitoringCapabilities;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.data.WearOSData;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class HealthDataService extends Service implements PassiveListenerCallback {
    private static final String TAG = HealthDataService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private final ArrayList<Consumer<HealthDataEventArgs>> eventHandlers = new ArrayList<>();
    PassiveMonitoringClient passiveMonitoringClient;
    Set<DataType<?, ?>> supportedDataTypes;
    DataType<?, ?> elevationGainDailyType;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        passiveMonitoringClient = HealthServices.getClient(this).getPassiveMonitoringClient();
        startMonitoring();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String channelID = "osh_channel_id";
        NotificationChannel channel = new NotificationChannel(channelID,
                "Open Sensor Hub",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Open Sensor Hub")
                .setContentText("Open Sensor Hub background health monitoring").build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {
        PassiveListenerCallback.super.onNewDataPointsReceived(dataPoints);

        WearOSData data = new WearOSData();
        HealthDataEventArgs eventArgs = new HealthDataEventArgs();
        Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());

        dataPoints.getSampleDataPoints().forEach(dataPoint -> {
            if (dataPoint.getDataType().getName().equals("HeartRate")) {
                data.addHeartRate(dataPoint.getTimeInstant(bootInstant), (double) dataPoint.getValue());
                eventArgs.setHeartRate((double) dataPoint.getValue());
            }
        });

        dataPoints.getIntervalDataPoints().forEach(dataPoint -> {
            switch (dataPoint.getDataType().getName()) {
                case "Elevation Gain":
                    data.addElevationGain(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Elevation Gain":
                    data.addElevationGainDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    eventArgs.setElevationGainDaily((double) dataPoint.getValue());
                    break;
                case "Floors":
                    data.addFloors(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Floors":
                    data.addFloorsDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    eventArgs.setFloorsDaily((double) dataPoint.getValue());
                    break;
                case "Steps":
                    data.addSteps(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (long) dataPoint.getValue());
                    break;
                case "Daily Steps":
                    data.addStepsDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (long) dataPoint.getValue());
                    eventArgs.setStepsDaily((long) dataPoint.getValue());
                    break;
                case "Distance":
                    data.addDistance(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Distance":
                    data.addDistanceDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    eventArgs.setDistanceDaily((double) dataPoint.getValue());
                    break;
                case "Calories":
                    data.addCalories(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Calories":
                    data.addCaloriesDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    eventArgs.setCaloriesDaily((double) dataPoint.getValue());
                    break;
                default:
                    break;
            }
        });
        notifyEventHandlers(eventArgs);

        Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(this).sendMessage(node.getId(), Constants.DATA_PATH, data.toJSon().getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    public void onDataPointReceived(Consumer<HealthDataEventArgs> handler) {
        eventHandlers.add(handler);
    }

    private void notifyEventHandlers(HealthDataEventArgs eventArgs) {
        for (Consumer<HealthDataEventArgs> handler : eventHandlers) {
            handler.accept(eventArgs);
        }
    }

    public void startMonitoring() {
        if (supportedDataTypes == null) {
            ListenableFuture<PassiveMonitoringCapabilities> capabilitiesFuture = passiveMonitoringClient.getCapabilitiesAsync();
            Futures.addCallback(capabilitiesFuture, new FutureCallback<PassiveMonitoringCapabilities>() {
                @Override
                public void onSuccess(@Nullable PassiveMonitoringCapabilities result) {
                    if (result == null) {
                        return;
                    }
                    supportedDataTypes = result.getSupportedDataTypesPassiveMonitoring();
                    for (DataType<?, ?> supportedDataType : supportedDataTypes) {
                        if (supportedDataType.getName().equals("Elevation Gain Daily")) {
                            elevationGainDailyType = supportedDataType;
                        }
                    }
                    startMonitoring();
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Log.e(TAG, "Failed to get passive monitoring capabilities", t);
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            PassiveListenerConfig passiveListenerConfig = new PassiveListenerConfig.Builder()
                    .setDataTypes(getMonitoredDataTypes())
                    .build();
            passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, this);
        }
    }

    private Set<DataType<?, ?>> getMonitoredDataTypes() {
        Set<DataType<?, ?>> dataTypes = new HashSet<>();

        addHeartRateDataType(dataTypes);
        addCaloriesDataType(dataTypes);
        addCaloriesDailyDataType(dataTypes);
        addDistanceDataType(dataTypes);
        addDistanceDailyDataType(dataTypes);
        addStepsDataType(dataTypes);
        addStepsDailyDataType(dataTypes);
        addFloorsDataType(dataTypes);
        addFloorsDailyDataType(dataTypes);
        addElevationGainDataType(dataTypes);
        addElevationGainDailyDataType(dataTypes);

        return dataTypes;
    }

    private void addHeartRateDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableHeartRate(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.HEART_RATE_BPM)) {
            dataTypes.add(DataType.HEART_RATE_BPM);
        }
    }

    private void addCaloriesDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableCalories(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.CALORIES)) {
            dataTypes.add(DataType.CALORIES);
        }
    }

    private void addCaloriesDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableCalories(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.CALORIES_DAILY)) {
            dataTypes.add(DataType.CALORIES_DAILY);
        }
    }

    private void addDistanceDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableDistance(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.DISTANCE)) {
            dataTypes.add(DataType.DISTANCE);
        }
    }

    private void addDistanceDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableDistance(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.DISTANCE_DAILY)) {
            dataTypes.add(DataType.DISTANCE_DAILY);
        }
    }

    private void addStepsDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableSteps(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.STEPS)) {
            dataTypes.add(DataType.STEPS);
        }
    }

    private void addStepsDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableSteps(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.STEPS_DAILY)) {
            dataTypes.add(DataType.STEPS_DAILY);
        }
    }

    private void addFloorsDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableFloors(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.FLOORS)) {
            dataTypes.add(DataType.FLOORS);
        }
    }

    private void addFloorsDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableFloors(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.FLOORS_DAILY)) {
            dataTypes.add(DataType.FLOORS_DAILY);
        }
    }

    private void addElevationGainDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableElevationGain(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.ELEVATION_GAIN)) {
            dataTypes.add(DataType.ELEVATION_GAIN);
        }
    }

    private void addElevationGainDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableElevationGain(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && elevationGainDailyType != null) {
            dataTypes.add(elevationGainDailyType);
        }
    }

    public class LocalBinder extends Binder {
        HealthDataService getService() {
            return HealthDataService.this;
        }
    }
}
