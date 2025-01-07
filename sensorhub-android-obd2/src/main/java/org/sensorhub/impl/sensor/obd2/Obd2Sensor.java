package org.sensorhub.impl.sensor.obd2;

import android.os.Build;

import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;

public class Obd2Sensor extends AbstractSensorModule<Obd2Config> {
    public Obd2Sensor() {
    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // set IDs
        this.xmlID = "OBD2_" + Build.SERIAL;
        this.uniqueID = Obd2Config.getUid();

        // init output(s)
    }


    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {
                sensorDescription.setDescription("Driver for OBD2 sensors conected via BLE");
            }
        }
    }

    @Override
    protected void doStart() throws SensorHubException {
    }

    @Override
    protected void doStop() throws SensorHubException {}

    @Override
    public void cleanup() throws SensorHubException {}

    @Override
    public boolean isConnected() {
        // return (gattClient != null);

        return true;
    }
}