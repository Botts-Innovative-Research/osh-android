package org.sensorhub.impl.sensor.wearos.watch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;

public class PreferencesManager {
    private static final String OUTPUTS_PREFS = "outputs";
    private static final String ENABLE_HEART_RATE = "enableHeartRate";
    private static final String ENABLE_CALORIES = "enableCalories";
    private static final String ENABLE_DISTANCE = "enableDistance";
    private static final String ENABLE_STEPS = "enableSteps";
    private static final String ENABLE_FLOORS = "enableFloors";
    private static final String ENABLE_ELEVATION_GAIN = "enableElevationGain";

    private PreferencesManager() {
    }

    public static boolean getEnableHeartRate(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).getBoolean(ENABLE_HEART_RATE, true);
    }

    public static void setEnableHeartRate(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_HEART_RATE, enable)
                .apply();
    }

    public static boolean getEnableCalories(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_CALORIES, true);
    }

    public static void setEnableCalories(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_CALORIES, enable)
                .apply();
    }

    public static boolean getEnableDistance(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_DISTANCE, true);
    }

    public static void setEnableDistance(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_DISTANCE, enable)
                .apply();
    }

    public static boolean getEnableSteps(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_STEPS, true);
    }

    public static void setEnableSteps(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_STEPS, enable)
                .apply();
    }

    public static boolean getEnableFloors(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_FLOORS, true);
    }

    public static void setEnableFloors(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_FLOORS, enable)
                .apply();
    }

    public static boolean getEnableElevationGain(Context context) {
        return context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE)
                .getBoolean(ENABLE_ELEVATION_GAIN, true);
    }

    public static void setEnableElevationGain(Context context, boolean enable) {
        context.getSharedPreferences(OUTPUTS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(ENABLE_ELEVATION_GAIN, enable)
                .apply();
    }
}
