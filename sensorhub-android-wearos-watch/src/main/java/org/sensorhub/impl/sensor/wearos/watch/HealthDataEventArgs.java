package org.sensorhub.impl.sensor.wearos.watch;

/**
 * This class is used to pass health data from the HealthDataService.
 */
public class HealthDataEventArgs {
    private Integer heartRate = null;
    private Double caloriesDaily = null;
    private Double distanceDaily = null;
    private Long stepsDaily = null;
    private Double floorsDaily = null;
    private Double elevationGainDaily = null;

    /**
     * Set the heart rate.
     *
     * @param heartRate Heart rate in beats per minute.
     */
    public void setHeartRate(double heartRate) {
        this.heartRate = (int) heartRate;
    }

    /**
     * Set the daily calories.
     *
     * @param caloriesDaily Daily calories in kilocalories.
     */
    public void setCaloriesDaily(double caloriesDaily) {
        this.caloriesDaily = caloriesDaily;
    }

    /**
     * Set the daily distance.
     *
     * @param distanceDaily Daily distance in meters.
     */
    public void setDistanceDaily(double distanceDaily) {
        this.distanceDaily = distanceDaily;
    }

    /**
     * Set the daily steps.
     *
     * @param stepsDaily Daily steps.
     */
    public void setStepsDaily(long stepsDaily) {
        this.stepsDaily = stepsDaily;
    }

    /**
     * Set the daily floors.
     *
     * @param floorsDaily Daily floors.
     */
    public void setFloorsDaily(double floorsDaily) {
        this.floorsDaily = floorsDaily;
    }

    /**
     * Set the daily elevation gain.
     *
     * @param elevationGainDaily Daily elevation gain in meters.
     */
    public void setElevationGainDaily(double elevationGainDaily) {
        this.elevationGainDaily = elevationGainDaily;
    }

    /**
     * Get the heart rate.
     *
     * @return Heart rate in beats per minute. Null if not available.
     */
    public Integer getHeartRate() {
        return heartRate;
    }

    /**
     * Get the daily calories.
     *
     * @return Daily calories in kilocalories. Null if not available.
     */
    public Double getCaloriesDaily() {
        return caloriesDaily;
    }

    /**
     * Get the daily distance.
     *
     * @return Daily distance in meters. Null if not available.
     */
    public Double getDistanceDaily() {
        return distanceDaily;
    }

    /**
     * Get the daily steps.
     *
     * @return Daily steps. Null if not available.
     */
    public Long getStepsDaily() {
        return stepsDaily;
    }

    /**
     * Get the daily floors.
     *
     * @return Daily floors. Null if not available.
     */
    public Double getFloorsDaily() {
        return floorsDaily;
    }

    /**
     * Get the daily elevation gain.
     *
     * @return Daily elevation gain in meters. Null if not available.
     */
    public Double getElevationGainDaily() {
        return elevationGainDaily;
    }
}
