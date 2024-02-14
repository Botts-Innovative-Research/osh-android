package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.TextView;

public class UIManager {
    MainActivity mainActivity;
    TextView warningTextView;
    TextView heartRateTextView;
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
        heartRateTextView = mainActivity.findViewById(R.id.heartRate);
        elevationTextView = mainActivity.findViewById(R.id.elevation);
        caloriesTextView = mainActivity.findViewById(R.id.calories);
        floorsTextView = mainActivity.findViewById(R.id.floors);
        stepsTextView = mainActivity.findViewById(R.id.steps);
        distanceTextView = mainActivity.findViewById(R.id.distance);
    }

    public void refreshUI() {
        setHeartRate();
        setElevation();
        setFloors();
        setSteps();
        setDistance();
        setCalories();
    }

    public void setWarning(boolean visible) {
        if (visible) {
            warningTextView.setVisibility(View.VISIBLE);
        } else {
            warningTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void setHeartRate() {
        if (mainActivity.checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableHeartRate(mainActivity)) {
                heartRateTextView.setText(R.string.heartRateDefault);
            }
        } else {
            heartRateTextView.setText(R.string.heartRateNoPermissions);
        }
    }

    public void setHeartRate(double heartRate) {
        if (PreferencesManager.getEnableHeartRate(mainActivity)) {
            heartRateTextView.setText(mainActivity.getResources().getString(R.string.heartRate, heartRate));
        } else {
            heartRateTextView.setText(R.string.heartRateDefault);
        }
    }

    private void setElevation() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableElevationGain(mainActivity)) {
                elevationTextView.setText(R.string.elevationDefault);
            }
        } else {
            elevationTextView.setText(R.string.elevationNoPermissions);
        }
    }

    public void setElevation(double elevation) {
        if (PreferencesManager.getEnableElevationGain(mainActivity)) {
            elevationTextView.setText(mainActivity.getResources().getString(R.string.elevation, elevation));
        } else {
            elevationTextView.setText(R.string.elevationDefault);
        }
    }

    private void setFloors() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableFloors(mainActivity)) {
                floorsTextView.setText(R.string.floorsDefault);
            }
        } else {
            floorsTextView.setText(R.string.floorsNoPermissions);
        }
    }

    public void setFloors(double floors) {
        if (PreferencesManager.getEnableFloors(mainActivity)) {
            floorsTextView.setText(mainActivity.getResources().getString(R.string.floors, floors));
        } else {
            floorsTextView.setText(R.string.floorsDefault);
        }
    }

    private void setSteps() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableSteps(mainActivity)) {
                stepsTextView.setText(R.string.stepsDefault);
            }
        } else {
            stepsTextView.setText(R.string.stepsNoPermissions);
        }
    }

    public void setSteps(long steps) {
        if (PreferencesManager.getEnableSteps(mainActivity)) {
            stepsTextView.setText(mainActivity.getResources().getString(R.string.steps, steps));
        } else {
            stepsTextView.setText(R.string.stepsDefault);
        }
    }

    private void setDistance() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableDistance(mainActivity)) {
                distanceTextView.setText(R.string.distanceDefault);
            }
        } else {
            distanceTextView.setText(R.string.distanceNoPermissions);
        }
    }

    public void setDistance(double distance) {
        if (PreferencesManager.getEnableDistance(mainActivity)) {
            distanceTextView.setText(mainActivity.getResources().getString(R.string.distance, distance));
        } else {
            distanceTextView.setText(R.string.distanceDefault);
        }
    }

    private void setCalories() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableCalories(mainActivity)) {
                caloriesTextView.setText(R.string.caloriesDefault);
            }
        } else {
            caloriesTextView.setText(R.string.caloriesNoPermissions);
        }
    }

    public void setCalories(double calories) {
        if (PreferencesManager.getEnableCalories(mainActivity)) {
            caloriesTextView.setText(mainActivity.getResources().getString(R.string.calories, calories));
        } else {
            caloriesTextView.setText(R.string.caloriesDefault);
        }
    }
}
