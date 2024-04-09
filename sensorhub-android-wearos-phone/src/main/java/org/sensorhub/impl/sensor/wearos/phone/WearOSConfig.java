package org.sensorhub.impl.sensor.wearos.phone;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.wearos.lib.Outputs;

public class WearOSConfig extends SensorConfig {
    private final String deviceName;
    private final String deviceID;
    private final Outputs outputs;

    /**
     * Constructor for WearOSConfig
     *
     * @param deviceName          The name of the device configured in OpenSensorHub settings
     * @param deviceID            The ID of the device
     * @param enableHeartRate     Whether to enable the heart rate output
     * @param enableElevationGain Whether to enable the elevation gain output
     * @param enableCalories      Whether to enable the calories output
     * @param enableFloors        Whether to enable the floors output
     * @param enableSteps         Whether to enable the steps output
     * @param enableDistance      Whether to enable the distance output
     */
    public WearOSConfig(String deviceName, String deviceID, boolean enableHeartRate, boolean enableElevationGain, boolean enableCalories, boolean enableFloors, boolean enableSteps, boolean enableDistance) {
        this.moduleClass = WearOSDriver.class.getCanonicalName();
        this.deviceName = deviceName;
        this.deviceID = deviceID;
        this.outputs = new Outputs(enableHeartRate, enableElevationGain, enableCalories, enableFloors, enableSteps, enableDistance);
    }

    /**
     * Get the name of the device configured in the OpenSensorHub settings
     *
     * @return The name of the device
     */
    public String getDeviceName() {
        return this.deviceName;
    }

    /**
     * Get the ID of the device
     *
     * @return The ID of the device
     */
    public String getDeviceID() {
        return this.deviceID;
    }

    /**
     * Get the outputs configured in the OpenSensorHub settings
     *
     * @return The outputs
     */
    public Outputs getOutputs() {
        return this.outputs;
    }

    @DisplayInfo.Required
    @DisplayInfo(label = "GPS Data Location", desc = "Location to pull GPS data from")
    public WearOSConfigGPSDataLocation gpsDataLocation = new WearOSConfigGPSDataLocation();
}
