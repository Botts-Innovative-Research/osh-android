/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import android.content.Context;
import android.provider.Settings;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.android.audio.AudioEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig;


/**
 * <p>
 * Configuration class for the generic Android sensors driver
 * </p>
 *

 */
public class AndroidModuleConfig extends SensorConfig
{

    public String deviceName;
    public String runName;
    public String runDescription;

}
