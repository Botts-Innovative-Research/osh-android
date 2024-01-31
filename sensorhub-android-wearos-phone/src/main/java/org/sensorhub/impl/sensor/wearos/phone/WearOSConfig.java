package org.sensorhub.impl.sensor.wearos.phone;

import org.sensorhub.api.sensor.SensorConfig;

public class WearOSConfig extends SensorConfig {
    public WearOSConfig() {
        this.moduleClass = WearOSDriver.class.getCanonicalName();
    }
}
