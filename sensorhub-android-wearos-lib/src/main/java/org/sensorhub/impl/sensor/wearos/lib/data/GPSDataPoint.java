package org.sensorhub.impl.sensor.wearos.lib.data;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class GPSDataPoint {
    private final double latitude;
    private final double longitude;
    private final String color;

    public GPSDataPoint(double latitude, double longitude, String color) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.color = color;
    }

    @Override
    @NonNull
    public String toString() {
        return "GPSData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", color=" + color +
                '}';
    }

    /**
     * Returns the latitude of the point.
     *
     * @return The latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of the point.
     *
     * @return The longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the color of the point.
     *
     * @return A string representing the color. No validation is done on the string;
     * it is up to the caller to ensure it is a valid color.
     */
    public String getColor() {
        return color;
    }
}
