//package org.sensorhub.impl.sensor.polar;
//
//import android.Manifest;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothGatt;
//import android.bluetooth.BluetoothGattCallback;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattDescriptor;
//import android.bluetooth.BluetoothGattService;
//import android.bluetooth.BluetoothProfile;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.HandlerThread;
//
//import com.polar.sdk.api.PolarBleApi;
//import com.polar.sdk.api.PolarBleApiDefaultImpl;
//import com.polar.sdk.api.errors.PolarInvalidArgument;
//
//import net.opengis.sensorml.v20.PhysicalComponent;
//
//import org.sensorhub.api.common.SensorHubException;
//import org.sensorhub.impl.sensor.AbstractSensorModule;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//public class polarh9 {
//}
///***************************** BEGIN LICENSE BLOCK ***************************
//
// The contents of this file are subject to the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one
// at http://mozilla.org/MPL/2.0/.
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
// for the specific language governing rights and limitations under the License.
//
// Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
// ******************************* END LICENSE BLOCK ***************************/
//
//package org.sensorhub.impl.sensor.polar;
//
//        import android.Manifest;
//        import android.bluetooth.BluetoothAdapter;
//        import android.bluetooth.BluetoothDevice;
//        import android.bluetooth.BluetoothGatt;
//        import android.bluetooth.BluetoothGattCallback;
//        import android.bluetooth.BluetoothGattCharacteristic;
//        import android.bluetooth.BluetoothGattDescriptor;
//        import android.bluetooth.BluetoothGattService;
//        import android.bluetooth.BluetoothProfile;
//        import android.content.Context;
//        import android.content.pm.PackageManager;
//        import android.os.Build;
//        import android.os.HandlerThread;
//
//        import com.polar.sdk.api.PolarBleApi;
//        import com.polar.sdk.api.PolarBleApiDefaultImpl;
//        import com.polar.sdk.api.errors.PolarInvalidArgument;
//
//        import net.opengis.sensorml.v20.PhysicalComponent;
//
//        import org.sensorhub.api.common.SensorHubException;
//        import org.sensorhub.impl.sensor.AbstractSensorModule;
//        import org.slf4j.Logger;
//        import org.slf4j.LoggerFactory;
//
//        import java.util.ArrayList;
//        import java.util.Arrays;
//        import java.util.HashSet;
//        import java.util.List;
//        import java.util.Set;
//        import java.util.UUID;
//
//
//public class Polar extends AbstractSensorModule<PolarConfig> {
//    //    private final SensorMLBuilder smlBuilder;
//    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());
//
//    private static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"); //GATT Characteristic and Object Type 0x2A38 Body Sensor Location
//    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
//    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");  //GATT Characteristic and Object Type 0x2A19 Battery Level
//    private static final UUID MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB"); //GATT Characteristic and Object Type 0x2A24 Model Number String
//    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
//    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
//    private static final String DEVICE_NAME = "POLAR H9 C18F2B23";
//    BluetoothGattCharacteristic heartRate;
//    BluetoothGattCharacteristic batteryLevel;
//    BluetoothGattCharacteristic serialNumber;
//    BluetoothGattCharacteristic modelNumber;
//    BluetoothGattCharacteristic manufacturerName;
//    BluetoothGattCharacteristic bodyLocation;
//    BluetoothGattService deviceInformation;
//
//    Context context;
//    PolarOutput output;
//    PolarBleApi api;
//    BluetoothAdapter btAdapter;
//    boolean deviceConnected = false;
//    String deviceId;
//    HandlerThread eventThread;
//    BluetoothGatt btGatt;
//
//    public Polar() {
//        ArrayList<PhysicalComponent> smlComponents = new ArrayList<PhysicalComponent>();
//    }
//
//    @Override
//    public void doInit() {
//        logger.info("Initializing Polar heart monitor Sensor");
//        this.xmlID = "POLAR-H9" + Build.SERIAL;
//        this.uniqueID = PolarConfig.getAndroidSensorsUid();
//
//        Set<PolarBleApi.PolarBleSdkFeature> features = new HashSet<>(Arrays.asList(
//                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
//                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
//                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
//                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
//                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
//                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
//        ));
//        api = PolarBleApiDefaultImpl.defaultImplementation(context, features);
//        api.setApiCallback(new PolarCallback());
//
//        try {
//            if (deviceConnected) {
//                api.disconnectFromDevice(deviceId);
//            } else {
//                api.connectToDevice(deviceId);
//            }
//        } catch (PolarInvalidArgument e) {
//            throw new RuntimeException(e);
//        }
//
//        output = new PolarOutput(this);
//        output.doInit();
//        addOutput(output, false);
//
//    }
//
//    @Override
//    public void doStart() throws SensorHubException {
//        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
//        BluetoothDevice device = null;
//        for (BluetoothDevice d : devices) {
//            if (d.getName().equals(DEVICE_NAME)) {
//                device = d;
//            }
//        }
//        if (null == device) {
//            throw new SensorHubException("Could not find Bluetooth device, unable to start.");
//        }
//        btGatt = device.connectGatt(context, true, gattCallback);
//
//        eventThread = new HandlerThread("PolarH9Event");
//        eventThread.start();
//
//    }
//
//    @Override
//    public void doStop() {
//
//    }
//
//    @Override
//    public boolean isConnected() {
//        return false;
//    }
//
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
//
//
////        private void parse(byte[] msgValue) {
////            int heartRate = 0;
////            boolean heartRateValid = false;
////            int lastHeartRate = 0;
////            for (int i = 0; i < msgValue.length - 8; i++) {
////                heartRateValid = packetValid(msgValue, i);
////                if (heartRateValid) {
////                    heartRate = msgValue[i + 5] & 0xFF;
////                    break;
////                }
////            }
////
////            if (!heartRateValid) {
////                heartRate = (int) (lastHeartRate * 0.8);
////                if (heartRate < 50) {
////                    heartRate = 0;
////                }
////            }
////            lastHeartRate = heartRate;
////
//////            String[] msg = new String[0];
////            //heart rate output
////            output.sendData();
////        }
////        private boolean packetValid(byte[] buffer, int i){
////            boolean headerValid = (buffer[i]* 0xFF) == 0xFE;
////            boolean checkByteValid = (buffer[i + 2] & 0xFF) == (0xFF - (buffer[i + 1] & 0xFF));
////            boolean sequenceValid = (buffer[i + 3] & 0xFF) < 16;
////            return headerValid && checkByteValid && sequenceValid;
////        }
//    };
//}
