package org.sensorhub.impl.sensor.rs350.outputs;

import net.opengis.swe.v20.DataRecord;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.rs350.RS350Sensor;
import org.sensorhub.impl.sensor.rs350.messages.RadInstrumentData;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class AlarmOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Alarm";

    private static final Logger logger = LoggerFactory.getLogger(AlarmOutput.class);

    public AlarmOutput(RS350Sensor parentSensor) {
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
                .label("Alarm")
                .definition(RADHelper.getRadUri("Alarm-output"))
                // Derived Data
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Duration",
                        radHelper.createQuantity()
                                .name("Duration")
                                .label("Duration")
                                .definition(RADHelper.getRadUri("duration")))
                .addField("Remark",
                        radHelper.createText()
                                .name("Remark")
                                .label("Remark")
                                .definition(RADHelper.getRadUri("alarm-remark")))
                .addField("MeasurementClassCode", radHelper.createMeasurementClassCode())
                // RAD Alarm
                .addField("AlarmCategoryCode", radHelper.createAlarmCatCode())
                .addField("AlarmDescription",
                        radHelper.createText()
                                .name("AlarmDescription")
                                .label("Alarm Description")
                                .definition(RADHelper.getRadUri("alarm-description")))
                .build();
    }

    @Override
    public void onNewMessage(RadInstrumentData message) {
        if (message.getDerivedData() != null) {
            createOrRenewDataBlock();

            dataBlock.setLongValue(0, message.getDerivedData().getStartDateTime() / 1000);
            dataBlock.setDoubleValue(1, message.getDerivedData().getRealTimeDuration());
            dataBlock.setStringValue(2, message.getDerivedData().getRemark());
            dataBlock.setStringValue(3, message.getDerivedData().getMeasurementClassCode());
            dataBlock.setStringValue(4, message.getAnalysisResults().getRadAlarm().getRadAlarmCategoryCode());
            dataBlock.setStringValue(5, message.getAnalysisResults().getRadAlarm().getRadAlarmDescription());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, AlarmOutput.this, dataBlock));
        }
    }
}
