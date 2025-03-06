package org.sensorhub.impl.sensor.obd2.commands;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class Obd2CommandTask implements Callable<HashMap<Integer, String>> {
    private Obd2Command command;
    private InputStream in;
    private OutputStream out;

    public Obd2CommandTask(Obd2Command command, InputStream in, OutputStream out) {
        this.command = command;
        this.in = in;
        this.out = out;
    }

    @Override
    public HashMap<Integer, String> call() {
        HashMap<Integer, String> result = new HashMap<>();
        String classRef = command.getClassRef();
        Object[] args = {in, out};

        try {
            Class<?> clazz = Class.forName(classRef);
            Object commandInstance = clazz.getDeclaredConstructor().newInstance();
            // TODO I'm expecting this and the next call to by synchronous
            clazz.getMethod("run", InputStream.class, OutputStream.class).invoke(commandInstance, args);
            result.put(
                    command.getIndex(),
                    (String) clazz.getMethod("getCalculatedResult").invoke(commandInstance)
            );
        } catch (ClassNotFoundException e) {
            // TODO Is this how I want to handle errors?
            System.err.println("Class not found: " + classRef);
        } catch (NoSuchMethodException e) {
            System.err.println("Method 'run' not found in class: " + classRef);
        } catch (Exception e) {
            System.out.println("*** ERROR: " + e);
            e.printStackTrace();
        }

        return result;
    }
}