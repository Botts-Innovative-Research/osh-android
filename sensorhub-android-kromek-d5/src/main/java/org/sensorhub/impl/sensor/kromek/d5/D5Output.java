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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

/**
 * Output for the Kromek D5 sensor
 *
 * @author Michael Elmore
 * @since Nov 2013
 */
public class D5Output extends AbstractSensorOutput<D5Sensor> {
    private static final Logger logger = LoggerFactory.getLogger(D5Output.class);
    DataComponent dataComponent;
    DataEncoding dataEncoding;
    int count = 0;

    protected D5Output(D5Sensor parent) {
        super("Kromek D5 Data", parent);

        logger.info("Creating D5 Output");
    }

    public void doInit() {
        logger.info("Initializing D5 Output");
        SWEHelper fac = new SWEHelper();

        dataComponent = fac.createRecord()
                .name("Test")
                .label("Test")
                .addField("time", fac.createTime().asSamplingTimeIsoUTC()
                        .label("Time Stamp"))
                .addField("Count", fac.createQuantity()
                        .label("Count")
                        .description("Just a count field, for testing."))
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataComponent;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 1;
    }

    public void setData() {
        logger.info("Setting D5 Output Data");
        DataBlock dataBlock = dataComponent.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setDoubleValue(1, count++);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
        logger.info("D5 Output Data Set");
    }
}
