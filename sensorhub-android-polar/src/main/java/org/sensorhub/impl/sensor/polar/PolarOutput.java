/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.polar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;


/**
 *
 * @author Kalyn Stricklin
 * @since Jan 13, 2023
 */
public class PolarOutput extends AbstractSensorOutput<Polar>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    private static final String SENSOR_OUTPUT_NAME = "POLAR_HEART_RATE";
    private static final String SENSOR_OUTPUT_LABEL = "Polar HeartRate Monitor";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Wearable sensor to monitor heartrate";
    private static final Logger logger = LoggerFactory.getLogger(PolarOutput.class);

    protected PolarOutput(Polar parent) {
        super("Polar Heart Monitor Data", parent);
    }
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("samplingTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("heartRate", fac.createQuantity()
                        .label("Heart Rate")
                        .definition(SWEHelper.getPropertyUri("HeartRate"))
                        .description("heart rate")
                        .uom("/min")
                        .build())
                .addField("batteryLevel", fac.createQuantity()
                        .label("Battery Level")
                        .definition(SWEHelper.getPropertyUri("BatteryLevel"))
                        .description("Heart rate monitors battery level")
                        .uom("%")
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

    public void setData(int hr, int batteryLevel) {
        DataBlock dataBlock = dataStruct.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setIntValue(1, hr);
        dataBlock.setIntValue(2, batteryLevel);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
