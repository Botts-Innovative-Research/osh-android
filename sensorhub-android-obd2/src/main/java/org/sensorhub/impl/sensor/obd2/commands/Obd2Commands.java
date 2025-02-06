package org.sensorhub.impl.sensor.obd2.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.io.InputStream;
import java.io.FileNotFoundException;

public final class Obd2Commands {
    private static volatile Obd2Commands instance;
    private Map<String, Obd2Command> commands;

    private Obd2Commands() {
        String fileName = "commands.json";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("File not found: " + fileName);
        }

        System.out.println("*** RESOURCE: " + inputStream);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            commands = objectMapper.readValue(inputStream, new TypeReference<Map<String, Obd2Command>>() {});
        } catch (IOException e) {
            // TODO
            System.out.println("*** ERROR READING FILE OBJECT: " + e);
        }

        System.out.println("*** COMMANDS: " + commands);

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
        System.out.println("*** COMMAND NAME: " + name);
        System.out.println(commands);
        return commands.get(name);
    }
}