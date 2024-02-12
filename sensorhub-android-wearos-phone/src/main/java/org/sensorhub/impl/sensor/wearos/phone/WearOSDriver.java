package org.sensorhub.impl.sensor.wearos.phone;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.data.CaloriesData;
import org.sensorhub.impl.sensor.wearos.lib.data.DistanceData;
import org.sensorhub.impl.sensor.wearos.lib.data.ElevationGainData;
import org.sensorhub.impl.sensor.wearos.lib.data.FloorsData;
import org.sensorhub.impl.sensor.wearos.lib.data.StepsData;
import org.sensorhub.impl.sensor.wearos.lib.data.WearOSData;
import org.sensorhub.impl.sensor.wearos.phone.output.CaloriesOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.DistanceOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.ElevationGainOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.FloorsOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.HeartRateOutput;
import org.sensorhub.impl.sensor.wearos.phone.output.StepsOutput;

import java.nio.charset.StandardCharsets;

public class WearOSDriver extends AbstractSensorModule<WearOSConfig> implements MessageClient.OnMessageReceivedListener {
    private HeartRateOutput heartRateOutput;
    private ElevationGainOutput elevationGainOutput;
    private CaloriesOutput caloriesOutput;
    private FloorsOutput floorsOutput;
    private StepsOutput stepsOutput;
    private DistanceOutput distanceOutput;
    private Context context;

    @Override
    public boolean isConnected() {
        return true;
    }

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
    }

    @Override
    public void doStop() {
        Wearable.getMessageClient(context).removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.DATA_PATH)) {
            WearOSData data = WearOSData.fromJSon(new String(messageEvent.getData()));
            if (data != null) {
                setHeartRateData(data);
                setElevationGainData(data);
                setCaloriesData(data);
                setFloorsData(data);
                setStepsData(data);
                setDistanceData(data);
            }

            Wearable.getMessageClient(context).sendMessage(messageEvent.getSourceNodeId(), Constants.CONFIRMATION_PATH, "Received".getBytes(StandardCharsets.UTF_8));
        }
    }

    public void createOutputs() {
        if (config.getEnableHeartRate()) {
            heartRateOutput = new HeartRateOutput(this);
            heartRateOutput.doInit();
            addOutput(heartRateOutput, false);
        } else {
            heartRateOutput = null;
        }

        if (config.getEnableElevationGain()) {
            elevationGainOutput = new ElevationGainOutput(this);
            elevationGainOutput.doInit();
            addOutput(elevationGainOutput, false);
        } else {
            elevationGainOutput = null;
        }

        if (config.getEnableCalories()) {
            caloriesOutput = new CaloriesOutput(this);
            caloriesOutput.doInit();
            addOutput(caloriesOutput, false);
        } else {
            caloriesOutput = null;
        }

        if (config.getEnableFloors()) {
            floorsOutput = new FloorsOutput(this);
            floorsOutput.doInit();
            addOutput(floorsOutput, false);
        } else {
            floorsOutput = null;
        }

        if (config.getEnableSteps()) {
            stepsOutput = new StepsOutput(this);
            stepsOutput.doInit();
            addOutput(stepsOutput, false);
        } else {
            stepsOutput = null;
        }

        if (config.getEnableDistance()) {
            distanceOutput = new DistanceOutput(this);
            distanceOutput.doInit();
            addOutput(distanceOutput, false);
        } else {
            distanceOutput = null;
        }
    }

    public void setHeartRateData(@NonNull WearOSData data) {
        if (heartRateOutput == null) {
            return;
        }

        for (int i = 0; i < data.getHeartRateSize(); i++) {
            heartRateOutput.setData(data.getHeartRate(i).getTimestamp(), data.getHeartRate(i).getValue());
        }
    }

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
}
