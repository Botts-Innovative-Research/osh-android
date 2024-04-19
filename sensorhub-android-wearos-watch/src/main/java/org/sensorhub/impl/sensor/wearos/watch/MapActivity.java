package org.sensorhub.impl.sensor.wearos.watch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSData;

public class MapActivity extends FragmentActivity implements LocationListener {
    private static final String TAG = MapActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;
    private final AutoResetValue<Boolean> userMovedCamera = new AutoResetValue<>(false, 1000);
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private MarkerManager markerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(onMapReadyCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);

        Wearable.getMessageClient(this).addListener(messageListener);

        HandlerThread eventThread = new HandlerThread("LocationWatcher");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 1000, 0.0f, this, eventHandler.getLooper());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);

        Wearable.getMessageClient(this).removeListener(messageListener);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        updateLocationUI();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (googleMap == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        runOnUiThread(() -> googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)));
    }

    /**
     * Stops the map from rotating when the user interacts with it.
     */
    private final GoogleMap.OnCameraMoveStartedListener onCameraMoveStartedListener = reason -> {
        if (reason != GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
            userMovedCamera.setValue(true);
        }
    };

    /**
     * Stops the map from rotating when the user interacts with it.
     */
    private final GoogleMap.OnCameraMoveListener onCameraMoveListener = () -> {
        if (Boolean.TRUE.equals(userMovedCamera.getValue())) {
            userMovedCamera.setValue(true);
        }
    };

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    private final OnMapReadyCallback onMapReadyCallback = gMap -> {
        this.googleMap = gMap;
        googleMap.setOnCameraMoveStartedListener(onCameraMoveStartedListener);
        googleMap.setOnCameraMoveListener(onCameraMoveListener);
        markerManager = new MarkerManager(googleMap);

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    };

    /**
     * Listener for messages from the Android phone.
     * Updates the map with the GPS data points received.
     */
    private final MessageClient.OnMessageReceivedListener messageListener = messageEvent -> {
        if (messageEvent.getPath().equals(Constants.GPS_DATA_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);
            GPSData gpsData = GPSData.fromJson(message);
            markerManager.updateMarkers(gpsData.getPoints());
        }
    };

    /**
     * Listener for the rotation sensor.
     * Updates the map's camera bearing based on the device's orientation.
     */
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Boolean.TRUE.equals(userMovedCamera.getValue())) return;
            if (googleMap == null) return;

            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                float[] orientationValues = new float[3];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                float bearing = (float) Math.toDegrees(orientationValues[0]);

                CameraPosition oldPos = googleMap.getCameraPosition();
                CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }
    };

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        // The result of the permission request is handled by a callback, onRequestPermissionsResult.
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (googleMap == null) return;

        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        // Get the best and most recent location of the device,
        // which may be null in rare cases when a location is not available.
        try {
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Set the map's camera position to the current location of the device.
                    Location lastKnownLocation = task.getResult();
                    if (lastKnownLocation != null) {
                        LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    }
                }
            });
        } catch (SecurityException e) {
            if (e.getMessage() != null) Log.e(TAG, e.getMessage());
        }
    }
}