package org.sensorhub.impl.sensor.wearos.lib;

import com.google.gson.Gson;

/**
 * This class represents the outputs that the sensor will provide.
 * This data is used by the phone app to configure the watch app.
 */
public class Outputs {
    private final boolean enableHeartRate;
    private final boolean enableElevationGain;
    private final boolean enableCalories;
    private final boolean enableFloors;
    private final boolean enableSteps;
    private final boolean enableDistance;

    /**
     * Constructor
     *
     * @param enableHeartRate     Whether to enable heart rate data
     * @param enableElevationGain Whether to enable elevation gain data
     * @param enableCalories      Whether to enable calories data
     * @param enableFloors        Whether to enable floors data
     * @param enableSteps         Whether to enable steps data
     * @param enableDistance      Whether to enable distance data
     */
    public Outputs(boolean enableHeartRate, boolean enableElevationGain, boolean enableCalories, boolean enableFloors, boolean enableSteps, boolean enableDistance) {
        this.enableHeartRate = enableHeartRate;
        this.enableElevationGain = enableElevationGain;
        this.enableCalories = enableCalories;
        this.enableFloors = enableFloors;
        this.enableSteps = enableSteps;
        this.enableDistance = enableDistance;
    }

    /**
     * Get whether heart rate data is enabled
     *
     * @return Whether to enable heart rate data
     */
    public boolean getEnableHeartRate() {
        return enableHeartRate;
    }

    /**
     * Get whether elevation gain data is enabled
     *
     * @return Whether to enable elevation gain data
     */
    public boolean getEnableElevationGain() {
        return enableElevationGain;
    }

    /**
     * Get whether calories data is enabled
     *
     * @return Whether to enable calories data
     */
    public boolean getEnableCalories() {
        return enableCalories;
    }

    /**
     * Get whether floors data is enabled
     *
     * @return Whether to enable floors data
     */
    public boolean getEnableFloors() {
        return enableFloors;
    }

    /**
     * Get whether steps data is enabled
     *
     * @return Whether to enable steps data
     */
    public boolean getEnableSteps() {
        return enableSteps;
    }

    /**
     * Get whether distance data is enabled
     *
     * @return Whether to enable distance data
     */
    public boolean getEnableDistance() {
        return enableDistance;
    }

    /**
     * Convert this object to a JSON string for transmission to the watch
     *
     * @return JSON string
     */
    public String toJSon() {
        return new Gson().toJson(this);
    }

    /**
     * Convert a JSON string to an Outputs object
     *
     * @param json JSON string
     * @return Outputs object
     */
    public static Outputs fromJSon(String json) {
        return new Gson().fromJson(json, Outputs.class);
    }
}
