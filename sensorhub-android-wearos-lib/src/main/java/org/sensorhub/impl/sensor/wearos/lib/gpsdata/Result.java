package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

import androidx.annotation.NonNull;

import java.util.List;

public class Result {
    private final int gpsDataCount;
    private final List<GPSDataPoint> gpsDataPoint;

    public Result(int gpsDataCount, List<GPSDataPoint> gpsDataPoint) {
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
}
