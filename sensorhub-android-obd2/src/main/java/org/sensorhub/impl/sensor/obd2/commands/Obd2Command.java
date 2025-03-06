package org.sensorhub.impl.sensor.obd2.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.opengis.swe.v20.DataRecord;

import org.vast.swe.SWEHelper;

import java.io.InputStream;
import java.io.OutputStream;

public class  Obd2Command extends Thread {
    private DataRecord record;
    private int index;
    private String classRef;
    private String name;

    public Obd2Command() {}

    @JsonCreator
    public Obd2Command(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("index") int index,
            @JsonProperty("classRef") String classRef
    ) {
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
        this.name = name; // TODO Is this necessary or can i get it from the record?
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

    public String getCommandName() { return name; }

    // TODO Does this need a hashcode method since im storing these elsewhere in a Map?
    // TODO What about toString()?
}