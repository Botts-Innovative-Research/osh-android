package org.sensorhub.impl.sensor.wearos.phone.data.systems;

import androidx.annotation.NonNull;

public class Items {
    private final String type;
    private final String id;
    private final Properties properties;

    public Items(String type, String id, Properties properties) {
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

    public Properties getProperties() {
        return properties;
    }
}
