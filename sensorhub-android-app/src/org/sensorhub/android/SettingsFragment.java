package org.sensorhub.android;

import static android.content.Context.WIFI_SERVICE;

import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.ByteOrder;


/*
 * Fragment for settings preferences
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String PREF_SAVED_SERVERS = "saved_servers_set";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey);


        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
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
        setupSavedServers();
        setupOAuthToggle();
    }

    // ==================== Saved Servers ====================

    private void setupSavedServers() {
        Preference selectPref = findPreference("saved_servers");
        Preference savePref = findPreference("save_current_server");
        Preference removePref = findPreference("remove_saved_server");

        if (selectPref != null) {
            updateSavedServersSummary(selectPref);
            selectPref.setOnPreferenceClickListener(p -> {
                showSelectServerDialog();
                return true;
            });
        }

        if (savePref != null) {
            savePref.setOnPreferenceClickListener(p -> {
                saveCurrentServer();
                return true;
            });
        }

        if (removePref != null) {
            removePref.setOnPreferenceClickListener(p -> {
                showRemoveServerDialog();
                return true;
            });
        }
    }

    private List<String> getSavedServers() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> serverSet = prefs.getStringSet(PREF_SAVED_SERVERS, new HashSet<>());
        return new ArrayList<>(serverSet);
    }

    private void putSavedServers(Set<String> servers) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putStringSet(PREF_SAVED_SERVERS, new HashSet<>(servers)).apply();
    }

    private String getDisplayName(String entry) {
        // entry format: "ip|port|name"
        String[] parts = entry.split("\\|", 3);
        if (parts.length >= 3) return parts[2] + " (" + parts[0] + ":" + parts[1] + ")";
        return entry;
    }

    private void updateSavedServersSummary(Preference pref) {
        List<String> servers = getSavedServers();
        if (servers.isEmpty()) {
            pref.setSummary("No saved servers");
        } else {
            pref.setSummary(servers.size() + " saved server(s)");
        }
    }

    private void saveCurrentServer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String name = prefs.getString("server_name", "").trim();
        String ip = prefs.getString("ip_address", "").trim();
        String port = prefs.getString("port", "").trim();

        if (ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Server address and port are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            name = ip + ":" + port;
        }

        String entry = ip + "|" + port + "|" + name;

        Set<String> servers = new HashSet<>(getSavedServers());

        // Check for duplicate ip:port
        String finalIp = ip;
        String finalPort = port;
        servers.removeIf(s -> {
            String[] parts = s.split("\\|", 3);
            return parts.length >= 2 && parts[0].equals(finalIp) && parts[1].equals(finalPort);
        });

        servers.add(entry);
        putSavedServers(servers);

        Preference selectPref = findPreference("saved_servers");
        if (selectPref != null) updateSavedServersSummary(selectPref);

        Toast.makeText(requireContext(), "Server saved: " + name, Toast.LENGTH_SHORT).show();
    }

    private void showSelectServerDialog() {
        List<String> servers = getSavedServers();
        if (servers.isEmpty()) {
            Toast.makeText(requireContext(), "No saved servers", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] displayNames = new String[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
            displayNames[i] = getDisplayName(servers.get(i));
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("Select Server")
            .setItems(displayNames, (dialog, which) -> {
                String[] parts = servers.get(which).split("\\|", 3);
                if (parts.length < 3) return;

                EditTextPreference ipPref = findPreference("ip_address");
                EditTextPreference portPref = findPreference("port");
                EditTextPreference namePref = findPreference("server_name");

                if (ipPref != null) ipPref.setText(parts[0]);
                if (portPref != null) portPref.setText(parts[1]);
                if (namePref != null) namePref.setText(parts[2]);

                Toast.makeText(requireContext(), "Loaded: " + parts[2], Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showRemoveServerDialog() {
        List<String> servers = getSavedServers();
        if (servers.isEmpty()) {
            Toast.makeText(requireContext(), "No saved servers to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] displayNames = new String[servers.size()];
        boolean[] checked = new boolean[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
            displayNames[i] = getDisplayName(servers.get(i));
            checked[i] = false;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Saved Servers")
            .setMultiChoiceItems(displayNames, checked, (dialog, which, isChecked) ->
                checked[which] = isChecked
            )
            .setPositiveButton("Remove", (dialog, which) -> {
                Set<String> remaining = new HashSet<>();
                for (int i = 0; i < servers.size(); i++) {
                    if (!checked[i]) remaining.add(servers.get(i));
                }
                putSavedServers(remaining);

                Preference selectPref = findPreference("saved_servers");
                if (selectPref != null) updateSavedServersSummary(selectPref);

                int removed = servers.size() - remaining.size();
                Toast.makeText(requireContext(), removed + " server(s) removed", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ==================== Client Mode & OAuth ====================

    private void setupOAuthToggle() {
        SwitchPreferenceCompat clientMode = findPreference("enable_client");
        SwitchPreferenceCompat oauth = findPreference("o_auth_enabled");

        Preference token = findPreference("token_endpoint");
        Preference clientId = findPreference("client_id");
        Preference secret = findPreference("client_secret");

        if (clientMode != null) {
            boolean isConSys = clientMode.isChecked();
            setVisibility(isConSys, oauth);
            setVisibility(isConSys && oauth != null && oauth.isChecked(), token, clientId, secret);

            clientMode.setOnPreferenceChangeListener((pref, value) -> {
                boolean enabled = (Boolean) value;
                setVisibility(enabled, oauth);
                setVisibility(enabled && oauth != null && oauth.isChecked(), token, clientId, secret);
                return true;
            });
        }

        if (oauth != null) {
            oauth.setOnPreferenceChangeListener((pref, value) -> {
                boolean isEnabled = (Boolean) value;
                setVisibility(isEnabled, token, clientId, secret);
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
