/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.controller;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Abstract base for data interfaces connecting to Android Controller API
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 05/26/2024
 */

public class ButtonsOutputs extends AbstractSensorOutput<ControllerDriver>{
    public static DataComponent dataStruct;
    public static DataEncoding dataEncoding;

    private static final Logger logger = LoggerFactory.getLogger(ButtonsOutputs.class);

    public ButtonsOutputs(ControllerDriver parent) {
        super("Android Controller Data", parent);
    }
    public void doInit(){
        logger.debug("Initializing Output");
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(name)
                .definition(SWEHelper.getPropertyUri("AndroidController"))
                .label("AndroidController")
                .addField("sampleTime", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build())
                .addField("gamepadData", fac.createRecord()
                        .definition(SWEHelper.getPropertyUri("GamePad"))
                        .addField("buttons", fac.createRecord()
                                .addField("Mode", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("A", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("B", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("X", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("Y", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("LeftThumb",fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("RightThumb", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("LeftThumb2", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("RightThumb2",fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .build())
                )
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

//    public void setData(long timestamp, boolean mode, boolean A, boolean B, boolean X, boolean Y, boolean LeftBumper, boolean LeftThumb, boolean RightThumb, boolean RightBumper,float x, float y, float rx, float ry, float pov)
//    {
//        logger.debug("Output data received: mode={}, A={}, B={}, X={}, Y={}, LeftThumb={}, RightThumb={}, LeftBumper={}, RightBumper={}, x={}, y={}, rx={}, ry={}, pov={}",
//                mode, A,B, X, Y, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, rx, ry, pov);
//
//        DataBlock dataBlock = dataStruct.createDataBlock();
//
//        dataBlock.setLongValue(0, timestamp / 1000);
//        dataBlock.setBooleanValue(1, mode);
//        dataBlock.setBooleanValue(2, A);
//        dataBlock.setBooleanValue(3, B);
//        dataBlock.setBooleanValue(4, X);
//        dataBlock.setBooleanValue(5, Y);
//        dataBlock.setBooleanValue(6,LeftThumb);
//        dataBlock.setBooleanValue(7,RightThumb);
//        dataBlock.setBooleanValue(8,LeftBumper);
//        dataBlock.setBooleanValue(9,RightBumper);
//        dataBlock.setFloatValue(10,x);
//        dataBlock.setFloatValue(11,y);
//        dataBlock.setFloatValue(12,rx);
//        dataBlock.setFloatValue(13,ry);
//        dataBlock.setFloatValue(14,pov);
//
//        latestRecord = dataBlock;
//        latestRecordTime = System.currentTimeMillis();
//        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
//
//    }

    public void setButtonData(long timestamp, float mode, float A, float B, float X, float Y,  float LeftThumb, float RightThumb,float LeftBumper, float RightBumper){
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setLongValue(0, timestamp / 1000);
        dataBlock.setFloatValue(1, mode);
        dataBlock.setFloatValue(2, A);
        dataBlock.setFloatValue(3, B);
        dataBlock.setFloatValue(4, X);
        dataBlock.setFloatValue(5, Y);
        dataBlock.setFloatValue(6,LeftThumb);
        dataBlock.setFloatValue(7,RightThumb);
        dataBlock.setFloatValue(8,LeftBumper);
        dataBlock.setFloatValue(9,RightBumper);
//        dataBlock.setBooleanValue(10,pov_up);
//        dataBlock.setBooleanValue(11,pov_down);
//        dataBlock.setBooleanValue(12,pov_left);
//        dataBlock.setBooleanValue(13,pov_right);
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));

    }

}
