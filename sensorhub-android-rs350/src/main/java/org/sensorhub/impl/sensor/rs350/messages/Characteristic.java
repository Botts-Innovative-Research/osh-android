package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class Characteristic extends ElementBase {
    String characteristicName;
    String characteristicValue;
    String characteristicValueUnits;
    String characteristicValueDataClassCode;

    public Characteristic(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "Characteristic");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "CharacteristicName":
                    characteristicName = parser.nextText();
                    break;
                case "CharacteristicValue":
                    characteristicValue = parser.nextText();
                    break;
                case "CharacteristicValueUnits":
                    characteristicValueUnits = parser.nextText();
                    break;
                case "CharacteristicValueDataClassCode":
                    characteristicValueDataClassCode = parser.nextText();
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Characteristic{" +
                "characteristicName='" + characteristicName + '\'' +
                ", characteristicValue='" + characteristicValue + '\'' +
                ", characteristicValueUnits='" + characteristicValueUnits + '\'' +
                ", characteristicValueDataClassCode='" + characteristicValueDataClassCode + '\'' +
                '}';
    }

    public String getCharacteristicName() {
        return characteristicName;
    }

    public String getCharacteristicValue() {
        return characteristicValue;
    }

    public String getCharacteristicValueUnits() {
        return characteristicValueUnits;
    }

    public String getCharacteristicValueDataClassCode() {
        return characteristicValueDataClassCode;
    }
}
