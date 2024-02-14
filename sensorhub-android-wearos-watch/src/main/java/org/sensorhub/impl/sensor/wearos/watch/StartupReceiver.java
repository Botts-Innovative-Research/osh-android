package org.sensorhub.impl.sensor.wearos.watch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, HealthDataService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
