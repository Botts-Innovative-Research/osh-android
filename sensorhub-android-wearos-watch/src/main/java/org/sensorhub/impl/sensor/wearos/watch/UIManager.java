package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * This class is used to manage the UI of the watch app.
 */
public class UIManager {
    MainActivity mainActivity;
    TextView warningTextView;
    TextView heartRateTextView;
    TextView elevationTextView;
    TextView caloriesTextView;
    TextView floorsTextView;
    TextView stepsTextView;
    TextView distanceTextView;
    Button radarButton;

    public UIManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        // Initialize the UI
        mainActivity.setContentView(R.layout.main);
        warningTextView = mainActivity.findViewById(R.id.warning);
        heartRateTextView = mainActivity.findViewById(R.id.heartRate);
        elevationTextView = mainActivity.findViewById(R.id.elevation);
        caloriesTextView = mainActivity.findViewById(R.id.calories);
        floorsTextView = mainActivity.findViewById(R.id.floors);
        stepsTextView = mainActivity.findViewById(R.id.steps);
        distanceTextView = mainActivity.findViewById(R.id.distance);
        radarButton = mainActivity.findViewById(R.id.radarButton);

        // Set the radar button to open the radar activity
        radarButton.setOnClickListener(v -> {
            Intent intent = new Intent(mainActivity, CompassActivity.class);
            mainActivity.startActivity(intent);
        });
    }

    /**
     * Update the UI. This method should be called whenever outputs are toggled or permissions are granted.
     */
    public void refreshUI() {
        setHeartRate();
        setElevation();
        setFloors();
        setSteps();
        setDistance();
        setCalories();
    }

    /**
     * Set the visibility of the warning message.
     *
     * @param visible Whether the warning message should be visible
     */
    public void setWarning(boolean visible) {
        if (visible) {
            warningTextView.setVisibility(View.VISIBLE);
        } else {
            warningTextView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets the heart rate text view based on the current permissions and preferences.
     */
    private void setHeartRate() {
        if (mainActivity.checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableHeartRate(mainActivity)) {
                heartRateTextView.setText(R.string.heartRateDefault);
            }
        } else {
            heartRateTextView.setText(R.string.heartRateNoPermissions);
        }
    }

    /**
     * Sets the heart rate text view with the given value.
     */
    public void setHeartRate(double heartRate) {
        if (PreferencesManager.getEnableHeartRate(mainActivity)) {
            heartRateTextView.setText(mainActivity.getResources().getString(R.string.heartRate, heartRate));
        } else {
            heartRateTextView.setText(R.string.heartRateDefault);
        }
    }

    /**
     * Sets the elevation text view based on the current permissions and preferences.
     */
    private void setElevation() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableElevationGain(mainActivity)) {
                elevationTextView.setText(R.string.elevationDefault);
            }
        } else {
            elevationTextView.setText(R.string.elevationNoPermissions);
        }
    }

    /**
     * Sets the elevation text view with the given value.
     */
    public void setElevation(double elevation) {
        if (PreferencesManager.getEnableElevationGain(mainActivity)) {
            elevationTextView.setText(mainActivity.getResources().getString(R.string.elevation, elevation));
        } else {
            elevationTextView.setText(R.string.elevationDefault);
        }
    }

    /**
     * Sets the floors text view based on the current permissions and preferences.
     */
    private void setFloors() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableFloors(mainActivity)) {
                floorsTextView.setText(R.string.floorsDefault);
            }
        } else {
            floorsTextView.setText(R.string.floorsNoPermissions);
        }
    }

    /**
     * Sets the floors text view with the given value.
     */
    public void setFloors(double floors) {
        if (PreferencesManager.getEnableFloors(mainActivity)) {
            floorsTextView.setText(mainActivity.getResources().getString(R.string.floors, floors));
        } else {
            floorsTextView.setText(R.string.floorsDefault);
        }
    }

    /**
     * Sets the steps text view based on the current permissions and preferences.
     */
    private void setSteps() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableSteps(mainActivity)) {
                stepsTextView.setText(R.string.stepsDefault);
            }
        } else {
            stepsTextView.setText(R.string.stepsNoPermissions);
        }
    }

    /**
     * Sets the steps text view with the given value.
     */
    public void setSteps(long steps) {
        if (PreferencesManager.getEnableSteps(mainActivity)) {
            stepsTextView.setText(mainActivity.getResources().getString(R.string.steps, steps));
        } else {
            stepsTextView.setText(R.string.stepsDefault);
        }
    }

    /**
     * Sets the distance text view based on the current permissions and preferences.
     */
    private void setDistance() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableDistance(mainActivity)) {
                distanceTextView.setText(R.string.distanceDefault);
            }
        } else {
            distanceTextView.setText(R.string.distanceNoPermissions);
        }
    }

    /**
     * Sets the distance text view with the given value.
     */
    public void setDistance(double distance) {
        if (PreferencesManager.getEnableDistance(mainActivity)) {
            distanceTextView.setText(mainActivity.getResources().getString(R.string.distance, distance));
        } else {
            distanceTextView.setText(R.string.distanceDefault);
        }
    }

    /**
     * Sets the calories text view based on the current permissions and preferences.
     */
    private void setCalories() {
        if (mainActivity.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (!PreferencesManager.getEnableCalories(mainActivity)) {
                caloriesTextView.setText(R.string.caloriesDefault);
            }
        } else {
            caloriesTextView.setText(R.string.caloriesNoPermissions);
        }
    }

    /**
     * Sets the calories text view with the given value.
     */
    public void setCalories(double calories) {
        if (PreferencesManager.getEnableCalories(mainActivity)) {
            caloriesTextView.setText(mainActivity.getResources().getString(R.string.calories, calories));
        } else {
            caloriesTextView.setText(R.string.caloriesDefault);
        }
    }
}
