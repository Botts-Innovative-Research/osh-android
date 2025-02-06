package org.sensorhub.impl.sensor.obd2;

import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.obd2.commands.Obd2Commands;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class Obd2Output extends AbstractSensorOutput<Obd2Sensor> {
    private static final String SENSOR_OUTPUT_NAME = "Obd2Output";
    private static final String SENSOR_OUTPUT_LABEL = "OBD2 Output";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Output data from the OBD2 Sensors";
    DataComponent dataStruct;
    DataEncoding dataEnc;
//    DataRecord dataRecord;

    public Obd2Output(Obd2Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void doInit() {
        SWEHelper sweHelper = new SWEHelper();

        Obd2Commands commands = Obd2Commands.getInstance();
        DataRecord distanceMILOn = commands.get("DistanceMILOn").getRecord();
        DataRecord DistanceSinceCC = commands.get("DistanceSinceCC").getRecord();

        dataStruct = sweHelper.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField(distanceMILOn.getName(), sweHelper.createQuantity()
                        .label("xxx")
                        .description(distanceMILOn.getDescription()))
                .addField(DistanceSinceCC.getName(), sweHelper.createQuantity()
                        .label("xxx")
                        .description(DistanceSinceCC.getDescription()))
                .build();

        dataEnc = sweHelper.newTextEncoding(",", "\n");

        System.out.println("*** COMPLETED OBD2 OUTPUT INIT");
    }

    // TODO Order of results is not guaranteed so i can't rely on the index
    public void setData(ArrayList<HashMap<Integer, String>> results) {
        long timestamp = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? dataStruct.createDataBlock() : latestRecord.renew();
        dataBlock.setDoubleValue(0, timestamp / 1000d);

        for (HashMap<Integer, String> result: results) {
            int key = result.keySet().iterator().next();
            dataBlock.setStringValue(key, result.get(key));
        }

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timestamp, Obd2Output.this, dataBlock));
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
