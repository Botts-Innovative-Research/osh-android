package org.sensorhub.impl.sensor.controller.helpers;

import android.view.InputDevice;

public class GameControllerState {

    private final InputDevice device;

    public GameControllerState(InputDevice inputDevice){
        this.device = inputDevice;
    }
    public InputDevice getDevice(){
        return device;
    }

}
