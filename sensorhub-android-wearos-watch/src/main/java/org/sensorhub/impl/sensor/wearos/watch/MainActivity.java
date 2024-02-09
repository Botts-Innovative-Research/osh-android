package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.PassiveListenerConfig;
import androidx.health.services.client.data.PassiveMonitoringCapabilities;
import androidx.health.services.client.data.UserActivityInfo;
import androidx.health.services.client.data.UserActivityState;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, PassiveListenerCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONFIRMATION_PATH = "/OSH/Confirmation";
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS = 1;
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND = 2;
    PassiveMonitoringClient passiveMonitoringClient;
    Date lastConfirmationDate = new Date(0);
    DataType<?, ?> elevationGainDailyType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        passiveMonitoringClient = HealthServices.getClient(this).getPassiveMonitoringClient();

        stopMonitoring();
        requestPermissions();
        startMonitoring();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_BODY_SENSORS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND);
            }
            startMonitoring();
        }
    }

    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {
        PassiveListenerCallback.super.onNewDataPointsReceived(dataPoints);
        Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());

        dataPoints.getIntervalDataPoints().forEach(dataPoint -> {
            switch (dataPoint.getDataType().getName()) {
                case "Daily Elevation Gain":
                    ((TextView) findViewById(R.id.elevationValue)).setText(String.format(new Locale("en"), "%.2f", (double) dataPoint.getValue()));
                    break;
                case "Daily Floors":
                    ((TextView) findViewById(R.id.floorsValue)).setText(String.format(new Locale("en"), "%.2f", (double) dataPoint.getValue()));
                    break;
                case "Daily Steps":
                    ((TextView) findViewById(R.id.stepsValue)).setText(String.valueOf((long) dataPoint.getValue()));
                    break;
                case "Daily Distance":
                    ((TextView) findViewById(R.id.distanceValue)).setText(String.format(new Locale("en"), "%.2f", (double) dataPoint.getValue()));
                    break;
                case "Daily Calories":
                    ((TextView) findViewById(R.id.caloriesValue)).setText(String.format(new Locale("en"), "%.2f", (double) dataPoint.getValue()));
                    break;
                default:
                    break;
            }
        });

        dataPoints.getSampleDataPoints().forEach(dataPoint -> {
            if (dataPoint.getDataType().getName().equals("HeartRate")) {
                Instant dataPointInstant = dataPoint.getTimeInstant(bootInstant);
                Date date = Date.from(dataPointInstant);
                double value = (double) dataPoint.getValue();
                int heartRate = (int) value;
                ((TextView) findViewById(R.id.heartRateValue)).setText(String.valueOf(heartRate));

                // Send heart rate to the phone
                Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
                nodesTask.addOnSuccessListener(nodes -> {
                    for (Node node : nodes) {
                        String message = date.getTime() + "," + heartRate;
                        Wearable.getMessageClient(this).sendMessage(node.getId(), HEART_RATE_PATH, message.getBytes(StandardCharsets.UTF_8));
                    }
                });
            }
        });

        // If the last confirmation was more than 10 seconds ago, show the warning
        if (Duration.between(lastConfirmationDate.toInstant(), Instant.now()).getSeconds() > 10) {
            ((TextView) findViewById(R.id.warning)).setText(getResources().getString(R.string.warning));
        }
    }

    @Override
    public void onUserActivityInfoReceived(@NonNull UserActivityInfo info) {
        PassiveListenerCallback.super.onUserActivityInfoReceived(info);

        if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_PASSIVE) {
            ((TextView) findViewById(R.id.activityStateValue)).setText(getResources().getString(R.string.activityStatePassive));
        } else if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_ASLEEP) {
            ((TextView) findViewById(R.id.activityStateValue)).setText(getResources().getString(R.string.activityStateAsleep));
        } else if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_EXERCISE) {
            ((TextView) findViewById(R.id.activityStateValue)).setText(getResources().getString(R.string.activityStateExercise));
        } else {
            ((TextView) findViewById(R.id.activityStateValue)).setText(getResources().getString(R.string.activityStateUnknown));
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(CONFIRMATION_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);
            Log.d(TAG, "Message from phone: " + messageEvent.getPath() + " " + message);

            // Hide the warning
            ((TextView) findViewById(R.id.warning)).setText("");

            lastConfirmationDate = new Date();
        }
    }

    private void startMonitoring() {
        MainActivity mainActivity = this;

        ListenableFuture<PassiveMonitoringCapabilities> capabilitiesFuture = passiveMonitoringClient.getCapabilitiesAsync();
        Futures.addCallback(capabilitiesFuture, new FutureCallback<PassiveMonitoringCapabilities>() {
            @Override
            public void onSuccess(@Nullable PassiveMonitoringCapabilities result) {
                if (result == null) {
                    return;
                }

                result.getSupportedDataTypesPassiveMonitoring().forEach(dataType -> {
                    if (dataType.getName().equals("Daily Elevation Gain")) {
                        elevationGainDailyType = dataType;
                    }
                });

                java.util.Set<DataType<?, ?>> dataTypes = new java.util.HashSet<>();
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
                    dataTypes.add(DataType.HEART_RATE_BPM);
                }
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    dataTypes.add(DataType.CALORIES);
                    dataTypes.add(DataType.CALORIES_DAILY);
                    dataTypes.add(DataType.DISTANCE);
                    dataTypes.add(DataType.DISTANCE_DAILY);
                    dataTypes.add(DataType.STEPS);
                    dataTypes.add(DataType.STEPS_DAILY);
                    dataTypes.add(DataType.FLOORS);
                    dataTypes.add(DataType.FLOORS_DAILY);
                    dataTypes.add(DataType.ELEVATION_GAIN);
                    if (elevationGainDailyType != null) {
                        dataTypes.add(elevationGainDailyType);
                    }
                }

                PassiveListenerConfig.Builder builder = new PassiveListenerConfig.Builder()
                        .setDataTypes(dataTypes);
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    builder.setShouldUserActivityInfoBeRequested(true);
                }
                PassiveListenerConfig passiveListenerConfig = builder.build();

                passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, mainActivity);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "Failed to get passive monitoring capabilities", t);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopMonitoring() {
        ListenableFuture<Void> clearFuture = passiveMonitoringClient.clearPassiveListenerCallbackAsync();

        Futures.addCallback(clearFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // Do nothing
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                // Do nothing
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }
        // Can't request both BODY_SENSORS and BODY_SENSORS_BACKGROUND at the same time
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSORS);
        }
    }
}