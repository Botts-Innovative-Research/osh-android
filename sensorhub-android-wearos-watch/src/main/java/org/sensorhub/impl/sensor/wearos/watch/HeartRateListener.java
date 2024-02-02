package org.sensorhub.impl.sensor.wearos.watch;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.data.DataPointContainer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class HeartRateListener implements PassiveListenerCallback {
    private static final String TAG = HeartRateListener.class.getSimpleName();
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";
    private final Context context;

    public HeartRateListener(Context context) {
        this.context = context;
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

            // Send heart rate to the phone
            Task<List<Node>> nodesTask = Wearable.getNodeClient(context).getConnectedNodes();
            nodesTask.addOnSuccessListener(nodes -> {
                for (Node node : nodes) {
                    String message = date.getTime() + "," + heartRate;
                    Wearable.getMessageClient(context).sendMessage(node.getId(), HEART_RATE_PATH, message.getBytes(StandardCharsets.UTF_8));
                }
            });
        });
    }
}
