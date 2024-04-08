package org.sensorhub.impl.sensor.wearos.phone.data.observations;

import com.google.gson.Gson;

import java.util.List;

public class DataContainer {
    private final List<Items> items;

    public DataContainer(List<Items> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "DataContainer{" +
                "items=" + items +
                '}';
    }

    /**
     * Create a DataContainer object from a JSON string
     *
     * @param json the JSON string
     * @return the DataContainer object
     */
    public static DataContainer fromJson(String json) {
        return new Gson().fromJson(json, DataContainer.class);
    }

    /**
     * Get the list of items
     *
     * @return the list of items
     */
    public List<Items> getItems() {
        return items;
    }
}
