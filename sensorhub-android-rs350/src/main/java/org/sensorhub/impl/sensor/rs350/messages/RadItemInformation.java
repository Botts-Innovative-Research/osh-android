package org.sensorhub.impl.sensor.rs350.messages;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import javax.annotation.Nonnull;

public class RadItemInformation extends ElementBase {
    RadItemCharacteristics radItemCharacteristics;

    public RadItemInformation(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadItemInformation");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("RadItemCharacteristics")) {
                radItemCharacteristics = new RadItemCharacteristics(parser);
                radItemCharacteristics.parse();
            } else {
                skip(parser);
            }
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return "RadItemInformation{" +
                "radItemCharacteristics=" + radItemCharacteristics +
                '}';
    }

    public RadItemCharacteristics getRadItemCharacteristics() {
        return radItemCharacteristics;
    }
}
