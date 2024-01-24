package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DoseRate extends ElementBase {
    String id;
    String radDetectorInformationReference;
    double DoseRateValue;

    public DoseRate(XmlPullParser parser, String id, String radDetectorInformationReference) {
        super(parser);
        this.id = id;
        this.radDetectorInformationReference = radDetectorInformationReference;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "DoseRate");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("DoseRateValue")) {
                DoseRateValue = Double.parseDouble(parser.nextText());
            } else {
                skip(parser);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "DoseRate{" +
                "id='" + id + '\'' +
                ", radDetectorInformationReference='" + radDetectorInformationReference + '\'' +
                ", DoseRateValue=" + DoseRateValue +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadDetectorInformationReference() {
        return radDetectorInformationReference;
    }

    public double getDoseRateValue() {
        return DoseRateValue;
    }
}
