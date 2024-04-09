package org.sensorhub.impl.sensor.wearos.phone.oshdata;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ObservationsItems {
    private final String id;
    private final String phenomenonTime;
    private final String resultTime;
    private final Object result;

    public ObservationsItems(String id, String phenomenonTime, String resultTime, Object result) {
        this.id = id;
        this.phenomenonTime = phenomenonTime;
        this.resultTime = resultTime;
        this.result = result;
    }

    @Override
    @NonNull
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", phenomenonTime='" + phenomenonTime + '\'' +
                ", resultTime='" + resultTime + '\'' +
                ", result=" + result +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getPhenomenonTime() {
        return phenomenonTime;
    }

    public String getResultTime() {
        return resultTime;
    }

    public Object getResult() {
        return result;
    }

    /**
     * Get the observations from the OpenSensorHub node for a data stream
     *
     * @param auth         The authentication string
     * @param apiRoot      The API root
     *                     (e.g. "http://localhost:8181/sensorhub/api")
     * @param dataStreamID The ID of the data stream
     * @return The observations
     */
    public static List<String> getObservationsJSon(String auth, String apiRoot, String dataStreamID) throws IOException {
        List<String> observations = new ArrayList<>();

        URL url = new URL(apiRoot + "/datastreams/" + dataStreamID + "/observations?f=application%2Fjson");
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

        DataContainer<ObservationsItems> dataContainer = DataContainer.fromJson(response.toString(), ObservationsItems.class);
        for (ObservationsItems item : dataContainer.getItems()) {
            observations.add(item.getResult().toString());
        }

        return observations;
    }
}
