package org.sensorhub.impl.sensor.obd2;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.obd2.commands.Obd2Command;
import org.sensorhub.api.command.CommandException;
import org.vast.swe.SWEHelper;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

import java.util.HashMap;

public class Obd2Control extends AbstractSensorControl<Obd2Sensor> {
    DataChoice commandData;
    DataRecord commandStruct;
    HashMap<String, Obd2Command> commands;

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
            DataComponent component = commandData.getField("Command");
            String commandName = component.getData().getStringValue();
            Obd2Command command = commands.get(commandName);

            try {
                 command.run();
                 // TODO How do I get the data back and what do I do with it?
            } catch (Exception e) {
                // TODO Handle errors
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public void init() {
        File file = new File("./commands/commands.json");
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            commands = objectMapper.readValue(file, HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SWEHelper swe = new SWEHelper();
        DataChoice command = swe.createChoice()
                .name("Command")
                .label("Command")
                .description("Commands accepted by the platform")
                .definition(SWEHelper.getPropertyUri("Command"))
                .addItem()
                .build();

        for (Obd2Command cmd : commands.values()) {
            command.addItem(cmd.getCommandRecord().getName(), cmd.getCommandRecord());
        }

    }

    public void stop() {
        // TODO Auto-generated method stub
    }
}