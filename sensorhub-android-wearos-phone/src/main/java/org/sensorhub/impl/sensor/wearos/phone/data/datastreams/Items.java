package org.sensorhub.impl.sensor.wearos.phone.data.datastreams;

public class Items {
    private final String id;
    private final String name;
    private final String outputName;
    private final String[] validTime;

    public Items(String id, String name, String outputName, String[] validTime) {
        this.id = id;
        this.name = name;
        this.outputName = outputName;
        this.validTime = validTime;
    }

    @Override
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", outputName='" + outputName + '\'' +
                ", validTime=" + validTime +
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
