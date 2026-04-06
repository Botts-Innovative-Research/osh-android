package org.sensorhub.android;

import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule;

import java.util.ArrayList;

public interface SensorHubServiceProvider {
    SensorHubService getBoundService();
    boolean isOshStarted();
    void setOshStarted(boolean started);
    IModuleConfigRepository getSensorhubConfig();
    ArrayList<SOSTClient> getSostClients();
    ArrayList<ConSysApiClientModule> getConSysClients();
    AndroidSensorsDriver getAndroidSensors();
    void setAndroidSensors(AndroidSensorsDriver driver);
    boolean getShowVideo();
    void updateConfig(android.content.SharedPreferences prefs, String runName);
    void startSensorHub();
    void stopSensorHub();
}
