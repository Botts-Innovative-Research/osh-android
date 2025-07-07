# OpenSensorHub for Android

This repository includes Android-Specific modules that enable sensor data collection and streaming from Android devices using OpenSensorHub(OSH).

- A demo Android app
- A SensorHub service usable by other Android apps
- Drivers for sensors accessible through the Android
  [Sensor API](http://developer.android.com/guide/topics/sensors/sensors_overview.html),
  [Camera API](http://developer.android.com/guide/topics/media/camera.html) and
  [Location API](http://developer.android.com/guide/topics/location/index.html)


## Features
The demo app allows a phone or tablet running Android 5.0 (Lollipop) or later to send sensor data to a remote OSH node using the SOS-T standard and the Connected Systems standard.

Supported sensors include the ones that are on-board the phone:
- GPS
- Raw IMU (Accelerometers, Gyroscopes, Magnetometers)
- Fused Orientation (Quaternions or Euler angles)
- Video Camera (MJPEG or H264 codec)

But also other sensors connected via USB, Bluetooth or Bluetooth Smart:
- FLIR One Thermal Camera (USB)
- Trupulse 360 Range Finder (Bluetooth)
- Angel Sensor Wrist Band (Bluetooth Smart)


## Build

Building has been tested with Gradle 7.2 and Android Studio 3.2.1.

### Prerequisites
- Android SDK and set its path in the [local.properties](local.properties)
- source code from the `osh-core` and `osh-addons` repositories

### Using Command Line

```bash
cd sensorhub-android-app
../gradlew build
```

The APK will be located in: `sensorhub-android-app/build/outputs/apk/debug`

To install it on a connected device:
```bash
../gradlew installDebug
```

### Using Android Studio
- Open Android Studio > **File** > **Open**, and select `osh-android` project
- Click **Build** > **Build Bundle(s)/APK(s)** > **Build APK(s)**
- Navigate to: `sensorhub-android-app/build/outputs/apk/debug`
  To build using Android Studio, open Android Studio and click File > Open and navigate to the directory of your `osh-android` project, click the `Build` in the toolbar on the top of the IDE and then click `Build Bundle(s) / APK(s)` then `Build APK(s)`, open the File Explorer, and navigate to the project `osh-android` and navigate to `sensorhub-android-app/build/outputs/apk/debug`


## Connecting to OSH Server

The App is designed to be connected to an OSH server (for instance, you can use the [osh-base](https://github.com/opensensorhub/osh-distros/tree/master/osh-base) distribution) and upload inertial, GPS and video data to it.

For the connection to be established, you'll have to configure the App with the correct endpoint of the server (see the [App Documentation](https://docs.opensensorhub.org/docs/osh-node/android/install))

You'll also have to enable transactional operations on the server side so the phone can register itself and send its data. For this, use the following steps:

- Open the web admin interface at <http://your_domain:port/sensorhub/admin>
- Go to the **Services** tab on the left
- Select **SOS Service**
- Check **Enable Transactional**
- Click **Apply Changes** at the top right
- Optionally click the **Save** button to keep this configuration after restart


## App Configuration

General Settings
- Click the three dots to open a menu
- Select **Settings** > **General** and enter the following details:
    - `IP Address`, `Port`, and optional `User` and `Password`
- Enable services:
    - **ConSysApi Service**
    - **SOS Service**
- Select client
    - `SOS-T Client` or `Connected Systems Client`

Sensors
- Swipe right or go back to the main Settings menu
- Click on the **Sensors** tab to enable/disable the sensors

> **Note**: You must enable **Network Location Data**
- If using video or audio, configure:
    - **Video Settings**
    - **Audio Settings**


### Streaming Video from Android Device
To stream video data from the Android Device:

**Enable the Sensor:**
- In **Sensors**, toggle **Video Data** to ON.
- Tap the **Video Output Options** and enable **Push Remote**

**Configure Video Settings:**
- **Video Codec**: Select the preferred codec (e.g., JPEG)
- **Frame Rate**: Choose desired FPS (e.g., 30)
- **Selected Preset**: Choose a video preset (see below)
- **Selected Camera**: Choose camera index (e.g., 0 = back, 1 = front)

**Creating a Video Preset:**
- Choose a **Video Preset #1 - #5**
- Set:
    - **Frame Size** (e.g., 1920x1080)
    - **Min BitRate** (e.g., 3000)
    - **Max BitRate** (e.g., 3000)
- Assign the preset to **Selected Preset**
- Test with "Camera Index" (values may vary from device)

### Streaming Audio from Android Device
todo