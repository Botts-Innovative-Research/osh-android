package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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

    /**
     * Get the systems from the OpenSensorHub node
     *
     * @param auth    The authentication string
     * @param apiRoot The API root
     *                (e.g. "http://localhost:8181/sensorhub/api")
     * @return The systems
     */
    public static List<SystemsItems> getGetSystems(String auth, String apiRoot) throws IOException {
        URL url = new URL(apiRoot + "/systems?f=application%2Fjson");
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

        DataContainer<SystemsItems> dataContainer = DataContainer.fromJson(response.toString(), SystemsItems.class);
        return dataContainer.getItems();
    }
}
