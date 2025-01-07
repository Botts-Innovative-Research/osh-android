package org.sensorhub.impl.sensor.obd2;

import android.content.Context;
import android.provider.Settings;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorConfig;

public class Obd2Config extends SensorConfig {
    public Obd2Config() {
        this.moduleClass = Obd2Sensor.class.getCanonicalName();
    }

    public static String getUid() {
        Context context = SensorHubService.getContext();
        return "urn:android:obd2:" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
