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

    public boolean getEnableHeartRate() {
        return this.outputs.getEnableHeartRate();
    }

    public boolean getEnableElevationGain() {
        return this.outputs.getEnableElevationGain();
    }

    public boolean getEnableCalories() {
        return this.outputs.getEnableCalories();
    }

    public boolean getEnableFloors() {
        return this.outputs.getEnableFloors();
    }

    public boolean getEnableSteps() {
        return this.outputs.getEnableSteps();
    }

    public boolean getEnableDistance() {
        return this.outputs.getEnableDistance();
    }
}
