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
 * <p>
 * Abstract base for data interfaces connecting to Android sensor API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class PolarOutput extends AbstractSensorOutput<Polar>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    String name = "Polar H9";
    private static final String SENSOR_OUTPUT_NAME = "POLAR_HEART_RATE";
    private static final String SENSOR_OUTPUT_LABEL = "POLAR HEART MONITOR DATA";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";
    private static final Logger logger = LoggerFactory.getLogger(PolarOutput.class);
    private DataRecord dataRecord;
    double lastBatteryLevel;
    double lastHeartRate;
    BufferedReader bufferedReader;

    protected PolarOutput(Polar parent) {
        super("Polar Heart Monitor Data", parent);
    }
    public void doInit(){
        logger.debug("Initializing Output");
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(name)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("time", fac.createTime().asSamplingTimeIsoUTC()
                        .label("Time Stamp")
                        .build()
                )
                .addField("HeartRate", fac.createQuantity()
                        .label("Heart Rate")
                        .definition("http://qudt.org/vocab/quantitykind/HeartRate")
                        .description("heart rate")
                        .uom("/min")
                        .build())
                .addField("batteryLevel", fac.createQuantity()
                        .label("Battery Status")
                        .definition("http://sensorml.com/ont/isa/property/Reserve_Capacity")
                        .description("Polar H9 battery level")
                        .uom("%")
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
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

    public void setData(int hr) {
        DataBlock dataBlock = dataStruct.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setIntValue(1, hr);
//        dataBlock.setDoubleValue(2, batteryLevel);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

//    protected void sendData(){
//        try{
//            long time = 0;
//            String line = bufferedReader.readLine();
//            Polar.logger.debug("Message received: {}", line);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        DataBlock data;
//        if (latestRecord == null)
//            data = dataStruct.createDataBlock();
//        else
//            data = latestRecord.renew();
//        long timeStamp = System.currentTimeMillis();
//        data.setDoubleValue(0, timeStamp / 1000.);
////        data.setFloatValue(1, lastHeartRate);
//        data.setDoubleValue(2, lastBatteryLevel);
//
//        // update latest record and send event
//        latestRecord = data;
//        latestRecordTime = timeStamp;
//        eventHandler.publish(new DataEvent(latestRecordTime, PolarOutput.this, data));
//
//    }
//    public void newHeartRate(int [] hr) {
//        this.lastHeartRate = hr;
//        sendData();
//    }

//    public void newBatteryLevel(int level) {
//        this.lastBatteryLevel = level;
//        sendData();
//    }
}
