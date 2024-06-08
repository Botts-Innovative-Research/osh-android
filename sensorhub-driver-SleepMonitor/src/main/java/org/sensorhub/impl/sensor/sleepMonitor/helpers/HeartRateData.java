package org.sensorhub.impl.sensor.sleepMonitor.helpers;
public class HeartRateData {
    private long timestamp;
    float hr;

    public final int UPPER_SAFETY_THRESHOLD = 118;
    public final int LOWER_SAFETY_THRESHOLD = 75;
    public HeartRateData(long timestamp, float hr){
        this.timestamp =timestamp;
        this.hr = hr;
    }
    public long getTimestamp() {return timestamp;}
    public float getHR() {return hr;}
}
