package org.sensorhub.impl.sensor.controller;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

public class JoystickOutputs extends AbstractSensorOutput<ControllerDriver> {
    public static DataComponent dataStruct;
    public static DataEncoding dataEncoding;
    private static final Logger logger = LoggerFactory.getLogger(JoystickOutputs.class);
    public JoystickOutputs(ControllerDriver parent) {
        super("Joystick Data", parent);
    }
    public void doInit(){
        logger.debug("Initializing Output");
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(name)
                .definition(SWEHelper.getPropertyUri("Joystick"))
                .label("Joystick Data")
                .addField("sampleTime", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build())
                .addField("joystickData", fac.createRecord()
                                .definition(SWEHelper.getPropertyUri("GamePad"))
                        .addField("joystick", fac.createRecord()
                                .addField("x", fac.createQuantity())
                                .addField("y", fac.createQuantity())
                                .addField("rx",fac.createQuantity())
                                .addField("ry",fac.createQuantity())
                                .addField("pov", fac.createQuantity().addAllowedValues(0.0, 0.125, 0.250, 0.375, 0.500, 0.625, 0.750, 0.875, 1.0).value(0.0))
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

    public void setJoystickData(long timestamp, float x, float y, float rx, float ry, float pov){
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setLongValue(0, timestamp / 1000);
        dataBlock.setFloatValue(1,x);
        dataBlock.setFloatValue(2,y);
        dataBlock.setFloatValue(3,rx);
        dataBlock.setFloatValue(4,ry);
        dataBlock.setFloatValue(5,pov);
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

}