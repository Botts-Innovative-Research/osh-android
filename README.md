# OpenSensorHub for Android

An Android application that collects sensor data from phones and tablets and streams it to a remote [OpenSensorHub](https://opensensorhub.org/) (OSH) server using OGC standards (SOS-T and Connected Systems API).

## Supported Sensors

**On-Device:**
- GPS location
- IMU (accelerometers, gyroscopes, magnetometers)
- Fused orientation (quaternions or Euler angles)
- Video camera (H.264, H.265, VP8, VP9, MJPEG)
- Audio microphone (AAC, OPUS)

**External (USB / Bluetooth / BLE):**
- FLIR One thermal camera (USB)
- Trupulse 360 range finder (Bluetooth)
- Angel Sensor wrist band (BLE)
- Kestrel weather meter (BLE)
- Polar H9/H10 heart rate monitor (Bluetooth)
- Meshtastic mesh radio (Bluetooth)
- STE radiation pager
- BLE beacons

## Requirements

- **Android device** running Android 14 (API 34) or later
- **JDK 17**
- **Android SDK** with Build Tools 30.0.2 and API level 33 installed
- **Android Studio** (recommended) or Gradle 7.4+ for command-line builds
- **Git** (with submodule support)

## Setup

### 1. Clone the Repository

```bash
git clone --recursive https://github.com/botts-innovative-research/osh-android.git
cd osh-android
```

If you already cloned without `--recursive`, initialize the submodules separately:

```bash
git submodule update --init --recursive
```

This pulls in two required submodule dependencies:
- **osh-core** -- the core OpenSensorHub framework
- **osh-addons** -- additional sensor drivers and processing modules

### 2. Configure the Android SDK Path

Create or verify the `local.properties` file in the project root:

```properties
sdk.dir=/path/to/your/Android/sdk
```

On macOS this is typically `~/Library/Android/sdk`. On Linux it is often `~/Android/Sdk`.

### 3. Build the App

#### Option A: Android Studio (Recommended)

1. Open Android Studio
2. **File > Open** and select the `osh-android` project directory
3. Wait for Gradle sync to complete
4. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. The APK is output to `sensorhub-android-app/build/outputs/apk/debug/`

#### Option B: Command Line

```bash
./gradlew build -x test -x javadoc -x lintDebug
```

The APK is output to `sensorhub-android-app/build/outputs/apk/debug/`.

### 4. Install on a Device

Connect an Android device via USB with **USB debugging** enabled (Settings > Developer Options > USB Debugging), then run:

```bash
./gradlew installDebug
```

Or from Android Studio, click **Run > Run 'app'** with the device selected.

## App Configuration

### Connect to an OSH Server

1. Tap the three-dot menu in the app
2. Select **Settings > General**
3. Enter the server connection details:
   - **IP Address** and **Port**
   - **User** and **Password** (if required)
4. Enable at least one service:
   - **ConSysApi Service** (Connected Systems API)
   - **SOS Service** (SOS-T protocol)
5. Select a client type:
   - **Connected Systems Client** or **SOS-T Client**

### Enable Sensors

1. From the Settings screen, tap the **Sensors** tab
2. Toggle on the sensors you want to stream (GPS, IMU, Orientation, Video, Audio, etc.)
3. For each enabled sensor, tap its options to configure **Push Remote** if you want data sent to the server

### Configure Video Streaming

1. In **Sensors**, enable **Video Data** and turn on **Push Remote** in its options
2. Go to **Video Settings** and configure:
   - **Video Codec** (e.g., JPEG, H.264)
   - **Frame Rate** (e.g., 30 FPS)
   - **Selected Camera** (0 = back, 1 = front)
3. Set up a video preset (Presets 1--5):
   - **Frame Size** (e.g., 1920x1080)
   - **Min / Max BitRate** (e.g., 3000 kbps)
4. Assign the preset under **Selected Preset**

### Configure Audio Streaming

1. In **Sensors**, enable the audio sensor and turn on **Push Remote**
2. Go to **Audio Settings** and configure:
   - **Audio Codec** (e.g., AAC, OPUS)
   - **Sample Rate** (e.g., 8000)
   - **BitRate** (64 kbps)

### Start Streaming

1. Return to the main screen
2. Tap the three-dot menu
3. Select **Start SmartHub**

The app will register with the OSH server and begin streaming data from all enabled sensors.

## OSH Server Setup

The app sends data to an OSH server. You can use the [osh-node-dev-template](https://github.com/opensensorhub/osh-node-dev-template.git) to get started.

To accept data from the Android app, setup the correct services on the server:

1. Open the admin interface at `http://<server-ip>:<port>/sensorhub/admin`
2. Go to the **Services** tab
3. Select **Connected Systems API Service**
4. Click **Lookup Module** to add a Database ID
5. Select the **Database** from the list 
6. Click **Apply Changes**
7. Optionally click **Save** to persist the configuration across restarts

For full server documentation, see the [OSH Node Android docs](https://docs.opensensorhub.org/docs/osh-node/android/install).

## Project Structure

```
osh-android/
  sensorhub-android-app/       # Main demo application (APK)
  sensorhub-android-service/   # Background SensorHub service library
  sensorhub-android-lib/       # Dependency aggregation library
  sensorhub-driver-android/    # Core Android sensor & camera driver
  sensorhub-android-polar/     # Polar heart rate monitor driver
  sensorhub-android-meshtastic/# Meshtastic mesh radio driver
  sensorhub-android-ste/       # STE radiation pager driver
  sensorhub-android-flirone/   # FLIR One thermal camera driver
  sensorhub-android-blebeacon/ # BLE beacon detector driver
  submodules/
    osh-core/                   # Core OSH framework
    osh-addons/                 # Additional drivers & processing
```

## Troubleshooting

- **Gradle sync fails**: Verify that submodules are initialized (`git submodule update --init --recursive`) and that `local.properties` points to a valid Android SDK.
- **Build errors with JDK**: Ensure JDK 17 is installed and selected in Android Studio (File > Settings > Build > Gradle > Gradle JDK).
- **App crashes on launch**: Confirm the device runs Android 14 (API 34) or later.
- **No data reaching server**: Check that the server IP/port are correct, and the device has network connectivity.
- **Bluetooth sensors not connecting**: Pair the external sensor in Android system Bluetooth settings before enabling it in the app.
