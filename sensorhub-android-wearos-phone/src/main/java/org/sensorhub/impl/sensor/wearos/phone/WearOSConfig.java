package org.sensorhub.impl.sensor.wearos.phone;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.wearos.lib.Outputs;

public class WearOSConfig extends SensorConfig {
    private final String deviceName;
    private final Outputs outputs;

    /**
     * Constructor for WearOSConfig
     *
     * @param deviceName          The name of the Wear OS device
     * @param enableHeartRate     Whether to enable the heart rate output
     * @param enableElevationGain Whether to enable the elevation gain output
     * @param enableCalories      Whether to enable the calories output
     * @param enableFloors        Whether to enable the floors output
     * @param enableSteps         Whether to enable the steps output
     * @param enableDistance      Whether to enable the distance output
     */
    public WearOSConfig(String deviceName, boolean enableHeartRate, boolean enableElevationGain, boolean enableCalories, boolean enableFloors, boolean enableSteps, boolean enableDistance) {
        this.moduleClass = WearOSDriver.class.getCanonicalName();
        this.deviceName = deviceName;
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
