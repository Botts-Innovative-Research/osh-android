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
import androidx.health.services.client.data.DeltaDataType;
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

/**
 * Service that monitors health data and sends it to the phone.
 * The service is started when the watch is booted and runs in the background.
 */
public class HealthDataService extends Service implements PassiveListenerCallback {
    private static final String TAG = HealthDataService.class.getSimpleName();
    private static final DataType<?, ?> elevationGainDailyType = new DeltaDataType<>("Daily Elevation Gain", DataType.TimeType.INTERVAL, double.class);
    private final IBinder binder = new LocalBinder();
    private final ArrayList<Consumer<HealthDataEventArgs>> eventHandlers = new ArrayList<>();
    private Set<DataType<?, ?>> supportedDataTypes;

    /**
     * Called when the service is started.
     *
     * @param intent  The intent that was used to start the service.
     * @param flags   Flags indicating how the service was started.
     * @param startId The start ID of the service.
     * @return The service's start mode.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMonitoring();
        return Service.START_STICKY;
    }

    /**
     * Called when the service is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Starting a foreground service requires a notification and for startForeground to be called.
        // Without this, the service will fail to start properly
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

    /**
     * Called when the service is bound to an activity.
     *
     * @param intent The intent that was used to bind the service.
     * @return The binder for the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Called when new data points are received from the passive monitoring client.
     *
     * @param dataPoints The data points received.
     */
    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {
        PassiveListenerCallback.super.onNewDataPointsReceived(dataPoints);

        // WearOSData is a data class that holds the data points for each data type.
        // It will be serialized to JSON and sent to the phone.
        WearOSData data = new WearOSData();
        // HealthDataEventArgs is used to notify the event handlers of the new data.
        // Data is set, not added, meaning that the event handlers will only receive the latest data point for each data type.
        HealthDataEventArgs eventArgs = new HealthDataEventArgs();
        Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());

        // Heart rate is a sample data type, so it needs to be handled separately
        dataPoints.getSampleDataPoints().forEach(dataPoint -> {
            if (dataPoint.getDataType().getName().equals("HeartRate")) {
                data.addHeartRate(dataPoint.getTimeInstant(bootInstant), (double) dataPoint.getValue());
                eventArgs.setHeartRate((double) dataPoint.getValue());
            }
        });

        // The rest of the data types are interval data types
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

        // Notify the event handlers of the new data
        notifyEventHandlers(eventArgs);

        // Send the data to the phone
        Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(this).sendMessage(node.getId(), Constants.DATA_PATH, data.toJSon().getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    /**
     * Adds a new event handler to be notified when new data points are received.
     * Subscribers will be notified of the latest data point for each data type.
     * Note that data may be null if the data type is not supported,
     * if the user has not granted the necessary permissions,
     * if the user has disabled the data type by turning off the corresponding output on the phone,
     * or if data was not received during the last monitoring interval.
     *
     * @param handler The event handler to be notified.
     */
    public void onDataPointReceived(Consumer<HealthDataEventArgs> handler) {
        eventHandlers.add(handler);
    }

    /**
     * Notifies all event handlers of the new data.
     *
     * @param eventArgs The new data.
     */
    private void notifyEventHandlers(HealthDataEventArgs eventArgs) {
        for (Consumer<HealthDataEventArgs> handler : eventHandlers) {
            handler.accept(eventArgs);
        }
    }

    /**
     * Starts monitoring health data.
     * Calling this method after the service has been started will update the data types being monitored, if necessary.
     */
    public void startMonitoring() {
        PassiveMonitoringClient passiveMonitoringClient = HealthServices.getClient(this).getPassiveMonitoringClient();
        if (supportedDataTypes == null) {
            ListenableFuture<PassiveMonitoringCapabilities> capabilitiesFuture = passiveMonitoringClient.getCapabilitiesAsync();
            Futures.addCallback(capabilitiesFuture, new FutureCallback<PassiveMonitoringCapabilities>() {
                @Override
                public void onSuccess(@Nullable PassiveMonitoringCapabilities result) {
                    if (result == null) {
                        return;
                    }
                    supportedDataTypes = result.getSupportedDataTypesPassiveMonitoring();
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

    /**
     * Gets the data types to be monitored.
     * The data types are determined by the user's settings and the permissions granted by the user.
     *
     * @return The data types to be monitored.
     */
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

    /**
     * Adds the heart rate data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addHeartRateDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableHeartRate(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.HEART_RATE_BPM)) {
            dataTypes.add(DataType.HEART_RATE_BPM);
        }
    }

    /**
     * Adds the calories data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addCaloriesDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableCalories(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.CALORIES)) {
            dataTypes.add(DataType.CALORIES);
        }
    }

    /**
     * Adds the daily calories data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addCaloriesDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableCalories(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.CALORIES_DAILY)) {
            dataTypes.add(DataType.CALORIES_DAILY);
        }
    }

    /**
     * Adds the distance data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addDistanceDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableDistance(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.DISTANCE)) {
            dataTypes.add(DataType.DISTANCE);
        }
    }

    /**
     * Adds the daily distance data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addDistanceDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableDistance(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.DISTANCE_DAILY)) {
            dataTypes.add(DataType.DISTANCE_DAILY);
        }
    }

    /**
     * Adds the steps data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addStepsDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableSteps(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.STEPS)) {
            dataTypes.add(DataType.STEPS);
        }
    }

    /**
     * Adds the daily steps data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addStepsDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableSteps(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.STEPS_DAILY)) {
            dataTypes.add(DataType.STEPS_DAILY);
        }
    }

    /**
     * Adds the floors data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addFloorsDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableFloors(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.FLOORS)) {
            dataTypes.add(DataType.FLOORS);
        }
    }

    /**
     * Adds the daily floors data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addFloorsDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableFloors(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.FLOORS_DAILY)) {
            dataTypes.add(DataType.FLOORS_DAILY);
        }
    }

    /**
     * Adds the elevation gain data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addElevationGainDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableElevationGain(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(DataType.ELEVATION_GAIN)) {
            dataTypes.add(DataType.ELEVATION_GAIN);
        }
    }

    /**
     * Adds the daily elevation gain data type to the set of data types to be monitored.
     * Will only be added if enabled, permissions are granted, and the data type is supported.
     *
     * @param dataTypes The set of data types to be monitored.
     */
    private void addElevationGainDailyDataType(Set<DataType<?, ?>> dataTypes) {
        if (PreferencesManager.getEnableElevationGain(this)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                && supportedDataTypes.contains(elevationGainDailyType)) {
            dataTypes.add(elevationGainDailyType);
        }
    }

    /**
     * Local binder for the service. Used to get the service instance from the activity.
     */
    public class LocalBinder extends Binder {
        HealthDataService getService() {
            return HealthDataService.this;
        }
    }
}
