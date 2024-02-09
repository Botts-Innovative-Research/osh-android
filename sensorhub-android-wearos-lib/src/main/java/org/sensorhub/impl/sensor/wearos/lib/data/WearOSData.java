package org.sensorhub.impl.sensor.wearos.lib.data;

import com.google.gson.Gson;

import java.time.Instant;
import java.util.ArrayList;

public class WearOSData {
    private ArrayList<HeartRateData> heartRate;
    private ArrayList<ElevationGainData> elevationGain;
    private ArrayList<ElevationGainData> elevationGainDaily;
    private ArrayList<CaloriesData> calories;
    private ArrayList<CaloriesData> caloriesDaily;
    private ArrayList<FloorsData> floors;
    private ArrayList<FloorsData> floorsDaily;
    private ArrayList<StepsData> steps;
    private ArrayList<StepsData> stepsDaily;
    private ArrayList<DistanceData> distance;
    private ArrayList<DistanceData> distanceDaily;

    public void addHeartRate(Instant timestamp, double heartRate) {
        if (this.heartRate == null) {
            this.heartRate = new ArrayList<>();
        }
        this.heartRate.add(new HeartRateData(timestamp, heartRate));
    }

    public void addElevationGain(Instant startTime, Instant endTime, double elevationGain) {
        if (this.elevationGain == null) {
            this.elevationGain = new ArrayList<>();
        }
        this.elevationGain.add(new ElevationGainData(startTime, endTime, elevationGain));
    }

    public void addElevationGainDaily(Instant startTime, Instant endTime, double elevationGain) {
        if (elevationGainDaily == null) {
            elevationGainDaily = new ArrayList<>();
        }
        elevationGainDaily.add(new ElevationGainData(startTime, endTime, elevationGain));
    }

    public void addCalories(Instant startTime, Instant endTime, double calories) {
        if (this.calories == null) {
            this.calories = new ArrayList<>();
        }
        this.calories.add(new CaloriesData(startTime, endTime, calories));
    }

    public void addCaloriesDaily(Instant startTime, Instant endTime, double calories) {
        if (caloriesDaily == null) {
            caloriesDaily = new ArrayList<>();
        }
        caloriesDaily.add(new CaloriesData(startTime, endTime, calories));
    }

    public void addFloors(Instant startTime, Instant endTime, double floors) {
        if (this.floors == null) {
            this.floors = new ArrayList<>();
        }
        this.floors.add(new FloorsData(startTime, endTime, floors));
    }

    public void addFloorsDaily(Instant startTime, Instant endTime, double floors) {
        if (floorsDaily == null) {
            floorsDaily = new ArrayList<>();
        }
        floorsDaily.add(new FloorsData(startTime, endTime, floors));
    }

    public void addSteps(Instant startTime, Instant endTime, long steps) {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(new StepsData(startTime, endTime, steps));
    }

    public void addStepsDaily(Instant startTime, Instant endTime, long steps) {
        if (stepsDaily == null) {
            stepsDaily = new ArrayList<>();
        }
        stepsDaily.add(new StepsData(startTime, endTime, steps));
    }

    public void addDistance(Instant startTime, Instant endTime, double distance) {
        if (this.distance == null) {
            this.distance = new ArrayList<>();
        }
        this.distance.add(new DistanceData(startTime, endTime, distance));
    }

    public void addDistanceDaily(Instant startTime, Instant endTime, double distance) {
        if (distanceDaily == null) {
            distanceDaily = new ArrayList<>();
        }
        distanceDaily.add(new DistanceData(startTime, endTime, distance));
    }

    public int getHeartRateSize() {
        if (heartRate == null) {
            return 0;
        }
        return heartRate.size();
    }

    public int getElevationGainSize() {
        if (elevationGain == null) {
            return 0;
        }
        return elevationGain.size();
    }

    public int getElevationGainDailySize() {
        if (elevationGainDaily == null) {
            return 0;
        }
        return elevationGainDaily.size();
    }

    public int getCaloriesSize() {
        if (calories == null) {
            return 0;
        }
        return calories.size();
    }

    public int getCaloriesDailySize() {
        if (caloriesDaily == null) {
            return 0;
        }
        return caloriesDaily.size();
    }

    public int getFloorsSize() {
        if (floors == null) {
            return 0;
        }
        return floors.size();
    }

    public int getFloorsDailySize() {
        if (floorsDaily == null) {
            return 0;
        }
        return floorsDaily.size();
    }

    public int getStepsSize() {
        if (steps == null) {
            return 0;
        }
        return steps.size();
    }

    public int getStepsDailySize() {
        if (stepsDaily == null) {
            return 0;
        }
        return stepsDaily.size();
    }

    public int getDistanceSize() {
        if (distance == null) {
            return 0;
        }
        return distance.size();
    }

    public int getDistanceDailySize() {
        if (distanceDaily == null) {
            return 0;
        }
        return distanceDaily.size();
    }

    public HeartRateData getHeartRate(int index) {
        return heartRate.get(index);
    }

    public ElevationGainData getElevationGain(int index) {
        return elevationGain.get(index);
    }

    public ElevationGainData getElevationGainDaily(int index) {
        return elevationGainDaily.get(index);
    }

    public CaloriesData getCalories(int index) {
        return calories.get(index);
    }

    public CaloriesData getCaloriesDaily(int index) {
        return caloriesDaily.get(index);
    }

    public FloorsData getFloors(int index) {
        return floors.get(index);
    }

    public FloorsData getFloorsDaily(int index) {
        return floorsDaily.get(index);
    }

    public StepsData getSteps(int index) {
        return steps.get(index);
    }

    public StepsData getStepsDaily(int index) {
        return stepsDaily.get(index);
    }

    public DistanceData getDistance(int index) {
        return distance.get(index);
    }

    public DistanceData getDistanceDaily(int index) {
        return distanceDaily.get(index);
    }

    public void removeHeartRate(HeartRateData data) {
        if (heartRate != null) {
            heartRate.remove(data);
        }
    }

    public void removeElevationGain(ElevationGainData data) {
        if (elevationGain != null) {
            elevationGain.remove(data);
        }
    }

    public void removeElevationGainDaily(ElevationGainData data) {
        if (elevationGainDaily != null) {
            elevationGainDaily.remove(data);
        }
    }

    public void removeCalories(CaloriesData data) {
        if (calories != null) {
            calories.remove(data);
        }
    }

    public void removeCaloriesDaily(CaloriesData data) {
        if (caloriesDaily != null) {
            caloriesDaily.remove(data);
        }
    }

    public void removeFloors(FloorsData data) {
        if (floors != null) {
            floors.remove(data);
        }
    }

    public void removeFloorsDaily(FloorsData data) {
        if (floorsDaily != null) {
            floorsDaily.remove(data);
        }
    }

    public void removeSteps(StepsData data) {
        if (steps != null) {
            steps.remove(data);
        }
    }

    public void removeStepsDaily(StepsData data) {
        if (stepsDaily != null) {
            stepsDaily.remove(data);
        }
    }

    public void removeDistance(DistanceData data) {
        if (distance != null) {
            distance.remove(data);
        }
    }

    public void removeDistanceDaily(DistanceData data) {
        if (distanceDaily != null) {
            distanceDaily.remove(data);
        }
    }

    public ElevationGainData getMatchingElevationGainDaily(ElevationGainData data) {
        if (elevationGain == null || data == null) {
            return null;
        }
        for (ElevationGainData dataDaily : elevationGainDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    public CaloriesData getMatchingCaloriesDaily(CaloriesData data) {
        if (calories == null || data == null) {
            return null;
        }
        for (CaloriesData dataDaily : caloriesDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    public FloorsData getMatchingFloorsDaily(FloorsData data) {
        if (floors == null || data == null) {
            return null;
        }
        for (FloorsData dataDaily : floorsDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    public StepsData getMatchingStepsDaily(StepsData data) {
        if (steps == null || data == null) {
            return null;
        }
        for (StepsData dataDaily : stepsDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    public DistanceData getMatchingDistanceDaily(DistanceData data) {
        if (distance == null || data == null) {
            return null;
        }
        for (DistanceData dataDaily : distanceDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    public String toJSon() {
        return new Gson().toJson(this);
    }

    public static WearOSData fromJSon(String json) {
        return new Gson().fromJson(json, WearOSData.class);
    }
}
