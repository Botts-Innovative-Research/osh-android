package org.sensorhub.impl.sensor.obd2.commands;

import net.opengis.swe.v20.DataRecord;

import org.vast.swe.SWEHelper;

import java.io.InputStream;
import java.io.OutputStream;

public class Obd2Command extends Thread {
    private DataRecord record;
    private int index;
    private String classRef;

    public Obd2Command(String name, String description, int index, String classRef) {
        SWEHelper swe = new SWEHelper();
        this.record = swe.createRecord()
                .name(name)
                .updatable(true)
                .label(name)
                .description(description)
                .definition(SWEHelper.getPropertyUri(name))
                .build();

        this.index = index;
        this.classRef = classRef;
    }

    public DataRecord getRecord() {
        return record;
    }

    public int getIndex() {
        return index;
    }

    public String getClassRef() {
        return classRef;
    }

    // TODO Does this need a hashcode method since im storing these elsewhere in a Map?
    // TODO What about toString()?
}