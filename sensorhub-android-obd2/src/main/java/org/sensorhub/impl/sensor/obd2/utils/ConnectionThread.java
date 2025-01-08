package org.sensorhub.impl.sensor.obd2.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import org.sensorhub.api.sensor.SensorException;

import java.util.UUID;
import java.io.IOException;

public class ConnectionThread extends Thread {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothSocket btSocket;
    private final BluetoothDevice btDevice;
    private final BluetoothAdapter btAdapter;
    volatile boolean active = true;

    public ConnectionThread(BluetoothAdapter adapter, BluetoothDevice device) throws SensorException {
        btAdapter = adapter;
        btDevice = device;
        BluetoothSocket tmpSocket = null;

        try {
            tmpSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            // TODO Is this what I want to happen if creating the socket fails?
            throw new SensorException("Could not create client socket", e);
        }

        btSocket = tmpSocket;
    }

    public void run() {
        while (active) {
            btAdapter.cancelDiscovery();

            // connect to the bluetooth device
            try {
                btSocket.connect();
            } catch (IOException connectException) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    // TODO What should I do if I can't close the socket?
                }
            }
        }
    }

    public void stopThread() {
        active = false;
    }

    public boolean isConnected() {
        return btSocket.isConnected();
    }
}