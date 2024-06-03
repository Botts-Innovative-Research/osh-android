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

public class ControllerOutput extends AbstractSensorOutput<ControllerDriver>{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    String name = "Android Controller";
    private static final String SENSOR_OUTPUT_NAME = "ANDROID_CONTROLLER";
    private static final String SENSOR_OUTPUT_LABEL = "ANDROID CONTROLLER DATA";
    private static final Logger logger = LoggerFactory.getLogger(ControllerOutput.class);
    protected ControllerOutput(ControllerDriver parent) {
        super("Android Controller Data", parent);
    }
    public void doInit(){
        logger.debug("Initializing Output");
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(name)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .addField("time", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build()
                )
                .addField("controller", fac.createRecord()
                        .definition(SWEHelper.getPropertyUri("GamePad"))
                        .label("gamePad buttons")
                                .addField("Mode", fac.createBoolean().value(false))
                                .addField("A", fac.createBoolean().value(false))
                                .addField("B", fac.createBoolean().value(false))
                                .addField("X", fac.createBoolean().value(false))
                                .addField("Y",fac.createBoolean().value(false))
                                .addField("D_PAD_UP",fac.createBoolean().value(false))
                                .addField("D_PAD_DOWN", fac.createBoolean().value(false))
                                .addField("D_PAD_LEFT", fac.createBoolean().value(false))
                                .addField("D_PAD_RIGHT", fac.createBoolean().value(false))
                                .addField("LeftThumb", fac.createBoolean().value(false))
                                .addField("RightThumb", fac.createBoolean().value(false))
                                .addField("LeftBumper", fac.createBoolean().value(false))
                                .addField("RightBumper", fac.createBoolean().value(false))
                                .addField("x", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("y", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("rx", fac.createQuantity().addAllowedValues(0.0, 1.0))
                                .addField("ry", fac.createQuantity().addAllowedValues(0.0, 1.0))
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

    public void setGamepadData(boolean mode, boolean A, boolean X, boolean Y, boolean dpad_down, boolean dpad_left, boolean dpad_up, boolean dpad_right, boolean LeftThumb, boolean RightThumb, boolean LeftBumper, boolean RightBumper, boolean B){

    }
    public void setJoyStickData(float x, float y, float rx, float ry){

    }
    public void setDataBlock(boolean mode, boolean A, boolean X, boolean Y, boolean dpad_down, boolean dpad_left, boolean dpad_up, boolean dpad_right, boolean LeftThumb, boolean RightThumb, boolean LeftBumper, boolean RightBumper,float x, float y, boolean B, float rx, float ry)
    {
        logger.debug("Output data received: mode={}, A={}, X={}, Y={}, dpad_down={}, dpad_left={}, dpad_up={}, dpad_right={}, LeftThumb={}, RightThumb={}, LeftBumper={}, RightBumper={}, x={}, y={}, B={}, rx={}, ry={}",
                mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, B, rx, ry);

        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setLongValue(0, System.currentTimeMillis() / 1000);

        dataBlock.setBooleanValue(1, mode);
        dataBlock.setBooleanValue(2, A);
        dataBlock.setBooleanValue(3, B);
        dataBlock.setBooleanValue(4, X);
        dataBlock.setBooleanValue(5, Y);
        dataBlock.setBooleanValue(6, dpad_down);
        dataBlock.setBooleanValue(7, dpad_up);
        dataBlock.setBooleanValue(8, dpad_left);
        dataBlock.setBooleanValue(9, dpad_right);
        dataBlock.setBooleanValue(10,LeftThumb);
        dataBlock.setBooleanValue(11,RightThumb);
        dataBlock.setBooleanValue(12,LeftBumper);
        dataBlock.setBooleanValue(13,RightBumper);
        dataBlock.setFloatValue(14,x);
        dataBlock.setFloatValue(15,y);
        dataBlock.setFloatValue(16,rx);
        dataBlock.setFloatValue(17,ry);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));

    }
}
