package org.sensorhub.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

public class ServerProfile {
    public String id;
    public String name;
    public String host;
    public int port;
    public String endpointPath;
    public String username;
    public boolean enableTls;
    public boolean disableSslCheck;
    public boolean useConSysClient;
    public boolean oAuthEnabled;
    public boolean enabled;
    public String password;
    public String clientId;
    public String clientSecret;
    public String tokenEndpoint;

    public ServerProfile() {
        this.id = UUID.randomUUID().toString();
        this.name = "Local Server";
        this.host = "127.0.0.1";
        this.port = 8080;
        this.endpointPath = "/sensorhub/api";
        this.username = "";
        this.enableTls = false;
        this.disableSslCheck = false;
        this.useConSysClient = true;
        this.oAuthEnabled = false;
        this.enabled = true;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("host", host);
        obj.put("port", port);
        obj.put("endpointPath", endpointPath);
        obj.put("username", username);
        obj.put("enableTls", enableTls);
        obj.put("disableSslCheck", disableSslCheck);
        obj.put("useConSysClient", useConSysClient);
        obj.put("oAuthEnabled", oAuthEnabled);
        obj.put("enabled", enabled);
        return obj;
    }

    public static ServerProfile fromJson(JSONObject obj) throws JSONException {
        ServerProfile p = new ServerProfile();
        p.id = obj.getString("id");
        p.name = obj.optString("name", "");
        p.host = obj.optString("host", "127.0.0.1");
        p.port = obj.optInt("port", 8080);
        p.endpointPath = obj.optString("endpointPath", "/sensorhub/api");
        p.username = obj.optString("username", "");
        p.enableTls = obj.optBoolean("enableTls", false);
        p.disableSslCheck = obj.optBoolean("disableSslCheck", false);
        p.useConSysClient = obj.optBoolean("useConSysClient", true);
        p.oAuthEnabled = obj.optBoolean("oAuthEnabled", false);
        p.enabled = obj.optBoolean("enabled", true);
        return p;
    }

    public URL buildClientUrl() {
        String cleanHost = host.replace("http://", "").replace("https://", "").trim();
        if (cleanHost.isEmpty())
            cleanHost = "127.0.0.1";

        String path = endpointPath != null ? endpointPath.trim() : "";
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }


        String urlStr = (enableTls ? "https://" : "http://") + cleanHost + ":" + port + path;
        try {
            return new URI(urlStr).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            return null;
        }
    }

    public String getDisplaySummary() {
        return host + ":" + port + (endpointPath != null ? endpointPath : "");
    }

    public String getClientModeLabel() {
        return useConSysClient ? "Connected Systems" : "SOS-T";
    }
}
