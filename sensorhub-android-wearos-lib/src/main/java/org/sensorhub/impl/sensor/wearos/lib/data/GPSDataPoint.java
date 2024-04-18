package org.sensorhub.impl.sensor.wearos.lib.data;

import android.graphics.Color;

import androidx.annotation.NonNull;

public class GPSDataPoint {
    private final double latitude;
    private final double longitude;
    private final String color;
    private final String pointName;

    public GPSDataPoint(double latitude, double longitude, String color, String pointName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.color = color;
        this.pointName = pointName;
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

    /**
     * Returns the name of the point.
     *
     * @return The name of the point.
     */
    public String getPointName() {
        return pointName;
    }

    /**
     * Returns the color of the point as an integer.
     *
     * @return The color integer.
     */
    public int getColorValue() {
        String colorString = getColor();
        if (colorString == null || colorString.isEmpty()) {
            colorString = "#B30000"; // Slightly darker red
        }

        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException e) {
            return Color.GRAY;
        }
    }
}
