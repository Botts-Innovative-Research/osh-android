/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.controller;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.HandlerThread;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.impl.sensor.AbstractSensorModule;
//import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.sensorhub.impl.sensor.controller.helpers.ControllerData;
import org.sensorhub.impl.sensor.controller.helpers.FindController;
import org.sensorhub.impl.sensor.controller.helpers.GameControllerState;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Configuration class for the generic Android Controller driver
 * </p>
 *
 * @author Kalyn Stricklin
 * @since 05/26/2024
 */

public class ControllerDriver extends AbstractSensorModule<ControllerConfig>  {
    private HandlerThread eventThread;
    static ButtonsOutputs buttonsOutputs;
    static JoystickOutputs joystickOutputs;
    private final ArrayList<PhysicalComponent> smlComponents;
//    private final SensorMLBuilder smlBuilder;
    private Context context;
    InputManager inputManager;
    GameControllerState gamepad;
    ControllerData data;


    public ControllerDriver() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
//        this.smlBuilder = new SensorMLBuilder();
    }
    @Override
    public void doInit() {
        generateUniqueID("urn:osh:", config.getDeviceName());
        generateXmlID("android:controller:", config.getDeviceName());

        context = SensorHubService.getContext();
        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);

        createOutputs();
    }

    @Override
    public void doStart(){
        //get controller!
        FindController findController = new FindController(inputManager);
        List<GameControllerState> controllers = findController.getConnectedControllers();

        if(controllers.isEmpty()){
            throw new IllegalStateException("No controller found!");
        }
        gamepad = controllers.get(0);

        eventThread = new HandlerThread("ControllerEvents");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

        data = new ControllerData();
        if(data!= null){
            setButtonData(data);
            setJoystickData(data);
        }


    }
    public void createOutputs(){
        if(config.getOutputs().getEnabledGamepad()){
            buttonsOutputs = new ButtonsOutputs(this);
            buttonsOutputs.doInit();
            addOutput(buttonsOutputs, false);

        }else{
            buttonsOutputs =null;
        }

        if(config.getOutputs().getEnabledJoystick()){
            joystickOutputs = new JoystickOutputs(this);
            joystickOutputs.doInit();
            addOutput(joystickOutputs, false);
        } else{
            joystickOutputs=null;
        }
    }
    public static void setButtonData(ControllerData data){
        if(buttonsOutputs==null){
            return;
        }
        for(int i=0; i < data.getButtonSize(); i++){
            buttonsOutputs.setButtonData(
                    data.getButtons(i).getTimestamp(),
                    data.getButtons(i).getMode(),
                    data.getButtons(i).getA(),
                    data.getButtons(i).getB(),
                    data.getButtons(i).getX(),
                    data.getButtons(i).getY(),
                    data.getButtons(i).getLeftThumb(),
                    data.getButtons(i).getRightThumb(),
                    data.getButtons(i).getLeftBumper(),
                    data.getButtons(i).getRightBumper()
//                    data.getButtons(i).getPovUp(),
//                    data.getButtons(i).getPovDown(),
//                    data.getButtons(i).getPovLeft(),
//                    data.getButtons(i).getPovRight()
                    );
        }
    }
    public static void setJoystickData(ControllerData data){
        if(joystickOutputs==null){
            return;
        }
        for(int i=0; i < data.getJoystickSize(); i++){
            joystickOutputs.setJoystickData(
                    data.getJoystick(i).getTimestamp(),
                    data.getJoystick(i).get_x(),
                    data.getJoystick(i).get_y(),
                    data.getJoystick(i).getRx(),
                    data.getJoystick(i).getRy(),
                    data.getJoystick(i).getPov()
            );
        }
    }
    @Override
    public void doStop() {}

    @Override
    public boolean isConnected() {return true;}

}

