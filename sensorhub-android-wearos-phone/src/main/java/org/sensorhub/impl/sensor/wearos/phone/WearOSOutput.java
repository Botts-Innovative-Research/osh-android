package org.sensorhub.impl.sensor.wearos.phone;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class WearOSOutput extends AbstractSensorOutput<WearOSDriver> {
    DataComponent dataComponent;
    DataEncoding dataEncoding;

    protected WearOSOutput(WearOSDriver parent) {
        super("Wear OS Data", parent);
    }

    public void doInit() {
        SWEHelper fac = new SWEHelper();

        dataComponent = fac.createRecord()
                .name(name)
                .label(name)
                .definition(SWEHelper.getPropertyUri("wear-os-data"))
                .addField("time", fac.createTime().asSamplingTimeIsoUTC()
                        .label("Time Stamp")
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
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
}
