package org.sensorhub.impl.sensor.rs350;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class RS350Descriptor implements IModuleProvider {
    @Override
    public String getModuleName() {
        return "RS350 Driver";
    }

    @Override
    public String getModuleDescription() {
        return "Driver for RS350 backpack radiation sensor";
    }

    @Override
    public String getModuleVersion() {
        return "1.0";
    }

    @Override
    public String getProviderName() {
        return "Botts Innovative Research, Inc.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return RS350Sensor.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return RS350Config.class;
    }
}
