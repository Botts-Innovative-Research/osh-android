package org.sensorhub.impl.sensor.wearos.phone.output;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.wearos.phone.WearOSDriver;
import org.vast.swe.SWEHelper;

/**
 * Output for steps data
 */
public class StepsOutput extends AbstractSensorOutput<WearOSDriver> {
    DataComponent dataComponent;
    DataEncoding dataEncoding;

    public StepsOutput(WearOSDriver parent) {
        super("Steps Data", parent);
    }

    public void doInit() {
        SWEHelper sweHelper = new SWEHelper();

        dataComponent = sweHelper.createRecord()
                .name(name)
                .label(name)
                .definition(SWEHelper.getPropertyUri("stepsData"))
                .addField("time", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build())
                .addField("startTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Start Time")
                        .build())
                .addField("endTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("End Time")
                        .build())
                .addField("steps", sweHelper.createQuantity()
                        .label("Steps")
                        .description("Steps")
                        .definition(SWEHelper.getPropertyUri("steps"))
                        .dataType(DataType.LONG))
                .addField("startTimeDaily", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Daily Start Time")
                        .build())
                .addField("endTimeDaily", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Daily End Time")
                        .build())
                .addField("stepsDaily", sweHelper.createQuantity()
                        .label("Daily Steps")
                        .description("Daily Steps")
                        .definition(SWEHelper.getPropertyUri("stepsDaily"))
                        .dataType(DataType.LONG))
                .build();

        dataEncoding = sweHelper.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataComponent;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }

    public void setData(long startTime, long endTime, long value, long startTimeDaily, long endTimeDaily, long valueDaily) {
        DataBlock dataBlock = dataComponent.createDataBlock();

        dataBlock.setLongValue(0, System.currentTimeMillis() / 1000);
        dataBlock.setLongValue(1, startTime / 1000);
        dataBlock.setLongValue(2, endTime / 1000);
        dataBlock.setLongValue(3, value);
        dataBlock.setLongValue(4, startTimeDaily / 1000);
        dataBlock.setLongValue(5, endTimeDaily / 1000);
        dataBlock.setLongValue(6, valueDaily);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
