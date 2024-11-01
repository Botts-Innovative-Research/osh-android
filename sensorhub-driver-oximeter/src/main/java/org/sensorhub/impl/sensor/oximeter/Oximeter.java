/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.oximeter;

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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;
//import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat;

import net.opengis.sensorml.v20.PhysicalComponent;
import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * Configuration class for the generic Android Pulse Oximeter
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 05/26/2024
 */

public class Oximeter extends AbstractSensorModule<OximeterConfig>  {
    private HandlerThread eventThread;
    static OximeterOutput oximeterOutput;
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    private Context context;
    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID PLX_SPOT_CHECK_MEASUREMENT = UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OXIMETER_SERVICE = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic oximeterCharacteristic;
    private BluetoothGattCharacteristic serialNumberCharacteristic;
    private BluetoothGattCharacteristic modelNumberCharacteristic;
    private BluetoothGattCharacteristic manufacturerNameCharacteristic;
    private BluetoothGattService deviceInformationService;
    private BluetoothGattService plxSpotCheckService;
    private BluetoothGatt btGatt;
    private BluetoothAdapter btAdapter;
    boolean isConnected;
    public Oximeter() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }
    @Override
    public void doInit() {
        generateUniqueID("urn:osh:", config.getDeviceName());
        generateXmlID("android:oximeter:", config.getDeviceName());
        context = SensorHubService.getContext();

        final BluetoothManager btManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter =btManager.getAdapter();
        if (btAdapter == null) {
            Toast.makeText(context, "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!btAdapter.isEnabled()) {
            Toast.makeText(context, "Device does not have bluetooth enabled", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }

        oximeterOutput = new OximeterOutput(this);
        oximeterOutput.doInit();
        addOutput(oximeterOutput, false);
    }

    @Override
    public void doStart() throws SensorException {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        }
        // set of currently paired devices
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        BluetoothDevice oximeter = null;
        if(pairedDevices.size() >0){
            for(BluetoothDevice device : pairedDevices){
                logger.debug("Device Information: \n");
                String device_name = device.getName();
                String device_address = device.getAddress();
                logger.debug("Device Name: {} Device Address: {}", device_name, device_address);
                oximeter = device;
                btAdapter.cancelDiscovery();
            }
        }
        if(oximeter == null){
            throw new SensorException("No ble device found, cannot start module");
        }

        btGatt = oximeter.connectGatt(context, true, gattCallback);

        eventThread = new HandlerThread("ControllerEvents");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());
    }

    @Override
    public void doStop() {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        }
        btGatt.disconnect();
        btGatt.close();
    }

    @Override
    public boolean isConnected() {
        return true;
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                boolean discoveryStarting = gatt.discoverServices();
                isConnected = true;
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                isConnected = false;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            logger.debug("characteristic changed");
            if(characteristic.getUuid().equals(PLX_SPOT_CHECK_MEASUREMENT)){
                byte[]  packet = {(byte)0x86, (byte)0x16, (byte)0x03, (byte)0x41, (byte)0x62};
                int spo2 = packet[4]; // 41 =  65
                int pulseRate = packet[3] | ((packet[2] & 64) << 1);
                System.out.println(spo2);
                System.out.println(pulseRate);
                oximeterOutput.setData(pulseRate, spo2);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            logger.debug("on services discovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //device Information Service
                deviceInformationService = btGatt.getService(DEVICE_INFORMATION_SERVICE);
                modelNumberCharacteristic = deviceInformationService.getCharacteristic(MODEL_NUMBER);
                manufacturerNameCharacteristic = deviceInformationService.getCharacteristic(MANUFACTURER_NAME);
                serialNumberCharacteristic = deviceInformationService.getCharacteristic(SERIAL_NUMBER);

                plxSpotCheckService = btGatt.getService(PULSE_OXIMETER_SERVICE);
                oximeterCharacteristic = plxSpotCheckService.getCharacteristic(PLX_SPOT_CHECK_MEASUREMENT);

                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                }
                gatt.setCharacteristicNotification(modelNumberCharacteristic, true); //device info service
                gatt.setCharacteristicNotification(manufacturerNameCharacteristic, true); //device info service
                gatt.setCharacteristicNotification(serialNumberCharacteristic, true); //device info service
                gatt.setCharacteristicNotification(oximeterCharacteristic, true);  //plx spot service
                logger.debug("Device Information: \n");
                logger.debug("Model Number: {}, Manufacturer Name: {}, Serial Number: {}", modelNumberCharacteristic, manufacturerNameCharacteristic, serialNumberCharacteristic);

                BluetoothGattDescriptor oxyDescriptor = oximeterCharacteristic.getDescriptor(UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb "));
                oxyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(oxyDescriptor);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logger.info("Failed to write to characteristic");
            }
        }
    };

}

