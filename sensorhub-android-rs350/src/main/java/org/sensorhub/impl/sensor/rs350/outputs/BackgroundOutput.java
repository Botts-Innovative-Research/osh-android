package org.sensorhub.impl.sensor.rs350.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.rs350.RS350Sensor;
import org.sensorhub.impl.sensor.rs350.messages.RadInstrumentData;
import org.sensorhub.impl.sensor.rs350.messages.RadMeasurement;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;

public class BackgroundOutput extends OutputBase {
    private static final String SENSOR_OUTPUT_NAME = "RS350 Background Report";
    private static final String LIN_SPECTRUM_NAME = "LinSpectrum";
    private static final String CMP_SPECTRUM_NAME = "CmpSpectrum";

    private static final Logger logger = LoggerFactory.getLogger(BackgroundOutput.class);

    public BackgroundOutput(RS350Sensor parentSensor) {
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
        final String LIN_SPEC_ID = "lin-spectrum";
        final String CMP_SPEC_ID = "cmp-spectrum";

        return radHelper.createRecord()
                .name(getName())
                .label("Background Report")
                .definition(RADHelper.getRadUri("background-report"))
                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                .addField("Duration",
                        radHelper.createQuantity()
                                .name("Duration")
                                .label("Duration")
                                .definition(RADHelper.getRadUri("duration")))
                .addField("LinSpectrumSize", radHelper.createArraySize("Lin Spectrum Size", LIN_SPEC_ID))
                .addField(LIN_SPECTRUM_NAME, radHelper.createLinSpectrum(LIN_SPEC_ID))
                .addField("CmpSpectrumSize", radHelper.createArraySize("Cmp Spectrum Size", CMP_SPEC_ID))
                .addField(CMP_SPECTRUM_NAME, radHelper.createCmpSpectrum(CMP_SPEC_ID))
                .addField("GammaGrossCount", radHelper.createGammaGrossCount())
                .addField("Neutron Gross Count", radHelper.createNeutronGrossCount())
                .build();
    }

    @Override
    public void onNewMessage(RadInstrumentData message) {
        RadMeasurement radMeasurement = message.getBackgroundRadMeasurement();
        if (radMeasurement != null) {
            dataStruct = createDataRecord();
            DataBlock dataBlock = dataStruct.createDataBlock();
            dataStruct.setData(dataBlock);

            int index = 0;

            dataBlock.setLongValue(index++, radMeasurement.getStartDateTime() / 1000);
            dataBlock.setDoubleValue(index++, radMeasurement.getRealTimeDuration());

            double[] linEnCalSpectrum = radMeasurement.getLinEnCalSpectrum().getChannelData();
            dataBlock.setIntValue(index++, linEnCalSpectrum.length);
            ((DataArrayImpl) dataStruct.getComponent(LIN_SPECTRUM_NAME)).updateSize();
            for (double v : linEnCalSpectrum) {
                dataBlock.setDoubleValue(index++, v);
            }

            double[] cmpEnCalSpectrum = radMeasurement.getCmpEnCalSpectrum().getChannelData();
            dataBlock.setIntValue(index++, cmpEnCalSpectrum.length);
            ((DataArrayImpl) dataStruct.getComponent(CMP_SPECTRUM_NAME)).updateSize();
            for (double v : cmpEnCalSpectrum) {
                dataBlock.setDoubleValue(index++, v);
            }

            dataBlock.setIntValue(index++, radMeasurement.getGammaGrossCounts().getCountData());
            dataBlock.setIntValue(index, radMeasurement.getNeutronGrossCounts().getCountData());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, BackgroundOutput.this, dataBlock));
        }
    }
}
