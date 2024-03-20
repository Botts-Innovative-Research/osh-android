package org.sensorhub.impl.sensor.wearos.watch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.sensorhub.impl.sensor.wearos.lib.Constants;
import org.sensorhub.impl.sensor.wearos.lib.data.GPSData;

import java.util.Map;

public class CompassActivity extends Activity implements MessageClient.OnMessageReceivedListener {
    ImageView compassImageView;
    TextView compassTextView;
    int azimuth;
    boolean isZooming = false;
    float startDistance;
    float startDistancePerPixel = 1;
    float distancePerPixel = 1;
    double centerLatitude = 0;
    double centerLongitude = 0;
    Map<Double, Double> points;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.radar);
        compassImageView = findViewById(R.id.compass);
        compassTextView = findViewById(R.id.compassText);

        // Register sensor listeners
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(sensorEventListener, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);

        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                isZooming = true;
                startDistance = getFingerSpacing(event);
                startDistancePerPixel = distancePerPixel;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                isZooming = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isZooming) {
                    float newDistance = getFingerSpacing(event);
                    float scale = newDistance / startDistance;
                    distancePerPixel = startDistancePerPixel * scale;
                    if (distancePerPixel < 0.1) {
                        distancePerPixel = 0.1f;
                    }
                    if (distancePerPixel > 10) {
                        distancePerPixel = 10f;
                    }
                    drawPoints();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.GPS_DATA_PATH)) {
            byte[] data = messageEvent.getData();
            String message = new String(data);
            GPSData gpsData = GPSData.fromJSon(message);
            centerLatitude = gpsData.getCenterLatitude();
            centerLongitude = gpsData.getCenterLongitude();
            points = gpsData.getPoints();
            drawPoints();
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                float[] orientationValues = new float[3];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                azimuth = (int) Math.toDegrees(orientationValues[0]);

                // Rotate the compass image view
                compassImageView.setRotation(-azimuth);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }
    };

    /**
     * Draw points on the compass image view.
     */
    public void drawPoints() {
        if (centerLatitude == 0 || centerLongitude == 0 || points == null) {
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.compass, options);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        float centerX = bitmap.getWidth() / 2f;
        float centerY = bitmap.getHeight() / 2f;
        int radius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 100;

        // Draw the center point
        paint.setColor(Color.BLUE);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // Draw a ring around the center point
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(centerX, centerY, (float) bitmap.getWidth() / 4, paint);

        // Update the compass text view
        compassTextView.setText(getResources().getString(R.string.compassZoom, (int) (bitmap.getWidth() / 4d * distancePerPixel)));

        // Draw the points
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        for (Map.Entry<Double, Double> entry : points.entrySet()) {
            double latitude = entry.getKey();
            double longitude = entry.getValue();
            double latitudeDistance = calculateLatitudeDistance(centerLatitude, latitude);
            double longitudeDistance = calculateLongitudeDistance(centerLongitude, longitude, centerLatitude);
            double distance = Math.sqrt(Math.pow(latitudeDistance, 2) + Math.pow(longitudeDistance, 2));
            double angle = Math.toDegrees(Math.atan2(longitudeDistance, latitudeDistance));
            float x = (float) (centerX + (distance / distancePerPixel) * Math.sin(Math.toRadians(angle)));
            float y = (float) (centerY - (distance / distancePerPixel) * Math.cos(Math.toRadians(angle)));
            canvas.drawCircle(x, y, radius, paint);
        }

        compassImageView.setImageBitmap(bitmap);
    }

    public static double calculateLatitudeDistance(double lat1, double lat2) {
        final double earthRadius = 6371000;
        double deltaPhi = Math.toRadians(lat2 - lat1);
        return deltaPhi * earthRadius;
    }

    public static double calculateLongitudeDistance(double lon1, double lon2, double avgLat) {
        final double earthRadius = 6371000;
        double deltaLambda = Math.toRadians(lon2 - lon1);
        return deltaLambda * earthRadius * Math.cos(Math.toRadians(avgLat));
    }
}