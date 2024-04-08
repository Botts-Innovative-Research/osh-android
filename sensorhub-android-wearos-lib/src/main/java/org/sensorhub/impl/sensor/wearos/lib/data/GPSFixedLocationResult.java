package org.sensorhub.impl.sensor.wearos.lib.data;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.List;

public class GPSFixedLocationResult {
    private final int gpsDataCount;
    private final List<GPSDataPoint> gpsDataPoint;

    public GPSFixedLocationResult(int gpsDataCount, List<GPSDataPoint> gpsDataPoint) {
        this.gpsDataCount = gpsDataCount;
        this.gpsDataPoint = gpsDataPoint;
    }

    @Override
    @NonNull
    public String toString() {
        return "Result{" +
                "gpsDataCount=" + gpsDataCount +
                ", gpsDataPoint=" + gpsDataPoint +
                '}';
    }

    /**
     * Returns a list of GPS data points.
     *
     * @return The list of GPS data points.
     */
    public List<GPSDataPoint> getGpsDataPoint() {
        return gpsDataPoint;
    }

    public static GPSFixedLocationResult fromJson(String json) {
        return new Gson().fromJson(json, GPSFixedLocationResult.class);
    }
}
