package org.sensorhub.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.sensorhub.android.server.ServerProfileRepository;
import org.sensorhub.android.server.ServerProfilesActivity;


/*
 * Fragment for settings preferences
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey);

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
        ServerProfileRepository repo = ServerProfileRepository.getInstance(requireContext());
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
            boolean isDiscovery = enableDiscovery.isChecked();
            setVisibility(isDiscovery, rules);

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
