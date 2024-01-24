package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class GeographicPoint extends ElementBase {
    double latitudeValue;
    double longitudeValue;
    double elevationValue;

    public GeographicPoint(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "GeographicPoint");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "LatitudeValue":
                    latitudeValue = Double.parseDouble(parser.nextText());
                    break;
                case "LongitudeValue":
                    longitudeValue = Double.parseDouble(parser.nextText());
                    break;
                case "ElevationValue":
                    elevationValue = Double.parseDouble(parser.nextText());
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
        return "GeographicPoint{" +
                "latitudeValue=" + latitudeValue +
                ", longitudeValue=" + longitudeValue +
                ", elevationValue=" + elevationValue +
                '}';
    }

    public double getLatitudeValue() {
        return latitudeValue;
    }

    public double getLongitudeValue() {
        return longitudeValue;
    }

    public double getElevationValue() {
        return elevationValue;
    }
}
