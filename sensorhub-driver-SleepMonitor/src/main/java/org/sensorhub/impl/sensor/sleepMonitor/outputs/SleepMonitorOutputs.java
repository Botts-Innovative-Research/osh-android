package org.sensorhub.impl.sensor.sleepMonitor.outputs;

public class SleepMonitorOutputs {
    private final boolean enableHR;
    private final boolean enableOxygen;

    public SleepMonitorOutputs(boolean enableHR, boolean enableOxygen){
        this.enableOxygen = enableOxygen;
        this.enableHR = enableHR;
    }

    public boolean getEnabledHR(){
        return enableHR;
    }
    public boolean getEnabledOxygen(){
        return enableOxygen;
    }
}
