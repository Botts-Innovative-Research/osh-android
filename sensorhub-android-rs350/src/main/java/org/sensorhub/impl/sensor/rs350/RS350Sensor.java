package org.sensorhub.impl.sensor.rs350;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rs350.outputs.AlarmOutput;
import org.sensorhub.impl.sensor.rs350.outputs.BackgroundOutput;
import org.sensorhub.impl.sensor.rs350.outputs.ForegroundOutput;
import org.sensorhub.impl.sensor.rs350.outputs.LocationOutput;
import org.sensorhub.impl.sensor.rs350.outputs.StatusOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RS350Sensor extends AbstractSensorModule<RS350Config> {
    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;
    LocationOutput locationOutput;

    StatusOutput statusOutput;

    BackgroundOutput backgroundOutput;

    ForegroundOutput foregroundOutput;

    AlarmOutput alarmOutput;
    RS350MessageHandler rs350MessageHandler;

    InputStream msgIn;

    public RS350Sensor() {

    }

    @Override
    public void doInit() {
        generateUniqueID("urn:rsi:rs350:", config.serialNumber);
        generateXmlID("rsi_rs350_", config.serialNumber);

        if (config.outputs.enableLocationOutput) {
            locationOutput = new LocationOutput(this);
            addOutput(locationOutput, false);
            locationOutput.init();
        }

        if (config.outputs.enableStatusOutput) {
            statusOutput = new StatusOutput(this);
            addOutput(statusOutput, false);
            statusOutput.init();
        }

        if (config.outputs.enableBackgroundOutput) {
            backgroundOutput = new BackgroundOutput(this);
            addOutput(backgroundOutput, false);
            backgroundOutput.init();
        }

        if (config.outputs.enableForegroundOutput) {
            foregroundOutput = new ForegroundOutput(this);
            addOutput(foregroundOutput, false);
            foregroundOutput.init();
        }

        if (config.outputs.enableAlarmOutput) {
            alarmOutput = new AlarmOutput(this);
            addOutput(alarmOutput, false);
            alarmOutput.init();
        }
    }

    @Override
    public void doStart() throws SensorHubException {
        // init comm provider
        if (commProvider == null) {

            // we need to recreate comm provider here because it can be changed by UI
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                ModuleRegistry moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();

            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        // connect to data stream
        try {
            msgIn = new BufferedInputStream(commProvider.getInputStream());
        } catch (IOException e) {
            throw new SensorException("Error while initializing communications ", e);
        }

        rs350MessageHandler = new RS350MessageHandler(msgIn, "</RadInstrumentData>");

        if (config.outputs.enableLocationOutput) {
            rs350MessageHandler.addMessageListener(locationOutput);
        }

        if (config.outputs.enableStatusOutput) {
            rs350MessageHandler.addMessageListener(statusOutput);
        }

        if (config.outputs.enableBackgroundOutput) {
            rs350MessageHandler.addMessageListener(backgroundOutput);
        }

        if (config.outputs.enableForegroundOutput) {
            rs350MessageHandler.addMessageListener(foregroundOutput);
        }

        if (config.outputs.enableAlarmOutput) {
            rs350MessageHandler.addMessageListener(alarmOutput);
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        if (commProvider != null) {
            try {
                commProvider.stop();
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to stop comm module", e);
            } finally {
                commProvider = null;
            }
        }

        rs350MessageHandler.stopProcessing();
    }

    @Override
    public boolean isConnected() {
        if (commProvider == null) {
            return false;
        } else {
            return commProvider.isStarted();
        }
    }
}
