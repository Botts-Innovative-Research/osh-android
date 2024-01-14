package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class StateVector extends ElementBase {
    GeographicPoint geographicPoint;

    public StateVector(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "StateVector");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("GeographicPoint")) {
                geographicPoint = new GeographicPoint(parser);
                geographicPoint.parse();
            } else {
                skip(parser);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "StateVector{" +
                "geographicPoint=" + geographicPoint +
                '}';
    }

    public GeographicPoint getGeographicPoint() {
        return geographicPoint;
    }
}
