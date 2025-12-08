/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import net.opengis.sensorml.v20.PhysicalComponent;
import net.opengis.sensorml.v20.PhysicalSystem;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.android.comm.ble.BleConfig;
import org.sensorhub.android.comm.ble.BleNetwork;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;


/**
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class Kestrel extends AbstractSensorModule<KestrelConfig> {
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    static final Logger logger = LoggerFactory.getLogger(Kestrel.class.getSimpleName());
    private Context context;
    private BleNetwork bleNetwork;

    private IGattClient gattClient;
    private boolean btConnected = false;
    EnvironmentalOutput environmentalOutput;
    private HandlerThread eventThread;
    private static final UUID ENVIRONMENTAL_SERVICE = UUID.fromString("03290000-eab4-dea1-b24e-44ec023874db");
    private static final UUID SENSOR_MEASUREMENTS_CHAR = UUID.fromString("03290310-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_1_CHAR = UUID.fromString("03290320-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_2_CHAR = UUID.fromString("03290330-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_3_CHAR = UUID.fromString("03290340-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_4_CHAR = UUID.fromString("03290350-eab4-dea1-b24e-44ec023874db");
    private static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private IGattCharacteristic sensorMeasurements;
    private IGattCharacteristic derivedMeasurementsOne;
    private IGattCharacteristic derivedMeasurementsTwo;
    private IGattCharacteristic derivedMeasurementsThree;
    private IGattCharacteristic derivedMeasurementsFour;


    KestrelEnvData env = new KestrelEnvData();


    public Kestrel() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }

    @Override
    public void doInit() {
        logger.info("Initializing sensor");
        this.xmlID = "KESTREL_WEATHER" + Build.SERIAL;
        this.uniqueID = KestrelConfig.getUid();

        context = SensorHubService.getContext();

        BleConfig bleConfig = new BleConfig();
        bleConfig.androidContext = context;

        bleNetwork = new BleNetwork();
        try {
            bleNetwork.init(bleConfig);
            bleNetwork.start();
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }

        addOutputs();
    }

    private void addOutputs() {
        environmentalOutput = new EnvironmentalOutput(this);
        environmentalOutput.doInit();
        addOutput(environmentalOutput, false);
    }

    @Override
    public void doStart() throws SensorException {

        if (bleNetwork == null) {
            logger.error("BLE network is not initialized");
        }

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        System.out.println("config.device address: "+ config.deviceAddress);
        bleNetwork.connectGatt(config.deviceAddress, gattCallback);

        eventThread = new HandlerThread("KestrelBallisticsThread");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());
    }


    Queue<IGattDescriptor> descriptorWriteQueue = new LinkedList<>();

    private GattCallback gattCallback = new GattCallback() {

        @Override
        public void onConnected(IGattClient gatt, int status) {
            gattClient = gatt;
            btConnected = true;
            logger.info("Kestrel Weather Monitor is connected");
            gattClient.discoverServices();
        }

        @Override
        public void onDisconnected(IGattClient gatt, int status) {
            btConnected = false;
            logger.info("Kestrel Device is disconnected");
        }

        @Override
        public void onServicesDiscovered(IGattClient gatt, int status) {
            IGattService enviroService = null;

            for (IGattService svc : gatt.getServices()) {
                System.out.println("Kestrel --" + "Service: " + svc.getType());
                UUID uuid = svc.getType();

                if (uuid.equals(ENVIRONMENTAL_SERVICE))
                    enviroService = svc;

                for (IGattCharacteristic c : svc.getCharacteristics()) {
                    System.out.println("Kestrel --" + "  Char: " + c.getType() + " props=" + c.getProperties() + " perms=" + c.getPermissions());
                }
            }


            for (IGattCharacteristic characteristic : enviroService.getCharacteristics()) {
                UUID uuid = characteristic.getType();

                if (uuid.equals(SENSOR_MEASUREMENTS_CHAR)) {
                    sensorMeasurements = characteristic;
                    if (sensorMeasurements != null)
                        enableNotification(sensorMeasurements);
                } else if (uuid.equals(DERIVED_MEASUREMENTS_1_CHAR)) {
                    derivedMeasurementsOne = characteristic;
                    if (derivedMeasurementsOne != null)
                        enableNotification(derivedMeasurementsOne);
                } else if (uuid.equals(DERIVED_MEASUREMENTS_2_CHAR)) {
                    derivedMeasurementsTwo = characteristic;
                    if (derivedMeasurementsTwo != null)
                        enableNotification(derivedMeasurementsTwo);
                } else if (uuid.equals(DERIVED_MEASUREMENTS_3_CHAR)) {
                    derivedMeasurementsThree = characteristic;
                    if (derivedMeasurementsThree != null)
                        enableNotification(derivedMeasurementsThree);
                } else if (uuid.equals(DERIVED_MEASUREMENTS_4_CHAR)) {
                    derivedMeasurementsFour = characteristic;
                    if (derivedMeasurementsFour != null)
                        enableNotification(derivedMeasurementsFour);
                }
            }
        }

        private void enableNotification(IGattCharacteristic ch) {
            if (ch == null)
                return;

            gattClient.setCharacteristicNotification(ch, true);

            Map<UUID,? extends IGattDescriptor> descriptors = ch.getDescriptors();

            IGattDescriptor cccd = descriptors.get(CLIENT_CONFIG);

            if (cccd != null) {
                cccd.setValue(ByteBuffer.wrap(IGattCharacteristic.ENABLE_NOTIFICATION_VALUE));
                descriptorWriteQueue.add(cccd);

                if (descriptorWriteQueue.size() == 1) {
                    gattClient.writeDescriptor(cccd);
                }
            }
        }

        @Override public void onDescriptorWrite(IGattClient gatt, IGattDescriptor descriptor, int status) {
            descriptorWriteQueue.poll();
            if (!descriptorWriteQueue.isEmpty()) {
                gatt.writeDescriptor(descriptorWriteQueue.peek());
            }
        }

        @Override
        public void onCharacteristicChanged(IGattClient gatt, IGattField field) {
            byte[] val = field.getValue().array();
            handleCharacteristicBytes(field.getType(), val);
        }

        @Override
        public void onCharacteristicWrite(IGattClient gatt, IGattField field, int status) {
            if ( status != IGattClient.GATT_SUCCESS ) {
                System.out.println("Failed to write to characteristic");
            }
        }
    };

    private void handleCharacteristicBytes(UUID uuid, byte[] raw) {
        if (uuid.equals(SENSOR_MEASUREMENTS_CHAR)) {
            double windSpeedRaw = (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.windSpeed = (windSpeedRaw == (short)0xFFFF) ? 0.0 : windSpeedRaw / 1000.0;

            double dryBulbTempRaw = (((raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8)));
            if (dryBulbTempRaw >= 0x8000)
                dryBulbTempRaw -= 0x10000;
            env.dryBulbTemp = (dryBulbTempRaw == (short)0x8001) ? 0.0 : dryBulbTempRaw / 100.0;

            double globeTempRaw = (raw[4] & 0xFF) | ((raw[5] & 0xFF) << 8);
            if (globeTempRaw >= 0x8000)
                globeTempRaw -= 0x10000;
            env.globeTemp = (globeTempRaw == (short)0x8001) ? 0.0 : globeTempRaw / 100.0;

            env.relativeHumidity = ((raw[6] & 0xFF) | ((raw[7] & 0xFF) << 8)) / 100.0;
            env.stationPress = ((raw[8] & 0xFF) | ((raw[9] & 0xFF) << 8)) / 10.0;
            env.magDirection = (raw[10] & 0xFF) | ((raw[11] & 0xFF) << 8);
            env.airSpeed = ((raw[12] & 0xFF) | ((raw[13] & 0xFF) << 8)) / 1000.0;
            env.markSensorMeasurementsReceived();
        } else if (uuid.equals(DERIVED_MEASUREMENTS_1_CHAR)) {
            double trueDirectionRaw =  (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.trueDirection = (trueDirectionRaw == (short)0xFFFF) ? 0.0 : trueDirectionRaw;

            double airDensityRaw = (raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8);
            env.airDensity = (airDensityRaw == (short)0xFFFF) ? 0.0 : airDensityRaw / 1000.0;

            int altitudeRaw = (raw[4] & 0xFF) | ((raw[5] & 0xFF) << 8) | ((raw[6] & 0xFF) << 16);
            altitudeRaw = (altitudeRaw << 8) >> 8;
            env.altitude = (altitudeRaw == 0x800001) ? 0.0 : altitudeRaw / 10.0;

            double pressureRaw = (raw[7] & 0xFF) | ((raw[8] & 0xFF) << 8);
            env.pressure = (pressureRaw == (short)0xFFFF) ? 0.0 : pressureRaw / 10.0;

            double crosswindRaw = (raw[9] & 0xFF) | ((raw[10] & 0xFF) << 8);
            env.crosswind = (crosswindRaw == (short)0xFFFF) ? 0.0 : crosswindRaw / 1000.0;

            int headwindRaw = (raw[11] & 0xFF) | ((raw[12] & 0xFF) << 8) | ((raw[13] & 0xFF) << 16);
            headwindRaw = (headwindRaw << 8) >> 8;
            env.headwind = (headwindRaw == 0x800001) ? 0.0 :  headwindRaw / 1000.0;

            int densityAltitudeRaw = (((raw[14] & 0xFF) | ((raw[15] & 0xFF) << 8)) | ((raw[16] & 0xFF) << 16));
            densityAltitudeRaw = (densityAltitudeRaw << 8) >> 8;
            env.densityAlt = (densityAltitudeRaw == 0x80001) ? 0.0 :  densityAltitudeRaw / 10.0;

            double relAirDensityRaw = (raw[17] & 0xFF) | ((raw[18] & 0xFF) << 8);
            env.relativeAirDensity = (relAirDensityRaw == (short)0xFFFF) ? 0.0 : relAirDensityRaw / 10.0;

            env.markDerived1Received();
        } else if (uuid.equals(DERIVED_MEASUREMENTS_2_CHAR)) {

            double dewPointRaw = (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.dewPoint = (dewPointRaw == (short)0x8001) ? 0.0 : dewPointRaw / 100.0;

            int heatIndexRaw = (((raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8)) | ((raw[4] & 0xFF) << 16));
            heatIndexRaw = (heatIndexRaw << 8) >> 8;
            env.heatIndex = (heatIndexRaw == 0x80001) ? 0.0 : heatIndexRaw / 100.0;

            double wetBulb = (raw[16] & 0xFF) | ((raw[17] & 0xFF) << 8);
            env.wetBulb = (wetBulb == (short)0x8001) ? 0.0 : wetBulb / 100.0;

            double windChillRaw = (raw[18] & 0xFF) | ((raw[19] & 0xFF) << 8);
            env.chill = (windChillRaw == (short)0x8001) ? 0.0 : windChillRaw / 100.0;

            env.markDerived2Received();
        }

        if (env.isComplete()) {
            System.out.println("env snapshot: "+ env.snapshot().toString());
            environmentalOutput.setData(env.snapshot());
            env.reset();
        }
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
        return btConnected;
    }

    public class KestrelEnvData {
        // Sensor measurements (SENSOR_MEASUREMENTS_CHAR)
        public double windSpeed = Double.NaN;
        public double dryBulbTemp = Double.NaN;
        public double globeTemp = Double.NaN;
        public double relativeHumidity = Double.NaN;
        public double stationPress = Double.NaN;
        public double magDirection = Double.NaN;
        public double airSpeed = Double.NaN;

        // Derived measurements 1 (DERIVED_MEASUREMENTS_1_CHAR)
        public double trueDirection = Double.NaN;
        public double airDensity = Double.NaN;
        public double altitude = Double.NaN;
        public double pressure = Double.NaN;
        public double crosswind = Double.NaN;
        public double headwind = Double.NaN;
        public double densityAlt = Double.NaN;
        public double relativeAirDensity = Double.NaN;

        // Derived measurements 2 (DERIVED_MEASUREMENTS_2_CHAR)
        public double dewPoint = Double.NaN;
        public double heatIndex = Double.NaN;
        public double wetBulb = Double.NaN;
        public double chill = Double.NaN;

        private boolean hasSensorMeasurements = false;
        private boolean hasDerived1 = false;
        private boolean hasDerived2 = false;
        private boolean hasDerived3 = false;
        private boolean hasDerived4 = false;

        public void markSensorMeasurementsReceived() {
            hasSensorMeasurements = true;
        }

        public void markDerived1Received() {
            hasDerived1 = true;
        }

        public void markDerived2Received() {
            hasDerived2 = true;
        }

        public void markDerived3Received() {
            hasDerived3 = true;
        }

        public void markDerived4Received() {
            hasDerived4 = true;
        }
        public boolean isComplete() {
            return hasDerived1 && hasDerived2 && hasSensorMeasurements;
        }

        public void reset() {
            windSpeed = dryBulbTemp = globeTemp = relativeHumidity = Double.NaN;
            stationPress = magDirection = airSpeed = Double.NaN;
            trueDirection = airDensity = altitude = pressure = Double.NaN;
            crosswind = headwind = densityAlt = relativeAirDensity = Double.NaN;
            dewPoint = heatIndex = chill = wetBulb = Double.NaN;

            // Reset flags
            hasSensorMeasurements = false;
            hasDerived1 = false;
            hasDerived2 = false;
            hasDerived3 = false;
            hasDerived4 = false;
        }

        public KestrelEnvData snapshot() {
            KestrelEnvData copy = new KestrelEnvData();
            copy.windSpeed = this.windSpeed;
            copy.dryBulbTemp = this.dryBulbTemp;
            copy.globeTemp = this.globeTemp;
            copy.relativeHumidity = this.relativeHumidity;
            copy.stationPress = this.stationPress;
            copy.magDirection = this.magDirection;
            copy.airSpeed = this.airSpeed;
            copy.trueDirection = this.trueDirection;
            copy.airDensity = this.airDensity;
            copy.altitude = this.altitude;
            copy.pressure = this.pressure;
            copy.crosswind = this.crosswind;
            copy.headwind = this.headwind;
            copy.densityAlt = this.densityAlt;
            copy.relativeAirDensity = this.relativeAirDensity;
            copy.dewPoint = this.dewPoint;
            copy.heatIndex = this.heatIndex;
            copy.chill = this.chill;
            copy.wetBulb = this.wetBulb;
            copy.hasSensorMeasurements = this.hasSensorMeasurements;
            copy.hasDerived1 = this.hasDerived1;
            copy.hasDerived2 = this.hasDerived2;
            copy.hasDerived3 = this.hasDerived3;
            copy.hasDerived4 = this.hasDerived4;
            return copy;
        }
    }
}