package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

import androidx.annotation.NonNull;

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
    @NonNull
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

    /**
     * Returns the Result object, which contains the actual data.
     *
     * @return The Result object.
     */
    public Result getResult() {
        return result;
    }
}
