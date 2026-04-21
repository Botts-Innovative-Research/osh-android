package org.sensorhub.android;

import static android.content.Context.WIFI_SERVICE;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


/*
 * Fragment for settings preferences
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey);


        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = "Unable to get IP Address";
        }

        Preference ipAddressLabel = getPreferenceScreen().findPreference("nop_ipAddress");
        ipAddressLabel.setSummary(ipAddressString);

        manageServerProfiles();
        setupDiscoveryToggle();

    }

    @Override
    public void onResume() {
        super.onResume();
        Preference serverPref = findPreference("manage_servers");
        if (serverPref != null) updateServerProfilesSummary(serverPref);
    }

    private void manageServerProfiles() {
        Preference serverPref = findPreference("manage_servers");

        if (serverPref != null) {
            updateServerProfilesSummary(serverPref);
            serverPref.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(requireContext(), ServerProfilesActivity.class));
                return true;
            });
        }
    }


    private void updateServerProfilesSummary(Preference pref) {
        ServerProfileRepository repo = new ServerProfileRepository(requireContext());
        int total = repo.getAll().size();
        int enabled = repo.getEnabled().size();
        if (total == 0) {
            pref.setSummary("No server profiles configured");
        } else {
            pref.setSummary(enabled + " of " + total + " server(s) enabled");
        }
    }

    private void setupDiscoveryToggle() {
        SwitchPreferenceCompat enableDiscovery = findPreference("discovery_service");

        Preference rules = findPreference("rules_link");

        if (enableDiscovery != null) {
            enableDiscovery.setOnPreferenceChangeListener((pref, value) -> {
                boolean isEnabled = (Boolean) value;
                setVisibility(isEnabled, rules);
                return true;
            });
        }
    }

    private void setVisibility(boolean visible, Preference... prefs) {
        for (Preference p : prefs) {
            if (p != null) p.setVisible(visible);
        }
    }
}
