package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class DataStreamItems {
    private final String id;
    private final String name;
    private final String outputName;
    private final String[] validTime;

    public DataStreamItems(String id, String name, String outputName, String[] validTime) {
        this.id = id;
        this.name = name;
        this.outputName = outputName;
        this.validTime = validTime;
    }

    @Override
    @NonNull
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", outputName='" + outputName + '\'' +
                ", validTime=" + Arrays.toString(validTime) +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOutputName() {
        return outputName;
    }

    public String[] getValidTime() {
        return validTime;
    }
}
