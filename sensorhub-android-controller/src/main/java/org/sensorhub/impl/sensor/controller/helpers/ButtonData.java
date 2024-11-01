package org.sensorhub.impl.sensor.controller.helpers;


public class ButtonData {
    private long timestamp;
    float Y, X, A, B, LeftThumb, RightThumb, LeftBumper, RightBumper, mode, pov_down, pov_up, pov_left, pov_right, x, y, rx, ry;
//    float x, y, rx, ry, pov;
    public ButtonData(long timestamp, float mode,float A,float B, float X, float Y, float LeftThumb, float RightThumb, float LeftBumper, float RightBumper){
        this.timestamp =timestamp;
//        this.pov_up = pov_up;
//        this.pov_down = pov_down;
//        this.pov_left= pov_left;
//        this.pov_right = pov_right;
        this.A=A;
        this.B=B;
        this.X=X;
        this.Y=Y;
        this.RightBumper=RightBumper;
        this.LeftBumper =LeftBumper;
        this.RightThumb= RightThumb;
        this.LeftThumb=LeftThumb;
        this.mode=mode;

    }
    public long getTimestamp() {return timestamp;}

    public float getA() {return A;}
    public float getB() {return B;}
    public float getX() {return X;}
    public float getY() {return Y;}
    public float getRightThumb() {return RightThumb;}
    public float getLeftThumb() {return LeftThumb;}
    public float getRightBumper() {return RightBumper;}
    public float getLeftBumper() {return LeftBumper;}
    public float getMode() {return mode;}
//    public boolean getPovUp() {return pov_up;}
//    public boolean getPovDown() {return pov_down;}
//    public boolean getPovLeft() {return pov_left;}
//    public boolean getPovRight() {return pov_right;}
//    public boolean getRx() {return rx;}
//    public boolean getRy() {return ry;}
//    public boolean get_y() {return y;}
//    public boolean get_x() {return x;}


}
