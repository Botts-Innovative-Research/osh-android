package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();

        // Request android.permission.BODY_SENSORS
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS);
        Log.d(TAG, " permissionCheck: " + permissionCheck);

        if (permissionCheck == -1)
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 1);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        Log.d(TAG, " sensor: " + (sensor == null ? "null" : "not null"));

        boolean sensorRegistered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d(TAG, " Sensor registered: " + sensorRegistered);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int heartRate = Math.round(event.values[0]);
        Log.d("Heart Rate", "Heart Rate: " + heartRate);

        // Send heart rate to the phone
        Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        wearableList.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                String nodeId = node.getId();
                String message = Integer.toString(heartRate);
                Task<Integer> sendMessageTask = Wearable.getMessageClient(getApplicationContext()).sendMessage(nodeId, HEART_RATE_PATH, message.getBytes(StandardCharsets.UTF_8));
                sendMessageTask.addOnSuccessListener(integer -> Log.d(TAG, "Message sent successfully"));
                sendMessageTask.addOnFailureListener(e -> Log.d(TAG, "Message failed"));
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}