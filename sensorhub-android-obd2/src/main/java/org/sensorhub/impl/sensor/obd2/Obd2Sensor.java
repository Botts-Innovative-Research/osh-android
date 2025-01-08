package org.sensorhub.impl.sensor.obd2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;

import java.util.Set;

public class Obd2Sensor extends AbstractSensorModule<Obd2Config> {
    private String deviceName;
    private Context context;
    private BluetoothAdapter btAdapter;
    private BluetoothGatt btGatt;
    Obd2Output output;
    private boolean btConnected = false;

    public Obd2Sensor(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    protected void doInit() throws SensorHubException {
        // TODO Do I need this
        // super.doInit();

        // set IDs
        this.xmlID = "OBD2_" + Build.SERIAL;
        this.uniqueID = Obd2Config.getUid();

        // set up bluetooth adapter
        context = SensorHubService.getContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        if (!btAdapter.isEnabled() || btAdapter == null) {
            throw new SensorException("Could not get bluetooth adapter, unable to initiate.");
        }

        // TODO The STE sensor uses location data. I need to determine if I need it

        // init output(s)
        output = new Obd2Output(this);
        output.doInit();
        addOutput(output, false);
    }

    @Override
    protected void doStart() throws SensorHubException {
        // TODO Do I need to call super.doStart()?

        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        BluetoothDevice device = null;

        // find the device
        for (BluetoothDevice d : devices) {
            if (d.getName().equals(deviceName)) {
                device = d;
            }
        }

        if (device == null) {
            throw new SensorException("Could not find bluetooth device, unable to start.");
        }

        // request bluetooth permissions
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        // connect to the device
        btGatt = device.connectGatt(context, true, gattCallback);
    }

    @Override
    protected void doStop() throws SensorHubException {
        btGatt.disconnect();
        btGatt.close();
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

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {};
}