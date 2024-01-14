package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class RadInstrumentState extends ElementBase {
    String radInstrumentInformationReference;
    StateVector stateVector;

    public RadInstrumentState(XmlPullParser parser, String radInstrumentInformationReference) {
        super(parser);
        this.radInstrumentInformationReference = radInstrumentInformationReference;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadInstrumentState");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("StateVector")) {
                stateVector = new StateVector(parser);
                stateVector.parse();
            } else {
                skip(parser);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RadInstrumentState{" +
                "radInstrumentInformationReference='" + radInstrumentInformationReference + '\'' +
                ", stateVector=" + stateVector +
                '}';
    }

    public String getRadInstrumentInformationReference() {
        return radInstrumentInformationReference;
    }

    public StateVector getStateVector() {
        return stateVector;
    }
}
