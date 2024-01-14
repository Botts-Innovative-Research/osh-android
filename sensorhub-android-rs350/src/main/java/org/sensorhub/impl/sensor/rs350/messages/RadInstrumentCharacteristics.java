package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RadInstrumentCharacteristics extends ElementBase {
    List<Characteristic> characteristics = new ArrayList<>();
    String deviceName;
    double batteryCharge = 0.0;

    public RadInstrumentCharacteristics(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadInstrumentCharacteristics");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("Characteristic")) {
                Characteristic characteristic = new Characteristic(parser);
                characteristic.parse();
                characteristics.add(characteristic);
            } else {
                skip(parser);
            }
        }

        // Parse info from arrays
        for (Characteristic characteristic : characteristics) {
            switch (characteristic.getCharacteristicName()) {
                case "RsiDeviceName":
                    deviceName = characteristic.getCharacteristicValue();
                    break;
                case "Battery Charge":
                    batteryCharge = Double.parseDouble(characteristic.getCharacteristicValue());
                    break;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RadInstrumentCharacteristics{" +
                "characteristics=" + characteristics +
                '}';
    }

    public List<Characteristic> getCharacteristics() {
        return characteristics;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public double getBatteryCharge() {
        return batteryCharge;
    }
}
