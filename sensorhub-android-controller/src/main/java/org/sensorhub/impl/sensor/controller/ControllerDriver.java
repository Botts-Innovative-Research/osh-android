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
import android.os.Looper;

import net.opengis.sensorml.v20.PhysicalComponent;

import org.sensorhub.android.SensorHubService;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.sensorhub.impl.sensor.controller.helpers.FindController;
import org.sensorhub.impl.sensor.controller.helpers.GameControllerState;
import org.sensorhub.impl.sensor.controller.helpers.GameControllerView;

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

public class ControllerDriver extends AbstractSensorModule<ControllerConfig> {
    private HandlerThread eventThread;
    ControllerOutput output;
    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;
    private Context context;
    private GameControllerView gameControllerView;
    InputManager inputManager;
    GameControllerState gamepad;
    boolean Y, X, A, B, LeftThumb, RightThumb, LeftBumper, RightBumper, mode, x, y, rx, ry, dpad_up, dpad_left, dpad_right, dpad_down;
    public ControllerDriver() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }
    @Override
    public void doInit() {
        generateUniqueID("urn:osh:", config.getDeviceName());
        generateXmlID("android:controller:", config.getDeviceName());

        context = SensorHubService.getContext();
        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);

        output = new ControllerOutput(this);
        output.doInit();
        addOutput(output, false);
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

        gameControllerView = new GameControllerView(context, eventHandler, output);
        gameControllerView.onInputDeviceAdded(gamepad.getDevice().getId());

    }

//    public void updateButtonStates(int keycode, boolean pressed){
//        logger.debug("updating button states: keycode: {}, pressed: {}", keycode, pressed);
//        switch (keycode) {
//            case KeyEvent.KEYCODE_DPAD_LEFT:
//                dpad_left = pressed;
//                break;
//            case KeyEvent.KEYCODE_DPAD_RIGHT:
//                dpad_right = pressed;
//                break;
//            case KeyEvent.KEYCODE_DPAD_UP:
//                dpad_up = pressed;
//                break;
//            case KeyEvent.KEYCODE_DPAD_DOWN:
//                dpad_down = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_A:
//                A = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_B:
//                B = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_X:
//                X = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_Y:
//                Y = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_R1:
//                RightThumb = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_L1:
//                LeftThumb = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_R2:
//                RightBumper = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_L2:
//                LeftBumper = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_THUMBL:
//                x = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_THUMBR:
//                rx = pressed;
//                break;
//            case KeyEvent.KEYCODE_BUTTON_MODE:
//                mode = pressed;
//                break;
//        }
//        logger.debug("Setting data: mode={}, A={}, X={}, Y={}, dpad_down={}, dpad_left={}, dpad_up={}, dpad_right={}, LeftThumb={}, RightThumb={}, LeftBumper={}, RightBumper={}, x={}, y={}, B={}, rx={}, ry={}",
//                mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, B, rx, ry);
//
//        output.setData(mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, B, rx, ry);
//    }

    @Override
    public void doStop() {}

    @Override
    public boolean isConnected() {return true;}

}

