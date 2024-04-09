package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Get the data streams from the OpenSensorHub node for a system
     *
     * @param auth     The authentication string
     * @param apiRoot  The API root
     *                 (e.g. "http://localhost:8181/sensorhub/api")
     * @param systemID The ID of the system
     * @return The data streams
     */
    public static List<DataStreamItems> getGetDataStreams(String auth, String apiRoot, String systemID) throws IOException {
        URL url = new URL(apiRoot + "/systems/" + systemID + "/datastreams?f=application%2Fjson");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", auth);
        connection.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        response.append(System.lineSeparator());
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append(System.lineSeparator());
        }
        in.close();
        connection.disconnect();

        DataContainer<DataStreamItems> dataContainer = DataContainer.fromJson(response.toString(), DataStreamItems.class);
        return dataContainer.getItems();
    }
}
