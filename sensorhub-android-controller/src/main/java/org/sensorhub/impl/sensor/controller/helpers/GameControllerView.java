package org.sensorhub.impl.sensor.controller.helpers;

import android.content.Context;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.sensorhub.impl.sensor.controller.ControllerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GameControllerView extends View implements InputDeviceListener{
    private static final Logger logger = LoggerFactory.getLogger(GameControllerView.class);
    boolean Y, X, A, B, LeftThumb, RightThumb, LeftBumper, RightBumper, mode, pov_up, pov_left, pov_right, pov_down;
    float x, y, rx, ry, pov;
    InputManager inputManager;
    ControllerData controllerData;
    public GameControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();
        setFocusableInTouchMode(true);

        inputManager = (InputManager)context.getSystemService(context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, null);
        controllerData = new ControllerData();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent keyEvent){
        return handleKeyEvent(keycode, keyEvent, true);
    }
    @Override
    public boolean onKeyUp(int keycode, KeyEvent keyEvent){
        return handleKeyEvent(keycode, keyEvent, false);
    }
    public boolean handleKeyEvent(int keycode, KeyEvent keyEvent, boolean pressed){
        logger.debug("Key {} {}", pressed ? "down": "up", keycode);
        if((keyEvent.getSource()& InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD){
            if(keyEvent.getRepeatCount()==0) {
                updateButtonStates(keycode, pressed);
            }
            updateControllerData();
            ControllerDriver.setButtonData(controllerData);
        }

        return super.onKeyDown(keycode, keyEvent);
    }
    private void updateButtonStates(int keycode, boolean pressed){

        switch (keycode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                pov = 1.0f;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                pov = 0.5f;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                pov = 0.25f;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                pov =0.75f;
                break;
            case KeyEvent.KEYCODE_DPAD_UP_LEFT:
                pov= 0.125f;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                pov =0.875f;
                break;
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
                pov= 0.375f;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                pov =0.625f;
                break;
            case KeyEvent.KEYCODE_BUTTON_A:
                A = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
                B = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_X:
                X = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_Y:
                Y = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_R1:
                RightThumb = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_L1:
                LeftThumb = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_R2:
                RightBumper = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_L2:
                LeftBumper = pressed;
                break;
            case KeyEvent.KEYCODE_BUTTON_MODE:
                mode = pressed;
                break;
        }
    }
    private void updateControllerData() {
        long timestamp = System.currentTimeMillis();
        controllerData.addButtons(timestamp, mode, A, B, X, Y, LeftThumb, RightThumb, LeftBumper, RightBumper);
        controllerData.addJoystick(timestamp,x, y, rx, ry, pov);
    }


    public void updateJoystickState(MotionEvent event, int historyPos){
        InputDevice inputDevice = event.getDevice();

        x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos);
        y= getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos);
        rx = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos);
        ry = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos);


        float xaxis = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float yaxis = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        if(Float.compare(xaxis, -1.0f)==0){
            //left
            pov = 1.0f;
        }else if(Float.compare(xaxis, 1.0f)==0){
            //right
            pov =0.5f;
        }else if(Float.compare(yaxis, -1.0f)==0){
            //up
            pov = 0.25f;
        } else if (Float.compare(yaxis, 1.0f) == 0) {
            //down
            pov = 0.75f;
        }
//        logger.debug("Setting data: x={}, y={}, rx={}, ry={}", x1,y1,x2,y2);
        updateControllerData();
        ControllerDriver.setJoystickData(controllerData);

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
