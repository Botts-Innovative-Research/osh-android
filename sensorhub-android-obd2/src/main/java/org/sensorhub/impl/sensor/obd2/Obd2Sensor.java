package org.sensorhub.impl.sensor.obd2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;

import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Obd2Sensor extends AbstractSensorModule<Obd2Config> {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String deviceName;
    private Context context;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket; // TODO Should this be an attribute?
    // private BluetoothGatt btGatt;
    Obd2Output output;
    private boolean btConnected = false;

    public Obd2Sensor(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    protected void doInit() throws SensorHubException {
        // TODO Do I to call super.doInit()?

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
            if (d.getName().equals(deviceName)) {
                device = d;
            }
        }

        if (device == null) {
            throw new SensorException("Could not find bluetooth device, unable to start.");
        }

        // create a bluetooth socket
        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            // TODO Is this what I want to happen if creating the socket fails?
            throw new SensorException("Could not create client socket", e);
        }

        // TODO Do I need to use location data?

        // init osh output(s)
        output = new Obd2Output(this);
        output.doInit();
        addOutput(output, false);
    }

    @Override
    protected void doStart() throws SensorHubException {
        // TODO Do I need to call super.doStart()?

        btAdapter.cancelDiscovery();

        // connect to the bluetooth device
        try {
            btSocket.connect();
        } catch (IOException connectException) {
            try {
                btSocket.close();
            } catch (IOException e) {
                // TODO Is this what I want to happen if connecting to the socket fails?
                throw new SensorException("Could not close the client socket", e);
            }
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        // TODO Close the socket. Here and/or elsewhere too?
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
        return btConnected;
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                btConnected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                btConnected = false;
                // TODO What should I do if connection is lost?
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        }
    };
}