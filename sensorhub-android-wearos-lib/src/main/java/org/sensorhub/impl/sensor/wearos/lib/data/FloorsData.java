package org.sensorhub.impl.sensor.wearos.lib.data;

import java.time.Instant;

public class FloorsData {
    private final long startTime;
    private final long endTime;
    private final double value;

    public FloorsData(Instant startTime, Instant endTime, double value) {
        this.startTime = startTime.toEpochMilli();
        this.endTime = endTime.toEpochMilli();
        this.value = value;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getValue() {
        return value;
    }
}
