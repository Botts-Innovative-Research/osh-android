package org.sensorhub.impl.sensor.wearos.lib.data;

import java.time.Instant;

/**
 * Data class for heart rate data.
 */
public class HeartRateData {
    private final long timestamp;
    private final int value;

    /**
     * Constructor for heart rate data.
     *
     * @param timestamp The timestamp of the data.
     * @param value     The value of the data.
     */
    public HeartRateData(Instant timestamp, double value) {
        this.timestamp = timestamp.toEpochMilli();
        this.value = (int) value;
    }

    /**
     * Get the timestamp of the data.
     *
     * @return The timestamp of the data.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the value of the data.
     *
     * @return The value of the data.
     */
    public int getValue() {
        return value;
    }
}
