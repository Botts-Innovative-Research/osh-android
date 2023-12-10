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

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

/**
 * Module descriptor for the Kromek D5 sensor
 *
 * @author Michael Elmore
 * @since Nov 2013
 */
public class D5Descriptor implements IModuleProvider {
    @Override
    public String getModuleName() {
        return "Kromek D5 Driver";
    }

    @Override
    public String getModuleDescription() {
        return "Driver for the Kromek D5 radiation sensor";
    }

    @Override
    public String getModuleVersion() {
        return "0.1";
    }

    @Override
    public String getProviderName() {
        return "Botts Innovative Research, Inc.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return D5Sensor.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return D5Config.class;
    }
}
