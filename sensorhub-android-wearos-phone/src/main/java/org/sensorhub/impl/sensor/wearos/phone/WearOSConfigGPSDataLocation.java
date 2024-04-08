package org.sensorhub.impl.sensor.wearos.phone;

import org.sensorhub.api.config.DisplayInfo;

public class WearOSConfigGPSDataLocation {
    @DisplayInfo(desc="Address of remote host to connect to for GPS data points")
    @DisplayInfo.Required
    public String gpsHost = "http://localhost:8181/sensorhub/api";

    @DisplayInfo(label="User Name", desc="Remote user name")
    public String user;

    @DisplayInfo(label="Password", desc="Remote password")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    public String password;
}
