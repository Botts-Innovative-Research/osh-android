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
 * Output for calories data
 */
public class CaloriesOutput extends AbstractSensorOutput<WearOSDriver> {
    DataComponent dataComponent;
    DataEncoding dataEncoding;

    public CaloriesOutput(WearOSDriver parent) {
        super("Calories Data", parent);
    }

    public void doInit() {
        SWEHelper sweHelper = new SWEHelper();

        dataComponent = sweHelper.createRecord()
                .name(name)
                .label(name)
                .definition(SWEHelper.getPropertyUri("caloriesData"))
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
                .addField("calories", sweHelper.createQuantity()
                        .label("Calories")
                        .description("Calories")
                        .definition(SWEHelper.getPropertyUri("calories"))
                        .dataType(DataType.DOUBLE))
                .addField("startTimeDaily", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Daily Start Time")
                        .build())
                .addField("endTimeDaily", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Daily End Time")
                        .build())
                .addField("caloriesDaily", sweHelper.createQuantity()
                        .label("Daily Calories")
                        .description("Daily Calories")
                        .definition(SWEHelper.getPropertyUri("caloriesDaily"))
                        .dataType(DataType.DOUBLE))
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

    public void setData(long startTime, long endTime, double value, long startTimeDaily, long endTimeDaily, double valueDaily) {
        DataBlock dataBlock = dataComponent.createDataBlock();

        dataBlock.setLongValue(0, System.currentTimeMillis() / 1000);
        dataBlock.setLongValue(1, startTime / 1000);
        dataBlock.setLongValue(2, endTime / 1000);
        dataBlock.setDoubleValue(3, value);
        dataBlock.setLongValue(4, startTimeDaily / 1000);
        dataBlock.setLongValue(5, endTimeDaily / 1000);
        dataBlock.setDoubleValue(6, valueDaily);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
