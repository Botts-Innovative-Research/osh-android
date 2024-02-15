package org.sensorhub.impl.sensor.wearos.lib.data;

import com.google.gson.Gson;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Class for storing data from the WearOS sensor.
 * All data is stored in lists since data is collected in varying intervals.
 */
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

    /**
     * Adds a heart rate data point to the list.
     *
     * @param timestamp The time the data was collected.
     * @param heartRate The heart rate value.
     */
    public void addHeartRate(Instant timestamp, double heartRate) {
        if (this.heartRate == null) {
            this.heartRate = new ArrayList<>();
        }
        this.heartRate.add(new HeartRateData(timestamp, heartRate));
    }

    /**
     * Adds a daily elevation gain data point to the list.
     *
     * @param startTime     The start time of the data collection period.
     * @param endTime       The end time of the data collection period.
     * @param elevationGain The elevation gain value.
     */
    public void addElevationGain(Instant startTime, Instant endTime, double elevationGain) {
        if (this.elevationGain == null) {
            this.elevationGain = new ArrayList<>();
        }
        this.elevationGain.add(new ElevationGainData(startTime, endTime, elevationGain));
    }

    /**
     * Adds a daily elevation gain data point to the list.
     *
     * @param startTime     The start time of the data collection period.
     * @param endTime       The end time of the data collection period.
     * @param elevationGain The elevation gain value.
     */
    public void addElevationGainDaily(Instant startTime, Instant endTime, double elevationGain) {
        if (elevationGainDaily == null) {
            elevationGainDaily = new ArrayList<>();
        }
        elevationGainDaily.add(new ElevationGainData(startTime, endTime, elevationGain));
    }

    /**
     * Adds a calories data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param calories  The calories value.
     */
    public void addCalories(Instant startTime, Instant endTime, double calories) {
        if (this.calories == null) {
            this.calories = new ArrayList<>();
        }
        this.calories.add(new CaloriesData(startTime, endTime, calories));
    }

    /**
     * Adds a daily calories data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param calories  The calories value.
     */
    public void addCaloriesDaily(Instant startTime, Instant endTime, double calories) {
        if (caloriesDaily == null) {
            caloriesDaily = new ArrayList<>();
        }
        caloriesDaily.add(new CaloriesData(startTime, endTime, calories));
    }

    /**
     * Adds a floors data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param floors    The floors value.
     */
    public void addFloors(Instant startTime, Instant endTime, double floors) {
        if (this.floors == null) {
            this.floors = new ArrayList<>();
        }
        this.floors.add(new FloorsData(startTime, endTime, floors));
    }

    /**
     * Adds a daily floors data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param floors    The floors value.
     */
    public void addFloorsDaily(Instant startTime, Instant endTime, double floors) {
        if (floorsDaily == null) {
            floorsDaily = new ArrayList<>();
        }
        floorsDaily.add(new FloorsData(startTime, endTime, floors));
    }

    /**
     * Adds a steps data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param steps     The steps value.
     */
    public void addSteps(Instant startTime, Instant endTime, long steps) {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(new StepsData(startTime, endTime, steps));
    }

    /**
     * Adds a daily steps data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param steps     The steps value.
     */
    public void addStepsDaily(Instant startTime, Instant endTime, long steps) {
        if (stepsDaily == null) {
            stepsDaily = new ArrayList<>();
        }
        stepsDaily.add(new StepsData(startTime, endTime, steps));
    }

    /**
     * Adds a distance data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param distance  The distance value.
     */
    public void addDistance(Instant startTime, Instant endTime, double distance) {
        if (this.distance == null) {
            this.distance = new ArrayList<>();
        }
        this.distance.add(new DistanceData(startTime, endTime, distance));
    }

    /**
     * Adds a daily distance data point to the list.
     *
     * @param startTime The start time of the data collection period.
     * @param endTime   The end time of the data collection period.
     * @param distance  The distance value.
     */
    public void addDistanceDaily(Instant startTime, Instant endTime, double distance) {
        if (distanceDaily == null) {
            distanceDaily = new ArrayList<>();
        }
        distanceDaily.add(new DistanceData(startTime, endTime, distance));
    }

    /**
     * Returns the number of heart rate data points.
     *
     * @return The number of heart rate data points.
     */
    public int getHeartRateSize() {
        if (heartRate == null) {
            return 0;
        }
        return heartRate.size();
    }

    /**
     * Returns the number of elevation gain data points.
     *
     * @return The number of elevation gain data points.
     */
    public int getElevationGainSize() {
        if (elevationGain == null) {
            return 0;
        }
        return elevationGain.size();
    }

    /**
     * Returns the number of daily elevation gain data points.
     *
     * @return The number of daily elevation gain data points.
     */
    public int getElevationGainDailySize() {
        if (elevationGainDaily == null) {
            return 0;
        }
        return elevationGainDaily.size();
    }

    /**
     * Returns the number of calories data points.
     *
     * @return The number of calories data points.
     */
    public int getCaloriesSize() {
        if (calories == null) {
            return 0;
        }
        return calories.size();
    }

    /**
     * Returns the number of daily calories data points.
     *
     * @return The number of daily calories data points.
     */
    public int getCaloriesDailySize() {
        if (caloriesDaily == null) {
            return 0;
        }
        return caloriesDaily.size();
    }

    /**
     * Returns the number of floors data points.
     *
     * @return The number of floors data points.
     */
    public int getFloorsSize() {
        if (floors == null) {
            return 0;
        }
        return floors.size();
    }

    /**
     * Returns the number of daily floors data points.
     *
     * @return The number of daily floors data points.
     */
    public int getFloorsDailySize() {
        if (floorsDaily == null) {
            return 0;
        }
        return floorsDaily.size();
    }

    /**
     * Returns the number of steps data points.
     *
     * @return The number of steps data points.
     */
    public int getStepsSize() {
        if (steps == null) {
            return 0;
        }
        return steps.size();
    }

    /**
     * Returns the number of daily steps data points.
     *
     * @return The number of daily steps data points.
     */
    public int getStepsDailySize() {
        if (stepsDaily == null) {
            return 0;
        }
        return stepsDaily.size();
    }

    /**
     * Returns the number of distance data points.
     *
     * @return The number of distance data points.
     */
    public int getDistanceSize() {
        if (distance == null) {
            return 0;
        }
        return distance.size();
    }

    /**
     * Returns the number of daily distance data points.
     *
     * @return The number of daily distance data points.
     */
    public int getDistanceDailySize() {
        if (distanceDaily == null) {
            return 0;
        }
        return distanceDaily.size();
    }

    /**
     * Returns the heart rate data point at the specified index.
     *
     * @param index The index of the heart rate data point.
     * @return The heart rate data point at the specified index.
     */
    public HeartRateData getHeartRate(int index) {
        return heartRate.get(index);
    }

    /**
     * Returns the elevation gain data point at the specified index.
     *
     * @param index The index of the elevation gain data point.
     * @return The elevation gain data point at the specified index.
     */
    public ElevationGainData getElevationGain(int index) {
        return elevationGain.get(index);
    }

    /**
     * Returns the daily elevation gain data point at the specified index.
     *
     * @param index The index of the daily elevation gain data point.
     * @return The daily elevation gain data point at the specified index.
     */
    public ElevationGainData getElevationGainDaily(int index) {
        return elevationGainDaily.get(index);
    }

    /**
     * Returns the calories data point at the specified index.
     *
     * @param index The index of the calories data point.
     * @return The calories data point at the specified index.
     */
    public CaloriesData getCalories(int index) {
        return calories.get(index);
    }

    /**
     * Returns the daily calories data point at the specified index.
     *
     * @param index The index of the daily calories data point.
     * @return The daily calories data point at the specified index.
     */
    public CaloriesData getCaloriesDaily(int index) {
        return caloriesDaily.get(index);
    }

    /**
     * Returns the floors data point at the specified index.
     *
     * @param index The index of the floors data point.
     * @return The floors data point at the specified index.
     */
    public FloorsData getFloors(int index) {
        return floors.get(index);
    }

    /**
     * Returns the daily floors data point at the specified index.
     *
     * @param index The index of the daily floors data point.
     * @return The daily floors data point at the specified index.
     */
    public FloorsData getFloorsDaily(int index) {
        return floorsDaily.get(index);
    }

    /**
     * Returns the steps data point at the specified index.
     *
     * @param index The index of the steps data point.
     * @return The steps data point at the specified index.
     */
    public StepsData getSteps(int index) {
        return steps.get(index);
    }

    /**
     * Returns the daily steps data point at the specified index.
     *
     * @param index The index of the daily steps data point.
     * @return The daily steps data point at the specified index.
     */
    public StepsData getStepsDaily(int index) {
        return stepsDaily.get(index);
    }

    /**
     * Returns the distance data point at the specified index.
     *
     * @param index The index of the distance data point.
     * @return The distance data point at the specified index.
     */
    public DistanceData getDistance(int index) {
        return distance.get(index);
    }

    /**
     * Returns the daily distance data point at the specified index.
     *
     * @param index The index of the daily distance data point.
     * @return The daily distance data point at the specified index.
     */
    public DistanceData getDistanceDaily(int index) {
        return distanceDaily.get(index);
    }

    /**
     * Removes the specified heart rate data point from the list.
     *
     * @param data The heart rate data point to remove.
     */
    public void removeHeartRate(HeartRateData data) {
        if (heartRate != null) {
            heartRate.remove(data);
        }
    }

    /**
     * Removes the specified elevation gain data point from the list.
     *
     * @param data The elevation gain data point to remove.
     */
    public void removeElevationGain(ElevationGainData data) {
        if (elevationGain != null) {
            elevationGain.remove(data);
        }
    }

    /**
     * Removes the specified daily elevation gain data point from the list.
     *
     * @param data The daily elevation gain data point to remove.
     */
    public void removeElevationGainDaily(ElevationGainData data) {
        if (elevationGainDaily != null) {
            elevationGainDaily.remove(data);
        }
    }

    /**
     * Removes the specified calories data point from the list.
     *
     * @param data The calories data point to remove.
     */
    public void removeCalories(CaloriesData data) {
        if (calories != null) {
            calories.remove(data);
        }
    }

    /**
     * Removes the specified daily calories data point from the list.
     *
     * @param data The daily calories data point to remove.
     */
    public void removeCaloriesDaily(CaloriesData data) {
        if (caloriesDaily != null) {
            caloriesDaily.remove(data);
        }
    }

    /**
     * Removes the specified floors data point from the list.
     *
     * @param data The floors data point to remove.
     */
    public void removeFloors(FloorsData data) {
        if (floors != null) {
            floors.remove(data);
        }
    }

    /**
     * Removes the specified daily floors data point from the list.
     *
     * @param data The daily floors data point to remove.
     */
    public void removeFloorsDaily(FloorsData data) {
        if (floorsDaily != null) {
            floorsDaily.remove(data);
        }
    }

    /**
     * Removes the specified steps data point from the list.
     *
     * @param data The steps data point to remove.
     */
    public void removeSteps(StepsData data) {
        if (steps != null) {
            steps.remove(data);
        }
    }

    /**
     * Removes the specified daily steps data point from the list.
     *
     * @param data The daily steps data point to remove.
     */
    public void removeStepsDaily(StepsData data) {
        if (stepsDaily != null) {
            stepsDaily.remove(data);
        }
    }

    /**
     * Removes the specified distance data point from the list.
     *
     * @param data The distance data point to remove.
     */
    public void removeDistance(DistanceData data) {
        if (distance != null) {
            distance.remove(data);
        }
    }

    /**
     * Removes the specified daily distance data point from the list.
     *
     * @param data The daily distance data point to remove.
     */
    public void removeDistanceDaily(DistanceData data) {
        if (distanceDaily != null) {
            distanceDaily.remove(data);
        }
    }

    /**
     * Returns the daily elevation gain data point that shares the same end time as the specified data point.
     *
     * @param data The elevation gain data point to match.
     * @return The matching daily elevation gain data point.
     */
    public ElevationGainData getMatchingElevationGainDaily(ElevationGainData data) {
        if (elevationGainDaily == null || data == null) {
            return null;
        }
        for (ElevationGainData dataDaily : elevationGainDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    /**
     * Returns the daily calories data point that shares the same end time as the specified data point.
     *
     * @param data The calories data point to match.
     * @return The matching daily calories data point.
     */
    public CaloriesData getMatchingCaloriesDaily(CaloriesData data) {
        if (caloriesDaily == null || data == null) {
            return null;
        }
        for (CaloriesData dataDaily : caloriesDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    /**
     * Returns the daily floors data point that shares the same end time as the specified data point.
     *
     * @param data The floors data point to match.
     * @return The matching daily floors data point.
     */
    public FloorsData getMatchingFloorsDaily(FloorsData data) {
        if (floorsDaily == null || data == null) {
            return null;
        }
        for (FloorsData dataDaily : floorsDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    /**
     * Returns the daily steps data point that shares the same end time as the specified data point.
     *
     * @param data The steps data point to match.
     * @return The matching daily steps data point.
     */
    public StepsData getMatchingStepsDaily(StepsData data) {
        if (stepsDaily == null || data == null) {
            return null;
        }
        for (StepsData dataDaily : stepsDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    /**
     * Returns the daily distance data point that shares the same end time as the specified data point.
     *
     * @param data The distance data point to match.
     * @return The matching daily distance data point.
     */
    public DistanceData getMatchingDistanceDaily(DistanceData data) {
        if (distanceDaily == null || data == null) {
            return null;
        }
        for (DistanceData dataDaily : distanceDaily) {
            if (dataDaily.getEndTime() == data.getEndTime()) {
                return dataDaily;
            }
        }
        return null;
    }

    /**
     * Serializes the data to a JSON string.
     *
     * @return The JSON string.
     */
    public String toJSon() {
        return new Gson().toJson(this);
    }

    /**
     * Deserializes the data from a JSON string.
     *
     * @param json The JSON string.
     * @return The WearOSData object.
     */
    public static WearOSData fromJSon(String json) {
        return new Gson().fromJson(json, WearOSData.class);
    }
}
