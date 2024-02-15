package org.sensorhub.impl.sensor.wearos.lib.data;

import java.time.Instant;

/**
 * Data class for steps data.
 */
public class StepsData {
    private final long startTime;
    private final long endTime;
    private final long value;

    /**
     * Constructor for steps data.
     *
     * @param startTime The start time of the sampling period.
     * @param endTime   The end time of the sampling period.
     * @param value     The value of the data.
     */
    public StepsData(Instant startTime, Instant endTime, long value) {
        this.startTime = startTime.toEpochMilli();
        this.endTime = endTime.toEpochMilli();
        this.value = (int) value;
    }

    /**
     * Get the start time of the sampling period.
     *
     * @return The start time of the sampling period.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the end time of the sampling period.
     *
     * @return The end time of the sampling period.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Get the value of the data.
     *
     * @return The value of the data.
     */
    public long getValue() {
        return value;
    }
}
