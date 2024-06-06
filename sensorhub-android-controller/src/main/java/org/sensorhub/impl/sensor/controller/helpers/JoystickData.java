package org.sensorhub.impl.sensor.controller.helpers;

public class JoystickData {
        float x, y, rx, ry, pov;
        long timestamp;
    public JoystickData(long timestamp, float x, float y, float rx, float ry, float pov){
        this.pov = pov;
        this.rx =rx;
        this.x=x;
        this.y=y;
        this.ry=ry;
    }
    public long getTimestamp() {return timestamp;}
    public float getPov() {return pov;}
    public float getRx() {return rx;}
    public float getRy() {return ry;}
    public float get_y() {return y;}
    public float get_x() {return x;}

}
