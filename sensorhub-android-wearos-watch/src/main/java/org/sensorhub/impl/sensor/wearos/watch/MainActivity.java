package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, PassiveListenerCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONFIRMATION_PATH = "/OSH/Confirmation";
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";
    Date lastConfirmationDate = new Date(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

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
                .setDataTypes(Collections.singleton(DataType.HEART_RATE_BPM))
                .build();

        passiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, this);
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onNewDataPointsReceived(@NonNull DataPointContainer dataPointContainer) {
        dataPointContainer.getSampleDataPoints().forEach(dataPoint -> {
            Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());
            Instant dataPointInstant = dataPoint.getTimeInstant(bootInstant);
            Date date = Date.from(dataPointInstant);
            double value = (double) dataPoint.getValue();
            int heartRate = (int) value;
            Log.d(TAG, "onNewDataPointsReceived{\n" +
                    "  name: " + dataPoint.getDataType().getName() + "\n" +
                    "  value: " + heartRate + "\n" +
                    "  date: " + date + "\n" +
                    "}");

            // Set the heart rate value to the UI
            runOnUiThread(() -> ((TextView) findViewById(R.id.heartRateValue)).setText(String.valueOf(heartRate)));

            // Send heart rate to the phone
            Task<List<Node>> nodesTask = Wearable.getNodeClient(this).getConnectedNodes();
            nodesTask.addOnSuccessListener(nodes -> {
                for (Node node : nodes) {
                    String message = date.getTime() + "," + heartRate;
                    Wearable.getMessageClient(this).sendMessage(node.getId(), HEART_RATE_PATH, message.getBytes(StandardCharsets.UTF_8));
                }
            });

            // If the last confirmation was more than 10 seconds ago, show the warning
            Log.d(TAG, "date.getTime() - lastConfirmationDate.getTime(): " + (date.getTime() - lastConfirmationDate.getTime()));
            if (date.getTime() - lastConfirmationDate.getTime() > 10000) {
                runOnUiThread(() -> (findViewById(R.id.warning)).setVisibility(View.VISIBLE));
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
            runOnUiThread(() -> (findViewById(R.id.warning)).setVisibility(View.INVISIBLE));

            lastConfirmationDate = new Date();
        }
    }
}