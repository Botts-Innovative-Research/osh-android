package org.sensorhub.impl.sensor.obd2;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataComponent;

import org.checkerframework.checker.units.qual.A;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.obd2.commands.Obd2Command;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.obd2.commands.Obd2CommandTask;
import org.sensorhub.impl.sensor.obd2.commands.Obd2Commands;
import org.vast.swe.SWEHelper;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Obd2Control extends AbstractSensorControl<Obd2Sensor> {
    DataChoice commandData;    DataRecord commandStruct;
    Obd2Commands commands;

    public Obd2Control(Obd2Sensor parentSensor) {
        super("control", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStruct;
    }

    @Override
    protected boolean execCommand(DataBlock commandBlock) throws CommandException {
        try {
            DataRecord commandData = commandStruct.copy();
            commandData.setData(commandBlock);
//            DataComponent component = commandData.getField("Command");
//            String commandName = component.getData().getStringValue();
//            Obd2Command command = commands.get(commandName);
            // TODO Is this ok?
            InputStream in = parentSensor.getBtSocket().getInputStream();
            OutputStream out = parentSensor.getBtSocket().getOutputStream();

            try {
                ArrayList<HashMap<Integer, String>> results = new ArrayList<>();
                ExecutorService executor = Executors.newFixedThreadPool(commands.getCommands().size());

                for (Obd2Command command: commands.getCommands().values()) {
                    Obd2Command obd2Command = commands.get(command.getName());
                    Callable<HashMap<Integer, String>> task = new Obd2CommandTask(obd2Command, in, out);
                    Future<HashMap<Integer, String>> future = executor.submit(task);

                    HashMap<Integer, String> result = null;
                    try {
                        result = future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        // TODO
                    }

                    results.add(result);
                }
                executor.shutdown();
                parentSensor.getOutput().setData(results);
            } catch (Exception e) {
                // TODO Handle errors
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public void init() {
        commands = Obd2Commands.getInstance();
        SWEHelper swe = new SWEHelper();

        commandStruct = swe.createRecord()
                .name("xxx")
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("xxx"))
                .label("xxx")
                .description("Sends read commands to the OBD-II device")
                .addField("Trigger",
                        swe.createBoolean()
                                .name("Trigger reading")
                                .label("Triggers reading")
                                .definition(SWEHelper.getPropertyUri("TriggerControl"))
                                .description("Triggers the OBD-II sensor to read all available data"))
                .build();
    }

    public void stop() {
        // TODO Auto-generated method stub
    }
}