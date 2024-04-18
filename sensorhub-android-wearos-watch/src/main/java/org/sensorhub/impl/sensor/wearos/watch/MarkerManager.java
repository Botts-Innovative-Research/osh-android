package org.sensorhub.impl.sensor.wearos.watch;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.sensorhub.impl.sensor.wearos.lib.data.GPSDataPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerManager {
    private final GoogleMap googleMap;
    private final Map<String, Marker> markers = new HashMap<>();

    public MarkerManager(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public void updateMarkers(List<GPSDataPoint> gpsDataPoints) {
        // Update and add markers
        for (GPSDataPoint gpsDataPoint : gpsDataPoints) {
            if (!updateMarker(gpsDataPoint)) {
                addMarker(gpsDataPoint);
            }
        }

        // Remove markers that are not in the list
        List<String> pointsToRemove = new ArrayList<>();
        for (Map.Entry<String, Marker> entrySet : markers.entrySet()) {
            boolean found = false;

            for (GPSDataPoint gpsDataPoint : gpsDataPoints) {
                if (gpsDataPoint.getPointName().equals(entrySet.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                pointsToRemove.add(entrySet.getKey());
            }
        }

        for (String pointName : pointsToRemove) {
            removeMarker(pointName);
        }
    }

    private boolean updateMarker(GPSDataPoint gpsDataPoint) {
        Marker marker = markers.get(gpsDataPoint.getPointName());
        if (marker != null) {
            LatLng latLng = new LatLng(gpsDataPoint.getLatitude(), gpsDataPoint.getLongitude());
            marker.setPosition(latLng);
            return true;
        }

        return false;
    }

    private void addMarker(GPSDataPoint gpsDataPoint) {
        LatLng latLng = new LatLng(gpsDataPoint.getLatitude(), gpsDataPoint.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(gpsDataPoint.getPointName())
                .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(gpsDataPoint.getColorValue())));
        Marker marker = googleMap.addMarker(markerOptions);
        markers.put(gpsDataPoint.getPointName(), marker);
    }

    private void removeMarker(String pointName) {
        Marker marker = markers.get(pointName);
        if (marker != null) {
            marker.remove();
            markers.remove(pointName);
        }
    }

    private Bitmap createCustomMarker(int color) {
        int size = 32;
        int radius = size / 2;

        int darkerColor = Color.rgb(
                Math.max(Color.red(color) - 50, 0),
                Math.max(Color.green(color) - 50, 0),
                Math.max(Color.blue(color) - 50, 0)
        );

        Paint paint = new Paint();
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw a darker circle
        paint.setColor(darkerColor);
        canvas.drawCircle(radius, radius, radius, paint);

        // Draw a circle with the specified color
        paint.setColor(color);
        canvas.drawCircle(radius, radius, radius - 2f, paint);

        return bitmap;
    }
}
