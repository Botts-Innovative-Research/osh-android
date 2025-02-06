package org.sensorhub.impl.sensor.obd2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;

import java.util.Set;
import java.util.UUID;

public class Obd2Sensor extends AbstractSensorModule<Obd2Config> {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String deviceName;
    private Context context;
    private Obd2Connect connectionThread;
    private BluetoothAdapter btAdapter;
    private Obd2Output output;
    private Obd2Control control;
    private boolean btConnected = false;

    public Obd2Sensor() {}

    public BluetoothSocket getBtSocket() {
        return connectionThread.getBtSocket();
    }

    @Override
    public void doInit() throws SensorHubException {
        System.out.println("*** INITIALIZING OBD2 SENSOR ***");

        // TODO Do I to call super.doInit()?
        super.doInit();

        // set IDs
        this.xmlID = "OBD2_" + Build.SERIAL;
        this.uniqueID = Obd2Config.getUid();

        // get the android's bluetooth adapter
        context = SensorHubService.getContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        if (!btAdapter.isEnabled() || btAdapter == null) {
            throw new SensorException("Could not get bluetooth adapter, unable to initiate.");
        }

        BluetoothDevice device = null;
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();

        // find the bluetooth device
        for (BluetoothDevice d : devices) {
            // TODO Get device name from config
            if (d.getName().equals("VEEPEAK")) {
                device = d;
            }
        }

        if (device == null) {
            throw new SensorException("Could not find bluetooth device, unable to start.");
        }

        // create a bluetooth socket via a thread
        connectionThread = new Obd2Connect(btAdapter, device);

        // TODO Do I need to use location data?

        // init osh output(s)
        output = new Obd2Output(this);
        output.doInit();
        addOutput(output, false);

        control = new Obd2Control(this);
        addControlInput(control);
        control.init();

        System.out.println("*** COMPLETED OBD2 SENSOR INIT");
    }

    @Override
    protected void doStart() throws SensorHubException {
        // TODO Do I need to call super.doStart()?
        System.out.println("*** STARTING OBD2 SENSOR...");
        super.doStart();

        // connect to the bluetooth device via a thread
        // TODO I think we're already connected so there's no point in doing this unless i want to connect via osh and not the device
        //  connectionThread.start();

        // TODO and then what? i think we'll need a command class to send commands to read data. where do we interface with the driver? android? computer? api?
    }

    @Override
    protected void doStop() {
        connectionThread.cancel();
    }

    @Override
    public void cleanup() throws SensorHubException {}

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
    public boolean isConnected() {
        return connectionThread.isConnected();
    }

    // TODO Is this an ok pattern?
    public Obd2Output getOutput() {
        return output;
    }
}