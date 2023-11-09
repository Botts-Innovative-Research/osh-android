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
            throw new SensorException("Error while initializing message router ", e);
        }

        processLock = false;
    }

    @Override
    public void doStop() {
        logger.info("Stopping sensor");
        processLock = true;

        if (messageRouter != null) {
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
            KromekDetectorRadiometricsV1Report report = new KromekDetectorRadiometricsV1Report();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekDetectorRadiometricsV1Report.class, output);
        }
        if (config.outputs.enableKromekSerialRadiometricStatusReport) {
            KromekSerialRadiometricStatusReport report = new KromekSerialRadiometricStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRadiometricStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialCompressionEnabledReport) {
            KromekSerialCompressionEnabledReport report = new KromekSerialCompressionEnabledReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialCompressionEnabledReport.class, output);
        }
        if (config.outputs.enableKromekSerialEthernetConfigReport) {
            KromekSerialEthernetConfigReport report = new KromekSerialEthernetConfigReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialEthernetConfigReport.class, output);
        }
        if (config.outputs.enableKromekSerialStatusReport) {
            KromekSerialStatusReport report = new KromekSerialStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUnitIDReport) {
            KromekSerialUnitIDReport report = new KromekSerialUnitIDReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUnitIDReport.class, output);
        }
        if (config.outputs.enableKromekSerialDoseInfoReport) {
            KromekSerialDoseInfoReport report = new KromekSerialDoseInfoReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialDoseInfoReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationReport) {
            KromekSerialRemoteIsotopeConfirmationReport report = new KromekSerialRemoteIsotopeConfirmationReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteIsotopeConfirmationReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationStatusReport) {
            KromekSerialRemoteIsotopeConfirmationStatusReport report = new KromekSerialRemoteIsotopeConfirmationStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUTCReport) {
            KromekSerialUTCReport report = new KromekSerialUTCReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUTCReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteBackgroundStatusReport) {
            KromekSerialRemoteBackgroundStatusReport report = new KromekSerialRemoteBackgroundStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteBackgroundStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteExtendedIsotopeConfirmationStatusReport) {
            KromekSerialRemoteExtendedIsotopeConfirmationStatusReport report = new KromekSerialRemoteExtendedIsotopeConfirmationStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUIRadiationThresholdsReport) {
            KromekSerialUIRadiationThresholdsReport report = new KromekSerialUIRadiationThresholdsReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUIRadiationThresholdsReport.class, output);
        }
        if (config.outputs.enableKromekSerialAboutReport) {
            KromekSerialAboutReport report = new KromekSerialAboutReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialAboutReport.class, output);
        }
        if (config.outputs.enableKromekSerialOTGReport) {
            KromekSerialOTGReport report = new KromekSerialOTGReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialOTGReport.class, output);
        }
    }
}
