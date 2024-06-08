/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sleepMonitor;

import android.content.Context;
import android.provider.Settings;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.sleepMonitor.outputs.SleepMonitorOutputs;


/**
 * <p>
 * Configuration class for the generic Android Sleep Monitor driver
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 05/26/2024
 */
public class SleepMonitorConfig extends SensorConfig
{
    private final SleepMonitorOutputs output;
    public String deviceName;
    public String deviceID;
    public SleepMonitorOutputs getOutputs()
    {
        return this.output;
    }
    public String getDeviceName(){
        return this.deviceName;
    }
    public String getDeviceID(){
        return this.deviceID;
    }

    public SleepMonitorConfig(String deviceName, String deviceID, boolean enableHR, boolean enableOxygen)
    {
        this.moduleClass = SleepMonitor.class.getCanonicalName();
        this.deviceID = deviceID;
        this.deviceName = deviceName;
        this.output = new SleepMonitorOutputs(enableHR, enableOxygen);
    }

    public static String getUid() {
        Context context = SensorHubService.getContext();
        return "urn:android:" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
