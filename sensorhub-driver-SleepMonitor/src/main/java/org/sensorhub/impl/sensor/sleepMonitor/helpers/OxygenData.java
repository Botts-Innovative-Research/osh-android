package org.sensorhub.impl.sensor.sleepMonitor.helpers;


public class OxygenData {
    private long timestamp;
    float oxy;

    public final int UPPER_SAFETY_THRESHOLD = 100;
    public final int LOWER_SAFETY_THRESHOLD = 95;

    public OxygenData(long timestamp, float oxy){
        this.timestamp =timestamp;
        this.oxy = oxy;
    }
    public long getTimestamp() {return timestamp;}

    public float getOxygenLevel() {return oxy;}
}
