package org.sensorhub.impl.sensor.rs350;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class RS350Config extends SensorConfig {
    public RS350Config() {
        this.moduleClass = RS350Sensor.class.getCanonicalName();
    }

    @DisplayInfo.Required
    public String serialNumber;

    @DisplayInfo(desc = "Communication settings to connect to RS-350 data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo.Required
    @DisplayInfo(label = "Outputs", desc = "Configuration options for source data outputs from driver")
    public RS350Outputs outputs = new RS350Outputs();
}
