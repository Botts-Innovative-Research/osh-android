/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

package org.sensorhub.impl.sensor.kromek.d5;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekDetectorRadiometricsV1Report;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialAboutReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialCompressionEnabledReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialDoseInfoReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialEthernetConfigReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialOTGReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialRadiometricStatusReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialRemoteBackgroundStatusReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialRemoteExtendedIsotopeConfirmationStatusReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialRemoteIsotopeConfirmationReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialRemoteIsotopeConfirmationStatusReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialStatusReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialUIRadiationThresholdsReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialUTCReport;
import org.sensorhub.impl.sensor.kromek.d5.reports.KromekSerialUnitIDReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Main driver class for the Kromek D5 sensor. This class is responsible for initializing the
 * sensor, starting and stopping the sensor, and managing the outputs.
 *
 * @author Michael Elmore
 * @since Nov 2013
 */
public class D5Sensor extends AbstractSensorModule<D5Config> {
    static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);
    HashMap<Class<?>, D5Output> outputs;
    D5MessageRouter messageRouter;
    Boolean processLock;

    public D5Sensor() {
        logger.info("Creating D5 Sensor");
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void doInit() {
        logger.info("Initializing D5 Sensor");

        // Generate identifiers
        generateUniqueID("KROMEK_D5:", config.serialNumber);
        generateXmlID("urn:android:kromek:d5:", config.serialNumber);

        // Create and initialize output(s)
        outputs = new HashMap<>();
        createOutputs();

        for (D5Output output : outputs.values()) {
            logger.info("Initializing output: " + output.getName());
        }
    }

    @Override
    public void doStart() throws SensorException {
        logger.info("Starting D5 Sensor");

        String serialNumber = config.serialNumber.trim();

        BluetoothDevice device = getBluetoothDevice(serialNumber);
        if (device == null) {
            logger.error("D5 Device not found");
            return;
        }

        UUID uuid = device.getUuids()[0].getUuid();

        logger.info("D5 Device found: " + device.getName() + " " + device.getAddress() + " " + uuid);

        try {
            messageRouter = new D5MessageRouter(this, device);
            messageRouter.start();
        } catch (Exception e) {
            throw new SensorException("Error while initializing communications ", e);
        }

        processLock = false;
    }

    @Override
    public void doStop() {
        logger.info("Stopping sensor");
        processLock = true;

        if (messageRouter != null) {
            messageRouter.stop();
            messageRouter = null;
        }
    }

    public BluetoothDevice getBluetoothDevice(String serialNumber) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            logger.error("Device does not support Bluetooth");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                // Get a set of currently paired devices
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        logger.info("Paired device: " + deviceName + " " + deviceHardwareAddress);

                        if (Objects.equals(deviceName, serialNumber)) {
                            logger.info("Found device: " + deviceName);
                            bluetoothAdapter.cancelDiscovery();
                            return device;
                        }
                    }
                }
            } else {
                logger.error("Bluetooth is not enabled");
            }
        }

        return null;
    }

    /**
     * Create and initialize outputs
     */
    void createOutputs() {
        if (config.outputs.enableKromekDetectorRadiometricsV1Report) {
            D5Output output = new D5Output(this,
                    KromekDetectorRadiometricsV1Report.getReportName(),
                    KromekDetectorRadiometricsV1Report.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekDetectorRadiometricsV1Report());
            outputs.put(KromekDetectorRadiometricsV1Report.class, output);
        }
        if (config.outputs.enableKromekSerialRadiometricStatusReport) {
            D5Output output = new D5Output(this,
                    KromekSerialRadiometricStatusReport.getReportName(),
                    KromekSerialRadiometricStatusReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialRadiometricStatusReport());
            outputs.put(KromekSerialRadiometricStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialCompressionEnabledReport) {
            D5Output output = new D5Output(this,
                    KromekSerialCompressionEnabledReport.getReportName(),
                    KromekSerialCompressionEnabledReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialCompressionEnabledReport());
            outputs.put(KromekSerialCompressionEnabledReport.class, output);
        }
        if (config.outputs.enableKromekSerialEthernetConfigReport) {
            D5Output output = new D5Output(this,
                    KromekSerialEthernetConfigReport.getReportName(),
                    KromekSerialEthernetConfigReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialEthernetConfigReport());
            outputs.put(KromekSerialEthernetConfigReport.class, output);
        }
        if (config.outputs.enableKromekSerialStatusReport) {
            D5Output output = new D5Output(this,
                    KromekSerialStatusReport.getReportName(),
                    KromekSerialStatusReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialStatusReport());
            outputs.put(KromekSerialStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUnitIDReport) {
            D5Output output = new D5Output(this,
                    KromekSerialUnitIDReport.getReportName(),
                    KromekSerialUnitIDReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialUnitIDReport());
            outputs.put(KromekSerialUnitIDReport.class, output);
        }
        if (config.outputs.enableKromekSerialDoseInfoReport) {
            D5Output output = new D5Output(this,
                    KromekSerialDoseInfoReport.getReportName(),
                    KromekSerialDoseInfoReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialDoseInfoReport());
            outputs.put(KromekSerialDoseInfoReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationReport) {
            D5Output output = new D5Output(this,
                    KromekSerialRemoteIsotopeConfirmationReport.getReportName(),
                    KromekSerialRemoteIsotopeConfirmationReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteIsotopeConfirmationReport());
            outputs.put(KromekSerialRemoteIsotopeConfirmationReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationStatusReport) {
            D5Output output = new D5Output(this,
                    KromekSerialRemoteIsotopeConfirmationStatusReport.getReportName(),
                    KromekSerialRemoteIsotopeConfirmationStatusReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteIsotopeConfirmationStatusReport());
            outputs.put(KromekSerialRemoteIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUTCReport) {
            D5Output output = new D5Output(this,
                    KromekSerialUTCReport.getReportName(),
                    KromekSerialUTCReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialUTCReport());
            outputs.put(KromekSerialUTCReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteBackgroundStatusReport) {
            D5Output output = new D5Output(this,
                    KromekSerialRemoteBackgroundStatusReport.getReportName(),
                    KromekSerialRemoteBackgroundStatusReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteBackgroundStatusReport());
            outputs.put(KromekSerialRemoteBackgroundStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteExtendedIsotopeConfirmationStatusReport) {
            D5Output output = new D5Output(this,
                    KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.getReportName(),
                    KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteExtendedIsotopeConfirmationStatusReport());
            outputs.put(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUIRadiationThresholdsReport) {
            D5Output output = new D5Output(this,
                    KromekSerialUIRadiationThresholdsReport.getReportName(),
                    KromekSerialUIRadiationThresholdsReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialUIRadiationThresholdsReport());
            outputs.put(KromekSerialUIRadiationThresholdsReport.class, output);
        }
        if (config.outputs.enableKromekSerialAboutReport) {
            D5Output output = new D5Output(this,
                    KromekSerialAboutReport.getReportName(),
                    KromekSerialAboutReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialAboutReport());
            outputs.put(KromekSerialAboutReport.class, output);
        }
        if (config.outputs.enableKromekSerialOTGReport) {
            D5Output output = new D5Output(this,
                    KromekSerialOTGReport.getReportName(),
                    KromekSerialOTGReport.getPollingRate());
            addOutput(output, false);
            output.doInit(new KromekSerialOTGReport());
            outputs.put(KromekSerialOTGReport.class, output);
        }
    }
}
