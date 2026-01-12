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
import android.bluetooth.BluetoothManager;
import android.content.Context;
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
import org.sensorhub.android.comm.ble.BleConfig;
import org.sensorhub.android.comm.ble.BleNetwork;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.api.common.SensorHubException;
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

/**
 *
 * @author Kalyn Stricklin
 * @since Jan 13, 2023
 */
public class Polar extends AbstractSensorModule<PolarConfig> {
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());
    private static final UUID HEARTRATE_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothAdapter btAdapter;
    BatteryOutput batteryOutput;
    HeartRateOutput heartRateOutput;
    private HandlerThread eventThread;
    PolarBleApi api;

    private boolean btConnected = false;
    private BleNetwork bleNetwork;
    private IGattClient gattClient;
    private IGattCharacteristic heartRateChar;
    private IGattCharacteristic batteryLevelChar;

    public Polar() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }

    @Override
    public void doInit() {
        logger.info("Initializing Polar heart monitor sensor");
        this.xmlID = "POLAR_" + Build.SERIAL;
        this.uniqueID = config.getUid();

        context = SensorHubService.getContext();

        PolarApiCallback();

        BleConfig bleConfig = new BleConfig();
        bleConfig.androidContext = context;

        bleNetwork = new BleNetwork();
        try {
            bleNetwork.init(bleConfig);
            bleNetwork.start();
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }

        batteryOutput = new BatteryOutput(this);
        batteryOutput.doInit();
        addOutput(batteryOutput, false);

        heartRateOutput = new HeartRateOutput(this);
        heartRateOutput.doInit();
        addOutput(heartRateOutput, false);
    }

    @Override
    public void doStart() throws SensorException {
        if (bleNetwork == null) {
            logger.error("BLE network is not initialized");
        }

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        bleNetwork.connectGatt(config.device_name, gattCallback);

        eventThread = new HandlerThread("PolarMonitorThread");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());
    }

    @Override
    public void doStop() {
        if (gattClient != null) {
            gattClient.disconnect();
            gattClient.close();
            gattClient = null;
        }

        if (bleNetwork != null) {
            try {
                bleNetwork.stop();
            } catch (SensorHubException e) {
                logger.error("Error stopping BLE network");
            }
            bleNetwork = null;
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }


    private GattCallback gattCallback = new GattCallback() {
        @Override
        public void onConnected(IGattClient gatt, int status) {
            gattClient = gatt;
            btConnected = true;

            logger.info("Polar HR Monitor is connected");

            gattClient.discoverServices();

        }

        @Override
        public void onDisconnected(IGattClient gatt, int status) {
            btConnected = false;
            logger.info("Polar HR Monitor is disconnected");
        }


        @Override
        public void onServicesDiscovered(IGattClient gatt, int status) {
            IGattService hrService = null;
            IGattService batteryService = null;

            for (IGattService service : gattClient.getServices()) {

                UUID uuid = service.getType();
                if (uuid.equals(HEART_RATE_SERVICE)) {
                    hrService = service;
                } else if (uuid.equals(BATTERY_SERVICE)) {
                    batteryService = service;
                }
            }

            for (IGattCharacteristic characteristic : hrService.getCharacteristics()) {
                UUID uuid = characteristic.getType();

                if (uuid.equals(HEARTRATE_CHARACTERISTIC_UUID)) {
                    heartRateChar = characteristic;
                }
            }

            for (IGattCharacteristic characteristic : batteryService.getCharacteristics()) {
                UUID uuid = characteristic.getType();

                if (uuid.equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                    batteryLevelChar = characteristic;
                }
            }

            if (heartRateChar != null) {
                gattClient.setCharacteristicNotification(heartRateChar, true);
            }

            if (batteryLevelChar != null) {
                gattClient.setCharacteristicNotification(batteryLevelChar, true);
            }

            if (batteryLevelChar != null) {
                gattClient.readCharacteristic(batteryLevelChar);
            }
        }

        @Override
        public void onCharacteristicChanged(IGattClient gatt, IGattField characteristic) {

            UUID uuid = characteristic.getType();

            if (uuid.equals(HEARTRATE_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue().array();
                int hr = parseHeartRate(data);
                heartRateOutput.setData(hr);
            }

            if (uuid.equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue().array();
                int battery = data[0] & 0xFF;
                batteryOutput.setData(battery);
            }
        }
    };

    private int parseHeartRate(byte[] data) {
        if (data == null || data.length == 0)
            return 0;

        int heartRate = 0;
        int format = data[0] & 0x01;

        if (format == 0) {
            heartRate = data[1] & 0xFF;
        } else {
            heartRate = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
        }

        return heartRate;
    }

    public void PolarApiCallback() {
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

}
