package org.sensorhub.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ServerProfileRepository {
    private static final String KEY_PROFILES_JSON = "server_profiles_json";
    private final Context context;
    private final SharedPreferences prefs;

    public ServerProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public List<ServerProfile> getAll() {
        List<ServerProfile> profiles = new ArrayList<>();
        String json = prefs.getString(KEY_PROFILES_JSON, null);
        if (json == null) return profiles;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                profiles.add(ServerProfile.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // corrupted data, return empty
        }
        return profiles;
    }

    public List<ServerProfile> getEnabled() {
        List<ServerProfile> enabled = new ArrayList<>();
        for (ServerProfile p : getAll()) {
            if (p.enabled) enabled.add(p);
        }
        return enabled;
    }

    public ServerProfile getById(String id) {
        for (ServerProfile p : getAll()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public void save(ServerProfile profile) {
        List<ServerProfile> all = getAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(profile.id)) {
                all.set(i, profile);
                found = true;
                break;
            }
        }
        if (!found) all.add(profile);
        persist(all);
    }

    public void delete(String id) {
        List<ServerProfile> all = getAll();
        all.removeIf(p -> p.id.equals(id));
        persist(all);
        SecurePrefs.removeByPrefix(context, "profile_" + id + "_");
    }

    public void setEnabled(String id, boolean enabled) {
        ServerProfile p = getById(id);
        if (p != null) {
            p.enabled = enabled;
            save(p);
        }
    }

    public String getPassword(String profileId) {
        return SecurePrefs.get(context, "profile_" + profileId + "_password", null);
    }

    public void setPassword(String profileId, String password) {
        SecurePrefs.put(context, "profile_" + profileId + "_password", password);
    }

    public String getOAuthTokenEndpoint(String profileId) {
        return SecurePrefs.get(context, "profile_" + profileId + "_oauth_token_endpoint", "");
    }

    public void setOAuthTokenEndpoint(String profileId, String value) {
        SecurePrefs.put(context, "profile_" + profileId + "_oauth_token_endpoint", value);
    }

    public String getOAuthClientId(String profileId) {
        return SecurePrefs.get(context, "profile_" + profileId + "_oauth_client_id", "");
    }

    public void setOAuthClientId(String profileId, String value) {
        SecurePrefs.put(context, "profile_" + profileId + "_oauth_client_id", value);
    }

    public String getOAuthClientSecret(String profileId) {
        return SecurePrefs.get(context, "profile_" + profileId + "_oauth_client_secret", "");
    }

    public void setOAuthClientSecret(String profileId, String value) {
        SecurePrefs.put(context, "profile_" + profileId + "_oauth_client_secret", value);
    }

    private void persist(List<ServerProfile> profiles) {
        JSONArray arr = new JSONArray();
        for (ServerProfile p : profiles) {
            try {
                arr.put(p.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_PROFILES_JSON, arr.toString()).apply();
    }
}
