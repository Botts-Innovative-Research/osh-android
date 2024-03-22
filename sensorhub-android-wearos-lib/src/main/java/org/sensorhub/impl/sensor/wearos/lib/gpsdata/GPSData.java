package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

public class GPSData {
    private double latitude;
    private double longitude;

    public GPSData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "GPSData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
