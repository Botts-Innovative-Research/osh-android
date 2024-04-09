package org.sensorhub.impl.sensor.wearos.phone.data.systems;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class Properties {
    private final String uid;
    private final String featureType;
    private final String name;
    private final String[] validTime;

    public Properties(String uid, String featureType, String name, String[] validTime) {
        this.uid = uid;
        this.featureType = featureType;
        this.name = name;
        this.validTime = validTime;
    }

    @Override
    @NonNull
    public String toString() {
        return "Properties{" +
                "uid='" + uid + '\'' +
                ", featureType='" + featureType + '\'' +
                ", name='" + name + '\'' +
                ", validTime=" + Arrays.toString(validTime) +
                '}';
    }

    public String getUid() {
        return uid;
    }

    public String getFeatureType() {
        return featureType;
    }

    public String getName() {
        return name;
    }

    public String[] getValidTime() {
        return validTime;
    }
}
