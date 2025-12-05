/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;


/**
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class BallisticsOutput extends AbstractSensorOutput<Kestrel>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    private static final String SENSOR_OUTPUT_NAME = "ballistics";
    private static final String SENSOR_OUTPUT_LABEL = "Ballistics Output";
    private static final Logger logger = LoggerFactory.getLogger(BallisticsOutput.class);

    protected BallisticsOutput(Kestrel parent) {
        super(SENSOR_OUTPUT_NAME, parent);
    }
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .addField("samplingTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("elevation", fac.createQuantity()
                        .label("Elevation")
                        .definition(SWEHelper.getPropertyUri("Elevation"))
                        .build())
                .addField("windage", fac.createQuantity()
                        .label("Windage")
                        .definition(SWEHelper.getPropertyUri("Windage"))
                        .build())
                .addField("spinDrift", fac.createQuantity()
                        .label("Spin Drift")
                        .definition(SWEHelper.getPropertyUri("SpinDrift"))
                        .build())
                .addField("drop", fac.createQuantity()
                        .label("Drop")
                        .definition(SWEHelper.getPropertyUri("Drop"))
                        .build())
                .addField("targetRange", fac.createQuantity()
                        .label("Target Range")
                        .definition(SWEHelper.getPropertyUri("TargetRange"))
                        .build())
                .addField("corrections", fac.createQuantity()
                        .label("Correction")
                        .description("")
                        .definition(SWEHelper.getPropertyUri("Correction"))
                        .build())
                .addField("firingSolutionFlag", fac.createText()
                        .label("Elevation")
                        .definition(SWEHelper.getPropertyUri("Elevation"))
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
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

    public void setData(double elevation, double windage, double spinDrift, double drop, double targetRange, double corrections, String firingFlag) {
        DataBlock dataBlock = dataStruct.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setDoubleValue(1, elevation);
        dataBlock.setDoubleValue(2, windage);
        dataBlock.setDoubleValue(3, spinDrift);
        dataBlock.setDoubleValue(4, drop);
        dataBlock.setDoubleValue(5, targetRange);
        dataBlock.setDoubleValue(6, corrections);
        dataBlock.setStringValue(7, firingFlag);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
