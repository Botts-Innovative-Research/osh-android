package org.sensorhub.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.ModuleRegistry;

import java.util.Collection;


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

    public void launchAppStatus() {
        Intent statusIntent = new Intent(this, AppStatusActivity.class);

        if (boundService != null && boundService.sensorhub != null) {
            ModuleRegistry moduleRegistry = boundService.sensorhub.getModuleRegistry();
            Collection<IModule<?>> modules = moduleRegistry.getLoadedModules();

            for (IModule<?> module : modules) {
                var moduleConf = module.getConfiguration();

                if (moduleConf instanceof ModuleConfig) {
                    String status = module.getCurrentState().name();
                    String moduleId = ((ModuleConfig) moduleConf).id;

                    switch (moduleId) {
                        case "HTTP_SERVER_0":
                            statusIntent.putExtra("httpStatus", status);
                            break;
                        case "SOS_SERVICE":
                            statusIntent.putExtra("sosService", status);
                            break;
                        case "CON_SYS_SERVICE":
                            statusIntent.putExtra("conSysService", status);
                            break;
                        case "DISCOVERY_SERVICE":
                            statusIntent.putExtra("discoveryService", status);
                            break;
                        case "ANDROID_SENSORS":
                            statusIntent.putExtra("androidSensorStatus", status);
                            break;
                        case "ANDROID_SENSORS#storage":
                            statusIntent.putExtra("sensorStorageStatus", status);
                            break;
                    }
                }
            }
        } else {
            statusIntent.putExtra("sosService", "N/A");
            statusIntent.putExtra("conSysService", "N/A");
            statusIntent.putExtra("httpStatus", "N/A");
            statusIntent.putExtra("androidSensorStatus", "N/A");
            statusIntent.putExtra("sensorStorageStatus", "N/A");
        }

        startActivity(statusIntent);
    }
}
