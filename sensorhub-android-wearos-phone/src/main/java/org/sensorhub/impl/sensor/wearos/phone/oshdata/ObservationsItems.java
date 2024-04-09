package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

public class ObservationsItems {
    private final String id;
    private final String phenomenonTime;
    private final String resultTime;
    private final Object result;

    public ObservationsItems(String id, String phenomenonTime, String resultTime, Object result) {
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
