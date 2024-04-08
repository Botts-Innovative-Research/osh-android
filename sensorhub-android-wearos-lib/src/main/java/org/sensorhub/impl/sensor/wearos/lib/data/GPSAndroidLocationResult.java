package org.sensorhub.impl.sensor.wearos.lib.data;

import android.util.Log;

import com.google.gson.Gson;

public class GPSAndroidLocationResult {
    private final double lat;
    private final double lon;
    private final double alt;

    public GPSAndroidLocationResult(double lat, double lon, double alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    @Override
    public String toString() {
        return "Location{" +
                "lat=" + lat +
                ", lon=" + lon +
                ", alt=" + alt +
                '}';
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getAlt() {
        return alt;
    }

    public static GPSAndroidLocationResult fromJson(String json) {
        if (json.startsWith("{location=")) {
            json = json.substring(10, json.length() - 1);
        }
        return new Gson().fromJson(json, GPSAndroidLocationResult.class);
    }
}
