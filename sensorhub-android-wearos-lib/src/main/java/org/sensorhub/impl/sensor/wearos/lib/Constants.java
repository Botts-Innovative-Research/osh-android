package org.sensorhub.impl.sensor.wearos.lib;

/**
 * Constants for the WearOS drivers used by both the phone and the watch.
 */
public class Constants {
    /**
     * The path for the confirmation message.
     */
    public static final String CONFIRMATION_PATH = "/OSH/Confirmation";
    /**
     * The path for the data message.
     */
    public static final String DATA_PATH = "/OSH/Data";
    /**
     * The path for the outputs message.
     */
    public static final String OUTPUTS_PATH = "/OSH/Outputs";

    private Constants() {
        // Prevent instantiation
    }
}
