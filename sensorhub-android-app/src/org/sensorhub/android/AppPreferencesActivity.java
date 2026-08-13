package org.sensorhub.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class AppPreferencesActivity extends AppCompatActivity {

    SensorHubService boundService;

    private final ServiceConnection sConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            boundService = ((SensorHubService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            boundService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_app_preferences);

        MaterialToolbar toolbar = findViewById(R.id.app_prefs_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        bindService(new Intent(this, SensorHubService.class), sConn, Context.BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.app_prefs_container, new AppPreferencesFragment())
                .commit();
        }
    }

    @Override
    protected void onDestroy() {
        if (boundService != null) {
            unbindService(sConn);
            boundService = null;
        }
        super.onDestroy();
    }
}
