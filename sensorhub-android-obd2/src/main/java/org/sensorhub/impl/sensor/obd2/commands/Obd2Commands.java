package org.sensorhub.impl.sensor.obd2.commands;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class Obd2Commands {
    private static volatile Obd2Commands instance;
    private Map<String, Obd2Command> commands;

    private Obd2Commands() {
        File file = new File("/commands.json");
        ObjectMapper objectMapper = new ObjectMapper();

        // TODO Test that this works as expected
        try {
            commands = objectMapper.readValue(file, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Obd2Command.class));
        } catch (IOException e) {
            // TODO
        }
    }

    public static Obd2Commands getInstance() {
        Obd2Commands result = instance;
        if (result != null) {
            return result;
        }
        synchronized(Obd2Commands.class) {
            if (instance == null) {
                instance = new Obd2Commands();
            }
            return instance;
        }
    }

    public Map<String, Obd2Command> getCommands() {
        return commands;
    }

    public Obd2Command get(String name) {
        return commands.get(name);
    }
}