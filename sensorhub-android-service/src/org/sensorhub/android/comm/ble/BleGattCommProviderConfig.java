/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm.ble;

import org.sensorhub.api.comm.CommProviderConfig;

public class BleGattCommProviderConfig extends CommProviderConfig<BleGattProtocolConfig> {
    public BleGattCommProviderConfig() {
        this.moduleClass = BleGattCommProvider.class.getCanonicalName();
        this.protocol = new BleGattProtocolConfig();
    }
}
