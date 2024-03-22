package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

import java.util.List;

public class Result {
    private int gpsDataCount;
    private List<GPSData> gpsData;

    public Result(int gpsDataCount, List<GPSData> gpsData) {
        this.gpsDataCount = gpsDataCount;
        this.gpsData = gpsData;
    }

    @Override
    public String toString() {
        return "Result{" +
                "gpsDataCount=" + gpsDataCount +
                ", gpsData=" + gpsData +
                '}';
    }

    public int getGpsDataCount() {
        return gpsDataCount;
    }

    public List<GPSData> getGpsData() {
        return gpsData;
    }
}
