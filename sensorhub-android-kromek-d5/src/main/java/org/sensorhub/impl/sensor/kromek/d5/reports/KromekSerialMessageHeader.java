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

public class KromekSerialMessageHeader {
    public int length;
    public byte mode = Constants.KROMEK_SERIAL_MESSAGE_MODE;
    public static final int size = 3; // uint16_t length + uint8_t mode

    /**
     * Create a new message header with the given length and mode.
     *
     * @param length Total length including KROMEK_SERIAL_MESSAGE_OVERHEAD
     */
    public KromekSerialMessageHeader(int length) {
        this.length = length;
    }

    public byte[] encode() {
        byte[] header = new byte[3];
        header[0] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[2] = mode;
        return header;
    }
}
