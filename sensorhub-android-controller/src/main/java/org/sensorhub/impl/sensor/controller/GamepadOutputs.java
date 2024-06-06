package org.sensorhub.impl.sensor.controller;

public class GamepadOutputs {
    private final boolean enableGamepad;
    private final boolean enableJoystick;

    public GamepadOutputs(boolean enableGamepad, boolean enableJoystick){
        this.enableGamepad = enableGamepad;
        this.enableJoystick = enableJoystick;
    }

    public boolean getEnabledGamepad(){
        return enableGamepad;
    }
    public boolean getEnabledJoystick(){
        return enableJoystick;
    }
}
