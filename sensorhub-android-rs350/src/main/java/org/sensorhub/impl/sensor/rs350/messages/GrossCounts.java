package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.time.Duration;

public class GrossCounts extends ElementBase {
    String id;
    String radDetectorInformationReference;
    Double liveTimeDuration;
    int countData;

    public GrossCounts(XmlPullParser parser, String id, String radDetectorInformationReference) {
        super(parser);
        this.id = id;
        this.radDetectorInformationReference = radDetectorInformationReference;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "GrossCounts");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "LiveTimeDuration":
                    Duration duration = Duration.parse(parser.nextText());
                    liveTimeDuration = duration.toMillis() / 1000.0;
                    break;
                case "CountData":
                    countData = Integer.parseInt(parser.nextText());
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
        return "GrossCounts{" +
                "id='" + id + '\'' +
                ", radDetectorInformationReference='" + radDetectorInformationReference + '\'' +
                ", liveTimeDuration=" + liveTimeDuration +
                ", countData=" + countData +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadDetectorInformationReference() {
        return radDetectorInformationReference;
    }

    public double getLiveTimeDuration() {
        return liveTimeDuration;
    }

    public int getCountData() {
        return countData;
    }
}
