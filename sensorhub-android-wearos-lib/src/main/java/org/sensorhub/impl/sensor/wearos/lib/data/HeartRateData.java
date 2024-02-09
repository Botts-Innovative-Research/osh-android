package org.sensorhub.impl.sensor.wearos.lib.data;

import java.time.Instant;

public class HeartRateData {
    private final long timestamp;
    private final int value;

    public HeartRateData(Instant timestamp, double value) {
        this.timestamp = timestamp.toEpochMilli();
        this.value = (int) value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getValue() {
        return value;
    }
}
