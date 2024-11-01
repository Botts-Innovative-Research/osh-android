package org.sensorhub.impl.sensor.oximeter.outputs.helpers;
public class PulseRateData {
    float pr;

    public final int UPPER_SAFETY_THRESHOLD = 118;
    public final int LOWER_SAFETY_THRESHOLD = 75;
    public PulseRateData(float pr){

        this.pr = pr;
    }

    public float getPR() {return pr;}
}
