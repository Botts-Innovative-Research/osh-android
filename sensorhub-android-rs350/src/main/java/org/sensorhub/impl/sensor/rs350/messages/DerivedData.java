package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class DerivedData extends ElementBase {
    String id;
    String remark;
    String measurementClassCode;
    long startDateTime;
    double realTimeDuration;

    public DerivedData(XmlPullParser parser, String id) {
        super(parser);
        this.id = id;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "DerivedData");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "Remark":
                    remark = parser.nextText();
                    break;
                case "MeasurementClassCode":
                    measurementClassCode = parser.nextText();
                    break;
                case "StartDateTime":
                    Instant instant = Instant.parse(parser.nextText());
                    Date date = Date.from(instant);
                    startDateTime = date.getTime();
                    break;
                case "RealTimeDuration":
                    Duration duration = Duration.parse(parser.nextText());
                    realTimeDuration = duration.toMillis() / 1000.0;
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
        return "DerivedData{" +
                "id='" + id + '\'' +
                ", remark='" + remark + '\'' +
                ", measurementClassCode='" + measurementClassCode + '\'' +
                ", startDateTime=" + startDateTime +
                ", realTimeDuration=" + realTimeDuration +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRemark() {
        return remark;
    }

    public String getMeasurementClassCode() {
        return measurementClassCode;
    }

    public long getStartDateTime() {
        return startDateTime;
    }

    public double getRealTimeDuration() {
        return realTimeDuration;
    }
}
