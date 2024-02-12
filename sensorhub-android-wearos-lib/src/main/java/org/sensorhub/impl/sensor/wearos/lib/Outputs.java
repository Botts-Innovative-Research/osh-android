package org.sensorhub.impl.sensor.wearos.lib;

public class Outputs {
    private final boolean enableHeartRate;
    private final boolean enableElevationGain;
    private final boolean enableCalories;
    private final boolean enableFloors;
    private final boolean enableSteps;
    private final boolean enableDistance;

    public Outputs(boolean enableHeartRate, boolean enableElevationGain, boolean enableCalories, boolean enableFloors, boolean enableSteps, boolean enableDistance) {
        this.enableHeartRate = enableHeartRate;
        this.enableElevationGain = enableElevationGain;
        this.enableCalories = enableCalories;
        this.enableFloors = enableFloors;
        this.enableSteps = enableSteps;
        this.enableDistance = enableDistance;
    }

    public boolean getEnableHeartRate() {
        return enableHeartRate;
    }

    public boolean getEnableElevationGain() {
        return enableElevationGain;
    }

    public boolean getEnableCalories() {
        return enableCalories;
    }

    public boolean getEnableFloors() {
        return enableFloors;
    }

    public boolean getEnableSteps() {
        return enableSteps;
    }

    public boolean getEnableDistance() {
        return enableDistance;
    }
}
