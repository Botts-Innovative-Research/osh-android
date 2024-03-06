package org.sensorhub.impl.sensor.wearos.lib.data;

import com.google.gson.Gson;

import java.util.Map;

public class GPSData {
    private final double centerLatitude;
    private final double centerLongitude;
    private final Map<Double, Double> points;

    public GPSData(double centerLatitude, double centerLongitude, Map<Double, Double> points) {
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.points = points;
    }

    /**
     * Serializes the data to a JSON string.
     *
     * @return The JSON string.
     */
    public String toJSon() {
        return new Gson().toJson(this);
    }

    /**
     * Deserializes the data from a JSON string.
     *
     * @param json The JSON string.
     * @return The GPSData object.
     */
    public static GPSData fromJSon(String json) {
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
    public Map<Double, Double> getPoints() {
        return points;
    }
}
