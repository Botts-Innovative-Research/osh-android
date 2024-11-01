package org.sensorhub.impl.sensor.oximeter.outputs.helpers;

import java.util.ArrayList;

public class SleepMonitorData {

    private ArrayList<OxygenData> oxygen;
    private ArrayList<PulseRateData> pulseRate;

    public int getOxygenSize() {
        if (oxygen == null) {
            return 0;
        }
        return oxygen.size();
    }

    public int getHeartRateSize(){
        if(pulseRate==null){
            return 0;
        }
        return pulseRate.size();
    }
    public void addHR(float pr){
        if(pulseRate == null){
            this.pulseRate = new ArrayList<>();
        }
        this.pulseRate.add(new PulseRateData(pr));
    }

    public void addOxygen(float oxy){
        if (oxygen == null) {
            this.oxygen= new ArrayList<>();
        }
        this.oxygen.add(new OxygenData(oxy));
    }

    public OxygenData getOxygen(int index) {
        return oxygen.get(index);
    }
    public PulseRateData getPulseRate(int index){
        return pulseRate.get(index);
    }
}
