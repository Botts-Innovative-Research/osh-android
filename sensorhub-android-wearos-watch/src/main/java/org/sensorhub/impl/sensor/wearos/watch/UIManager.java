package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.TextView;

import androidx.health.services.client.data.UserActivityState;

public class UIManager {
    MainActivity mainActivity;
    TextView warningTextView;
    TextView heartRateTextView;
    TextView activityStateTextView;
    TextView elevationTextView;
    TextView caloriesTextView;
    TextView floorsTextView;
    TextView stepsTextView;
    TextView distanceTextView;

    public UIManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void init() {
        // Initialize the UI
        mainActivity.setContentView(R.layout.main);
        warningTextView = mainActivity.findViewById(R.id.warning);
        activityStateTextView = mainActivity.findViewById(R.id.activityState);
        heartRateTextView = mainActivity.findViewById(R.id.heartRate);
        elevationTextView = mainActivity.findViewById(R.id.elevation);
        caloriesTextView = mainActivity.findViewById(R.id.calories);
        floorsTextView = mainActivity.findViewById(R.id.floors);
        stepsTextView = mainActivity.findViewById(R.id.steps);
        distanceTextView = mainActivity.findViewById(R.id.distance);
    }

    public void refreshUI() {
        boolean bodySensorPermission = mainActivity.checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
        boolean activityPermission = mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;

        if (bodySensorPermission) {
            if (!mainActivity.getOutputs().getEnableHeartRate()) {
                heartRateTextView.setText(R.string.heartRateDefault);
            }
        } else {
            heartRateTextView.setText(R.string.heartRateNoPermissions);
        }

        if (activityPermission) {
            if (!mainActivity.getOutputs().getEnableElevationGain()) {
                elevationTextView.setText(R.string.elevationDefault);
            }
            if (!mainActivity.getOutputs().getEnableCalories()) {
                caloriesTextView.setText(R.string.caloriesDefault);
            }
            if (!mainActivity.getOutputs().getEnableFloors()) {
                floorsTextView.setText(R.string.floorsDefault);
            }
            if (!mainActivity.getOutputs().getEnableSteps()) {
                stepsTextView.setText(R.string.stepsDefault);
            }
            if (!mainActivity.getOutputs().getEnableDistance()) {
                distanceTextView.setText(R.string.distanceDefault);
            }
        } else {
            activityStateTextView.setText(R.string.activityStateNoPermissions);
            elevationTextView.setText(R.string.elevationNoPermissions);
            caloriesTextView.setText(R.string.caloriesNoPermissions);
            floorsTextView.setText(R.string.floorsNoPermissions);
            stepsTextView.setText(R.string.stepsNoPermissions);
            distanceTextView.setText(R.string.distanceNoPermissions);
        }
    }

    public void setWarning(boolean visible) {
        if (visible) {
            warningTextView.setVisibility(View.VISIBLE);
        } else {
            warningTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void setHeartRate(double heartRate) {
        if (mainActivity.getOutputs().getEnableHeartRate()) {
            heartRateTextView.setText(mainActivity.getResources().getString(R.string.heartRate, heartRate));
        } else {
            heartRateTextView.setText(R.string.heartRateDefault);
        }
    }

    public void setActivityState(UserActivityState activityState) {
        if (activityState == UserActivityState.USER_ACTIVITY_PASSIVE) {
            activityStateTextView.setText(R.string.activityStatePassive);
        } else if (activityState == UserActivityState.USER_ACTIVITY_ASLEEP) {
            activityStateTextView.setText(R.string.activityStateAsleep);
        } else if (activityState == UserActivityState.USER_ACTIVITY_EXERCISE) {
            activityStateTextView.setText(R.string.activityStateExercise);
        } else {
            activityStateTextView.setText(R.string.activityStateUnknown);
        }
    }

    public void setElevation(double elevation) {
        if (mainActivity.getOutputs().getEnableElevationGain()) {
            elevationTextView.setText(mainActivity.getResources().getString(R.string.elevation, elevation));
        } else {
            elevationTextView.setText(R.string.elevationDefault);
        }
    }

    public void setFloors(double floors) {
        if (mainActivity.getOutputs().getEnableFloors()) {
            floorsTextView.setText(mainActivity.getResources().getString(R.string.floors, floors));
        } else {
            floorsTextView.setText(R.string.floorsDefault);
        }
    }

    public void setSteps(long steps) {
        if (mainActivity.getOutputs().getEnableSteps()) {
            stepsTextView.setText(mainActivity.getResources().getString(R.string.steps, steps));
        } else {
            stepsTextView.setText(R.string.stepsDefault);
        }
    }

    public void setDistance(double distance) {
        if (mainActivity.getOutputs().getEnableDistance()) {
            distanceTextView.setText(mainActivity.getResources().getString(R.string.distance, distance));
        } else {
            distanceTextView.setText(R.string.distanceDefault);
        }
    }

    public void setCalories(double calories) {
        if (mainActivity.getOutputs().getEnableCalories()) {
            caloriesTextView.setText(mainActivity.getResources().getString(R.string.calories, calories));
        } else {
            caloriesTextView.setText(R.string.caloriesDefault);
        }
    }
}
