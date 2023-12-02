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
import static org.sensorhub.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_MAX_UNIT_ID_LENGTH;
import static org.sensorhub.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID;

import android.support.annotation.NonNull;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;

import org.vast.swe.SWEHelper;

import java.util.Arrays;

public class KromekSerialUnitIDReport extends SerialReport {
    private final int[] unitID = new int[KROMEK_SERIAL_MAX_UNIT_ID_LENGTH];

    /**
     * Create a new report. This report has no data and is sent to the device to request a report.
     */
    public KromekSerialUnitIDReport() {
        this(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID, null);
    }

    /**
     * Create a new report from the given data. This constructor is used by reflection in the MessageRouter.
     *
     * @param componentId Component ID for the report
     * @param reportId    Report ID for the report
     * @param data        Data for the report, as received from the device
     */
    public KromekSerialUnitIDReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    @Override
    public void decodePayload(byte[] payload) {
        if (payload == null) return;

        for (int i = 0; i < KROMEK_SERIAL_MAX_UNIT_ID_LENGTH; i++)
            unitID[i] = bytesToUInt(payload[i]);
    }

    @Override
    @NonNull
    public String toString() {
        return KromekSerialUnitIDReport.class.getSimpleName() + " {" +
                "unitID=" + Arrays.toString(unitID) +
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
                .addField("unitID", sweFactory.createArray()
                        .label("Unit ID")
                        .description("Unit ID")
                        .definition(SWEHelper.getPropertyUri("unitID"))
                        .withFixedSize(KROMEK_SERIAL_MAX_UNIT_ID_LENGTH)
                        .withElement("unitID", sweFactory.createQuantity()
                                .label("Unit ID")
                                .description("Unit ID")
                                .definition(SWEHelper.getPropertyUri("unitID"))
                                .dataType(DataType.INT)))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        for (int i = 0; i < KROMEK_SERIAL_MAX_UNIT_ID_LENGTH; i++)
            dataBlock.setIntValue(++index, unitID[i]);
    }

    @Override
    void setReportInfo() {
        setReportName("KromekSerialUnitIDReport");
        setReportLabel("Unit ID");
        setReportDescription("Unit ID");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(1);
    }
}
