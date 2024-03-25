package org.sensorhub.impl.sensor.wearos.lib.gpsdata;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.List;

public class DataContainer {
    private final List<Items> items;

    public DataContainer(List<Items> items) {
        this.items = items;
    }

    @Override
    @NonNull
    public String toString() {
        return "DataContainer{" +
                "items=" + items +
                '}';
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static DataContainer fromJson(String json) {
        return new Gson().fromJson(json, DataContainer.class);
    }

    public List<Items> getItems() {
        return items;
    }
}
