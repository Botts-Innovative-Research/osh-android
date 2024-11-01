/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.oximeter.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.oximeter.Oximeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Abstract base for data interfaces connecting to Sleep Oxygen Monitor
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 06/06/2024
 */

public class PulseRateOutput extends AbstractSensorOutput<Oximeter>{
    public static DataComponent dataStruct;
    public static DataEncoding dataEncoding;
    private static final Logger logger = LoggerFactory.getLogger(PulseRateOutput.class);
    public PulseRateOutput(Oximeter parent) {
        super("Sleep Monitor Pulse Rate Data", parent);
    }
    public void doInit(){
        logger.debug("Initializing Output");
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(name)
                .definition(SWEHelper.getPropertyUri("Oximeter"))
                .label("Oximeter Pulse Rate Data")
                .addField("sampleTime", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build())
                .addField("pulseRate", fac.createQuantity()
                        .label("Pulse Rate")
                        .definition(SWEHelper.getPropertyUri("PulseRate"))
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
        logger.debug("Initializing Output Complete");
    }
    @Override
    public double getAverageSamplingPeriod() {return 0;}
    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }

    public void setPulseRateData(float pulseRate){
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setLongValue(0, System.currentTimeMillis() / 1000);
        dataBlock.setFloatValue(1, pulseRate);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));

    }

}
