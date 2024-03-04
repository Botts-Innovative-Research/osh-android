package org.sensorhub.impl.sensor.wearos.watch;

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
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CompassActivity extends Activity {
    ImageView compassImageView;
    int azimuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.radar);
        compassImageView = findViewById(R.id.compass);

        double centerLatitude = 34.71067672807872;
        double centerLongitude = -86.73418227527542;

        Map<Double, Double> points = new HashMap<>();
        points.put(34.71171301403239, -86.73417154644011);
        points.put(34.71122794562786, -86.7339301476455);
        points.put(34.71092808373677, -86.73629049141508);
        points.put(34.71369976390715, -86.73311680904119);
        drawPoints(centerLatitude, centerLongitude, points);

        // Register sensor listeners
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(sensorEventListener, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
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
    public void drawPoints(double centerLatitude, double centerLongitude, Map<Double, Double> points) {
        final double distancePerPixel = 1;

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

        // Add text outside of the ring indicating size in meters
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(25);

        String text = String.format(Locale.US, "%.0f m", (double) bitmap.getWidth() / 4 * distancePerPixel);
        float textX = centerX + (float) bitmap.getWidth() / 4 + 10f;
        canvas.drawText(text, textX, centerY, paint);

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
            float x = (float) (centerX + (distance / distancePerPixel) * Math.sin(Math.toRadians(azimuth + angle)));
            float y = (float) (centerY - (distance / distancePerPixel) * Math.cos(Math.toRadians(azimuth + angle)));
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