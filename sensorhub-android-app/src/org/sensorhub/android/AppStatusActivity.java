package org.sensorhub.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class AppStatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_status);

        MaterialToolbar toolbar = findViewById(R.id.status_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Intent intent = getIntent();

        String sosStatus = intent.getStringExtra("sosService");
        String consSysStatus = intent.getStringExtra("conSysService");
        String discoveryStatus = intent.getStringExtra("discoveryService");
        String httpStatus = intent.getStringExtra("httpStatus");
        String sensorStatus = intent.getStringExtra("androidSensorStatus");
        String sensorStorageStatus = intent.getStringExtra("sensorStorageStatus");

        TextView sosStatusView = findViewById(R.id.sos_service_state);
        TextView conSysStatusView = findViewById(R.id.consys_service_state);
        TextView discoveryStatusView = findViewById(R.id.discovery_service_state);
        TextView httpStatusView = findViewById(R.id.http_service_state);
        TextView sensorStatusView = findViewById(R.id.sensor_service_state);
        TextView storageStatusView = findViewById(R.id.storage_service_state);

        sosStatusView.setText(sosStatus);
        conSysStatusView.setText(consSysStatus);
        discoveryStatusView.setText(discoveryStatus);
        httpStatusView.setText(httpStatus);
        sensorStatusView.setText(sensorStatus);
        storageStatusView.setText(sensorStorageStatus);

        setStatusDotColor(findViewById(R.id.sos_status_dot), sosStatus);
        setStatusDotColor(findViewById(R.id.consys_status_dot), consSysStatus);
        setStatusDotColor(findViewById(R.id.discovery_status_dot), discoveryStatus);
        setStatusDotColor(findViewById(R.id.http_status_dot), httpStatus);
        setStatusDotColor(findViewById(R.id.sensor_status_dot), sensorStatus);
        setStatusDotColor(findViewById(R.id.storage_status_dot), sensorStorageStatus);
    }

    private void setStatusDotColor(View dot, String status) {
        int colorRes;
        if (status == null) {
            colorRes = R.color.status_unknown;
        } else {
            String lower = status.toLowerCase();
            if (lower.contains("started")) {
                colorRes = R.color.status_started;
            } else if (lower.contains("stopped")) {
                colorRes = R.color.status_stopped;
            } else if (lower.contains("starting") || lower.contains("initializ")) {
                colorRes = R.color.status_initializing;
            } else {
                colorRes = R.color.status_unknown;
            }
        }

        GradientDrawable background = (GradientDrawable) dot.getBackground();
        background.setColor(ContextCompat.getColor(this, colorRes));
    }
}
