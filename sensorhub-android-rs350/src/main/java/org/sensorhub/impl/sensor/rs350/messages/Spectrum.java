package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Spectrum extends ElementBase {
    String id;
    String radDetectorInformationReference;
    String energyCalibrationReference;
    Duration liveTimeDuration;
    double[] channelData;

    public Spectrum(XmlPullParser parser, String id, String radDetectorInformationReference, String energyCalibrationReference) {
        super(parser);
        this.id = id;
        this.radDetectorInformationReference = radDetectorInformationReference;
        this.energyCalibrationReference = energyCalibrationReference;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "Spectrum");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "LiveTimeDuration":
                    liveTimeDuration = Duration.parse(parser.nextText());
                    break;
                case "ChannelData":
                    String values = parser.nextText();
                    String[] valueArray = values.split(" ");
                    // Remove anything that isn't a number
                    List<String> valueList = new ArrayList<>();
                    for (String value : valueArray) {
                        if (value.matches("-?\\d+(\\.\\d+)?")) {
                            valueList.add(value);
                        }
                    }
                    channelData = new double[valueList.size()];
                    for (int i = 0; i < valueList.size(); i++) {
                        channelData[i] = Double.parseDouble(valueList.get(i));
                    }

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
        return "Spectrum{" +
                "energyCalibrationReference='" + energyCalibrationReference + '\'' +
                ", liveTimeDuration=" + liveTimeDuration +
                ", channelData=" + Arrays.toString(channelData) +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadDetectorInformationReference() {
        return radDetectorInformationReference;
    }

    public String getEnergyCalibrationReference() {
        return energyCalibrationReference;
    }

    public Duration getLiveTimeDuration() {
        return liveTimeDuration;
    }

    public double[] getChannelData() {
        return channelData;
    }
}
