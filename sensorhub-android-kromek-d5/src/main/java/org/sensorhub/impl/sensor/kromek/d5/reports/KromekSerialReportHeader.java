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

import static org.sensorhub.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_USE_COMPONENT;

public class KromekSerialReportHeader {
    /**
     * KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_x.
     */
    private final byte componentId;
    /**
     * KROMEK_SERIAL_REPORTS_IN_xxxx or KROMEK_SERIAL_REPORTS_OUT_xxxx.
     */
    private final byte reportId;
    public static final int size = KROMEK_SERIAL_REPORTS_USE_COMPONENT ? 2 : 1;

    public KromekSerialReportHeader(byte componentId, byte reportId) {
        this.componentId = componentId;
        this.reportId = reportId;
    }

    public byte[] encode() {
        if (KROMEK_SERIAL_REPORTS_USE_COMPONENT)
            return new byte[]{componentId, reportId};
        else
            return new byte[]{reportId};
    }
}