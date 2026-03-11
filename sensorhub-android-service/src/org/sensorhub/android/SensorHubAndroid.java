package org.sensorhub.android;

import org.sensorhub.api.ISensorHubConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.comm.NetworkManagerImpl;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.sensorhub.impl.common.IdEncodersDES;
import org.sensorhub.impl.database.registry.DefaultDatabaseRegistry;
import org.sensorhub.impl.datastore.mem.InMemorySystemStateDbConfig;
import org.sensorhub.impl.event.EventBus;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.module.ModuleClassFinder;
import org.sensorhub.impl.module.ModuleConfigJsonFile;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.ProcessingManagerImpl;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.security.SecurityManagerImpl;
import org.sensorhub.impl.system.DefaultSystemRegistry;
import org.sensorhub.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class SensorHubAndroid extends SensorHub {
    private static final Logger log = LoggerFactory.getLogger(SensorHub.class);
    private static final String ERROR_MSG = "Fatal error during sensorhub execution";

    SensorHubAndroid(ISensorHubConfig config, IModuleConfigRepository moduleConfigs){
        super(config, moduleConfigs);
    }

    public synchronized void initComponents() throws SensorHubException {
        if (started) return;

        log.info("*****************************************");
        log.info("Starting SensorHub...");
        log.info("Version : {}", ModuleUtils.getModuleInfo(SensorHub.class).getModuleVersion());
        log.info("CPU cores: {}", Runtime.getRuntime().availableProcessors());
        log.info("CommonPool Parallelism: {}", ForkJoinPool.commonPool().getParallelism());

        if (moduleConfigs == null) {
            var classFinder = new ModuleClassFinder(osgiContext);
            moduleConfigs = config.getModuleConfigPath() != null ?
                    new ModuleConfigJsonFile(config.getModuleConfigPath(), true, classFinder) :
                    new InMemoryConfigDb(classFinder);
        }

        this.moduleRegistry = new ModuleRegistry(this, moduleConfigs);
        this.eventBus = new EventBus();
        this.databaseRegistry = new DefaultDatabaseRegistry(this);
        this.driverRegistry = new DefaultSystemRegistry(this, new InMemorySystemStateDbConfig());

        this.securityManager = new SecurityManagerImpl(this);
        this.networkManager = new NetworkManagerImpl(this);
        this.processingManager = new ProcessingManagerImpl(this);
        this.idEncoders = new IdEncodersBase32();

        ClientAuth.createInstance("keystore");
    }

    public synchronized void start() throws SensorHubException {
        if (!started) {
            if (this.eventBus == null) {
                initComponents();
            }

            moduleRegistry.loadAllModules();
            started = true;
        }
    }
}