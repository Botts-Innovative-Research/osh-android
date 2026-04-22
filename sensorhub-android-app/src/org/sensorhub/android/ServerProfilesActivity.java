package org.sensorhub.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class ServerProfilesActivity extends AppCompatActivity implements ServerAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ServerAdapter adapter;
    private final List<ServerProfile> servers = new ArrayList<>();
    private ServerProfileRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_profiles);

        MaterialToolbar toolbar = findViewById(R.id.server_profiles_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        repo = new ServerProfileRepository(this);

        recyclerView = findViewById(R.id.server_list);
        emptyText = findViewById(R.id.empty_text);
        FloatingActionButton fab = findViewById(R.id.fab_add_server);

        adapter = new ServerAdapter(servers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> showServerDialog(null));

        refreshList();
    }

    @Override
    public void onEditClicked(ServerProfile profile) {
        showServerDialog(profile);
    }

    @Override
    public void onEnabledToggled(ServerProfile profile, boolean enabled) {
        repo.setEnabled(profile.id, enabled);
    }

    @Override
    public void onDeleteRequested(ServerProfile profile) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Server")
                .setMessage("Remove \"" + profile.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    repo.delete(profile.id);
                    refreshList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showServerDialog(ServerProfile existing) {
        boolean isEdit = existing != null;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_server_profile, null);

        EditText nameInput = dialogView.findViewById(R.id.edit_name);
        EditText hostInput = dialogView.findViewById(R.id.edit_host);
        EditText portInput = dialogView.findViewById(R.id.edit_port);
        EditText endpointInput = dialogView.findViewById(R.id.edit_endpoint);
        EditText usernameInput = dialogView.findViewById(R.id.edit_username);
        EditText passwordInput = dialogView.findViewById(R.id.edit_password);

        EditText tokenInput = dialogView.findViewById(R.id.edit_token_endpoint);
        EditText clientIdInput = dialogView.findViewById(R.id.edit_client_id);
        EditText clientSecretInput = dialogView.findViewById(R.id.edit_client_secret);

        MaterialSwitch tlsSwitch = dialogView.findViewById(R.id.switch_tls);
        MaterialSwitch sslSwitch = dialogView.findViewById(R.id.switch_disable_ssl);
        MaterialSwitch oauthSwitch = dialogView.findViewById(R.id.switch_oauth);
        MaterialSwitch clientModeSwitch = dialogView.findViewById(R.id.switch_client_mode);

        View oauthFields = dialogView.findViewById(R.id.oauth_fields);

        Runnable updateOAuthVisibility = () -> {
            boolean show = clientModeSwitch.isChecked() && oauthSwitch.isChecked();
            oauthFields.setVisibility(show ? View.VISIBLE : View.GONE);
        };

        clientModeSwitch.setOnCheckedChangeListener((btn, checked) -> {
            oauthSwitch.setVisibility(checked ? View.VISIBLE : View.GONE);
            updateOAuthVisibility.run();
        });
        oauthSwitch.setOnCheckedChangeListener((btn, checked) ->
                updateOAuthVisibility.run());

        if (isEdit) {
            nameInput.setText(existing.name);
            hostInput.setText(existing.host);
            portInput.setText(String.valueOf(existing.port));
            endpointInput.setText(existing.endpointPath);
            usernameInput.setText(existing.username);
            tlsSwitch.setChecked(existing.enableTls);
            sslSwitch.setChecked(existing.disableSslCheck);
            clientModeSwitch.setChecked(existing.useConSysClient);
            oauthSwitch.setChecked(existing.oAuthEnabled);

            passwordInput.setText(repo.getPassword(existing.id));
            tokenInput.setText(repo.getOAuthTokenEndpoint(existing.id));
            clientIdInput.setText(repo.getOAuthClientId(existing.id));
            clientSecretInput.setText(repo.getOAuthClientSecret(existing.id));
        }

        oauthSwitch.setVisibility(clientModeSwitch.isChecked() ? View.VISIBLE : View.GONE);
        updateOAuthVisibility.run();

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Edit Server" : "Add Server")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String host = hostInput.getText().toString().trim();
            String portStr = portInput.getText().toString().trim();
            String endpoint = endpointInput.getText().toString().trim();

            if (name.isEmpty() || host.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this, "Name, host, and port are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (host.contains(" ") || host.contains("://")) {
                Toast.makeText(this, "Host should not include a protocol (e.g. http://)", Toast.LENGTH_SHORT).show();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Port should be a number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (port < 1 || port > 65535) {
                Toast.makeText(this, "Port should be between 1 and 65535", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!endpoint.isEmpty() && !endpoint.startsWith("/")) {
               endpoint = "/" + endpoint;
            }
            

            ServerProfile profile = isEdit ? existing : new ServerProfile();
            profile.name = name;
            profile.host = host;
            profile.port = port;
            profile.endpointPath = endpoint;
            profile.username = usernameInput.getText().toString().trim();
            profile.enableTls = tlsSwitch.isChecked();
            profile.disableSslCheck = sslSwitch.isChecked();
            profile.useConSysClient = clientModeSwitch.isChecked();
            profile.oAuthEnabled = oauthSwitch.isChecked();

            repo.save(profile);

            repo.setPassword(profile.id, passwordInput.getText().toString().trim());
            repo.setOAuthClientId(profile.id, clientIdInput.getText().toString().trim());
            repo.setOAuthClientSecret(profile.id, clientSecretInput.getText().toString().trim());
            repo.setOAuthTokenEndpoint(profile.id, tokenInput.getText().toString().trim());

            refreshList();
            dialog.dismiss();
        });
    }

    private void refreshList() {
        servers.clear();
        servers.addAll(repo.getAll());
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
