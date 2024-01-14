package org.sensorhub.impl.sensor.rs350.messages;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

public class RadItemCharacteristics extends ElementBase {
    List<Characteristic> characteristics = new java.util.ArrayList<>();
    String rsiScanMode;
    int rsiScanTimeoutNumber;
    String rsiAnalysisEnabled;

    public RadItemCharacteristics(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadItemCharacteristics");
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
        for (Characteristic characteristic : getCharacteristics()) {
            switch (characteristic.getCharacteristicName()) {
                case "RsiScanMode":
                    rsiScanMode = characteristic.getCharacteristicValue();
                    break;
                case "RsiScanTimeoutNumber":
                    rsiScanTimeoutNumber = Integer.parseInt(characteristic.getCharacteristicValue());
                    break;
                case "RsiAnalysisEnabled":
                    rsiAnalysisEnabled = characteristic.getCharacteristicValue();
                    break;
            }
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return "RadItemCharacteristics{" +
                "characteristics=" + characteristics +
                '}';
    }

    public List<Characteristic> getCharacteristics() {
        return characteristics;
    }

    public String getRsiScanMode() {
        return rsiScanMode;
    }

    public int getRsiScanTimeoutNumber() {
        return rsiScanTimeoutNumber;
    }

    public String getRsiAnalysisEnabled() {
        return rsiAnalysisEnabled;
    }
}
