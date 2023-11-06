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

package org.sensorhub.impl.sensor.kromek.d5;

import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main driver class for the Kromek D5 sensor. This class is responsible for initializing the
 * sensor, starting and stopping the sensor, and managing the outputs.
 *
 * @author Michael Elmore
 * @since Nov 2013
 */
public class D5Sensor extends AbstractSensorModule<D5Config> {
    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);
    D5Output output;

    public D5Sensor() {
        logger.info("Creating D5 Sensor");
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void doInit() {
        logger.info("Initializing D5 Sensor");

        // Generate identifiers
        generateUniqueID("KROMEK_D5", config.serialNumber);
        generateXmlID("urn:android:kromek:d5:", config.serialNumber);

        // Create outputs
        output = new D5Output(this);
        addOutput(output, false);
        output.doInit();
    }

    @Override
    public void doStart() {
        logger.info("Starting D5 Sensor");

        output.setData();
    }

    @Override
    public void doStop() {
        logger.info("Stopping D5 Sensor");
    }
}
