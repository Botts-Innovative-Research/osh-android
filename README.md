### OpenSensorHub for Android

This repo contains modules specific to Android, including:

- A demo Android app
- A SensorHub service usable by other Android apps
- Drivers for sensors accessible through the Android
  [Sensor API](http://developer.android.com/guide/topics/sensors/sensors_overview.html),
  [Camera API](http://developer.android.com/guide/topics/media/camera.html) and
  [Location API](http://developer.android.com/guide/topics/location/index.html)

The demo app allows a phone or tablet running Android 5.0 (Lollipop) or later to send sensor data to a remote OSH node using the SOS-T standard.

Supported sensors include the ones that are on-board the phone:
- GPS
- Raw IMU (Accelerometers, Gyroscopes, Magnetometers)
- Fused Orientation (Quaternions or Euler angles)
- Video Camera (MJPEG or H264 codec)

But also other sensors connected via USB, Bluetooth or Bluetooth Smart:
- FLIR One Thermal Camera (USB)
- Trupulse 360 Range Finder (Bluetooth)
- Angel Sensor Wrist Band (Bluetooth Smart)


### Build

Building the latest version of the App has been successfully tested with Gradle 4.7 and Android Studio 3.2.1.

You'll need to install an up-to-date Android SDK and set its path in the [local.properties](local.properties) file. Also make sure you pulled source code from the `osh-core` and `osh-addons` repositories, in the same location as this repo.

To build using command line gradle, `cd` in the `sensorhub-android-app` folder and just run `../gradlew build`. You'll find the resulting APK in `sensorhub-android-app/build/outputs/apk`

You can also install it on a USB connected device by running `../gradlew installDebug`.


### How to use the App

The App is designed to be connected to an OSH server (for instance, you can use the [osh-base](https://github.com/opensensorhub/osh-distros/tree/master/osh-base) distribution) and upload inertial, GPS and video data to it.

For the connection to be established, you'll have to configure the App with the correct endpoint of the server (see the [App Documentation](http://docs.opensensorhub.org/user/android-app/))

You'll also have to enable transactional operations on the server side so the phone can register itself and send its data. For this, use the following steps:

- Open the web admin interface at <http://your_domain:port/sensorhub/admin>
- Go to the "Services" tab on the left
- Select "SOS Service"
- Check "Enable Transactional"
- Click "Apply Changes" at the top right
- Optionally click the "Save" button to keep this configuration after restart


### Rapid Sensor Enrollment
The Sensor Enrollment process enables rapid deployment of modules, including sensors, services, and databases, to the OSH Admin panel. This is achieved using the QR code scanner in the OSH Android App and the Module API service.

    Requirements:
        - Running OSH Node
        - Android Device
        - QR Code containing Sensors Module Class ID


### Configuring the OSH Android App

    Android Setup
    1. Open the OSH Android App.
    2. Tap the three dots in the top right corner to open the menu.
    3. Navigate to Settings > Sensor Enrollment:
        - This page contains the URL fetch and post endpoints along with authentication credentials.
        - Tap the Get URL field and update the ip:port to the location where the sensor's configuration file will be fetched.
        - Tap the Post URL field and update the ip:port to where the updated configuration will be sent.
    4. Exit the Sensor Enrollment settings and return to the main Settings page.
    5. Return to the app's home screen.

    Enrolling a Sensor
    1. Tap the three dots in the top right corner and select Sensor Enrollment.
    2. The Sensor Enrollment page provides two options:
        - Scan a QR Code: Uses the back camera to scan a QR code, automatically initiating the fetch request.
        - Enter Module ID Manually: Allows manual entry of the Module Class ID, followed by a submission.
    3. If the fetch request is successful, a configuration form will appear.
    4. Modify any necessary settings for the sensor.
    5. Tap the Send button to post the updated configuration.


### Configuring the Admin Panel
Before building the project, ensure the required sensors are included in the settings.gradle and build.gradle files. 
        
    Building the Node
        - Open the project files in your directory
        - Locate and extract the build module
        - Click the launch script to start the node
    Accessing the Admin Panel
        - Open a supported browser and go to 'http://ip:port/sensorhub/admin/'
        - Type in the user and pw
        - Click on the 'Services tab'
        - Add the 'Module API' service
