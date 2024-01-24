package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class RadDetectorInformation extends ElementBase {
    String id;
    String radDetectorName;
    String radDetectorCategoryCode;
    String radDetectorKindCode;

    public RadDetectorInformation(XmlPullParser parser, String id) {
        super(parser);
        this.id = id;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadDetectorInformation");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "RadDetectorName":
                    radDetectorName = parser.nextText();
                    break;
                case "RadDetectorCategoryCode":
                    radDetectorCategoryCode = parser.nextText();
                    break;
                case "RadDetectorKindCode":
                    radDetectorKindCode = parser.nextText();
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
        return "RadDetectorInformation{" +
                "id='" + id + '\'' +
                ", radDetectorName='" + radDetectorName + '\'' +
                ", radDetectorCategoryCode='" + radDetectorCategoryCode + '\'' +
                ", radDetectorKindCode='" + radDetectorKindCode + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadDetectorName() {
        return radDetectorName;
    }

    public String getRadDetectorCategoryCode() {
        return radDetectorCategoryCode;
    }

    public String getRadDetectorKindCode() {
        return radDetectorKindCode;
    }
}
