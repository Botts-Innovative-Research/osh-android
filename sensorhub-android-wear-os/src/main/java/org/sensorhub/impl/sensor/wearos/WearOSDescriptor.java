package org.sensorhub.impl.sensor.wearos;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class WearOSDescriptor implements IModuleProvider {
    @Override
    public String getModuleName() {
        return "Wear OS Driver";
    }

    @Override
    public String getModuleDescription() {
        return "Driver for Wear OS devices";
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
        return WearOSDriver.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return WearOSConfig.class;
    }
}
