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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.widget.Toast;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrData;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class Polar extends AbstractSensorModule<PolarConfig> {
    //    private final SensorMLBuilder smlBuilder;
    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());

//    private static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"); //GATT Characteristic and Object Type 0x2A38 Body Sensor Location
    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");  //GATT Characteristic and Object Type 0x2A19 Battery Level
//    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB"); //GATT Characteristic and Object Type 0x2A24 Model Number String
//    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
//    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_NAME = "POLAR H9 C18F2B23";
    BluetoothGattCharacteristic heartRate;
    BluetoothGattCharacteristic batteryLevel;
//    BluetoothGattCharacteristic serialNumber;
//    BluetoothGattCharacteristic modelNumber;
//    BluetoothGattCharacteristic manufacturerName;
//    BluetoothGattCharacteristic bodyLocation;
//    BluetoothGattService deviceInformation;

    Context context;
    PolarOutput output;
    BluetoothAdapter btAdapter;
    String deviceId ="C18F2B23";
    HandlerThread eventThread;
    BluetoothGatt btGatt;
    PolarBleApi api;
    ArrayList<Integer> HR = new ArrayList<>();
    ArrayList<Integer> RRS = new ArrayList<>();


    public Polar() {
        ArrayList<PhysicalComponent> smlComponents = new ArrayList<PhysicalComponent>();
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
           logger.debug("Bluetooth not enabled on device");
            Toast.makeText(context, "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
        }
        else{
            try{
                api.connectToDevice(deviceId);
                logger.debug("Connecting to device");
                Toast.makeText(context, "Ble connecting to device", Toast.LENGTH_SHORT).show();
            } catch (PolarInvalidArgument e) {
                e.printStackTrace();
                logger.debug("failed to connect to device: "+ deviceId);

            }
        }
        output = new PolarOutput(this);
        output.doInit();
        addOutput(output, false);

    }
    public void polarApi(){
        //set all desired features
        Set<PolarBleApi.PolarBleSdkFeature> features = new HashSet<>(Arrays.asList(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR ,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
        ));
        api = PolarBleApiDefaultImpl.defaultImplementation(context.getApplicationContext(), features);
        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void batteryLevelReceived(@NonNull String identifier, int level) {
                super.batteryLevelReceived(identifier, level);
                logger.debug("Battery: " + level);

//                output.newBatteryLevel(level);
            }

            @Override
            public void blePowerStateChanged(boolean powered) {
                super.blePowerStateChanged(powered);
                super.blePowerStateChanged(powered);
                if(powered){
                    context = SensorHubService.getContext();
                    final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
                    btAdapter = bluetoothManager.getAdapter();
                    if (btAdapter == null || !btAdapter.isEnabled()) {
                        Toast.makeText(context, "bluetooth adapter is null or not enabled", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(context, "Powered is false, no bluetooth connection", Toast.LENGTH_LONG).show();
                }
                logger.debug("Bluetooth state changed " + powered);

            }

            @Override
            public void bleSdkFeatureReady(String identifier, PolarBleApi.PolarBleSdkFeature feature) {
                super.bleSdkFeatureReady(identifier, feature);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
                super.deviceConnected(polarDeviceInfo);
                Toast.makeText(context,"Connected to polar device", Toast.LENGTH_SHORT).show();
                isConnected();
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                super.deviceConnecting(polarDeviceInfo);
                Toast.makeText(context,"Connecting to polar device", Toast.LENGTH_SHORT).show();
                logger.debug ("connecting"+ polarDeviceInfo.getDeviceId());
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                super.deviceDisconnected(polarDeviceInfo);
                Toast.makeText(context,"Disconnected from polar device", Toast.LENGTH_SHORT).show();
                logger.debug("Device disconnected " + polarDeviceInfo.getDeviceId());
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                super.disInformationReceived(identifier, uuid, value);
            }

            @Override
            public void hrFeatureReady(String identifier) {
                super.hrFeatureReady(identifier);
                Toast.makeText(context,"Heart Rate feature ready for polar device", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData.PolarHrSample data) {
                super.hrNotificationReceived(identifier, data);

                logger.debug("HR notifications received");
                int currentHR = data.getHr();
                logger.debug("Current HR: "+ currentHR);
                int restingHR = data.getRrsMs().get(0);
                logger.debug("RRS: "+ restingHR);

//                output.newHeartRate(currentHR);
            }

            @Override
            public void polarFtpFeatureReady(String identifier) {
                super.polarFtpFeatureReady(identifier);
            }

            @Override
            public void sdkModeFeatureAvailable(String identifier) {
                super.sdkModeFeatureAvailable(identifier);
            }

            @Override
            public void streamingFeaturesReady(String identifier, Set<? extends PolarBleApi.PolarDeviceDataType> features) {
                super.streamingFeaturesReady(identifier, features);
            }

            public void parse(BluetoothGattCharacteristic characteristic){
                int heartRate = 0;
                int batteryLevel = 0;
                if(HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())){
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if((flag & 0x01)!= 0){
                        format= BluetoothGattCharacteristic.FORMAT_UINT16;
                        logger.debug("HR format UINT16");
                    }else{
                        format= BluetoothGattCharacteristic.FORMAT_UINT8;
                        logger.debug("HR format UINT8");
                    }
                    heartRate = characteristic.getIntValue(format,1);
                }
                else if(BATTERY_LEVEL.equals(characteristic.getUuid())){
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if((flag & 0x01)!= 0){
                        format= BluetoothGattCharacteristic.FORMAT_UINT16;
                        logger.debug("format UINT16");
                    }else{
                        format= BluetoothGattCharacteristic.FORMAT_UINT8;
                        logger.debug("format UINT8");
                    }
                    batteryLevel = characteristic.getIntValue(format,1);
                }
                output.setData(heartRate,batteryLevel);
            }
        });
    }

    @Override
    public void doStart() throws SensorHubException {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        BluetoothDevice device = null;
        for (BluetoothDevice d : devices) {
            if (d.getName().equals(DEVICE_NAME)) {
                device = d;
            }
        }
        if (null == device) {
            throw new SensorHubException("Could not find Bluetooth device, unable to start.");
        }
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_DENIED) {
            // request permission
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        eventThread = new HandlerThread("PolarH9Event");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

    }

    @Override
    public void doStop() {
        try {
            api.disconnectFromDevice(deviceId);
        } catch (PolarInvalidArgument e) {
            e.printStackTrace();;
        }
    }
    @Override
    public boolean isConnected() {
        return true;
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        //
    }
//    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            boolean btConnected;
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                btConnected = true;
//                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
//                boolean discoveryStarted = gatt.discoverServices();
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                btConnected = false;
//            }
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            if (characteristic.getUuid().equals(HEART_RATE_MEASUREMENT)) {
//                parse(characteristic.getValue());//
//                byte[] data = characteristic.getValue();
//                int bpm = data[1] & 0xFF;
//                //TODO: clean up hr data
//                logger.debug("HR data received: "+ bpm);
//
//            } else if (characteristic.getUuid().equals(BATTERY_LEVEL)) {
//                parse(characteristic.getValue());
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
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            deviceInformation = btGatt.getService((DEVICE_INFORMATION_SERVICE));
//            modelNumber = deviceInformation.getCharacteristic(MODEL_NUMBER);
//            serialNumber = deviceInformation.getCharacteristic(SERIAL_NUMBER);
//            manufacturerName = deviceInformation.getCharacteristic(MANUFACTURER_NAME);
//            heartRate = deviceInformation.getCharacteristic(HEART_RATE_MEASUREMENT);
//            batteryLevel = deviceInformation.getCharacteristic(BATTERY_LEVEL);
//            bodyLocation = deviceInformation.getCharacteristic(BODY_SENSOR_LOCATION);
//
//            List<BluetoothGattCharacteristic> characteristicList = deviceInformation.getCharacteristics();
//            for(BluetoothGattCharacteristic characteristic: characteristicList){
//                for(BluetoothGattDescriptor descriptor : characteristic.getDescriptors()){
//
//
//                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        return;
//                    }
//                    gatt.setCharacteristicNotification(modelNumber, true);
//                    gatt.setCharacteristicNotification(serialNumber, true);
//                    gatt.setCharacteristicNotification(manufacturerName, true);
//                    gatt.setCharacteristicNotification(heartRate, true);
//                    gatt.setCharacteristicNotification(batteryLevel, true);
//                    gatt.setCharacteristicNotification(bodyLocation, true);
//                    gatt.writeDescriptor(descriptor);
//                    logger.debug("Connected device and registering info");
//                }
//
//            }
//
//        }
//
//        @Override
//        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            if (status != BluetoothGatt.GATT_SUCCESS) {
//                logger.info("Failed to write to characteristic");
//            }
//        }


//        private void parse(byte[] msgValue) {
//            int heartRate = 0;
//            boolean heartRateValid = false;
//            int lastHeartRate = 0;
//            for (int i = 0; i < msgValue.length - 8; i++) {
//                heartRateValid = packetValid(msgValue, i);
//                if (heartRateValid) {
//                    heartRate = msgValue[i + 5] & 0xFF;
//                    break;
//                }
//            }
//
//            if (!heartRateValid) {
//                heartRate = (int) (lastHeartRate * 0.8);
//                if (heartRate < 50) {
//                    heartRate = 0;
//                }
//            }
//            lastHeartRate = heartRate;
//
////            String[] msg = new String[0];
//            //heart rate output
//            output.sendData();
//        }
//        private boolean packetValid(byte[] buffer, int i){
//            boolean headerValid = (buffer[i]* 0xFF) == 0xFE;
//            boolean checkByteValid = (buffer[i + 2] & 0xFF) == (0xFF - (buffer[i + 1] & 0xFF));
//            boolean sequenceValid = (buffer[i + 3] & 0xFF) < 16;
//            return headerValid && checkByteValid && sequenceValid;
//        }
//    };
}
