/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sleepMonitor;

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

import androidx.core.app.ActivityCompat;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.sleepMonitor.helpers.SleepMonitorData;
import org.sensorhub.impl.sensor.sleepMonitor.outputs.HeartRateOutput;
import org.sensorhub.impl.sensor.sleepMonitor.outputs.OxygenOutput;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * Configuration class for the generic Android Controller driver
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 05/26/2024
 */

public class SleepMonitor extends AbstractSensorModule<SleepMonitorConfig>  {
    private HandlerThread eventThread;
    static HeartRateOutput heartRateOutput;
    static OxygenOutput oxygenOutput;
    private final ArrayList<PhysicalComponent> smlComponents;
//    private final SensorMLBuilder smlBuilder;
    private Context context;
    SleepMonitorData data;
    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB"); //GATT Characteristic and Object Type 0x2A24 Model Number String
    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_OXYGEN_MEASUREMENT = UUID.fromString("7274782E-6A69-7561-6E2E-504F56313100 ");
    private static final UUID HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_OXYGEN_SERVICE = UUID.fromString("636F6D2E-6A69-7561-6E2E-504F56313100");
    private BluetoothGattCharacteristic heartRate;
    private BluetoothGattCharacteristic oxygenLvl;
    private BluetoothGattCharacteristic serialNumber;
    private BluetoothGattCharacteristic modelNumber;
    private BluetoothGattCharacteristic manufacturerName;
    private BluetoothGattCharacteristic bodyLocation;
    private BluetoothGattService deviceInformation;
    private BluetoothGattService heartRateService;
    private BluetoothGattService bloodOxygenService;
    private BluetoothGatt btGatt;
    private BluetoothAdapter btAdapter;
    public SleepMonitor() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
//        this.smlBuilder = new SensorMLBuilder();
    }
    @Override
    public void doInit() {
        generateUniqueID("urn:osh:", config.getDeviceName());
        generateXmlID("android:controller:", config.getDeviceName());
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
        createOutputs();
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
        BluetoothDevice sleepMonitor = null;
        if(pairedDevices.size() >0){
            for(BluetoothDevice device : pairedDevices){
                String device_name = device.getName();
                String device_address = device.getAddress();
                sleepMonitor = device;
//                btAdapter.cancelDiscovery();
            }
        }
        if(sleepMonitor == null){
            throw new SensorException("No ble device found, cannot start module");
        }

//        btGatt = sleepMonitor.connectGatt(context, true, gattCallback);
//        logger.info("device found: name: {} bluetooth address: {}", sleepMonitor.getName(), sleepMonitor.getAddress());

        eventThread = new HandlerThread("ControllerEvents");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

        data = new SleepMonitorData();
        if(data != null){
            setHeartRateData(data);
            setOxygenData(data);
        }
    }
    public void createOutputs(){
        if(config.getOutputs().getEnabledHR()){
            heartRateOutput = new HeartRateOutput(this);
            heartRateOutput.doInit();
            addOutput(heartRateOutput, false);

        }else{
            heartRateOutput =null;
        }

        if(config.getOutputs().getEnabledOxygen()){
            oxygenOutput = new OxygenOutput(this);
            oxygenOutput.doInit();
            addOutput(oxygenOutput, false);
        } else{
            oxygenOutput=null;
        }
    }
    public static void setHeartRateData(SleepMonitorData data){
        if(heartRateOutput==null){
            return;
        }
        for(int i=0; i < data.getHeartRateSize();i++){
            heartRateOutput.setHRData(
                    data.getHeartRate(i).getTimestamp(),
                    data.getHeartRate(i).getHR()
                    );
        }
    }
    public static void setOxygenData(SleepMonitorData data){
        if(oxygenOutput==null){
            return;
        }
        for(int i=0; i < data.getOxygenSize(); i++){
            oxygenOutput.setOxygenData(
                    data.getOxygen(i).getTimestamp(),
                    data.getOxygen(i).getOxygenLevel()
            );
        }
    }
    @Override
    public void doStop() {
//        btGatt.disconnect();;
//        btGatt.close();
    }

    @Override
    public boolean isConnected() {return true;}
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                boolean discoveryStarting = gatt.discoverServices();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            logger.debug("characteristic changed");
            if (characteristic.getUuid().equals(HEART_RATE_MEASUREMENT)) {
                byte[] data = characteristic.getValue();
                logger.debug("parsing hr data");
                int hr = data[1] & 0xFF;
                heartRateOutput.setHRData(HEART_RATE_MEASUREMENT.timestamp(), hr);
            }
            else if (characteristic.getUuid().equals(BLOOD_OXYGEN_MEASUREMENT)) {
//                parse(characteristic.getValue());
                byte[] data = characteristic.getValue();
                int oxyLvl = data[1] & 0xFF;
                oxygenOutput.setOxygenData(BLOOD_OXYGEN_MEASUREMENT.timestamp(), oxyLvl);
//                logger.debug("bat level data received: " + batLevel);
            }

//            output.setData(hr, batLevel);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            logger.debug("on services discovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                deviceInformation = btGatt.getService(DEVICE_INFORMATION_SERVICE);
                heartRateService = btGatt.getService(HEART_RATE_SERVICE);
                bloodOxygenService = btGatt.getService(BLOOD_OXYGEN_SERVICE);
                //device info service
                modelNumber = deviceInformation.getCharacteristic(MODEL_NUMBER);
                manufacturerName = deviceInformation.getCharacteristic(MANUFACTURER_NAME);
                serialNumber = deviceInformation.getCharacteristic(SERIAL_NUMBER);

                heartRate = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT);
                bodyLocation = heartRateService.getCharacteristic(BODY_SENSOR_LOCATION);
                oxygenLvl = bloodOxygenService.getCharacteristic(BLOOD_OXYGEN_MEASUREMENT);

                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                }
                gatt.setCharacteristicNotification(modelNumber, true); //device info service
                gatt.setCharacteristicNotification(manufacturerName, true); //device info service
                gatt.setCharacteristicNotification(serialNumber, true); //device info service
                gatt.setCharacteristicNotification(heartRate, true); //hr service
                gatt.setCharacteristicNotification(oxygenLvl, true); //oxygen level service
                gatt.setCharacteristicNotification(bodyLocation, true);


                BluetoothGattDescriptor hrDescriptor = heartRate.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                hrDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(hrDescriptor);

                BluetoothGattDescriptor oxyDescriptor = heartRate.getDescriptor(UUID.fromString("7274782E-6A69-7561-6E2E-504F56313100 "));
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

