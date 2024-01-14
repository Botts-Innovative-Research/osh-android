package org.sensorhub.impl.sensor.rs350.messages;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RadInstrumentData extends ElementBase {
    Logger logger = LoggerFactory.getLogger(RadInstrumentData.class);

    RadInstrumentInformation radInstrumentInformation;
    List<RadDetectorInformation> radDetectorInformationList = new ArrayList<>();
    RadItemInformation radItemInformation;
    List<EnergyCalibration> energyCalibrationList = new ArrayList<>();
    List<RadMeasurement> radMeasurementList = new ArrayList<>();
    DerivedData derivedData;
    AnalysisResults analysisResults;

    String gammaID;
    String neutronID;
    double[] linEnCal;
    double[] cmpEnCal;
    RadMeasurement foregroundRadMeasurement;
    RadMeasurement backgroundRadMeasurement;

    public RadInstrumentData(XmlPullParser parser) {
        super(parser);
    }

    @Override
    public void parse() throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "RadInstrumentInformation":
                    radInstrumentInformation = new RadInstrumentInformation(parser);
                    radInstrumentInformation.parse();
                    break;
                case "RadDetectorInformation":
                    String radDetectorInformationID = parser.getAttributeValue(null, "id");
                    RadDetectorInformation radDetectorInformation = new RadDetectorInformation(parser, radDetectorInformationID);
                    radDetectorInformation.parse();
                    radDetectorInformationList.add(radDetectorInformation);

                    // Get IDs for gamma and neutron detectors, which will be used by RadMeasurement
                    if (radDetectorInformation.getRadDetectorCategoryCode().equals("Gamma")) {
                        gammaID = radDetectorInformationID;
                    } else if (radDetectorInformation.getRadDetectorCategoryCode().equals("Neutron")) {
                        neutronID = radDetectorInformationID;
                    }
                    break;
                case "RadItemInformation":
                    radItemInformation = new RadItemInformation(parser);
                    radItemInformation.parse();
                    break;
                case "EnergyCalibration":
                    String energyCalibrationID = parser.getAttributeValue(null, "id");
                    EnergyCalibration energyCalibration = new EnergyCalibration(parser, energyCalibrationID);
                    energyCalibration.parse();
                    energyCalibrationList.add(energyCalibration);
                    break;
                case "RadMeasurement":
                    String radMeasurementID = parser.getAttributeValue(null, "id");
                    String radMeasurementGroupReferences = parser.getAttributeValue(null, "radMeasurementGroupReferences");

                    RadMeasurement radMeasurement = new RadMeasurement(parser, radMeasurementID, radMeasurementGroupReferences, gammaID, neutronID);
                    radMeasurement.parse();
                    radMeasurementList.add(radMeasurement);
                    break;
                case "DerivedData":
                    String derivedDataID = parser.getAttributeValue(null, "id");
                    derivedData = new DerivedData(parser, derivedDataID);
                    derivedData.parse();
                    break;
                case "AnalysisResults":
                    String analysisResultsRadMeasurementGroupReferences = parser.getAttributeValue(null, "radMeasurementGroupReferences");
                    analysisResults = new AnalysisResults(parser, analysisResultsRadMeasurementGroupReferences);
                    analysisResults.parse();
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        // Parse info from arrays
        for (EnergyCalibration energyCalibration : getEnergyCalibrationList()) {
            if (energyCalibration.getId().equals("LinEnCal")) {
                linEnCal = energyCalibration.getCoefficientValues();
            } else if (energyCalibration.getId().equals("CmpEnCal")) {
                cmpEnCal = energyCalibration.getCoefficientValues();
            }
        }

        for (RadMeasurement radMeasurement : getRadMeasurementList()) {
            if (radMeasurement.getMeasurementClassCode().equals("Foreground")) {
                foregroundRadMeasurement = radMeasurement;
            } else if (radMeasurement.getMeasurementClassCode().equals("Background")) {
                backgroundRadMeasurement = radMeasurement;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RadInstrumentData{" + "\n" +
                "radInstrumentInformation=" + radInstrumentInformation + "\n" +
                "radDetectorInformationList=" + radDetectorInformationList + "\n" +
                "radItemInformation=" + radItemInformation + "\n" +
                "energyCalibrationList=" + energyCalibrationList + "\n" +
                "radMeasurementList=" + radMeasurementList + "\n" +
                "derivedData=" + derivedData + "\n" +
                "analysisResults=" + analysisResults + "\n" +
                '}';
    }

    public RadInstrumentInformation getRadInstrumentInformation() {
        return radInstrumentInformation;
    }

    public RadItemInformation getRadItemInformation() {
        return radItemInformation;
    }

    public List<EnergyCalibration> getEnergyCalibrationList() {
        return energyCalibrationList;
    }

    public List<RadMeasurement> getRadMeasurementList() {
        return radMeasurementList;
    }

    public DerivedData getDerivedData() {
        return derivedData;
    }

    public AnalysisResults getAnalysisResults() {
        return analysisResults;
    }

    public double[] getLinEnCal() {
        return linEnCal;
    }

    public double[] getCmpEnCal() {
        return cmpEnCal;
    }

    public RadMeasurement getForegroundRadMeasurement() {
        return foregroundRadMeasurement;
    }

    public RadMeasurement getBackgroundRadMeasurement() {
        return backgroundRadMeasurement;
    }
}
