package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RadMeasurement extends ElementBase {
    String id;
    String radMeasurementGroupReferences;
    String gammaID;
    String neutronID;
    String measurementClassCode;
    long startDateTime;
    double realTimeDuration;
    List<Spectrum> spectrumList = new ArrayList<>();
    List<GrossCounts> grossCountsList = new ArrayList<>();
    DoseRate doseRate;
    RadInstrumentState radInstrumentState;

    Spectrum linEnCalSpectrum;
    Spectrum cmpEnCalSpectrum;
    GrossCounts gammaGrossCounts;
    GrossCounts neutronGrossCounts;

    public RadMeasurement(XmlPullParser parser, String id, String radMeasurementGroupReferences, String gammaID, String neutronID) {
        super(parser);
        this.id = id;
        this.radMeasurementGroupReferences = radMeasurementGroupReferences;
        this.gammaID = gammaID;
        this.neutronID = neutronID;
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "RadMeasurement");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
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
                case "Spectrum":
                    String spectrumID = parser.getAttributeValue(null, "id");
                    String radDetectorInformationReference = parser.getAttributeValue(null, "radDetectorInformationReference");
                    String energyCalibrationReference = parser.getAttributeValue(null, "energyCalibrationReference");
                    Spectrum spectrum = new Spectrum(parser, spectrumID, radDetectorInformationReference, energyCalibrationReference);
                    spectrum.parse();
                    spectrumList.add(spectrum);
                    break;
                case "GrossCounts":
                    String grossCountsID = parser.getAttributeValue(null, "id");
                    String grossCountsRadDetectorInformationReference = parser.getAttributeValue(null, "radDetectorInformationReference");
                    GrossCounts grossCounts = new GrossCounts(parser, grossCountsID, grossCountsRadDetectorInformationReference);
                    grossCounts.parse();
                    grossCountsList.add(grossCounts);
                    break;
                case "DoseRate":
                    String doseRateID = parser.getAttributeValue(null, "id");
                    String doseRateRadDetectorInformationReference = parser.getAttributeValue(null, "radDetectorInformationReference");
                    doseRate = new DoseRate(parser, doseRateID, doseRateRadDetectorInformationReference);
                    doseRate.parse();
                    break;
                case "RadInstrumentState":
                    String radInstrumentStateRadInstrumentInformationReference = parser.getAttributeValue(null, "radInstrumentInformationReference");
                    radInstrumentState = new RadInstrumentState(parser, radInstrumentStateRadInstrumentInformationReference);
                    radInstrumentState.parse();
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        // Parse info from arrays
        for (Spectrum spectrum : getSpectrumList()) {
            switch (spectrum.getEnergyCalibrationReference()) {
                case "LinEnCal":
                    linEnCalSpectrum = spectrum;
                    break;
                case "CmpEnCal":
                    cmpEnCalSpectrum = spectrum;
                    break;
            }
        }

        for (GrossCounts grossCounts : getGrossCountsList()) {
            if (grossCounts.getRadDetectorInformationReference().equals(gammaID)) {
                gammaGrossCounts = grossCounts;
            } else if (grossCounts.getRadDetectorInformationReference().equals(neutronID)) {
                neutronGrossCounts = grossCounts;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RadMeasurement{" +
                "id='" + id + '\'' +
                ", radMeasurementGroupReferences='" + radMeasurementGroupReferences + '\'' +
                ", measurementClassCode='" + measurementClassCode + '\'' +
                ", startDateTime='" + startDateTime + '\'' +
                ", realTimeDuration=" + realTimeDuration +
                ", spectrumList=" + spectrumList +
                ", grossCountsList=" + grossCountsList +
                ", doseRate=" + doseRate +
                ", radInstrumentState=" + radInstrumentState +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getRadMeasurementGroupReferences() {
        return radMeasurementGroupReferences;
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

    public List<Spectrum> getSpectrumList() {
        return spectrumList;
    }

    public List<GrossCounts> getGrossCountsList() {
        return grossCountsList;
    }

    public DoseRate getDoseRate() {
        return doseRate;
    }

    public RadInstrumentState getRadInstrumentState() {
        return radInstrumentState;
    }

    public Spectrum getLinEnCalSpectrum() {
        return linEnCalSpectrum;
    }

    public Spectrum getCmpEnCalSpectrum() {
        return cmpEnCalSpectrum;
    }

    public GrossCounts getGammaGrossCounts() {
        return gammaGrossCounts;
    }

    public GrossCounts getNeutronGrossCounts() {
        return neutronGrossCounts;
    }
}
