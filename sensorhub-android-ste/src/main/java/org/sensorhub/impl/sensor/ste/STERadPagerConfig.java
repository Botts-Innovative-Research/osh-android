package org.sensorhub.impl.sensor.ste;

import org.sensorhub.api.sensor.SensorConfig;

public class STERadPagerConfig extends SensorConfig {
    public STERadPagerConfig() {
        this.moduleClass = STERadPager.class.getCanonicalName();
    }
}
