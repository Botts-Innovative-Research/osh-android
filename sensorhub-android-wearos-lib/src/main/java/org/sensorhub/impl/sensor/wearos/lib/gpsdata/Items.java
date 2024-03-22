package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

import com.google.gson.Gson;

public class Items {
    private final String id;
    private final String datastreamId;
    private final String phenomenonTime;
    private final String resultTime;
    private final Result result;

    public Items(String id, String datastreamId, String phenomenonTime, String resultTime, Result result) {
        this.id = id;
        this.datastreamId = datastreamId;
        this.phenomenonTime = phenomenonTime;
        this.resultTime = resultTime;
        this.result = result;
    }

    @Override
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", datastreamId='" + datastreamId + '\'' +
                ", phenomenonTime='" + phenomenonTime + '\'' +
                ", resultTime='" + resultTime + '\'' +
                ", result=" + result +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getDatastreamId() {
        return datastreamId;
    }

    public String getPhenomenonTime() {
        return phenomenonTime;
    }

    public String getResultTime() {
        return resultTime;
    }

    public Result getResult() {
        return result;
    }
}
