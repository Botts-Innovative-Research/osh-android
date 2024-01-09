/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

package org.sensorhub.impl.sensor.kromek.d5.reports;

import static org.sensorhub.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static org.sensorhub.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID;

import android.support.annotation.NonNull;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;

import org.sensorhub.impl.sensor.kromek.d5.enums.KromekSerialUSBOTGMode;
import org.vast.swe.SWEHelper;

public class KromekSerialOTGReport extends SerialReport {
    private KromekSerialUSBOTGMode mode;

    /**
     * Create a new report. This report has no data and is sent to the device to request a report.
     */
    public KromekSerialOTGReport() {
        this(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID, null);
    }

    /**
     * Create a new report from the given data. This constructor is used by reflection in the MessageRouter.
     *
     * @param componentId Component ID for the report
     * @param reportId    Report ID for the report
     * @param data        Data for the report, as received from the device
     */
    public KromekSerialOTGReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    @Override
    public void decodePayload(byte[] payload) {
        if (payload == null) return;

        mode = KromekSerialUSBOTGMode.values()[bytesToUInt(payload[0])];
    }

    @Override
    @NonNull
    public String toString() {
        return KromekSerialOTGReport.class.getSimpleName() + " {" +
                "mode=" + mode +
                '}';
    }

    @Override
    public DataRecord createDataRecord() {
        SWEHelper sweFactory = new SWEHelper();
        return sweFactory.createRecord()
                .name(getReportName())
                .label(getReportLabel())
                .description(getReportDescription())
                .definition(getReportDefinition())
                .addField("timestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Precision Time Stamp"))
                .addField("mode", sweFactory.createCategory()
                        .label("Mode")
                        .description("USB OTG Mode")
                        .definition(SWEHelper.getPropertyUri("mode")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, mode.toString());
    }

    @Override
    void setReportInfo() {
        setReportName("KromekSerialOTGReport");
        setReportLabel("USB OTG");
        setReportDescription("USB OTG");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(5);
    }
}