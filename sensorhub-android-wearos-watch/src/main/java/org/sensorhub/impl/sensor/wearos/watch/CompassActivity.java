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
import java.util.Map;

public class CompassActivity extends Activity {
    private static final String TAG = CompassActivity.class.getSimpleName();
    ImageView compassImageView;
    int azimuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.radar);
        compassImageView = findViewById(R.id.compass);

        Map<Integer, Integer> points = new HashMap<>();
        points.put(100, 100);
        points.put(-100, -100);
        drawPoints(points);


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
     *
     * @param points The points to draw
     */
    public void drawPoints(Map<Integer, Integer> points) {
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

        // Draw the points
        paint.setColor(Color.RED);
        for (Map.Entry<Integer, Integer> point : points.entrySet()) {
            canvas.drawCircle(centerX + point.getKey(), centerY + point.getValue(), radius, paint);
        }
        compassImageView.setImageBitmap(bitmap);
    }
}