/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm.ble;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BleGattCommProvider extends AbstractModule<BleGattCommProviderConfig> implements ICommProvider<BleGattCommProviderConfig> {
    static final Logger log = LoggerFactory.getLogger(BleGattCommProvider.class.getSimpleName());

    private BleNetwork bleNetwork;
    private IGattClient gattClient;
    private IGattCharacteristic readChar;
    private IGattCharacteristic writeChar;

    public BleGattCommProvider() {}

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public void start() throws SensorHubException {
        BleConfig bleConfig = new BleConfig();
        bleConfig.androidContext = SensorHubService.getContext();

        bleNetwork = new BleNetwork();
        bleNetwork.init(bleConfig);
        bleNetwork.start();

        bleNetwork.connectGatt(config.protocol.deviceAddress, gattCallback);
    }

    @Override
    public void stop() throws SensorHubException {
        if (gattClient != null) {
            gattClient.disconnect();
            gattClient.close();
            gattClient = null;
        }

        if (bleNetwork != null) {
            bleNetwork.stop();
            bleNetwork = null;
        }

    }

    @Override
    public void cleanup() throws SensorHubException {
    }

    GattCallback gattCallback = new GattCallback() {
        @Override
        public void onConnected(IGattClient gatt, int status) {
            gattClient = gatt;
            gatt.discoverServices();
        }

        @Override
        public void onDisconnected(IGattClient gatt, int status) {
            log.warn("BLE GATT disconnected, status: ", status);
        }

        @Override
        public void onServicesDiscovered(IGattClient gatt, int status) {
           if (status != IGattClient.GATT_SUCCESS) {
               log.error("Service discovery failed, status: ", status);
               return;
           }

           IGattService gattService = null;
           UUID serviceUUID = UUID.fromString(config.protocol.serviceUUID);
           UUID readUUID = UUID.fromString(config.protocol.readCharUUID);
           UUID writeUUID = UUID.fromString(config.protocol.writeCharUUID);

            for (IGattService service : gatt.getServices()) {
                if (service.getType().equals(serviceUUID)) {
                   gattService = service;
                }
            }

            for (IGattCharacteristic characteristic : gattService.getCharacteristics()) {
                UUID uuid = characteristic.getType();

                if (uuid.equals(readUUID))
                    readChar = characteristic;
                else if (uuid.equals(writeUUID)) {
                    writeChar = characteristic;
                }
                break;
            }
            if (readChar == null || writeChar == null) {
                log.error("Could not find required characteristics");
            }

            gatt.setCharacteristicNotification(readChar, true);


        }

        @Override
        public void onCharacteristicChanged(IGattClient gatt, IGattField characteristic) {
            if (readChar != null && characteristic.getType().equals(readChar.getType())) {
                byte[] data = characteristic.getValue().array();
                if (data.length > 0) {
                    // do stuff
                }
            }
        }

        @Override
        public void onCharacteristicRead(IGattClient gatt, IGattField characteristic, int status) {
            if (status == IGattClient.GATT_SUCCESS && characteristic.getType().equals(readChar.getType())) {
                byte[] data = characteristic.getValue().array();
                if (data.length > 0) {
                    // handle data
                }
            }
        }
    };

}
