package org.sensorhub.impl.sensor.controller.helpers;


public class ButtonData {
    private long timestamp;
    boolean Y, X, A, B, LeftThumb, RightThumb, LeftBumper, RightBumper, mode, pov_down, pov_up, pov_left, pov_right, x, y, rx, ry;
//    float x, y, rx, ry, pov;
    public ButtonData(long timestamp, boolean mode,boolean A,boolean B, boolean X, boolean Y, boolean LeftThumb, boolean RightThumb, boolean LeftBumper, boolean RightBumper){
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

    public boolean getA() {return A;}
    public boolean getB() {return B;}
    public boolean getX() {return X;}
    public boolean getY() {return Y;}
    public boolean getRightThumb() {return RightThumb;}
    public boolean getLeftThumb() {return LeftThumb;}
    public boolean getRightBumper() {return RightBumper;}
    public boolean getLeftBumper() {return LeftBumper;}
    public boolean getMode() {return mode;}
//    public boolean getPovUp() {return pov_up;}
//    public boolean getPovDown() {return pov_down;}
//    public boolean getPovLeft() {return pov_left;}
//    public boolean getPovRight() {return pov_right;}
//    public boolean getRx() {return rx;}
//    public boolean getRy() {return ry;}
//    public boolean get_y() {return y;}
//    public boolean get_x() {return x;}


}
