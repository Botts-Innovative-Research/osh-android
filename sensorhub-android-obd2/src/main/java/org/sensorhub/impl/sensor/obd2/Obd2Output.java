package org.sensorhub.impl.sensor.obd2;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.impl.sensor.AbstractSensorOutput;

public class Obd2Output extends AbstractSensorOutput<Obd2Sensor> {
    DataComponent dataStruct;
    DataEncoding dataEnc;

    public Obd2Output(Obd2Sensor parentSensor)
    {
        super("obd2Data", parentSensor);
    }

    protected void init() {
    }

    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0;
    }

    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
}
