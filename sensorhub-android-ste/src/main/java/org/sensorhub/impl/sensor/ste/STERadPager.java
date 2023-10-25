package org.sensorhub.impl.sensor.ste;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class STERadPager extends AbstractSensorModule<STERadPagerConfig> {

    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID MODEL_NUMBER_CHARACTERISTIC = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID UART_SERVICE = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID RX_CHARACTERISTIC = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private static final UUID TX_CHARACTERISTIC = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final String DEVICE_NAME = "RADIATION PAGER";

    private BluetoothGatt btGatt;
    private BluetoothAdapter btAdapter;
    private Context context;
    private BluetoothGattService deviceInformationService;
    private BluetoothGattService uartService;
    private BluetoothGattCharacteristic modelNumberCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCharacteristic txCharacteristic;
    STERadPagerOutput output;

    public STERadPager() {

    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void doInit() {
        this.xmlID = "STE_RADPAGER_" + Build.SERIAL;
        this.uniqueID = STERadPagerConfig.getUid();

        context = SensorHubService.getContext();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
        }
        output = new STERadPagerOutput(this);
        output.doInit();
        addOutput(output, false);
    }

    @Override
    public void doStart() throws SensorException {
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        BluetoothDevice device = null;
        for (BluetoothDevice d : devices) {
            if (d.getName().equals(DEVICE_NAME)) {
                device = d;
            }
        }
        if (null == device) {
            throw new SensorException("Could not find Bluetooth device, unable to start.");
        }

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            // request permission
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }


        btGatt = device.connectGatt(context, false, gattCallback);
        btGatt.connect();
    }

    @Override
    public void doStop() {

    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // do stuff on connection
                System.out.println("Connected");
                boolean discoveryStarted = gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // do stuff on disconnection
            }
        }

//        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
//            if(characteristic.getUuid().equals(MODEL_NUMBER_CHARACTERISTIC)) {
//                // do stuff with model number
//                byte[] someVal = characteristic.getValue();
//                String message = new String(someVal);
//            }else if(characteristic == rxCharacteristic) {
//                // do stuff with rx
//                byte[] someVal = characteristic.getValue();
//                String message = new String(someVal);
//            }else if(characteristic == txCharacteristic) {
//                // do stuff with tx
//                byte[] someVal = characteristic.getValue();
//                String message = new String(someVal);
//            }else {
//                System.out.println(characteristic.getUuid());
//            }
//        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (characteristic.getUuid().equals(MODEL_NUMBER_CHARACTERISTIC)) {
                // do stuff with model number
                byte[] someVal = characteristic.getValue();
                String message = new String(someVal);
            } else if (characteristic == rxCharacteristic) {
                // do stuff with rx
                byte[] someVal = characteristic.getValue();
                String message = new String(someVal);
            } else if (characteristic == txCharacteristic) {
                // do stuff with tx
                byte[] someVal = characteristic.getValue();
                String message = new String(someVal);
            } else {
                System.out.println(characteristic.getUuid());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RX_CHARACTERISTIC)) {
                parseMessage(characteristic.getValue());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            deviceInformationService = btGatt.getService((DEVICE_INFORMATION_SERVICE));
            uartService = btGatt.getService(UART_SERVICE);
            modelNumberCharacteristic = deviceInformationService.getCharacteristic(MODEL_NUMBER_CHARACTERISTIC);
            rxCharacteristic = uartService.getCharacteristic(RX_CHARACTERISTIC);
            txCharacteristic = uartService.getCharacteristic(TX_CHARACTERISTIC);


            gatt.setCharacteristicNotification(modelNumberCharacteristic, true);

            gatt.setCharacteristicNotification(rxCharacteristic, true);
            BluetoothGattDescriptor rxDescriptor = rxCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            rxDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(rxDescriptor);

            gatt.setCharacteristicNotification(txCharacteristic, true);

//            gatt.readCharacteristic(rxCharacteristic);
//            gatt.writeCharacteristic(txCharacteristic);
        }

        private void parseMessage(byte[] characteristicValue) {
            String message = new String(characteristicValue, StandardCharsets.UTF_8);
            message = message.replaceAll("\r\n", "");
            String[] splitMsg = message.split(",");
            logger.info("Alarm: {}, Counts: {}, Threshold: {}", splitMsg[1], splitMsg[2], splitMsg[3]);

            output.insertSensorData(splitMsg);
        }

    };
}
