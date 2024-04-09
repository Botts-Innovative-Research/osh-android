package org.sensorhub.impl.sensor.wearos.phone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.data.CaloriesData;
import org.sensorhub.impl.sensor.wearos.lib.data.DistanceData;
import org.sensorhub.impl.sensor.wearos.lib.data.ElevationGainData;
import org.sensorhub.impl.sensor.wearos.lib.data.FloorsData;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSAndroidLocationResult;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSData;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSDataPoint;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSFixedLocationResult;
import org.sensorhub.impl.sensor.wearos.lib.data.StepsData;
import org.sensorhub.impl.sensor.wearos.lib.data.WearOSData;
import org.sensorhub.impl.sensor.wearos.phone.oshdata.DataStreamItems;
import org.sensorhub.impl.sensor.wearos.phone.oshdata.ObservationsItems;
import org.sensorhub.impl.sensor.wearos.phone.oshdata.SystemsItems;
import org.sensorhub.impl.sensor.wearos.phone.output.CaloriesOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.DistanceOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.ElevationGainOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.FloorsOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.HeartRateOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.StepsOutput;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * The Wear OS driver module
 */
public class WearOSDriver extends AbstractSensorModule<WearOSConfig> implements MessageClient.OnMessageReceivedListener, LocationListener {
    private HeartRateOutput heartRateOutput;
    private ElevationGainOutput elevationGainOutput;
    private CaloriesOutput caloriesOutput;
    private FloorsOutput floorsOutput;
    private StepsOutput stepsOutput;
    private DistanceOutput distanceOutput;
    private Context context;
    private double latitude;
    private double longitude;
    private List<GPSDataPoint> points = new ArrayList<>();
    private String auth;

    @Override
    public void doInit() {
        generateUniqueID("urn:rsi:wearos:", config.getDeviceName());
        generateXmlID("wear-os_", config.getDeviceName());

        createOutputs();
        context = SensorHubService.getContext();
    }

    @Override
    public void doStart() {
        Wearable.getMessageClient(context).addListener(this);

        HandlerThread eventThread = new HandlerThread("LocationWatcher");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, this, eventHandler.getLooper());
        }

        broadcastEnabledOutputs();

        if (config.gpsDataLocation.user != null && config.gpsDataLocation.password != null) {
            auth = "Basic " + Base64.getEncoder().encodeToString((config.gpsDataLocation.user + ":" + config.gpsDataLocation.password).getBytes());
        }

        dataRequestThread.start();
    }

    @Override
    public void doStop() {
        Wearable.getMessageClient(context).removeListener(this);
        dataRequestThread.interrupt();
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    /**
     * Called when a message is received from the Wear OS device
     *
     * @param messageEvent The message received
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.DATA_PATH)) {
            // Parse the data and set the outputs
            WearOSData data = WearOSData.fromJson(new String(messageEvent.getData()));
            if (data != null) {
                setHeartRateData(data);
                setElevationGainData(data);
                setCaloriesData(data);
                setFloorsData(data);
                setStepsData(data);
                setDistanceData(data);
            }

            // Send a confirmation message
            Wearable.getMessageClient(context).sendMessage(messageEvent.getSourceNodeId(), Constants.CONFIRMATION_PATH, "Received".getBytes(StandardCharsets.UTF_8));
        } else if (messageEvent.getPath().equals(Constants.OUTPUTS_PATH)) {
            // Send the enabled outputs to the Wear OS device
            broadcastEnabledOutputs();
        }
    }

    /**
     * Thread to request GPS data from an OpenSensorHub node
     */
    Thread dataRequestThread = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(1000);
                points.clear();
                points.addAll(getFixedLocationData());
                points.addAll(getAndroidLocationData());
                sendGPSData();
            } catch (Exception e) {
                logger.error("Error", e);
            }
        }
    });

    /**
     * Send the GPS data to the Wear OS device
     */
    public void sendGPSData() {
        GPSData gpsData = new GPSData(latitude, longitude, points);
        Task<List<Node>> nodesTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(context).sendMessage(node.getId(), Constants.GPS_DATA_PATH, gpsData.toJson().getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    /**
     * Create the outputs based on the configuration
     */
    public void createOutputs() {
        if (config.getOutputs().getEnableHeartRate()) {
            heartRateOutput = new HeartRateOutput(this);
            heartRateOutput.doInit();
            addOutput(heartRateOutput, false);
        } else {
            heartRateOutput = null;
        }

        if (config.getOutputs().getEnableElevationGain()) {
            elevationGainOutput = new ElevationGainOutput(this);
            elevationGainOutput.doInit();
            addOutput(elevationGainOutput, false);
        } else {
            elevationGainOutput = null;
        }

        if (config.getOutputs().getEnableCalories()) {
            caloriesOutput = new CaloriesOutput(this);
            caloriesOutput.doInit();
            addOutput(caloriesOutput, false);
        } else {
            caloriesOutput = null;
        }

        if (config.getOutputs().getEnableFloors()) {
            floorsOutput = new FloorsOutput(this);
            floorsOutput.doInit();
            addOutput(floorsOutput, false);
        } else {
            floorsOutput = null;
        }

        if (config.getOutputs().getEnableSteps()) {
            stepsOutput = new StepsOutput(this);
            stepsOutput.doInit();
            addOutput(stepsOutput, false);
        } else {
            stepsOutput = null;
        }

        if (config.getOutputs().getEnableDistance()) {
            distanceOutput = new DistanceOutput(this);
            distanceOutput.doInit();
            addOutput(distanceOutput, false);
        } else {
            distanceOutput = null;
        }
    }

    /**
     * Broadcast the enabled outputs to the Wear OS device
     */
    public void broadcastEnabledOutputs() {
        Task<List<Node>> nodesTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(context).sendMessage(node.getId(), Constants.OUTPUTS_PATH, config.getOutputs().toJson().getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    /**
     * Parse the heart rate data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setHeartRateData(@NonNull WearOSData data) {
        if (heartRateOutput == null) {
            return;
        }

        for (int i = 0; i < data.getHeartRateSize(); i++) {
            heartRateOutput.setData(data.getHeartRate(i).getTimestamp(), data.getHeartRate(i).getValue());
        }
    }

    /**
     * Parse the elevation gain data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setElevationGainData(@NonNull WearOSData data) {
        if (elevationGainOutput == null) {
            return;
        }

        for (int i = 0; i < data.getElevationGainSize(); i++) {
            ElevationGainData elevationGain = data.getElevationGain(i);
            ElevationGainData elevationGainDaily = data.getMatchingElevationGainDaily(elevationGain);
            if (elevationGainDaily == null) {
                elevationGainOutput.setData(elevationGain.getStartTime(), elevationGain.getEndTime(), elevationGain.getValue(), 0, 0, 0);
            } else {
                elevationGainOutput.setData(elevationGain.getStartTime(), elevationGain.getEndTime(), elevationGain.getValue(), elevationGainDaily.getStartTime(), elevationGainDaily.getEndTime(), elevationGainDaily.getValue());
                data.removeElevationGainDaily(elevationGainDaily);
            }
        }
        for (int i = 0; i < data.getElevationGainDailySize(); i++) {
            elevationGainOutput.setData(0, 0, 0, data.getElevationGainDaily(i).getStartTime(), data.getElevationGainDaily(i).getEndTime(), data.getElevationGainDaily(i).getValue());
        }
    }

    /**
     * Parse the calories data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setCaloriesData(@NonNull WearOSData data) {
        if (caloriesOutput == null) {
            return;
        }

        for (int i = 0; i < data.getCaloriesSize(); i++) {
            CaloriesData calories = data.getCalories(i);
            CaloriesData caloriesDaily = data.getMatchingCaloriesDaily(calories);
            if (caloriesDaily == null) {
                caloriesOutput.setData(calories.getStartTime(), calories.getEndTime(), calories.getValue(), 0, 0, 0);
            } else {
                caloriesOutput.setData(calories.getStartTime(), calories.getEndTime(), calories.getValue(), caloriesDaily.getStartTime(), caloriesDaily.getEndTime(), caloriesDaily.getValue());
                data.removeCaloriesDaily(caloriesDaily);
            }
        }
        for (int i = 0; i < data.getCaloriesDailySize(); i++) {
            caloriesOutput.setData(0, 0, 0, data.getCaloriesDaily(i).getStartTime(), data.getCaloriesDaily(i).getEndTime(), data.getCaloriesDaily(i).getValue());
        }
    }

    /**
     * Parse the floors data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setFloorsData(@NonNull WearOSData data) {
        if (floorsOutput == null) {
            return;
        }

        for (int i = 0; i < data.getFloorsSize(); i++) {
            FloorsData floors = data.getFloors(i);
            FloorsData floorsDaily = data.getMatchingFloorsDaily(floors);
            if (floorsDaily == null) {
                floorsOutput.setData(floors.getStartTime(), floors.getEndTime(), floors.getValue(), 0, 0, 0);
            } else {
                floorsOutput.setData(floors.getStartTime(), floors.getEndTime(), floors.getValue(), floorsDaily.getStartTime(), floorsDaily.getEndTime(), floorsDaily.getValue());
                data.removeFloorsDaily(floorsDaily);
            }
        }
        for (int i = 0; i < data.getFloorsDailySize(); i++) {
            floorsOutput.setData(0, 0, 0, data.getFloorsDaily(i).getStartTime(), data.getFloorsDaily(i).getEndTime(), data.getFloorsDaily(i).getValue());
        }
    }

    /**
     * Parse the steps data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setStepsData(@NonNull WearOSData data) {
        if (stepsOutput == null) {
            return;
        }

        for (int i = 0; i < data.getStepsSize(); i++) {
            StepsData steps = data.getSteps(i);
            StepsData stepsDaily = data.getMatchingStepsDaily(steps);
            if (stepsDaily == null) {
                stepsOutput.setData(steps.getStartTime(), steps.getEndTime(), steps.getValue(), 0, 0, 0);
            } else {
                stepsOutput.setData(steps.getStartTime(), steps.getEndTime(), steps.getValue(), stepsDaily.getStartTime(), stepsDaily.getEndTime(), stepsDaily.getValue());
                data.removeStepsDaily(stepsDaily);
            }
        }
        for (int i = 0; i < data.getStepsDailySize(); i++) {
            stepsOutput.setData(0, 0, 0, data.getStepsDaily(i).getStartTime(), data.getStepsDaily(i).getEndTime(), data.getStepsDaily(i).getValue());
        }
    }

    /**
     * Parse the distance data and set the output
     *
     * @param data The data from the Wear OS device
     */
    public void setDistanceData(@NonNull WearOSData data) {
        if (distanceOutput == null) {
            return;
        }

        for (int i = 0; i < data.getDistanceSize(); i++) {
            DistanceData distance = data.getDistance(i);
            DistanceData distanceDaily = data.getMatchingDistanceDaily(distance);
            if (distanceDaily == null) {
                distanceOutput.setData(distance.getStartTime(), distance.getEndTime(), distance.getValue(), 0, 0, 0);
            } else {
                distanceOutput.setData(distance.getStartTime(), distance.getEndTime(), distance.getValue(), distanceDaily.getStartTime(), distanceDaily.getEndTime(), distanceDaily.getValue());
                data.removeDistanceDaily(distanceDaily);
            }
        }
        for (int i = 0; i < data.getDistanceDailySize(); i++) {
            distanceOutput.setData(0, 0, 0, data.getDistanceDaily(i).getStartTime(), data.getDistanceDaily(i).getEndTime(), data.getDistanceDaily(i).getValue());
        }
    }

    /**
     * Called when the location has changed
     *
     * @param location The new location
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        sendGPSData();
    }

    /**
     * Get the Android location data from the OpenSensorHub node
     *
     * @return The Android location data
     */
    private List<GPSDataPoint> getAndroidLocationData() {
        List<GPSDataPoint> locationData = new ArrayList<>();

        try {
            List<SystemsItems> items = SystemsItems.getGetSystems(auth, config.gpsDataLocation.gpsHost);
            for (SystemsItems item : items) {
                if (item.getProperties().getUid().startsWith("urn:android:device:") && !Objects.equals(item.getProperties().getUid(), "urn:android:device:" + config.getDeviceID())) {
                    List<DataStreamItems> dataStreams = DataStreamItems.getGetDataStreams(auth, config.gpsDataLocation.gpsHost, item.getId());
                    for (DataStreamItems dataStream : dataStreams) {
                        if (dataStream.getOutputName().equals("gps_data")) {
                            List<String> observations = ObservationsItems.getObservationsJSon(auth, config.gpsDataLocation.gpsHost, dataStream.getId());
                            for (String observation : observations) {
                                GPSAndroidLocationResult data = GPSAndroidLocationResult.fromJson(observation);
                                locationData.add(new GPSDataPoint(data.getLat(), data.getLon(), "blue"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getAndroidLocationData", e);
        }

        return locationData;
    }

    /**
     * Get the fixed location data from the OpenSensorHub node
     *
     * @return The fixed location data
     */
    private List<GPSDataPoint> getFixedLocationData() {
        List<GPSDataPoint> locationData = new ArrayList<>();

        try {
            List<SystemsItems> items = SystemsItems.getGetSystems(auth, config.gpsDataLocation.gpsHost);
            for (SystemsItems item : items) {
                if (item.getProperties().getUid().startsWith("wearos_gps_data_")) {
                    List<DataStreamItems> dataStreams = DataStreamItems.getGetDataStreams(auth, config.gpsDataLocation.gpsHost, item.getId());
                    for (DataStreamItems dataStream : dataStreams) {
                        if (dataStream.getOutputName().equals("GPSDataOutput")) {
                            List<String> observations = ObservationsItems.getObservationsJSon(auth, config.gpsDataLocation.gpsHost, dataStream.getId());
                            for (String observation : observations) {
                                GPSFixedLocationResult data = GPSFixedLocationResult.fromJson(observation);
                                locationData.addAll(data.getGpsDataPoint());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getFixedLocationData", e);
        }

        return locationData;
    }
}
