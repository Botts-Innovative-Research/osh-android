package org.sensorhub.android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/*
 * Fragment for sensor preferences
 */
public class SensorsFragment extends PreferenceFragmentCompat {

    private static final String[][] SWITCH_DEPENDENTS = {
        {"accel_enabled", "accel_options"},
        {"gyro_enabled", "gyro_options"},
        {"mag_enabled", "mag_options"},
        {"orient_quat_enabled", "orient_quat_options"},
        {"orient_euler_enabled","orient_euler_options"},
        {"gps_enabled", "gps_options"},
        {"netloc_enabled", "netloc_options"},
        {"cam_enabled", "cam_options", "video_codec", "video_framerate", "video_resolution", "camera_select"},
        {"video_roll_enabled", "video_roll_options"},
        {"audio_enabled", "audio_options", "audio_codec", "audio_samplerate", "audio_bitrate"},
        {"meshtastic_enabled", "meshtastic_device_address", "meshtastic_options"},
        {"polar_enabled", "polar_device_address", "polar_options"},
        {"kestrel_enabled", "kestrel_device_address", "kestrel_options"},
        {"trupulse_enabled", "trupulse_datasource", "trupulse_options", "trupulse_device_address", "trupulse_simu"},
        {"angel_enabled", "angel_address", "angel_options"},
        {"flirone_enabled", "flir_options"},
        {"ste_radpager_enabled","ste_radpager_options"},
        {"wardriving_enabled", "wardriving_options"},
        {"controller_enabled", "controller_options"},
        {"template_enabled", "template_device_address", "template_options"},

    };

    /** Keys of Preferences that use the Bluetooth device picker dialog */
    private static final String[] BT_DEVICE_PREF_KEYS = {
        "meshtastic_device_address",
        "polar_device_address",
        "kestrel_device_address",
        "trupulse_device_address",
        "template_device_address"
    };

    private ArrayList<String> frameRateList = new ArrayList<>();
    private ArrayList<String> resList = new ArrayList<>();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pref_sensors, rootKey);

        for (String[] group : SWITCH_DEPENDENTS) {
            String switchKey = group[0];
            SwitchPreferenceCompat switchPref = findPreference(switchKey);
            if (switchPref == null) continue;

            boolean isChecked = switchPref.isChecked();
            for (int i = 1; i < group.length; i++) {
                Preference dep = findPreference(group[i]);
                if (dep != null) dep.setVisible(isChecked);
            }

            switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                for (int i = 1; i < group.length; i++) {
                    Preference dep = findPreference(group[i]);
                    if (dep != null) dep.setVisible(enabled);
                }
                return true;
            });
        }

        setupVideoPreferences();
        setupAudioPreferences();

        setupBluetoothDevicePickers();
    }

    private void setupBluetoothDevicePickers() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        for (String key : BT_DEVICE_PREF_KEYS) {
            Preference pref = findPreference(key);
            if (pref == null) continue;

            String saved = prefs.getString(key, "");
            if (!saved.isEmpty()) {
                pref.setSummary(saved);
            }

            pref.setOnPreferenceClickListener(p -> {
                showDevicePickerDialog(key);
                return true;
            });
        }
    }

    private void showDevicePickerDialog(String prefKey) {
        List<String> names = new ArrayList<>();
        List<String> addresses = new ArrayList<>();

        // Gather all bonded Bluetooth devices (classic + BLE)
        BluetoothAdapter btAdapter = getBluetoothAdapter();
        if (btAdapter != null && btAdapter.isEnabled() && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                String name = device.getName();
                String mac = device.getAddress();
                names.add(name != null ? name + " (" + mac + ")" : mac);
                addresses.add(mac);
            }
        }

        names.add("Enter name or address manually...");
        addresses.add(null);

        String[] displayNames = names.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
            .setTitle("Select Device")
            .setItems(displayNames, (dialog, which) -> {
                if (addresses.get(which) == null) {
                    showManualAddressDialog(prefKey);
                } else {
                    saveDeviceAddress(prefKey, addresses.get(which), displayNames[which]);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showManualAddressDialog(String prefKey) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("e.g. Ballistic or AA:BB:CC:DD:EE:FF");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String current = prefs.getString(prefKey, "");
        if (!current.isEmpty()) {
            input.setText(current);
            input.selectAll();
        }

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
            .setTitle("Enter Device Name or Address")
            .setMessage("Enter a device name (e.g. \"Ballistic\") or MAC address. Names are matched from the start, case-insensitive.")
            .setView(container)
            .setPositiveButton("OK", (dialog, which) -> {
                String address = input.getText().toString().trim();
                if (!address.isEmpty()) {
                    saveDeviceAddress(prefKey, address, address);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveDeviceAddress(String prefKey, String address, String displayText) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putString(prefKey, address).apply();

        Preference pref = findPreference(prefKey);
        if (pref != null) {
            pref.setSummary(displayText);
        }
    }


    private void setupVideoPreferences() {
        // Camera selection
        ArrayList<String> cameras = new ArrayList<>();
        try {
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                cameras.add(Integer.toString(i));
            }
        } catch (Exception e) {
            cameras.add("0");
        }

        ListPreference cameraSelectList = findPreference("camera_select");
        if (cameraSelectList != null) {
            cameraSelectList.setEntries(cameras.toArray(new String[0]));
            cameraSelectList.setEntryValues(cameras.toArray(new String[0]));
            cameraSelectList.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d("CAMERA_SELECT", "New Camera Selected: " + newValue);
                updateCameraSettings(Integer.parseInt((String) newValue));
                return true;
            });
        }

        // Frame rates and resolutions from camera
        Camera camera = null;
        try {
            camera = Camera.open(0);
            Camera.Parameters camParams = camera.getParameters();
            for (int frameRate : camParams.getSupportedPreviewFrameRates())
                frameRateList.add(Integer.toString(frameRate));
            for (Camera.Size imgSize : camParams.getSupportedPreviewSizes())
                resList.add(imgSize.width + "x" + imgSize.height);
        } catch (Exception e) {
            frameRateList.add("30");
            resList.add("640x480");
        } finally {
            if (camera != null) camera.release();
        }

        ListPreference frameRatePrefList = findPreference("video_framerate");
        if (frameRatePrefList != null) {
            frameRatePrefList.setEntries(frameRateList.toArray(new String[0]));
            frameRatePrefList.setEntryValues(frameRateList.toArray(new String[0]));
        }

        // Resolution list
        ListPreference resolutionPrefList = findPreference("video_resolution");
        if (resolutionPrefList != null) {
            resolutionPrefList.setEntries(resList.toArray(new String[0]));
            resolutionPrefList.setEntryValues(resList.toArray(new String[0]));
            if (!resList.isEmpty() && resolutionPrefList.getValue() == null)
                resolutionPrefList.setValue(resList.get(0));
        }
    }

    private void updateCameraSettings(int cameraId) {
        Camera camera = null;
        try {
            frameRateList.clear();
            resList.clear();
            camera = Camera.open(cameraId);
            Camera.Parameters camParams = camera.getParameters();
            for (int frameRate : camParams.getSupportedPreviewFrameRates())
                frameRateList.add(Integer.toString(frameRate));
            for (Camera.Size imgSize : camParams.getSupportedPreviewSizes())
                resList.add(imgSize.width + "x" + imgSize.height);

            ListPreference frameRatePrefList = findPreference("video_framerate");
            if (frameRatePrefList != null) {
                frameRatePrefList.setEntries(frameRateList.toArray(new String[0]));
                frameRatePrefList.setEntryValues(frameRateList.toArray(new String[0]));
            }
            ListPreference resolutionPrefList = findPreference("video_resolution");
            if (resolutionPrefList != null) {
                resolutionPrefList.setEntries(resList.toArray(new String[0]));
                resolutionPrefList.setEntryValues(resList.toArray(new String[0]));
            }
        } catch (Exception e) {
            Log.e("SensorsFragment", "Error updating camera settings", e);
        } finally {
            if (camera != null) camera.release();
        }
    }


    private void setupAudioPreferences() {
        List<String> sampleRateList = Arrays.asList("8000", "11025", "22050", "44100", "48000");
        List<String> bitRateList = Arrays.asList("32", "64", "96", "128", "160", "192");

        ListPreference sampleRatePrefList = findPreference("audio_samplerate");
        if (sampleRatePrefList != null) {
            sampleRatePrefList.setEntries(sampleRateList.toArray(new String[0]));
            sampleRatePrefList.setEntryValues(sampleRateList.toArray(new String[0]));
        }

        ListPreference bitRatePrefList = findPreference("audio_bitrate");
        if (bitRatePrefList != null) {
            bitRatePrefList.setEntries(bitRateList.toArray(new String[0]));
            bitRatePrefList.setEntryValues(bitRateList.toArray(new String[0]));
        }
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager btManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return btManager != null ? btManager.getAdapter() : null;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }
}
