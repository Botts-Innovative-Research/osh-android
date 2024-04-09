package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class DataContainer<T> {
    private final List<T> items;

    public DataContainer(List<T> items) {
        this.items = items;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static <T> DataContainer<T> fromJson(String json, Class<T> clazz) {
        return new Gson().fromJson(json, TypeToken.getParameterized(DataContainer.class, clazz).getType());
    }

    @Override
    @NonNull
    public String toString() {
        return "DataContainer{" +
                "items=" + items +
                '}';
    }

    public List<T> getItems() {
        return items;
    }
}
