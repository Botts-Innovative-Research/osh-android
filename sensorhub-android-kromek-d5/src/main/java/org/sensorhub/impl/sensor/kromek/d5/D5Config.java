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

import android.content.Context;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration for the Kromek D5 sensor
 *
 * @author Michael Elmore
 * @since Nov 2013
 */
public class D5Config extends SensorConfig {
    private final Context context = SensorHubService.getContext();

    public D5Config() {
        moduleClass = D5Sensor.class.getCanonicalName();
    }

    public Context getContext() {
        return context;
    }

    /**
     * The unique identifier for the configured sensor.
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "D5M100000";
}
