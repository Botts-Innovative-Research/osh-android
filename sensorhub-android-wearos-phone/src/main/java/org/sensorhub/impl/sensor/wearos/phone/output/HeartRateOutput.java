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
 * Output for heart rate data
 */
public class HeartRateOutput extends AbstractSensorOutput<WearOSDriver> {
    DataComponent dataComponent;
    DataEncoding dataEncoding;

    public HeartRateOutput(WearOSDriver parent) {
        super("Heart Rate Data", parent);
    }

    public void doInit() {
        SWEHelper sweHelper = new SWEHelper();

        dataComponent = sweHelper.createRecord()
                .name(name)
                .label(name)
                .definition(SWEHelper.getPropertyUri("heartRateData"))
                .addField("time", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sampling Time")
                        .build())
                .addField("heartRate", sweHelper.createQuantity()
                        .label("Heart Rate")
                        .description("Heart Rate")
                        .definition(SWEHelper.getPropertyUri("heartRate"))
                        .dataType(DataType.INT))
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

    public void setData(long timeStamp, int value) {
        DataBlock dataBlock = dataComponent.createDataBlock();

        dataBlock.setLongValue(0, timeStamp / 1000);
        dataBlock.setIntValue(1, value);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
