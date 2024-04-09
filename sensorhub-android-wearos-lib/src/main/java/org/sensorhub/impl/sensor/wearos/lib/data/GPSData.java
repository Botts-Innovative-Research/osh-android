package org.sensorhub.impl.sensor.wearos.lib.data;

import com.google.gson.Gson;

import java.util.List;

public class GPSData {
    private final double centerLatitude;
    private final double centerLongitude;
    private final List<GPSDataPoint> points;

    public GPSData(double centerLatitude, double centerLongitude, List<GPSDataPoint> points) {
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.points = points;
    }

    /**
     * Serializes the data to a JSON string.
     *
     * @return The JSON string.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Deserializes the data from a JSON string.
     *
     * @param json The JSON string.
     * @return The GPSData object.
     */
    public static GPSData fromJson(String json) {
        return new Gson().fromJson(json, GPSData.class);
    }

    /**
     * Returns the latitude of the center of the map.
     *
     * @return The latitude.
     */
    public double getCenterLatitude() {
        return centerLatitude;
    }

    /**
     * Returns the longitude of the center of the map.
     *
     * @return The longitude.
     */
    public double getCenterLongitude() {
        return centerLongitude;
    }

    /**
     * Returns the points to draw on the map.
     *
     * @return The points.
     */
    public List<GPSDataPoint> getPoints() {
        return points;
    }
}
