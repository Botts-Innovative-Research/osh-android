## Adding a new driver to android app

To add a new driver to the android appliation there are a couple of things to consider.
1. Is the driver an existing driver from osh-addons or another addon-like driver repo or is it android specific?
2. If it is android specific, is it a hardware driver or external device driver? 
   
   **Hardware drivers are drivers that are built into the android device and are accessed through the android API whereas 
external drivers can be things connected through the android device through Bluetooth, USB, WiFi or other means.**

---
### Adding an existing driver from osh-addons or another addon-like driver repo
1. Include the correct commit from osh-addons or the other repo in the submodules directory of the project.
2. Add the driver to the *settings.gradle* file in the root directory of the project.
```
'osh-addons': [
        'comm/sensorhub-comm-ble',
        'comm/sensorhub-comm-ble-dbus',
        'services/sensorhub-service-video',
        'sensors/video/sensorhub-driver-videocam',
        'sensors/positioning/sensorhub-driver-trupulse',
        'sensors/health/sensorhub-driver-angelsensor',
        'processing/sensorhub-process-vecmath',
        'processing/sensorhub-process-geoloc'
    ]
 ```
3. Add the driver to the *build.gradle* file in the sensorhub-android-lib directory.
   **This buildfile is already included in the app's buildfile, so it will get passed along to most
   of the other subprojects.**
```
dependencies {
  api project(':sensorhub-android-service')
  api project(':sensorhub-driver-trupulse')
//  api project(':sensorhub-driver-angelsensor')
  api project(':sensorhub-driver-android')
//  api project(':sensorhub-android-flirone')
  //api project(':sensorhub-android-dji')
//  api project(':sensorhub-storage-h2')

  implementation 'org.slf4j:slf4j-android:1.6.1-RC1'
  implementation('javax.xml.stream:stax-api:1.0-2')
}
```
4. Add the appropriate information to the *strings.xml* file in the *res/values* directory.
**This may require more values to be added depending on the complexity of the driver.**
```
<string name="sensor_trupulse">TruPulse Range Finder Sensor</string>
```
5. Add the sensor to the *pref_sensors.xml* file in the *res/xml* directory. This is how we add the UI option to enable 
the sensor.
```
<SwitchPreference
            android:defaultValue="false"
            android:key="trupulse_enabled"
            android:summary="Enable streaming of TruPulse range finder data (sensor must be connected via Bluetooth on startup)"
            android:title="TruPulse Range Finder Data" />
```
6. Import the sensor into *MainActivity.java* in the *sensorhub-android-app/src/org/sensorhub/android/* directory.
7. From there, add a check in the `updateConfig()` method to see if the sensor is enabled. If it is, add it to 
`sensorsConfig`. How this is done depends on the sensor. For example, the TruPulse sensor we've been following needs a couple of things
set in its config before it is added to the overall config list.