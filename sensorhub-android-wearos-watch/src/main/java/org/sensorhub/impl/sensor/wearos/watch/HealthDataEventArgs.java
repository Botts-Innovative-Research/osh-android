package org.sensorhub.impl.sensor.wearos.watch;

public class HealthDataEventArgs {
    private Integer heartRate = null;
    private Double caloriesDaily = null;
    private Double distanceDaily = null;
    private Long stepsDaily = null;
    private Double floorsDaily = null;
    private Double elevationGainDaily = null;

    public void setHeartRate(double heartRate) {
        this.heartRate = (int) heartRate;
    }

    public void setCaloriesDaily(double caloriesDaily) {
        this.caloriesDaily = caloriesDaily;
    }

    public void setDistanceDaily(double distanceDaily) {
        this.distanceDaily = distanceDaily;
    }

    public void setStepsDaily(long stepsDaily) {
        this.stepsDaily = stepsDaily;
    }

    public void setFloorsDaily(double floorsDaily) {
        this.floorsDaily = floorsDaily;
    }

    public void setElevationGainDaily(double elevationGainDaily) {
        this.elevationGainDaily = elevationGainDaily;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public Double getCaloriesDaily() {
        return caloriesDaily;
    }

    public Double getDistanceDaily() {
        return distanceDaily;
    }

    public Long getStepsDaily() {
        return stepsDaily;
    }

    public Double getFloorsDaily() {
        return floorsDaily;
    }

    public Double getElevationGainDaily() {
        return elevationGainDaily;
    }
}
