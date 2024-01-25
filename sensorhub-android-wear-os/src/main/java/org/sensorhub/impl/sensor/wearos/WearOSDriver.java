package org.sensorhub.impl.sensor.wearos;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

public class WearOSDriver extends AbstractSensorModule<WearOSConfig> {
    WearOSOutput output;

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void doInit() {
        generateUniqueID("urn:rsi:wearos:", null);
        generateXmlID("wear-os_", null);

        output = new WearOSOutput(this);
        output.doInit();
        addOutput(output, false);
    }

    @Override
    public void doStart() throws SensorException {

    }

    @Override
    public void doStop() {

    }
}
