package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

public class SystemsItems {
    private final String type;
    private final String id;
    private final SystemsProperties properties;

    public SystemsItems(String type, String id, SystemsProperties properties) {
        this.type = type;
        this.id = id;
        this.properties = properties;
    }

    @Override
    @NonNull
    public String toString() {
        return "Items{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", properties=" + properties +
                '}';
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public SystemsProperties getProperties() {
        return properties;
    }
}
