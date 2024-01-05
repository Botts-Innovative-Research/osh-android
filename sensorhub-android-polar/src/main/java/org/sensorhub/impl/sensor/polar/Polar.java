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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.HandlerThread;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrData;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class Polar extends AbstractSensorModule<PolarConfig> {
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());
    private static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"); //GATT Characteristic and Object Type 0x2A38 Body Sensor Location
    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");  //GATT Characteristic and Object Type 0x2A19 Battery Level
    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB"); //GATT Characteristic and Object Type 0x2A24 Model Number String
    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final String SENSOR_DEVICE_NAME = "Polar H9 C18F2B23";
    private BluetoothGattCharacteristic heartRate;
    private BluetoothGattCharacteristic batteryLevel;
    private BluetoothGattCharacteristic serialNumber;
    private BluetoothGattCharacteristic modelNumber;
    private BluetoothGattCharacteristic manufacturerName;
    private BluetoothGattCharacteristic bodyLocation;
    private BluetoothGattService deviceInformation;
    private BluetoothGatt btGatt;
    private Context context;
    private BluetoothAdapter btAdapter;
    PolarOutput output;
    String deviceId = "C18F2B23";
    private HandlerThread eventThread;
    PolarBleApi api;
    String device_bt = "CB:14:3B:29:BF:47";

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
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
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
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(SENSOR_DEVICE_NAME)) {
                polarDevice = device;
                btAdapter.cancelDiscovery();
            }
        }
        if (polarDevice == null) {
            throw new SensorException("No ble device found, cannot start module");
        }
        btGatt = polarDevice.connectGatt(context, true, gattCallback);
        logger.info("device found: " + polarDevice.getName() + " " + polarDevice.getAddress());

        eventThread = new HandlerThread("PolarH9_Event");
        eventThread.start();
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

    private void polarApi() {
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

//                output.newHeartRate(currentHR);
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

            public void parse(BluetoothGattCharacteristic characteristic) {
                int heartRate = 0;
                int batteryLevel = 0;
                if (HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        logger.debug("HR format UINT16");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        logger.debug("HR format UINT8");
                    }
                    heartRate = characteristic.getIntValue(format, 1);
                } else if (BATTERY_LEVEL.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        logger.debug("format UINT16");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        logger.debug("format UINT8");
                    }
                    batteryLevel = characteristic.getIntValue(format, 1);
                }
                output.setData(heartRate);
            }
        });
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
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
            if (characteristic.getUuid().equals(HEART_RATE_MEASUREMENT)) {
                parse(characteristic.getValue());
//                byte[] data = characteristic.getValue();
//                hr = data[1] & 0xFF;

            } else if (characteristic.getUuid().equals(BATTERY_LEVEL)) {
                parse(characteristic.getValue());
//                byte[] data = characteristic.getValue();
//                batLevel = data[1] & 0xFF;
//                logger.debug("bat level data received: " + batLevel);
            }
//            } else if (characteristic.getUuid().equals(BODY_SENSOR_LOCATION)) {
//                byte[] characteristicData = characteristic.getValue();
//                byte firstByte = 0;
//                if (characteristicData == null || characteristicData.length == 0) {
//                    logger.debug("Error characteristic array is null");
//                } else {
//                    firstByte = characteristicData[0];
//                }
//                String location = "";
//                switch (firstByte) {
//                    case 0:
//                        location = "chest";
//                        break;
//                    case 1:
//                        location = "wrist";
//                        break;
//                    case 2:
//                        location = "Other";
//                        break;
//                }
//                logger.debug("Body Sensor Location: {}", location);
//            }}
//            output.insertData(hr, batLevel);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            deviceInformation = btGatt.getService((DEVICE_INFORMATION_SERVICE));
            modelNumber = deviceInformation.getCharacteristic(MODEL_NUMBER);
//            serialNumber = deviceInformation.getCharacteristic(SERIAL_NUMBER);
//            manufacturerName = deviceInformation.getCharacteristic(MANUFACTURER_NAME);
            heartRate = deviceInformation.getCharacteristic(HEART_RATE_MEASUREMENT);
            batteryLevel = deviceInformation.getCharacteristic(BATTERY_LEVEL);
            bodyLocation = deviceInformation.getCharacteristic(BODY_SENSOR_LOCATION);

            List<BluetoothGattCharacteristic> characteristicList = deviceInformation.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic: characteristicList){
                for(BluetoothGattDescriptor descriptor : characteristic.getDescriptors()){

                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    gatt.setCharacteristicNotification(modelNumber, true);
//                    gatt.setCharacteristicNotification(serialNumber, true);
//                    gatt.setCharacteristicNotification(manufacturerName, true);
                    gatt.setCharacteristicNotification(heartRate, true);
                    gatt.setCharacteristicNotification(batteryLevel, true);
                    gatt.setCharacteristicNotification(bodyLocation, true);
                    gatt.writeDescriptor(descriptor);
                    logger.debug("Connected device and registering info");
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logger.info("Failed to write to characteristic");
            }
        }

        private void parse(byte[] characteristicData) {
            int hr = 0;
            boolean heartRateValid = false;

            for(int i =0; i<characteristicData.length-8; i++){
                heartRateValid = packetValid(characteristicData, i);
                if(heartRateValid){
                    hr = characteristicData[i +5] & 0xFF;
                    break;
                }
            }
            output.setData(hr);
        }
        private boolean packetValid(byte[] buffer, int i){
            boolean headerValid = (buffer[i]* 0xFF) == 0xFE;
            boolean checkByteValid = (buffer[i + 2] & 0xFF) == (0xFF - (buffer[i + 1] & 0xFF));
            boolean sequenceValid = (buffer[i + 3] & 0xFF) < 16;
            return headerValid && checkByteValid && sequenceValid;
        }
    };
}
