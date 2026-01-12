package org.sensorhub.impl.sensor.meshtastic;

import android.content.Context;
import android.provider.Settings;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorConfig;

public class MeshtasticConfig extends SensorConfig {
    public MeshtasticConfig() {
        this.moduleClass = MeshtasticSensor.class.getCanonicalName();
    }

    public String uid_extension;
    public static String getUid() {
        Context context = SensorHubService.getContext();
        return "urn:android:meshtastic:" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getUidExt() {
        Context context = SensorHubService.getContext();
        if (uid_extension == null || uid_extension.isEmpty())
            return "urn:android:meshtastic:" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        return "urn:android:meshtastic:" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID) + ":" + uid_extension;
    }
    public String device_name;

}
