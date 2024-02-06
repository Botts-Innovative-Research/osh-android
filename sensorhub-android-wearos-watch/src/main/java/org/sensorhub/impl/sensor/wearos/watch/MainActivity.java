package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.PassiveListenerConfig;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, PassiveListenerCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONFIRMATION_PATH = "/OSH/Confirmation";
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS = 1;
    private static final int PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND = 2;
    Date lastConfirmationDate = new Date(0);
    Handler displayHandler;
    Runnable displayCallback;
    static int heartRate;
    static boolean displayWarning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        displayHandler = new Handler(Looper.getMainLooper());

        requestPermissions();
        startMonitoring();
        startRefreshingUI();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode + " " + Arrays.toString(permissions) + " " + Arrays.toString(grantResults));

        if (requestCode == PERMISSIONS_REQUEST_BODY_SENSORS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND);
            }
            startMonitoring();
        }
    }

    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPointContainer) {
        dataPointContainer.getSampleDataPoints().forEach(dataPoint -> {
            Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());
            Instant dataPointInstant = dataPoint.getTimeInstant(bootInstant);
            Date date = Date.from(dataPointInstant);
            double value = (double) dataPoint.getValue();
            heartRate = (int) value;
            Log.d(TAG, "onNewDataPointsReceived{" +
                    "  name: " + dataPoint.getDataType().getName() +
                    "  value: " + heartRate +
                    "  date: " + date +
                    "}");

            // Send heart rate to the phone
            Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
            nodesTask.addOnSuccessListener(nodes -> {
                for (Node node : nodes) {
                    String message = date.getTime() + "," + heartRate;
                    Wearable.getMessageClient(this).sendMessage(node.getId(), HEART_RATE_PATH, message.getBytes(StandardCharsets.UTF_8));
                }
            });

            // If the last confirmation was more than 10 seconds ago, show the warning
            if (date.getTime() - lastConfirmationDate.getTime() > 10000) {
                displayWarning = true;
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(CONFIRMATION_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);
            Log.d(TAG, "Message from phone: " + messageEvent.getPath() + " " + message);

            // Hide the warning
            displayWarning = false;

            lastConfirmationDate = new Date();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRefreshingUI();
        startMonitoring();
    }

    @Override
    protected void onPause() {
        stopRefreshingUI();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopRefreshingUI();
        super.onStop();
    }

    protected void startRefreshingUI() {
        if (displayCallback != null)
            return;

        // Handler to display async messages in UI
        displayCallback = new Runnable() {
            public void run() {
                ((TextView) findViewById(R.id.heartRateValue)).setText(String.valueOf(heartRate));
                ((TextView) findViewById(R.id.warning)).setText(displayWarning ? getResources().getString(R.string.warning) : "");
                displayHandler.postDelayed(this, 500);
            }
        };

        displayHandler.post(displayCallback);
    }


    protected void stopRefreshingUI() {
        if (displayCallback != null) {
            displayHandler.removeCallbacks(displayCallback);
            displayCallback = null;
        }
    }

    private void startMonitoring() {
        HealthServicesClient healthServicesClient = HealthServices.getClient(this);
        PassiveMonitoringClient passiveMonitoringClient = healthServicesClient.getPassiveMonitoringClient();

        PassiveListenerConfig passiveListenerConfig = new PassiveListenerConfig.Builder()
                .setDataTypes(Collections.singleton(DataType.HEART_RATE_BPM))
                .build();

        passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, this);
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, PERMISSIONS_REQUEST_BODY_SENSORS_BACKGROUND);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSORS);
        }
    }
}