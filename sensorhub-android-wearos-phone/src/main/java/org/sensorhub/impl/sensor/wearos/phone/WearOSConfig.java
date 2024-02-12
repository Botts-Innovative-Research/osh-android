package org.sensorhub.impl.sensor.wearos.phone;

import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.wearos.lib.Outputs;

public class WearOSConfig extends SensorConfig {
    private final String deviceName;
    private final Outputs outputs;

    public WearOSConfig(String deviceName, boolean enableHeartRate, boolean enableElevationGain, boolean enableCalories, boolean enableFloors, boolean enableSteps, boolean enableDistance) {
        this.moduleClass = WearOSDriver.class.getCanonicalName();
        this.deviceName = deviceName;
        this.outputs = new Outputs(enableHeartRate, enableElevationGain, enableCalories, enableFloors, enableSteps, enableDistance);
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public Outputs getOutputs() {
        return this.outputs;
    }
}
