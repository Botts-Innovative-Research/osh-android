package org.sensorhub.impl.sensor.obd2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import org.sensorhub.api.sensor.SensorException;

import java.util.UUID;
import java.io.IOException;

public class Obd2Connect extends Thread {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothSocket btSocket;
    private final BluetoothAdapter btAdapter;
    volatile boolean active = true;

    public Obd2Connect(BluetoothAdapter adapter, BluetoothDevice btDevice) throws SensorException {
        System.out.println("*** STARTING OBD2CONNECT");

        btAdapter = adapter;
        BluetoothSocket tmpSocket = null;

        try {
            tmpSocket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            // TODO Is this what I want to happen if creating the socket fails?
            throw new SensorException("Could not create client socket", e);
        }

        btSocket = tmpSocket;

        System.out.println("*** COMPLETED OBD2CONNECT");
    }

    public BluetoothSocket getBtSocket() {
        return btSocket;
    }

    public void run() {
        System.out.println("*** RUNNING OBD2CONNECT");

//        while (active) {
//            btAdapter.cancelDiscovery();
//
//            // connect to the bluetooth device
//            try {
//                btSocket.connect();
//            } catch (IOException connectException) {
//                try {
//                    btSocket.close();
//                } catch (IOException e) {
//                    // TODO What should I do if I can't close the socket?
//                }
//            }
//        }
    }

    public void cancel() {
        try {
            active = false;
            btSocket.close();
        } catch (IOException e) {
            // TODO What should I do if I can't close the socket?
        }
    }

    public boolean isConnected() {
        return btSocket.isConnected();
    }
}