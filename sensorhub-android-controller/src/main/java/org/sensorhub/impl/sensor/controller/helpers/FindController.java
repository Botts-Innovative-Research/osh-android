package org.sensorhub.impl.sensor.controller.helpers;

import android.hardware.input.InputManager;
import android.view.InputDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class FindController {
    private static final Logger logger = LoggerFactory.getLogger(FindController.class);
    private String id;
    private String name;
    protected int deviceId = -1;
    public ArrayList<GameControllerState> connectedControllers = new ArrayList<>();
    public FindController(InputManager inputManager){
        int[] deviceIds = inputManager.getInputDeviceIds();
        logger.debug("DeviceIds: "+ Arrays.toString(deviceIds));
        ArrayList<InputDevice> gameControllers = new ArrayList<>();
        for(int ids : deviceIds){
            InputDevice inputDevice = inputManager.getInputDevice(ids);
            if(isGamepadDevice(inputDevice)){
                gameControllers.add(inputDevice);
//                logger.debug("controller number: {}", inputDevice.getControllerNumber());
//                logger.debug("controller name: {}", inputDevice.getName());
//                logger.debug("controller descriptor: {}", inputDevice.getDescriptor());
//                logger.debug("controller product id: {}", inputDevice.getProductId());
//                logger.debug("controller keyboard type: {}", inputDevice.getKeyboardType());
            }
        }
        if(gameControllers.isEmpty()){
            connectedControllers = null;
            throw new IllegalStateException("No Gamepad controllers connected to Android!");
        }
        for(InputDevice dev : gameControllers){
            GameControllerState gamepad = new GameControllerState(dev);
            connectedControllers.add(gamepad);
        }

    }

    //    public ArrayList<FindControllers> findGameControllers(){
//        deviceIds = inputManager.getInputDeviceIds();
//        System.out.println("DeviceIds: "+ Arrays.toString(deviceIds));
//        for(int id: deviceIds){
//            InputDevice device = inputManager.getInputDevice(id);
//            if(device==null){
//                logger.debug("device is null!");
//            }else{
//                logger.debug("controller number: {}", device.getControllerNumber());
//                logger.debug("controller name: {}", device.getName());
//                logger.debug("controller descriptor: {}", device.getDescriptor());
//                logger.debug("controller product id: {}", device.getProductId());
//                logger.debug("controller keyboard type: {}", device.getKeyboardType());
//                if(isGameController(device)){
//                    FindControllers gameController = new FindControllers();
//                    gameController.setId(device.getDescriptor());
//                    gameController.setName(device.getName());
//                    gameController.deviceId = id;
//                    gameControllers.add(gameController);
//                    FindControllers.this.device = device;
//                }
//            }
//        }
//        return gameControllers;
//    }


    /**
     * @param device is or is not a gamepad
     * @return true if device is gamepad
     */
    public static boolean isGamepadDevice(InputDevice device){
        if(device == null){
            return false;
        }
        return device.supportsSource(InputDevice.SOURCE_GAMEPAD) || device.supportsSource(InputDevice.SOURCE_JOYSTICK);
//        int sources = device.getSources();
//        return ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK);
    }

    /**
     * @return connected android controllers
     */
    public ArrayList<GameControllerState> getConnectedControllers(){
        return connectedControllers;
    }
    /**
     * @return the id of the associated controller
     */
    public int getDeviceId(){
        return deviceId;
    }

    /**
     * @return the name of the associated controller
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * @return the id of the associated device
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id){
        this.id = id;
    }

}
