/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.polar;

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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrData;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class Polar extends AbstractSensorModule<PolarConfig> {
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());
    private static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"); //GATT Characteristic and Object Type 0x2A38 Body Sensor Location
    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");  //GATT Characteristic and Object Type 0x2A19 Battery Level
    private static final UUID BATTERY_SERVICE = UUID.fromString("000180F-0000-1000-8000-00805f9b34fb");  //GATT Characteristic and Object Type 0x2A19 Battery Level
    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB"); //GATT Characteristic and Object Type 0x2A24 Model Number String
    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final String SENSOR_DEVICE_NAME = "Polar H10 CA34FA28";
    private BluetoothGattCharacteristic heartRate;
    private BluetoothGattCharacteristic batteryLevel;
    private BluetoothGattCharacteristic serialNumber;
    private BluetoothGattCharacteristic modelNumber;
    private BluetoothGattCharacteristic manufacturerName;
    private BluetoothGattCharacteristic bodyLocation;
    private BluetoothGattService deviceInformation;
    private BluetoothGattService heartRateService;
    private BluetoothGattService batteryService;
    private BluetoothGatt btGatt;
    private Context context;
    private BluetoothAdapter btAdapter;
    PolarOutput output;
    private HandlerThread eventThread;
    PolarBleApi api;


    public Polar() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }

    @Override
    public void doInit() {
        logger.info("Initializing Polar heart monitor Sensor");
        this.xmlID = "POLAR-H9" + Build.SERIAL;
        this.uniqueID = PolarConfig.getAndroidSensorsUid();
        context = SensorHubService.getContext();
        polarApi();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        if (btAdapter == null) {
            Toast.makeText(context, "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!btAdapter.isEnabled()) {
            Toast.makeText(context, "Device does not have bluetooth enabled", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }

        output = new PolarOutput(this);
        output.doInit();
        addOutput(output, false);
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
        BluetoothDevice polarDevice = null;
        if (pairedDevices.size() > 0) {
            // there are paired devices
            //get name of device and address
            for (BluetoothDevice device : pairedDevices) {
                String device_name = device.getName();
                String device_address = device.getAddress();

                //code if you have the specific name hard coded but not probable or reusable
//            if (device.getName().equals(SENSOR_DEVICE_NAME)) {
                polarDevice = device;
                btAdapter.cancelDiscovery();
//            }
            }
        }

        if (polarDevice == null) {
            throw new SensorException("No ble device found, cannot start module");
        }
        btGatt = polarDevice.connectGatt(context, true, gattCallback);
        logger.info("device found: " + polarDevice.getName() + " " + polarDevice.getAddress());

        eventThread = new HandlerThread("Polar_Event");
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

    public void polarApi() {
        //set all desired features
        Set<PolarBleApi.PolarBleSdkFeature> features = new HashSet<>(Arrays.asList(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP

        ));
        api = PolarBleApiDefaultImpl.defaultImplementation(context.getApplicationContext(), features);
        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void batteryLevelReceived(@NonNull String identifier, int level) {
                super.batteryLevelReceived(identifier, level);
                logger.debug("Battery: " + level);
            }

            @Override
            public void blePowerStateChanged(boolean powered) {
                super.blePowerStateChanged(powered);
                if (powered) {
                    context = SensorHubService.getContext();
                    final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
                    btAdapter = bluetoothManager.getAdapter();
                    if (btAdapter == null || !btAdapter.isEnabled()) {
                        Toast.makeText(context, "bluetooth adapter is null or not enabled", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Powered is false, no bluetooth connection", Toast.LENGTH_LONG).show();
                }
                logger.debug("Bluetooth state changed " + powered);
            }

            @Override
            public void bleSdkFeatureReady(@NonNull String identifier, @NonNull PolarBleApi.PolarBleSdkFeature feature) {
                super.bleSdkFeatureReady(identifier, feature);
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                super.deviceConnected(polarDeviceInfo);
                Toast.makeText(context, "Connected to polar device", Toast.LENGTH_SHORT).show();
                isConnected();
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {
                super.deviceConnecting(polarDeviceInfo);
                Toast.makeText(context, "Connecting to polar device", Toast.LENGTH_SHORT).show();
                logger.debug("connecting" + polarDeviceInfo.getDeviceId());
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                super.deviceDisconnected(polarDeviceInfo);
                Toast.makeText(context, "Disconnected from polar device", Toast.LENGTH_SHORT).show();
                logger.debug("Device disconnected " + polarDeviceInfo.getDeviceId());
            }

            @Override
            public void disInformationReceived(@NonNull String identifier, @NonNull UUID uuid, @NonNull String value) {
                super.disInformationReceived(identifier, uuid, value);
            }

            @Override
            public void hrFeatureReady(@NonNull String identifier) {
                super.hrFeatureReady(identifier);
                Toast.makeText(context, "Heart Rate feature ready for polar device", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void hrNotificationReceived(@NonNull String identifier, @NonNull PolarHrData.PolarHrSample data) {
                super.hrNotificationReceived(identifier, data);
                logger.debug("HR notifications received");
                int currentHR = data.getHr();
                logger.debug("Current HR: " + currentHR);
                int restingHR = data.getRrsMs().get(0);
                logger.debug("RRS: " + restingHR);

            }

            @Override
            public void polarFtpFeatureReady(@NonNull String identifier) {
                super.polarFtpFeatureReady(identifier);
            }

            @Override
            public void sdkModeFeatureAvailable(@NonNull String identifier) {
                super.sdkModeFeatureAvailable(identifier);
            }

            @Override
            public void streamingFeaturesReady(@NonNull String identifier, @NonNull Set<? extends PolarBleApi.PolarDeviceDataType> features) {
                super.streamingFeaturesReady(identifier, features);
            }

        });
    }

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
                //watch index 2
                output.setData(hr);

            }
//            else if (characteristic.getUuid().equals(BATTERY_LEVEL)) {
//                parse(characteristic.getValue());
////                byte[] data = characteristic.getValue();
////                batLevel = data[1] & 0xFF;
////                logger.debug("bat level data received: " + batLevel);
//            }

//            output.setData(hr, batLevel);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            logger.debug("on services discovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                deviceInformation = btGatt.getService(DEVICE_INFORMATION_SERVICE);
                heartRateService = btGatt.getService(HEART_RATE_SERVICE);
                batteryService = btGatt.getService(BATTERY_SERVICE);
                //device info service
                modelNumber = deviceInformation.getCharacteristic(MODEL_NUMBER);
                manufacturerName = deviceInformation.getCharacteristic(MANUFACTURER_NAME);
                serialNumber = deviceInformation.getCharacteristic(SERIAL_NUMBER);
                //hr service
                heartRate = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT);
                bodyLocation = heartRateService.getCharacteristic(BODY_SENSOR_LOCATION);
                //battery service
                batteryLevel = batteryService.getCharacteristic(BATTERY_LEVEL);

                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                }
                gatt.setCharacteristicNotification(modelNumber, true); //device info service
                gatt.setCharacteristicNotification(manufacturerName, true); //device info service
                gatt.setCharacteristicNotification(serialNumber, true); //device info service
                gatt.setCharacteristicNotification(heartRate, true); //hr service
                gatt.setCharacteristicNotification(batteryLevel, true); //bat service
                gatt.setCharacteristicNotification(bodyLocation, true); //hr service

                BluetoothGattDescriptor descriptor = heartRate.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
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
