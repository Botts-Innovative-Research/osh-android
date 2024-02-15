### WearOS Driver
This Android OpenSensorHub driver receives data from the WearOS smartwatch and sends it to the OSH server. The driver is designed to be used with the OpenSensorHub app for WearOS devices. Please ensure that the phone and the WearOS device are paired, following the instructions provided by the manufacturer.

## Configuring a connection to OpenSensorHub
To configure a connection to an OpenSensorHub node, from the OpenSensorHub SmartHub main screen, tap the three dots in the top right and select Settings->General. Enter the address of your OpenSensorHub node in the SOS Endpoint URL field. This address typically ends with /sensorhub/sos. Enter the SOS Username and SOS Password required to sign in to the endpoint.

## Driver Configuration
To enable the driver, from the OpenSensorHub SmartHub main screen, tap the three dots in the top right and select Settings->Sensors. Find WearOS Date and toggle it on. You may also tap WearOS Output Options and enable or disable any data you wish to be sent from the WearOS device.

## Starting the WearOS driver
From the OpenSensorHub SmartHub main screen, tap the three dots in the top right and select Start SmartHub. You will see a list of enabled outputs and their status. If not data has yet been received, they will say NO OBS. Please ensure that the WearOS OpenSensorHub app is installed on the WearOS device and has been started. When the WearOS device is sleeping, it may be several minutes between between observations. Wake the device and start the OpenSensorHub app to confirm data is being sent.