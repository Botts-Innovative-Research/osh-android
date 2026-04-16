package org.sensorhub.android;

import static android.content.Context.WIFI_SERVICE;

import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONException;
import org.json.JSONObject;

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


        setupSecurePreference("password", "•••••••");
        setupSecurePreference("client_secret", "••••••••");
        setupSecurePreference("client_id", null);
        setupSecurePreference("token_endpoint", null);

        setupSavedServers();
        setupOAuthToggle();
    }

    private void setupSecurePreference(String key, String maskedSummary) {
        EditTextPreference pref = findPreference(key);
        if (pref == null) return;

        pref.setPersistent(false);

        String value = SecurePrefs.get(requireContext(), key, "");
        pref.setText(value);

        if (maskedSummary != null) {
            pref.setSummaryProvider(p -> {
                String v = SecurePrefs.get(requireContext(), key, "");
                return (v != null && !v.isEmpty()) ? maskedSummary : "Not set";
            });
        }

        pref.setOnPreferenceChangeListener((p, newValue) -> {
            SecurePrefs.put(requireContext(), key, (String) newValue);
            pref.setText((String) newValue);
            return false;
        });
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
        try {
            JSONObject obj = new JSONObject(entry);
            String name = obj.optString("name");
            String ip = obj.optString("ip");
            String port = obj.optString("port");
            return name + " (" + ip + ":" + port + ")";
        } catch (JSONException e) {
            return entry;
        }
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
        String username = prefs.getString("username", "").trim();
        String password = SecurePrefs.get(requireContext(), "password", "").trim();
        String endpoint = prefs.getString("endpoint_path", "").trim();

        if (ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Server address and port are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            name = ip + ":" + port;
        }

        String serverKey = ip + ":" + port + endpoint;
        JSONObject obj = new JSONObject();
        try {
            obj.put("ip", ip);
            obj.put("port", port);
            obj.put("name", name);
            obj.put("username", username);
            obj.put("endpoint", endpoint);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (!password.isEmpty()) {
            SecurePrefs.put(requireContext(), "server_pwd_" + serverKey, password);
        }

        Set<String> servers = new HashSet<>(getSavedServers());

        servers.removeIf(s -> {
            try {
                JSONObject existing = new JSONObject(s);
                return existing.optString("ip").equals(ip) &&
                        existing.optString("port").equals(port) &&
                        existing.optString("endpoint").equals(endpoint);
            } catch (JSONException e) {
                return false;
            }
        });

        servers.add(obj.toString());
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
                try {
                    JSONObject obj = new JSONObject(servers.get(which));

                    EditTextPreference ipPref = findPreference("ip_address");
                    EditTextPreference portPref = findPreference("port");
                    EditTextPreference namePref = findPreference("server_name");
                    EditTextPreference usernamePref = findPreference("username");
                    EditTextPreference passwordPref = findPreference("password");
                    EditTextPreference endpointPref = findPreference("endpoint_path");

                    if (ipPref != null) ipPref.setText(obj.optString("ip"));
                    if (portPref != null) portPref.setText(obj.optString("port"));
                    if (namePref != null) namePref.setText(obj.optString("name"));
                    if (usernamePref != null) usernamePref.setText(obj.optString("username"));
                    if (endpointPref != null) endpointPref.setText(obj.optString("endpoint"));

                    String serverKey = obj.optString("ip") + ":" + obj.optString("port") + obj.optString("endpoint");
                    String savedPwd = SecurePrefs.get(requireContext(), "server_pwd_" + serverKey, "");
                    if (passwordPref != null) {
                        passwordPref.setText(savedPwd);
                        SecurePrefs.put(requireContext(), "password", savedPwd);
                    }

                    Toast.makeText(requireContext(), "Loaded: " + obj.optString("name"), Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    Toast.makeText(requireContext(), "Failed to load server", Toast.LENGTH_SHORT).show();
                }
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
                    if (!checked[i]) {
                        remaining.add(servers.get(i));
                    } else {
                        try {
                            JSONObject obj = new JSONObject(servers.get(i));
                            String serverKey = obj.optString("ip") + ":" + obj.optString("port") + obj.optString("endpoint");
                            SecurePrefs.remove(requireContext(), "server_pwd_" + serverKey);
                        } catch (JSONException ignored) {}
                    }
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
