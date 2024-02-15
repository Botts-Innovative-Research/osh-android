package org.sensorhub.impl.sensor.wearos.watch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;

/**
 * This class is used to manage the state of the outputs.
 * Outputs can be enabled or disabled by phone app, and the watch app will only send data for the enabled outputs.
 */
public class PreferencesManager {
    private static final String OUTPUTS_PREFS = "outputs";
    private static final String ENABLE_HEART_RATE = "enableHeartRate";
    private static final String ENABLE_CALORIES = "enableCalories";
    private static final String ENABLE_DISTANCE = "enableDistance";
    private static final String ENABLE_STEPS = "enableSteps";
    private static final String ENABLE_FLOORS = "enableFloors";
    private static final String ENABLE_ELEVATION_GAIN = "enableElevationGain";

    private PreferencesManager() {
        // Prevent instantiation
    }

    /**
     * Get the current state of the heart rate output
     *
     * @param context The context
     * @return Whether the heart rate output is enabled
     */
    public static boolean getEnableHeartRate(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean(ENABLE_HEART_RATE, true);
    }

    /**
     * Set the state of the heart rate output
     *
     * @param context The context
     * @param enable  Whether to enable the heart rate output
     */
    public static void setEnableHeartRate(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_HEART_RATE, enable)
                .apply();
    }

    /**
     * Get the current state of the calories output
     *
     * @param context The context
     * @return Whether the calories output is enabled
     */
    public static boolean getEnableCalories(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_CALORIES, true);
    }

    /**
     * Set the state of the calories output
     *
     * @param context The context
     * @param enable  Whether to enable the calories output
     */
    public static void setEnableCalories(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_CALORIES, enable)
                .apply();
    }

    /**
     * Get the current state of the distance output
     *
     * @param context The context
     * @return Whether the distance output is enabled
     */
    public static boolean getEnableDistance(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_DISTANCE, true);
    }

    /**
     * Set the state of the distance output
     *
     * @param context The context
     * @param enable  Whether to enable the distance output
     */
    public static void setEnableDistance(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_DISTANCE, enable)
                .apply();
    }

    /**
     * Get the current state of the steps output
     *
     * @param context The context
     * @return Whether the steps output is enabled
     */
    public static boolean getEnableSteps(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_STEPS, true);
    }

    /**
     * Set the state of the steps output
     *
     * @param context The context
     * @param enable  Whether to enable the steps output
     */
    public static void setEnableSteps(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_STEPS, enable)
                .apply();
    }

    /**
     * Get the current state of the floors output
     *
     * @param context The context
     * @return Whether the floors output is enabled
     */
    public static boolean getEnableFloors(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_FLOORS, true);
    }

    /**
     * Set the state of the floors output
     *
     * @param context The context
     * @param enable  Whether to enable the floors output
     */
    public static void setEnableFloors(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_FLOORS, enable)
                .apply();
    }

    /**
     * Get the current state of the elevation gain output
     *
     * @param context The context
     * @return Whether the elevation gain output is enabled
     */
    public static boolean getEnableElevationGain(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_ELEVATION_GAIN, true);
    }

    /**
     * Set the state of the elevation gain output
     *
     * @param context The context
     * @param enable  Whether to enable the elevation gain output
     */
    public static void setEnableElevationGain(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_ELEVATION_GAIN, enable)
                .apply();
    }
}
