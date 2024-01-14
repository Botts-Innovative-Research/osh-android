package org.sensorhub.impl.sensor.rs350.messages;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import javax.annotation.Nonnull;

public class RadInstrumentInformation extends ElementBase {
    String manufacturerName;
    String identifier;
    String modelName;
    String classCode;
    RadInstrumentCharacteristics radInstrumentCharacteristics;

    public RadInstrumentInformation(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadInstrumentInformation");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            switch (name) {
                case "RadInstrumentManufacturerName":
                    manufacturerName = parser.nextText();
                    break;
                case "RadInstrumentIdentifier":
                    identifier = parser.nextText();
                    break;
                case "RadInstrumentModelName":
                    modelName = parser.nextText();
                    break;
                case "RadInstrumentClassCode":
                    classCode = parser.nextText();
                    break;
                case "RadInstrumentCharacteristics":
                    radInstrumentCharacteristics = new RadInstrumentCharacteristics(parser);
                    radInstrumentCharacteristics.parse();
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return "RadInstrumentInformation{" +
                "manufacturerName='" + manufacturerName + '\'' +
                ", identifier='" + identifier + '\'' +
                ", modelName='" + modelName + '\'' +
                ", classCode='" + classCode + '\'' +
                ", radInstrumentCharacteristics=" + radInstrumentCharacteristics +
                '}';
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getModelName() {
        return modelName;
    }

    public String getClassCode() {
        return classCode;
    }

    public RadInstrumentCharacteristics getRadInstrumentCharacteristics() {
        return radInstrumentCharacteristics;
    }
}
