package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class RadAlarm extends ElementBase {
    String id;
    String radDetectorInformationReferences;
    String radAlarmCategoryCode;
    String radAlarmDescription;

    public RadAlarm(XmlPullParser parser, String id, String radDetectorInformationReferences) {
        super(parser);
        this.id = id;
        this.radDetectorInformationReferences = radDetectorInformationReferences;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadAlarm");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "RadAlarmCategoryCode":
                    radAlarmCategoryCode = parser.nextText();
                    break;
                case "RadAlarmDescription":
                    radAlarmDescription = parser.nextText();
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
        return "RadAlarm{" +
                "id='" + id + '\'' +
                ", radDetectorInformationReferences='" + radDetectorInformationReferences + '\'' +
                ", radAlarmCategoryCode='" + radAlarmCategoryCode + '\'' +
                ", radAlarmDescription='" + radAlarmDescription + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadDetectorInformationReferences() {
        return radDetectorInformationReferences;
    }

    public String getRadAlarmCategoryCode() {
        return radAlarmCategoryCode;
    }

    public String getRadAlarmDescription() {
        return radAlarmDescription;
    }
}
