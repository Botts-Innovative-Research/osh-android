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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;
import androidx.annotation.RequiresPermission;

import net.opengis.sensorml.v20.PhysicalComponent;
import net.opengis.sensorml.v20.PhysicalSystem;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

    private BluetoothGatt btGatt;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner scanner;
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
    private BluetoothGattCharacteristic sensorMeasurements;
    private BluetoothGattCharacteristic derivedMeasurementsOne;
    private BluetoothGattCharacteristic derivedMeasurementsTwo;
    private BluetoothGattCharacteristic derivedMeasurementsThree;
    private BluetoothGattCharacteristic derivedMeasurementsFour;


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

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
        }

        addOutputs();
    }

    private void addOutputs() {
        environmentalOutput = new EnvironmentalOutput(this);
        environmentalOutput.doInit();
        addOutput(environmentalOutput, false);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void doStart() throws SensorException {

        startScan();

        eventThread = new HandlerThread("KestrelBallisticsThread");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null && (name.toLowerCase().contains(config.serialNumber))) {
                scanner.stopScan(this);
                connectToDevice(device);
            }
        }
    };


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        scanner = btAdapter.getBluetoothLeScanner();
        ScanFilter filter = new ScanFilter.Builder()
                // optionally filter by device name or service UUID
                //.setDeviceName("Kestrel 5700")
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToDevice(BluetoothDevice device) {
        btGatt = device.connectGatt(context, false, gattCallback);
    }


    Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if ( newState == BluetoothProfile.STATE_CONNECTED ) {
                btConnected = true;
                gatt.discoverServices();
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                // cleanup
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            for ( BluetoothGattService svc : gatt.getServices() ) {
                System.out.println("Kestrel --" + "Service: " + svc.getUuid());
                for (BluetoothGattCharacteristic c : svc.getCharacteristics()) {
                    System.out.println("Kestrel --"+ "  Char: " + c.getUuid() + " props=" + c.getProperties() + " perms=" + c.getPermissions());
                }
            }

            BluetoothGattService env = gatt.getService(ENVIRONMENTAL_SERVICE);
            if ( env != null ) {
                for (BluetoothGattCharacteristic ch : env.getCharacteristics()) {

                    if (ch.equals(SENSOR_MEASUREMENTS_CHAR)) {
                        sensorMeasurements = ch;
                        if (sensorMeasurements != null)
                            enableNotification(gatt, sensorMeasurements);
                    } else if (ch.equals(DERIVED_MEASUREMENTS_1_CHAR)) {
                        derivedMeasurementsOne = ch;
                        if (derivedMeasurementsOne != null)
                            enableNotification(gatt, derivedMeasurementsOne);
                    } else if (ch.equals(DERIVED_MEASUREMENTS_2_CHAR)) {
                        derivedMeasurementsTwo = ch;
                        if (derivedMeasurementsTwo != null)
                            enableNotification(gatt, derivedMeasurementsTwo);
                    } else if (ch.equals(DERIVED_MEASUREMENTS_3_CHAR)) {
                        derivedMeasurementsThree = ch;
                        if (derivedMeasurementsThree != null)
                            enableNotification(gatt, derivedMeasurementsThree);
                    } else if (ch.equals(DERIVED_MEASUREMENTS_4_CHAR)) {
                        derivedMeasurementsFour = ch;
                        if (derivedMeasurementsFour != null)
                            enableNotification(gatt, derivedMeasurementsFour);
                    } else
                        enableNotification(gatt, ch);
                }
            }

        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
            gatt.setCharacteristicNotification(ch, true);
            BluetoothGattDescriptor cccd = ch.getDescriptor(CLIENT_CONFIG);
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                descriptorWriteQueue.add(cccd);

                if (descriptorWriteQueue.size() == 1) {
                    gatt.writeDescriptor(cccd);
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            descriptorWriteQueue.poll();
            if (!descriptorWriteQueue.isEmpty()) {
                gatt.writeDescriptor(descriptorWriteQueue.peek());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] val = characteristic.getValue();
            handleCharacteristicBytes(characteristic.getUuid(), val);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if ( status != BluetoothGatt.GATT_SUCCESS ) {
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
            System.out.println("env snapshot: "+ env.snapshot());
            environmentalOutput.setData(env.snapshot());
            env.reset();
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void doStop() {
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt.close();
            btGatt = null;
        }
    }

    @Override
    public boolean isConnected() {
        return btConnected;
    }

    String manufacturerName;
    String serialNumber;
    String modelNumber;
    String firmwareVersion;
    String softwareVersion;

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            sensorDescription.setDescription("");

            SMLHelper helper = new SMLHelper();
            helper.edit((PhysicalSystem)sensorDescription)
                    .addClassifier(helper.classifiers.sensorType("Ballistics-Weather Meter"))

                    .addIdentifier(helper.identifiers.manufacturer("Nielsen-Kellerman (Kestrel Instruments)"))
                    .addIdentifier(helper.identifiers.modelNumber(modelNumber))
                    .addIdentifier(helper.identifiers.serialNumber(serialNumber))
                    .addIdentifier(helper.identifiers.firmwareVersion(firmwareVersion))
                    .addIdentifier(helper.identifiers.softwareVersion(softwareVersion))

                    .addCharacteristicList("operating_specs", helper.characteristics.operatingCharacteristics()
                            .add("voltage", helper.characteristics.operatingVoltageRange(0.9, 1.8, "V")) // Single AA battery: 0.9-1.8V typical range
                            .add("operating_temperature", helper.conditions.temperatureRange(-10.0, 55.0, "Cel")) // Typical Kestrel 5 series operating range
                            .add("storage_temperature", helper.conditions.temperatureRange(-30.0, 60.0, "Cel"))

                    )
//                            .add("battery_type", helper.characteristics.b("Lithium AA (recommended)")))

//                    .addCharacteristicList("environmental_specs", helper.characteristics.survivalCharacteristics()
//                            .add("waterproof_rating", helper.characteristics.("IP67 (1m for 30 min)"))
//                            .add("drop_test", helper.characteristics.("MIL-STD-810G"))
//                            .add("display_type", helper.characteristics.("High-resolution monochrome LCD"))
//                            .add("backlight", helper.characteristics("White/Red night-vision preserving")))

                    .addCapabilityList("wind_capabilities", helper.capabilities.systemCapabilities()
                            .add("wind_speed_range", helper.capabilities.measurementRange(0.6, 40.0, "m/s"))
                            .add("wind_speed_accuracy", helper.capabilities.relativeAccuracy(3.0)) // ±3% based on search results
                            .add("wind_speed_resolution", helper.capabilities.resolution(0.1, "m/s"))
                            .add("wind_direction_range", helper.capabilities.measurementRange(0.0, 360.0, "deg"))
                            .add("wind_direction_resolution", helper.capabilities.resolution(1.0, "deg")))

                    .addCapabilityList("temperature_capabilities", helper.capabilities.systemCapabilities()
                            .add("temp_range", helper.capabilities.measurementRange(-29.0, 70.0, "Cel")) // Typical thermistor range
                            .add("temp_accuracy", helper.capabilities.absoluteAccuracy(0.5, "Cel")) // ±0.5°C typical for Kestrel 5 series
                            .add("temp_resolution", helper.capabilities.resolution(0.1, "Cel")))

                    .addCapabilityList("humidity_capabilities", helper.capabilities.systemCapabilities()
                            .add("humidity_range", helper.capabilities.measurementRange(5.0, 95.0, "%"))
                            .add("humidity_accuracy", helper.capabilities.absoluteAccuracy(2.0, "%")) // ±2% RH typical
                            .add("humidity_resolution", helper.capabilities.resolution(0.1, "%"))
//                            .add("humidity_drift", helper.capabilities.drift(1, "RH/year"))
                    )

                    .addCapabilityList("pressure_capabilities", helper.capabilities.systemCapabilities()
                            .add("pressure_range", helper.capabilities.measurementRange(700.0, 1100.0, "hPa")) // Typical barometric range
                            .add("pressure_accuracy", helper.capabilities.absoluteAccuracy(1.5, "hPa")) // ±1.5 hPa typical
                            .add("pressure_resolution", helper.capabilities.resolution(0.1, "hPa"))
                    );

        }
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