/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;


/**
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class DeviceOutput extends AbstractSensorOutput<Kestrel>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    private static final String SENSOR_OUTPUT_NAME = "deviceInformation";
    private static final String SENSOR_OUTPUT_LABEL = "Device Info Output";
    private static final Logger logger = LoggerFactory.getLogger(DeviceOutput.class);

    protected DeviceOutput(Kestrel parent) {
        super(SENSOR_OUTPUT_NAME, parent);
    }
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .addField("samplingTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("batteryLevel", fac.createQuantity()
                        .label("Battery Level")
                        .uomCode("%")
                        .definition(SWEHelper.getPropertyUri("BatteryLevel"))
                        .build())
                .addField("firmwareVersion", fac.createText()
                        .label("Firmware Version")
                        .definition(SWEHelper.getPropertyUri("FirmwareVersion"))
                        .build())
                .addField("serialNumber", fac.createText()
                        .label("Serial Number")
                        .definition(SWEHelper.getPropertyUri("SerialNumber"))
                        .build())
                .addField("deviceSettings", fac.createText()
                        .label("Device Settings")
                        .definition(SWEHelper.getPropertyUri("DeviceSettings"))
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }

    public void setData(double batteryLevel, String firmware, String serialNumber, String deviceSettings) {
        DataBlock dataBlock = dataStruct.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setDoubleValue(1, batteryLevel);
        dataBlock.setStringValue(2, firmware);
        dataBlock.setStringValue(3, serialNumber);
        dataBlock.setStringValue(4, deviceSettings);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
