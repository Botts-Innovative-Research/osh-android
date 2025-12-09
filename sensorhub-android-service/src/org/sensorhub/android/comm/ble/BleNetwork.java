/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm.ble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IBleNetwork;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;


public class BleNetwork extends AbstractModule<BleConfig> implements IBleNetwork<BleConfig>
{
    static final Logger log = LoggerFactory.getLogger(BleNetwork.class.getSimpleName());
    
    Context aContext;
    BluetoothAdapter aBleAdapter;

    IDeviceScanner scanner;
    
    @Override
    public String getInterfaceName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public NetworkType getNetworkType()
    {
        return NetworkType.BLUETOOTH_LE;
    }
    
    
    @Override
    public boolean isOfType(NetworkType type)
    {
        return (type == getNetworkType());
    }


    @Override
    public IDeviceScanner getDeviceScanner()
    {
        if (scanner == null)
            scanner = new BleDeviceScanner();
        return scanner;
    }

    class BleDeviceScanner implements IDeviceScanner {
        private BluetoothLeScanner bluetoothLeScanner;
        private ScanCallback scanCallback;
        private boolean isScanning = false;
        private IDeviceScanCallback deviceScanCallback;

        @Override
        public void startScan(IDeviceScanCallback callback) {
            startScan(callback, null);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void startScan(IDeviceScanCallback callback, String idRegex) {
            log.debug("Starting Bluetooth LE scan");

            if (isScanning)
                return;

            if (aBleAdapter == null)
                return;

            this.deviceScanCallback = callback;
            bluetoothLeScanner = aBleAdapter.getBluetoothLeScanner();

            if (bluetoothLeScanner == null)
                return;

            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (result == null || result.getDevice() == null)
                        return;

                    BluetoothDevice device = result.getDevice();
                    String address = device.getAddress();
                    @SuppressLint("MissingPermission") String  name = device.getName();

                    log.debug("device found: name: {} address: {} rssi: {}" + name + address, result.getRssi());

                    BleDeviceInfo deviceInfo = new BleDeviceInfo(device, result.getRssi());

                    if (deviceScanCallback != null)
                        deviceScanCallback.onDeviceFound(deviceInfo);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    isScanning = false;
                    if (deviceScanCallback != null)
                        deviceScanCallback.onScanError(new Throwable("Scan Failed with error: " + errorCode));
                }
            };

            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0);

            List<ScanFilter> filters = new ArrayList<>();
            // filters.add(new ScanFilter.Builder().setDeviceName("DeviceName").build());

            bluetoothLeScanner.startScan(filters, settingsBuilder.build(), scanCallback);
            isScanning = true;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void stopScan() {
            log.debug("Stopping Bluetooth LE scan");
            if (scanner.isScanning()) {
                scanner.stopScan();
                scanner = null;
            }
            if (bluetoothLeScanner != null)
                bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
            scanCallback = null;
            deviceScanCallback = null;
        }

        @Override
        public boolean isScanning() {
            return isScanning;
        }
    }

    public static class BleDeviceInfo implements IDeviceInfo {

        private final BluetoothDevice device;
        private final int rssi;

        public BleDeviceInfo(BluetoothDevice device, int rssi){
            this.device = device;
            this.rssi = rssi;
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getSignalLevel() {
            return String.valueOf(rssi);
        }

        @Override
        public ICommConfig getCommConfig() {
            return null;
        }

        @SuppressLint("MissingPermission")
        public String getName() {
            return device.getName();
        }

        public String getAddress() {
            return device.getAddress();
        }

        public int getRssi() {
            return rssi;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
    }

    @Override
    public Collection<? extends INetworkInfo> getAvailableNetworks()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void start() throws SensorHubException
    {
        this.aContext = config.androidContext;
        BluetoothManager bluetoothManager = (BluetoothManager) aContext.getSystemService(Context.BLUETOOTH_SERVICE);
        aBleAdapter = bluetoothManager.getAdapter();
    }


    @Override
    public void stop() throws SensorHubException
    {
        // TODO Auto-generated method stub

        if (scanner != null && scanner.isScanning())
            scanner.stopScan();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stubs
    }


    @Override
    public boolean startPairing(String address)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void connectGatt(String address, GattCallback callback)
    {
        if (aBleAdapter == null) {
            log.error("BLE Adapter is null");
            throw new IllegalStateException("BLE Adapter is null");
        }
        BluetoothDevice btDevice = aBleAdapter.getRemoteDevice(address);
        GattClientImpl client = new GattClientImpl(aContext, btDevice, callback);
        client.connect();

        log.info("Connecting to BT device " + address + "...");
    }

}

