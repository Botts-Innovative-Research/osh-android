package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AnalysisResults extends ElementBase {
    String radMeasurementGroupReferences;
    RadAlarm radAlarm;

    public AnalysisResults(XmlPullParser parser, String radMeasurementGroupReferences) {
        super(parser);
        this.radMeasurementGroupReferences = radMeasurementGroupReferences;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "AnalysisResults");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("RadAlarm")) {
                String radAlarmId = parser.getAttributeValue(null, "id");
                String radAlarmRadDetectorInformationReferences = parser.getAttributeValue(null, "radDetectorInformationReferences");
                radAlarm = new RadAlarm(parser, radAlarmId, radAlarmRadDetectorInformationReferences);
                radAlarm.parse();
            } else {
                skip(parser);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "AnalysisResults{" +
                "radMeasurementGroupReferences='" + radMeasurementGroupReferences + '\'' +
                ", radAlarm=" + radAlarm +
                '}';
    }

    public String getRadMeasurementGroupReferences() {
        return radMeasurementGroupReferences;
    }

    public RadAlarm getRadAlarm() {
        return radAlarm;
    }
}
