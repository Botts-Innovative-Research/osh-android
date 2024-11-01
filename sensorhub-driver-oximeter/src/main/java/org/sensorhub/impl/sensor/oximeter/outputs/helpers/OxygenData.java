package org.sensorhub.impl.sensor.oximeter.outputs.helpers;


public class OxygenData {
    float oxy;

    public final int UPPER_SAFETY_THRESHOLD = 100;
    public final int LOWER_SAFETY_THRESHOLD = 95;

    public OxygenData(float oxy){
        this.oxy = oxy;
    }
    public float getOxygenLevel() {return oxy;}
}
