//package org.sensorhub.impl.sensor.polar;
//
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothManager;
//import android.content.Context;
//import android.widget.Toast;
//
//import com.polar.sdk.api.PolarBleApi;
//import com.polar.sdk.api.PolarBleApiCallback;
//import com.polar.sdk.api.model.PolarDeviceInfo;
//import com.polar.sdk.api.model.PolarHrData;
//
//import org.sensorhub.android.SensorHubService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//
//public class PolarCallback extends PolarBleApiCallback {
//    PolarBleApi mApi;
//    BluetoothAdapter btAdapter;
//    Context context;
//    static final Logger logger = LoggerFactory.getLogger(Polar.class.getSimpleName());
//
//    @Override
//    public void blePowerStateChanged(boolean powered) {
//        super.blePowerStateChanged(powered);
//        if(powered){
//            context = SensorHubService.getContext();
//            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
//            btAdapter = bluetoothManager.getAdapter();
//
//            if (btAdapter == null || !btAdapter.isEnabled()) {
//                Toast.makeText(context, "bluetooth adapter is null or not enabled", Toast.LENGTH_LONG).show();
//            }
//        }
//        else{
//            Toast.makeText(context, "Powered is false, no bluetooth connection", Toast.LENGTH_LONG).show();
//        }
//        logger.debug("Bluetooth state changed " + powered);
//    }
//
//    //    @Override
////    public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
////        super.deviceConnected(polarDeviceInfo);
////        logger.debug("Device connected " + polarDeviceInfo.getDeviceId());
////        mApi.startHrStreaming(polarDeviceInfo.getDeviceId());
////
////    }
//
//    @Override
//    public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
//        super.deviceConnecting(polarDeviceInfo);
//            logger.debug ("connecting"+ polarDeviceInfo.getDeviceId());
//    }
//
//    @Override
//    public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
//        super.deviceDisconnected(polarDeviceInfo);
//        logger.debug("Device disconnected " + polarDeviceInfo.getDeviceId());
//    }
//
//
//    @Override
//    public void bleSdkFeatureReady(String identifier, PolarBleApi.PolarBleSdkFeature feature) {
//        super.bleSdkFeatureReady(identifier, feature);
//    }
//
////    @Override
////    public void disInformationReceived( String identifier, UUID uuid, String value) {
////        super.disInformationReceived(identifier, uuid, value);
////    }
//
//
//    @Override
//    public void batteryLevelReceived(String identifier, int level) {
//        super.batteryLevelReceived(identifier, level);
//        logger.debug("Battery: " + level);
//    }
//
//    @Override
//    public void hrNotificationReceived(String identifier, PolarHrData.PolarHrSample hrSample) {
//        super.hrNotificationReceived(identifier, hrSample);
//        logger.debug("HR = " + hrSample.getHr());
//        int heartRate = hrSample.getHr();
//        List<Integer> rrsMs = hrSample.getRrsMs(); //resting heart r
//        String msg = "Heart Rate = " + hrSample.getHr() + "\n" + "RR";
//        logger.debug("Received heart rate: {} BPM", heartRate);
//    }
//}