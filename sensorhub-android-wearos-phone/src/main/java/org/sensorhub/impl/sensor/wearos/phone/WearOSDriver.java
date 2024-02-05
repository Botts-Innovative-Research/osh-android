package org.sensorhub.impl.sensor.wearos.phone;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class WearOSDriver extends AbstractSensorModule<WearOSConfig> implements MessageClient.OnMessageReceivedListener {
    private static final Logger logger = LoggerFactory.getLogger(WearOSDriver.class);
    private static final String CONFIRMATION_PATH = "/OSH/Confirmation";
    private static final String HEART_RATE_PATH = "/OSH/HeartRate";
    private WearOSOutput output;
    private Context context;

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void doInit() {
        generateUniqueID("urn:rsi:wearos:", null);
        generateXmlID("wear-os_", null);

        output = new WearOSOutput(this);
        output.doInit();
        addOutput(output, false);

        context = SensorHubService.getContext();
    }

    @Override
    public void doStart() {
        Wearable.getMessageClient(context).addListener(this);
    }

    @Override
    public void doStop() {
        Wearable.getMessageClient(context).removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(HEART_RATE_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);
            logger.info("Message from watch: {} {}", messageEvent.getPath(), message);
            String[] messageSplit = message.split(",");
            if (messageSplit.length == 2) {
                long timestamp = Long.parseLong(messageSplit[0]);
                int heartRate = Integer.parseInt(messageSplit[1]);
                output.setData(timestamp, heartRate);
            }

            Wearable.getMessageClient(context).sendMessage(messageEvent.getSourceNodeId(), CONFIRMATION_PATH, "Received".getBytes(StandardCharsets.UTF_8));
        }
    }
}
