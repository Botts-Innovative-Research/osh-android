package org.sensorhub.impl.sensor.controller.helpers;

import java.util.ArrayList;

public class ControllerData {

    private ArrayList<ButtonData> buttons;
    private ArrayList<JoystickData> joystick;

    public int getButtonSize() {
        if (buttons == null) {
            return 0;
        }
        return buttons.size();
    }

    public int getJoystickSize(){
        if(joystick==null){
            return 0;
        }
        return joystick.size();
    }
    public void addJoystick(long timestamp, float x, float y, float rx, float ry, float pov){
        if(joystick == null){
            this.joystick = new ArrayList<>();
        }
        this.joystick.add(new JoystickData(timestamp, x, y, rx, ry, pov));
    }

    public void addButtons(long timestamp, boolean mode, boolean A,boolean B, boolean X, boolean Y, boolean LeftThumb, boolean RightThumb, boolean LeftBumper, boolean RightBumper){
        if (buttons == null) {
            this.buttons= new ArrayList<>();
        }
        this.buttons.add(new ButtonData(timestamp,mode, A,B,X,Y,LeftThumb, RightThumb, LeftBumper, RightBumper));
    }

    public ButtonData getButtons(int index) {
        return buttons.get(index);
    }
    public JoystickData getJoystick(int index){
        return joystick.get(index);
    }
}
