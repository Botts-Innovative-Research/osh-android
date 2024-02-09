package org.sensorhub.impl.sensor.wearos.lib.data;

import java.time.Instant;

public class StepsData {
    private final long startTime;
    private final long endTime;
    private final long value;

    public StepsData(Instant startTime, Instant endTime, long value) {
        this.startTime = startTime.toEpochMilli();
        this.endTime = endTime.toEpochMilli();
        this.value = (int) value;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getValue() {
        return value;
    }
}
