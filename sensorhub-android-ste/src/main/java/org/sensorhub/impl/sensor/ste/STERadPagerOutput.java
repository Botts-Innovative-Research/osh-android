package org.sensorhub.impl.sensor.ste;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class STERadPagerOutput extends AbstractSensorOutput<STERadPager>{
    String name = "STE Rad Pager Data";
    DataComponent dataComponent;
    DataEncoding dataEncoding;

    protected STERadPagerOutput(STERadPager parent) {
        super("STE Rad Pager Data", parent);

        SWEHelper fac = new SWEHelper();

        dataComponent = fac.createRecord()
                .name(name)
                .label("STE Rad Pager Data")
                .addField("time", fac.createTime().asSamplingTimeIsoUTC()
                        .label("Time Stamp")
                        .build())
                .addField("Counts", fac.createQuantity()
                        .label("Counts")
                        .description("# of Gamma Detection Events measure by the rad sensing assembly every half second pre-scaled by 1/2")
                        .build())
                .addField("CPS", fac.createQuantity()
                        .label("Counts per Second")
                        .description("Counts per Second calculated by the Counts Property * 4")
                        .build())
                .addField("Saturation", fac.createBoolean()
                        .label("Saturation")
                        .description("If the Rad Sensor is saturated on the front end, invalidating the Counts value. USER SHOULD RETREAT IF THIS IS TRUE!!!")
                        .build())
                .addField("Threshold" , fac.createQuantity()
                        .label("Threshold")
                        .description("Measured background radiation threshold")
                        .build())
                .addField("Alarm Level Value", fac.createCategoryRange()
                        .label("Alarm Level - Value")
                        .description("Alarm Level indicated")
                        .addAllowedValues(new int[]{0,1,2,3,4,5,6,7,8,9})
                        .build())
                .addField("Alarm Level Expossure Rate", fac.createQuantity()
                        .label("Alarm Level - Exposure Rate")
                        .description("Exposure Rate indicated")
                        .uom("UROE/HR")
                        .build())
                .build();
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
