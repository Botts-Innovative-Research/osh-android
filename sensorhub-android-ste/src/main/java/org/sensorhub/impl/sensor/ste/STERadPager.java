package org.sensorhub.impl.sensor.ste;

import org.sensorhub.impl.sensor.AbstractSensorModule;

public class STERadPager extends AbstractSensorModule<STERadPagerConfig> {

    private static final String DEVICE_INFORMATION_SERVICE = "0000180A-0000-1000-8000-00805F9B34FB";
    private static final String MODEL_NUMBER_CHARACTERISTIC = "00002A24-0000-1000-8000-00805F9B34FB";
    private static final String UART_SERVICE = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    private static final String RX_CHARACTERISTIC = "49535343-1E4D-4BD9-BA61-23C647249616";
    private static final String TX_CHARACTERISTIC = "49535343-8841-43F4-A8D4-ECBE34729BB3";

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void doInit() {
        //
    }

    @Override
    public void doStart() {

    }

    @Override
    public void doStop() {

    }
}
