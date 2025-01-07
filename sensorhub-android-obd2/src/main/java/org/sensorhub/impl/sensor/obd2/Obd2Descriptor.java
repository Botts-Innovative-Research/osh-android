package org.sensorhub.impl.sensor.obd2;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class Obd2Descriptor implements IModuleProvider {
    @Override
    public String getModuleName() {
        return "OBD2 Driver";
    }

    @Override
    public String getModuleDescription() {
        return "Driver for OBD2 sensors conected via BLE";
    }

    @Override
    public String getModuleVersion() {
        return "0.1";
    }

    @Override
    public String getProviderName() {
        return "Botts Innovative Research, Inc.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return Obd2Sensor.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return Obd2Config.class;
    }
}
