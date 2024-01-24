package org.sensorhub.impl.sensor.rs350.outputs;

import net.opengis.swe.v20.DataRecord;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.rs350.RS350Sensor;
import org.sensorhub.impl.sensor.rs350.messages.RadInstrumentData;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class LocationOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Location";

    private static final Logger logger = LoggerFactory.getLogger(LocationOutput.class);

    public LocationOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    public void init() {
        dataStruct = createDataRecord();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public DataRecord createDataRecord() {
        RADHelper radHelper = new RADHelper();

        return radHelper.createRecord()
                .name(getName())
                .label("Location")
                .definition(RADHelper.getRadUri("location-output"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Sensor Location", radHelper.createLocationVectorLLA())
                .build();
    }

    @Override
    public void onNewMessage(RadInstrumentData message) {
        if (message.getForegroundRadMeasurement() != null) {
            createOrRenewDataBlock();

            dataBlock.setLongValue(0, message.getForegroundRadMeasurement().getStartDateTime() / 1000);
            dataBlock.setDoubleValue(1, message.getForegroundRadMeasurement().getRadInstrumentState().getStateVector().getGeographicPoint().getLatitudeValue());
            dataBlock.setDoubleValue(2, message.getForegroundRadMeasurement().getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue());
            dataBlock.setDoubleValue(3, message.getForegroundRadMeasurement().getRadInstrumentState().getStateVector().getGeographicPoint().getElevationValue());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, LocationOutput.this, dataBlock));
        }
    }
}
