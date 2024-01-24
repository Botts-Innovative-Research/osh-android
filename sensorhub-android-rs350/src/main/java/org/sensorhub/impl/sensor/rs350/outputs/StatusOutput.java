package org.sensorhub.impl.sensor.rs350.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.rs350.RS350Sensor;
import org.sensorhub.impl.sensor.rs350.messages.RadInstrumentData;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;

public class StatusOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Status";

    private static final Logger logger = LoggerFactory.getLogger(StatusOutput.class);

    public StatusOutput(RS350Sensor parentSensor) {
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
                .label("Status")
                .definition(RADHelper.getRadUri("device-status"))
                .addField("Latest Record Time", radHelper.createPrecisionTimeStamp())
                .addField("Battery Charge", radHelper.createBatteryCharge())
                .addField("Scan Mode",
                        radHelper.createText()
                                .name("ScanMode")
                                .label("Scan Mode")
                                .definition(RADHelper.getRadUri("scan-mode"))
                                .build())
                .addField("Scan Timeout",
                        radHelper.createQuantity()
                                .name("ScanTimeout")
                                .label("Scan Timeout")
                                .definition(RADHelper.getRadUri("scan-timeout"))
                                .build())
                .addField("Analysis Enabled",
                        radHelper.createText()
                                .name("AnalysisEnabled")
                                .label("Analysis Enabled")
                                .definition(RADHelper.getRadUri("analysis-enabled"))
                                .build())
                .addField("LinCalibration", radHelper.createLinCalibration())
                .addField("CmpCalibration", radHelper.createCmpCalibration())
                .build();
    }

    @Override
    public void onNewMessage(RadInstrumentData message) {
        if (message.getRadInstrumentInformation() != null) {
            dataStruct = createDataRecord();
            DataBlock dataBlock = dataStruct.createDataBlock();
            dataStruct.setData(dataBlock);

            double timestamp = System.currentTimeMillis() / 1000d;

            dataBlock.setDoubleValue(0, timestamp);
            dataBlock.setDoubleValue(1, message.getRadInstrumentInformation().getRadInstrumentCharacteristics().getBatteryCharge());
            dataBlock.setStringValue(2, message.getRadItemInformation().getRadItemCharacteristics().getRsiScanMode());
            dataBlock.setIntValue(3, message.getRadItemInformation().getRadItemCharacteristics().getRsiScanTimeoutNumber());
            dataBlock.setStringValue(4, message.getRadItemInformation().getRadItemCharacteristics().getRsiAnalysisEnabled());
            ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setUnderlyingObject(message.getLinEnCal());
            ((DataBlockMixed) dataBlock).getUnderlyingObject()[6].setUnderlyingObject(message.getCmpEnCal());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, StatusOutput.this, dataBlock));
        }
    }
}
