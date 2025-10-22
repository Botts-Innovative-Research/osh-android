package org.sensorhub.impl.sensor.meshtastic;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import net.opengis.sensorml.v20.PhysicalComponent;
import org.meshtastic.proto.MeshProtos;
import org.sensorhub.android.SensorHubService;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.android.SensorMLBuilder;
import org.sensorhub.impl.sensor.meshtastic.control.TextMessageControl;
import org.sensorhub.impl.sensor.meshtastic.outputs.AbstractMeshtasticOutput;
import org.sensorhub.impl.sensor.meshtastic.outputs.MyNodeInfoOutput;
import org.sensorhub.impl.sensor.meshtastic.outputs.NodeInfoOutput;
import org.sensorhub.impl.sensor.meshtastic.outputs.PositionPacketOutput;
import org.sensorhub.impl.sensor.meshtastic.outputs.TextMessagePacketOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class MeshtasticSensor extends AbstractSensorModule<MeshtasticConfig> {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID MODEL_NUMBER_CHARACTERISTIC = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID FROMRADIO_CHARACTERISTIC_UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002");
    private static final UUID TORADIO_CHARACTERISTIC_UUID = UUID.fromString("f75c76d2-129e-4dad-a1dd-7866124401e7");
    private static final UUID MESHTASTIC_SERVICE_UUID = UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd");
    private static final UUID FROMNUM_CHARACTERISTIC_UUID = UUID.fromString("ed9da18c-a800-4f66-a670-aa7547e34453");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    AtomicBoolean isProcessing = new AtomicBoolean(false);
    AtomicBoolean readFromRadio = new AtomicBoolean(false);

    private static final byte START1 = (byte) 0x94;
    private static final byte START2 = (byte) 0xC3;

    private final ArrayList<PhysicalComponent> smlComponents;
    private final SensorMLBuilder smlBuilder;

    private BluetoothGatt btGatt;
    private BluetoothAdapter btAdapter;
    private Context context;
    private BluetoothGattService deviceInformationService;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic modelNumberChar;
    private BluetoothGattCharacteristic toRadioChar;
    private BluetoothGattCharacteristic fromRadioChar;
    private BluetoothGattCharacteristic fromNumChar;
    NodeInfoOutput nodeInfoOutput;
    MyNodeInfoOutput myNodeInfoOutput;
    PositionPacketOutput positionPacketOutput;
    TextMessagePacketOutput textMessagePacketOutput;
    TextMessageControl textMessageControl;

    private boolean btConnected = false;
    private HandlerThread eventThread;

    public MeshtasticSensor() {
        this.smlComponents = new ArrayList<PhysicalComponent>();
        this.smlBuilder = new SensorMLBuilder();
    }

    @Override
    public boolean isConnected() {
        return btConnected;
    }

    @Override
    public void doInit() {
        this.xmlID = "MESHTASTIC" + Build.SERIAL;
        this.uniqueID = MeshtasticConfig.getUid();

        context = SensorHubService.getContext();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Activity.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()) {
            logger.debug("Bluetooth is not enabled");
        }


        nodeInfoOutput = new NodeInfoOutput(this);
        myNodeInfoOutput = new MyNodeInfoOutput(this);
        positionPacketOutput = new PositionPacketOutput(this);
        textMessagePacketOutput = new TextMessagePacketOutput(this);

        addOutput(nodeInfoOutput, false);
        addOutput(myNodeInfoOutput, false);
        addOutput(positionPacketOutput, false);
        addOutput(textMessagePacketOutput, false);

        textMessageControl = new TextMessageControl(this);
        addControlInput(textMessageControl);
    }

    @Override
    public void doStart() throws SensorException {
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        BluetoothDevice device = null;
        for (BluetoothDevice d : devices) {
            if (d.getAddress().equals(config.device_name)) {
                device = d;
            }
        }
        if (null == device) {
            reportError("Could not find Bluetooth device, unable to start.", new Throwable());
        }

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            // request permission
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        btGatt = device.connectGatt(context, true, gattCallback);


        eventThread = new HandlerThread("MeshtasticNodeEventThread");
        eventThread.start();
        Handler eventHandler = new Handler(eventThread.getLooper());

    }

    @Override
    public void doStop() {
        btGatt.disconnect();
        btGatt.close();
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                btConnected = true;
                boolean discoveryStarted = gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(TORADIO_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue();
                onMessage(data);
            } else if (characteristic.getUuid().equals(FROMRADIO_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue();
                onMessage(data);
            } else if (characteristic.getUuid().equals(FROMNUM_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue();
                onMessage(data);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            // set MTU size to 512
            gatt.requestMtu(512);

            // request handshake
            MeshProtos.ToRadio handshake = MeshProtos.ToRadio.newBuilder()
                    // TODO: Verify response ID in future if needed
                    .setWantConfigId(0)
                    .build();
            try {
                sendMessage(handshake);
            } catch (IOException e) {
                getLogger().error("Failed to send handshake message", e);
            }


            deviceInformationService = btGatt.getService((DEVICE_INFORMATION_SERVICE));
            service = btGatt.getService(MESHTASTIC_SERVICE_UUID);

            fromNumChar = service.getCharacteristic(FROMNUM_CHARACTERISTIC_UUID);
            fromRadioChar = service.getCharacteristic(FROMRADIO_CHARACTERISTIC_UUID);
            toRadioChar = service.getCharacteristic(TORADIO_CHARACTERISTIC_UUID);


            if (fromNumChar != null) {
                logger.debug("BluetoothDeviceManager", "Found FROMNUM characteristic");
            }
            if (fromRadioChar != null) {
                logger.debug("BluetoothDeviceManager", "Found FROMRADIO characteristic");
            }
            if (toRadioChar != null) {
                logger.debug("BluetoothDeviceManager", "Found TORADIO characteristic");
            }

            // read fromRadio until you get empty buffer

            while (!readFromRadio.get()) {
                gatt.readCharacteristic(fromRadioChar);
            }




            gatt.setCharacteristicNotification(fromNumChar, true);

            BluetoothGattDescriptor descriptor = fromNumChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logger.info("Failed to write to characteristic");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (characteristic.getUuid().equals(FROMNUM_CHARACTERISTIC_UUID)) {
//                   boolean isDone = false;
//                   while (!isDone) {
                      gatt.readCharacteristic(fromRadioChar);
//
//                   }

                }
                if (characteristic.getUuid().equals(FROMRADIO_CHARACTERISTIC_UUID)) {
                    byte[] data = characteristic.getValue();
//                    onMessage(data);
                    if (data.length == 0) {
                        readFromRadio.set(true);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }
    };


    public void sendMessage(MeshProtos.ToRadio message) throws IOException {
        byte[] bytes = message.toByteArray();

        toRadioChar.setValue(bytes);

        boolean ok = btGatt.writeCharacteristic(toRadioChar);
        if (!ok) {
            logger.debug("error");
        }
    }

    private void onMessage(byte[] bytes) {

        try {
            MeshProtos.FromRadio msg = MeshProtos.FromRadio.parseFrom(bytes);
            for (Map.Entry<String, ? extends IStreamingDataInterface> entry : getOutputs().entrySet()) {
                AbstractMeshtasticOutput output = (AbstractMeshtasticOutput) entry.getValue();
                if (output.canHandle(msg))
                    output.onMessage(msg);
            }
            getLogger().info("New message: " + msg);
        } catch (Exception e) {
            getLogger().error("Invalid protobuf: " + e.getMessage());
        }
    }

    private void startProcessing() {
//        executor.scheduleWithFixedDelay(() -> {
//               btGatt.readCharacteristic(fromNumChar);
//        }, 1, 1, TimeUnit.SECONDS);
    }

}
