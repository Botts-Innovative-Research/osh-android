package org.sensorhub.impl.sensor.sleepMonitor.helpers;

import java.util.ArrayList;

public class SleepMonitorData {

    private ArrayList<OxygenData> oxygen;
    private ArrayList<HeartRateData> heartRate;

    public int getOxygenSize() {
        if (oxygen == null) {
            return 0;
        }
        return oxygen.size();
    }

    public int getHeartRateSize(){
        if(heartRate==null){
            return 0;
        }
        return heartRate.size();
    }
    public void addHR(long timestamp, float hr){
        if(heartRate == null){
            this.heartRate = new ArrayList<>();
        }
        this.heartRate.add(new HeartRateData(timestamp, hr));
    }

    public void addOxygen(long timestamp, float oxy){
        if (oxygen == null) {
            this.oxygen= new ArrayList<>();
        }
        this.oxygen.add(new OxygenData(timestamp,oxy));
    }

    public OxygenData getOxygen(int index) {
        return oxygen.get(index);
    }
    public HeartRateData getHeartRate(int index){
        return heartRate.get(index);
    }
}
