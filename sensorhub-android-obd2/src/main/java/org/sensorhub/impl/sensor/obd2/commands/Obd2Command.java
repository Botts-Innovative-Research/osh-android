package org.sensorhub.impl.sensor.obd2.commands;

import net.opengis.swe.v20.DataRecord;

import org.vast.swe.SWEHelper;

public class Obd2Command {
    private DataRecord commandRecord;
    private String function;

    public Obd2Command(String name, String description, String function) {
        SWEHelper swe = new SWEHelper();

        this.function = function;
        this.commandRecord = swe.createRecord()
                .name(name)
                .updatable(true)
                .label(name)
                .description(description)
                .definition(SWEHelper.getPropertyUri(name))
                .build();
    }

    public DataRecord getCommandRecord() {
        return commandRecord;
    }
}