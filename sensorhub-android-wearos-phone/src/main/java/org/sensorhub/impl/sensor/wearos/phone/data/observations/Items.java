package org.sensorhub.impl.sensor.wearos.phone.data.observations;

import androidx.annotation.NonNull;

public class Items {
    private final String id;
    private final String phenomenonTime;
    private final String resultTime;
    private final Object result;

    public Items(String id, String phenomenonTime, String resultTime, String result) {
        this.id = id;
        this.phenomenonTime = phenomenonTime;
        this.resultTime = resultTime;
        this.result = result;
    }

    @Override
    @NonNull
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", phenomenonTime='" + phenomenonTime + '\'' +
                ", resultTime='" + resultTime + '\'' +
                ", result=" + result +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getPhenomenonTime() {
        return phenomenonTime;
    }

    public String getResultTime() {
        return resultTime;
    }

    public Object getResult() {
        return result;
    }

}
