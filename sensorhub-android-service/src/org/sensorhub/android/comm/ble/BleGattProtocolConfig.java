/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm.ble;

import org.sensorhub.api.config.DisplayInfo;

public class BleGattProtocolConfig {
    @DisplayInfo(label="Device Name/Address", desc="BLE device name or MAC address")
    public String deviceAddress;

    @DisplayInfo(label="Service UUID", desc="GATT service UUID to connect to")
    public String serviceUUID;

    @DisplayInfo(label="Read Characteristic UUID", desc="UUID of characteristic to read from (notifications)")
    public String readCharUUID;

    @DisplayInfo(label="Write Characteristic UUID", desc="UUID of characteristic to write to")
    public String writeCharUUID;
}
