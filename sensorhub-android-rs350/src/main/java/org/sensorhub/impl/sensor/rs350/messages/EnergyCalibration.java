package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

public class EnergyCalibration extends ElementBase {
    String id;
    double[] coefficientValues;

    public EnergyCalibration(XmlPullParser parser, String id) {
        super(parser);
        this.id = id;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "EnergyCalibration");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("CoefficientValues")) {
                String values = parser.nextText();
                String[] valueArray = values.split(" ");
                coefficientValues = new double[valueArray.length];
                for (int i = 0; i < valueArray.length; i++) {
                    coefficientValues[i] = Double.parseDouble(valueArray[i]);
                }
            } else {
                skip(parser);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "EnergyCalibration{" +
                "id='" + id + '\'' +
                ", coefficientValues=" + Arrays.toString(coefficientValues) +
                '}';
    }

    public String getId() {
        return id;
    }

    public double[] getCoefficientValues() {
        return coefficientValues;
    }
}
