package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.annotation.SuppressLint;
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

import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.Outputs;
import org.sensorhub.impl.sensor.wearos.lib.data.WearOSData;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, PassiveListenerCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS = 1;
    private static final String OUTPUTS_PREFS = "outputs";
    PassiveMonitoringClient passiveMonitoringClient;
    Date lastConfirmationDate = new Date(0);
    DataType<?, ?> elevationGainDailyType;
    Outputs outputs;
    boolean isStarting = false;

    TextView warningTextView;
    TextView heartRateTextView;
    TextView activityStateTextView;
    TextView elevationTextView;
    TextView caloriesTextView;
    TextView floorsTextView;
    TextView stepsTextView;
    TextView distanceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        outputs = new Outputs(getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableHeartRate", true),
                getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableElevationGain", true),
                getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableCalories", true),
                getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableFloors", true),
                getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableSteps", true),
                getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean("enableDistance", true));

        warningTextView = findViewById(R.id.warning);
        activityStateTextView = findViewById(R.id.activityState);
        heartRateTextView = findViewById(R.id.heartRate);
        elevationTextView = findViewById(R.id.elevation);
        caloriesTextView = findViewById(R.id.calories);
        floorsTextView = findViewById(R.id.floors);
        stepsTextView = findViewById(R.id.steps);
        distanceTextView = findViewById(R.id.distance);

        passiveMonitoringClient = HealthServices.getClient(this).getPassiveMonitoringClient();

        // Clear the passive listener callback in case one was set in a previous session
        passiveMonitoringClient.clearPassiveListenerCallbackAsync();
        requestOutputs();
        requestPermissions();
        startMonitoring();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_BODY_SENSORS) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, 0);
            }
            startMonitoring();
        }
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {
        WearOSData data = new WearOSData();
        PassiveListenerCallback.super.onNewDataPointsReceived(dataPoints);
        Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());

        dataPoints.getIntervalDataPoints().forEach(dataPoint -> {
            switch (dataPoint.getDataType().getName()) {
                case "Elevation Gain":
                    data.addElevationGain(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Elevation Gain":
                    data.addElevationGainDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    elevationTextView.setText(getResources().getString(R.string.elevation, dataPoint.getValue()));
                    break;
                case "Floors":
                    data.addFloors(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Floors":
                    data.addFloorsDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    floorsTextView.setText(getResources().getString(R.string.floors, dataPoint.getValue()));
                    break;
                case "Steps":
                    data.addSteps(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (long) dataPoint.getValue());
                    break;
                case "Daily Steps":
                    data.addStepsDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (long) dataPoint.getValue());
                    stepsTextView.setText(getResources().getString(R.string.steps, dataPoint.getValue()));
                    break;
                case "Distance":
                    data.addDistance(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Distance":
                    data.addDistanceDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    distanceTextView.setText(getResources().getString(R.string.distance, dataPoint.getValue()));
                    break;
                case "Calories":
                    data.addCalories(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    break;
                case "Daily Calories":
                    data.addCaloriesDaily(dataPoint.getStartInstant(bootInstant), dataPoint.getEndInstant(bootInstant), (double) dataPoint.getValue());
                    caloriesTextView.setText(getResources().getString(R.string.calories, dataPoint.getValue()));
                    break;
                default:
                    break;
            }
        });

        dataPoints.getSampleDataPoints().forEach(dataPoint -> {
            if (dataPoint.getDataType().getName().equals("HeartRate")) {
                data.addHeartRate(dataPoint.getTimeInstant(bootInstant), (double) dataPoint.getValue());
                heartRateTextView.setText(getResources().getString(R.string.heartRate, dataPoint.getValue()));
            }
        });

        Log.d(TAG, data.toJSon());

        // Send data to the phone
        Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(this).sendMessage(node.getId(), Constants.DATA_PATH, data.toJSon().getBytes(StandardCharsets.UTF_8));
            }
        });

        // If the last confirmation was more than 10 seconds ago, show the warning
        if (Duration.between(lastConfirmationDate.toInstant(), Instant.now()).getSeconds() > 10) {
            warningTextView.setVisibility(TextView.VISIBLE);
        }
    }

    @Override
    public void onUserActivityInfoReceived(@NonNull UserActivityInfo info) {
        PassiveListenerCallback.super.onUserActivityInfoReceived(info);

        if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_PASSIVE) {
            activityStateTextView.setText(R.string.activityStatePassive);
        } else if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_ASLEEP) {
            activityStateTextView.setText(R.string.activityStateAsleep);
        } else if (info.getUserActivityState() == UserActivityState.USER_ACTIVITY_EXERCISE) {
            activityStateTextView.setText(R.string.activityStateExercise);
        } else {
            activityStateTextView.setText(R.string.activityStateUnknown);
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.CONFIRMATION_PATH)) {
            // Hide the warning
            ((TextView) findViewById(R.id.warning)).setText("");

            lastConfirmationDate = new Date();
        } else if (messageEvent.getPath().equals(Constants.OUTPUTS_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);

            outputs = Outputs.fromJSon(message);

            if (!outputs.getEnableHeartRate()) {
                heartRateTextView.setText(R.string.heartRateDefault);
            }
            if (!outputs.getEnableElevationGain()) {
                elevationTextView.setText(R.string.elevationDefault);
            }
            if (!outputs.getEnableCalories()) {
                caloriesTextView.setText(R.string.caloriesDefault);
            }
            if (!outputs.getEnableFloors()) {
                floorsTextView.setText(R.string.floorsDefault);
            }
            if (!outputs.getEnableSteps()) {
                stepsTextView.setText(R.string.stepsDefault);
            }
            if (!outputs.getEnableDistance()) {
                distanceTextView.setText(R.string.distanceDefault);
            }

            getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                    .putBoolean("enableHeartRate", outputs.getEnableHeartRate())
                    .putBoolean("enableElevationGain", outputs.getEnableElevationGain())
                    .putBoolean("enableCalories", outputs.getEnableCalories())
                    .putBoolean("enableFloors", outputs.getEnableFloors())
                    .putBoolean("enableSteps", outputs.getEnableSteps())
                    .putBoolean("enableDistance", outputs.getEnableDistance())
                    .apply();

            startMonitoring();
        }
    }

    private void startMonitoring() {
        if (isStarting) {
            return;
        }
        isStarting = true;
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
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                        && outputs.getEnableHeartRate()) {
                    dataTypes.add(DataType.HEART_RATE_BPM);
                }
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    if (outputs.getEnableCalories()) {
                        dataTypes.add(DataType.CALORIES);
                        dataTypes.add(DataType.CALORIES_DAILY);
                    }
                    if (outputs.getEnableDistance()) {
                        dataTypes.add(DataType.DISTANCE);
                        dataTypes.add(DataType.DISTANCE_DAILY);
                    }
                    if (outputs.getEnableSteps()) {
                        dataTypes.add(DataType.STEPS);
                        dataTypes.add(DataType.STEPS_DAILY);
                    }
                    if (outputs.getEnableFloors()) {
                        dataTypes.add(DataType.FLOORS);
                        dataTypes.add(DataType.FLOORS_DAILY);
                    }
                    if (outputs.getEnableElevationGain()) {
                        dataTypes.add(DataType.ELEVATION_GAIN);
                        if (elevationGainDailyType != null) {
                            dataTypes.add(elevationGainDailyType);
                        }
                    }
                }

                PassiveListenerConfig.Builder builder = new PassiveListenerConfig.Builder()
                        .setDataTypes(dataTypes);
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    builder.setShouldUserActivityInfoBeRequested(true);
                }
                PassiveListenerConfig passiveListenerConfig = builder.build();

                passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, mainActivity);
                isStarting = false;
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "Failed to get passive monitoring capabilities", t);
                isStarting = false;
            }
        }, ContextCompat.getMainExecutor(this));
    }

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