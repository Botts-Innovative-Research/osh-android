package org.sensorhub.impl.sensor.rs350;

import android.os.Looper;
import android.widget.Toast;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
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
        Looper.prepare();

        // Init comm provider
        if (commProvider == null) {
            // We need to recreate comm provider here because it can be changed by UI
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

        if (!commProvider.isStarted()) {
            Toast.makeText(SensorHubService.getContext(), "Failed to connect to RS-350 via TCP.\nEnsure the IP Address and Port Number are correct and restart SmartHub.", Toast.LENGTH_LONG).show();
            throw new SensorHubException("Comm provider failed to start");
        }

        rs350MessageHandler = new RS350MessageHandler(this);

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

    public void stopCommProvider() {
        if (commProvider != null) {
            try {
                commProvider.stop();
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to stop comm module", e);
            }
        }
    }

    public void startCommProvider() {
        if (commProvider != null) {
            try {
                commProvider.start();
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to start comm module", e);
            }
        }
    }

    public InputStream getInputStream() {
        if (commProvider != null) {
            try {
                return new BufferedInputStream(commProvider.getInputStream());
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to get input stream", e);
            }
        }
        return null;
    }

    public boolean isCommProviderStarted() {
        if (commProvider != null) {
            return commProvider.isStarted();
        }
        return false;
    }
}
