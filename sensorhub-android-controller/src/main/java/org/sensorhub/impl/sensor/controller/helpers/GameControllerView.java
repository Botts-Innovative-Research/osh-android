package org.sensorhub.impl.sensor.controller.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.sensorhub.impl.sensor.controller.ControllerOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class GameControllerView extends View implements InputDeviceListener{
    private static final Logger logger = LoggerFactory.getLogger(GameControllerView.class);
    boolean Y, X, A, B, LeftThumb, RightThumb, LeftBumper, RightBumper, mode, dpad_up, dpad_left, dpad_right, dpad_down;
    float x, y, rx, ry;
    InputManager inputManager;
    ControllerOutput output;
    GameControllerState gamepad;
    private String StringDpad;
    private String StringButtons;

    Handler eventHandler;


    public GameControllerView(Context context, Handler eventHandler, ControllerOutput output) {
        super(context);
        isFocusable();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        this.output = output;
        inputManager = (InputManager)context.getSystemService(context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, eventHandler);

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent keyEvent){
        logger.debug("Key down {}", keycode);
        boolean pressed = true;
        logger.debug("updating button states: keycode: {}, pressed: {}", keycode, pressed);
        if(keyEvent.getRepeatCount()==0) {
            switch (keycode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    dpad_left = pressed;
                    StringDpad = "left";
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    dpad_right = pressed;
                    StringDpad = "right";
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    dpad_up = pressed;
                    StringDpad = "up";
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    dpad_down = pressed;
                    StringDpad = "down";
                    break;
                case KeyEvent.KEYCODE_BUTTON_A:
                    A = pressed;
                    StringButtons = "A";
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    B = pressed;
                    StringButtons = "B";
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    X = pressed;
                    StringButtons = "X";
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    Y = pressed;
                    StringButtons = "Y";
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    RightThumb = pressed;
                    StringButtons = "R1";
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    LeftThumb = pressed;
                    StringButtons = "L1";
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    RightBumper = pressed;
                    StringButtons = "R2";
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    LeftBumper = pressed;
                    StringButtons = "L2";
                    break;
                case KeyEvent.KEYCODE_BUTTON_MODE:
                    mode = pressed;
                    StringButtons = "MODE";
                    break;
            }
        }
        logger.debug("buttons: {}", StringButtons);
        logger.debug("dpad: {}", StringDpad);
//        updateButtonStates(keycode, true, keyEvent);
        return super.onKeyDown(keycode, keyEvent);
    }

    @Override
    public boolean onKeyUp(int keycode, KeyEvent keyEvent){
        logger.debug("key up {}", keycode);

        updateButtonStates(keycode,false, keyEvent);
        return super.onKeyUp(keycode, keyEvent);
    }

    public void updateJoystickState(MotionEvent event, int historyPos){
        InputDevice inputDevice = event.getDevice();

        float x1 = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos);
        float x2 = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos);

        float y1 = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos);
        float y2 = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos);

//        logger.debug("Setting data: x={}, y={}, rx={}, ry={}", x,y,z,rz);
        eventHandler.post(this:: updateGamepadOutputs);
//        updateGamepadOutputs();
//        output.setJoyStickData(x, y, rx, ry);
    }
    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis): event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    public void updateGamepadOutputs(){
        output.setDataBlock(mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, B, rx, ry);
    }
    public void updateButtonStates(int keycode, boolean pressed, KeyEvent event){
        logger.debug("updating button states: keycode: {}, pressed: {}", keycode, pressed);
        if(event.getRepeatCount()==0){
            switch (keycode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    dpad_left = pressed;
                    StringDpad = "left";
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    dpad_right = pressed;
                    StringDpad = "right";
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    dpad_up = pressed;
                    StringDpad = "up";
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    dpad_down = pressed;
                    StringDpad = "down";
                    break;
                case KeyEvent.KEYCODE_BUTTON_A:
                    A = pressed;
                    StringButtons = "A";
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    B = pressed;
                    StringButtons = "B";
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    X = pressed;
                    StringButtons = "X";
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    Y = pressed;
                    StringButtons = "Y";
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    RightThumb = pressed;
                    StringButtons = "R1";
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    LeftThumb = pressed;
                    StringButtons = "L1";
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    RightBumper = pressed;
                    StringButtons = "R2";
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    LeftBumper = pressed;
                    StringButtons = "L2";
                    break;
                case KeyEvent.KEYCODE_BUTTON_MODE:
                    mode = pressed;
                    StringButtons = "MODE";
                    break;
            }

        }

        logger.debug("Setting data: mode={}, A={}, X={}, Y={}, dpad_down={}, dpad_left={}, dpad_up={}, dpad_right={}, LeftThumb={}, RightThumb={}, LeftBumper={}, RightBumper={}, B={}",
                mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, B);

        eventHandler.post(this:: updateGamepadOutputs);

//        updateGamepadOutputs();
//        output.setGamepadData(mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, B);
//        output.setData(mode, A, X, Y, dpad_down, dpad_left, dpad_up, dpad_right, LeftThumb, RightThumb, LeftBumper, RightBumper, x, y, B, rx, ry);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE) {
            logger.debug("motion event detected");

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();
            logger.debug("history size: {}", historySize);

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                updateJoystickState(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            updateJoystickState(event, -1);
            return true;
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        logger.debug("device added: {}", deviceId);
        InputDevice device = inputManager.getInputDevice(deviceId);
        if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            logger.debug("Gamepad detected: {}", deviceId);
        }

    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        logger.debug("device removed: {}", deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        logger.debug("device changed: {}", deviceId);
    }

}
