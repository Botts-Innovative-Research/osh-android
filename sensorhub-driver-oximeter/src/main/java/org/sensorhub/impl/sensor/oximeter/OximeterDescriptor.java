/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.oximeter;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Descriptor of Sleep Oxygen Monitor driver module for automatic discovery
 * by the ModuleRegistry
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 06/06/2024
 */
public class OximeterDescriptor implements IModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "Sleep Oxygen Monitor Driver";
    }
    @Override
    public String getModuleDescription()
    {
        return "Driver supporting Sleep Oxygen Monitors";
    }
    @Override
    public String getModuleVersion()
    {
        return "0.1";
    }
    @Override
    public String getProviderName()
    {
        return "Botts Innovative Research, Inc.";
    }
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return Oximeter.class;
    }
    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return OximeterConfig.class;
    }

}
