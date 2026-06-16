package org.sensorhub.android.server;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import org.sensorhub.android.R;

public class EditServerProfileActivity extends AppCompatActivity {

    public static final String EXTRA_PROFILE_ID = "profile_id";

    private ServerProfileRepository repo;
    private ServerProfile profile;
    private boolean isEdit;

    private TextInputEditText nameInput, hostInput, portInput, endpointInput;
    private TextInputEditText usernameInput, passwordInput;
    private TextInputEditText tokenInput, clientIdInput, clientSecretInput;
    private MaterialSwitch tlsSwitch, sslSwitch;
    private MaterialButtonToggleGroup profileTypeToggle, authModeToggle;
    private View basicAuthFields, oauthFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_server_profile);

        repo = ServerProfileRepository.getInstance(this);

        String profileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        isEdit = profileId != null;
        profile = isEdit ? repo.getById(profileId) : new ServerProfile();

        if (isEdit && profile == null) {
            Toast.makeText(this, R.string.msg_profile_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        bindViews();
        setupListeners();
        if (isEdit) populateFields();
        updateSslVisibility();

        MaterialButton saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        toolbar.setTitle(isEdit ? getString(R.string.title_edit_server) : getString(R.string.btn_add_server));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        nameInput = findViewById(R.id.edit_name);
        hostInput = findViewById(R.id.edit_host);
        portInput = findViewById(R.id.edit_port);
        endpointInput = findViewById(R.id.edit_endpoint);
        usernameInput = findViewById(R.id.edit_username);
        passwordInput = findViewById(R.id.edit_password);
        tokenInput = findViewById(R.id.edit_token_endpoint);
        clientIdInput = findViewById(R.id.edit_client_id);
        clientSecretInput = findViewById(R.id.edit_client_secret);
        tlsSwitch = findViewById(R.id.switch_tls);
        sslSwitch = findViewById(R.id.switch_disable_ssl);
        profileTypeToggle = findViewById(R.id.toggle_profile_type);
        authModeToggle = findViewById(R.id.toggle_auth_mode);
        basicAuthFields = findViewById(R.id.basic_auth_fields);
        oauthFields = findViewById(R.id.oauth_fields);
    }

    private void setupListeners() {
        authModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateAuthVisibility();
        });

        tlsSwitch.setOnCheckedChangeListener((btn, checked) -> updateSslVisibility());
    }

    private void updateAuthVisibility() {
        int checkedId = authModeToggle.getCheckedButtonId();
        basicAuthFields.setVisibility(checkedId == R.id.btn_auth_basic ? View.VISIBLE : View.GONE);
        oauthFields.setVisibility(checkedId == R.id.btn_auth_oauth ? View.VISIBLE : View.GONE);
    }

    private void updateSslVisibility() {
        sslSwitch.setVisibility(tlsSwitch.isChecked() ? View.VISIBLE : View.GONE);
        if (!tlsSwitch.isChecked()) {
            sslSwitch.setChecked(false);
        }
    }

    private void populateFields() {
        nameInput.setText(profile.name);
        hostInput.setText(profile.host);
        portInput.setText(String.valueOf(profile.port));
        endpointInput.setText(profile.endpointPath);
        usernameInput.setText(profile.username);
        tlsSwitch.setChecked(profile.enableTls);
        sslSwitch.setChecked(profile.disableSslCheck);
        updateSslVisibility();

        // Profile type toggle
        profileTypeToggle.check(profile.useConSysClient ? R.id.btn_type_consys : R.id.btn_type_sos);

        // Auth mode toggle
        if (profile.oAuthEnabled) {
            authModeToggle.check(R.id.btn_auth_oauth);
        } else if (profile.username != null && !profile.username.isEmpty()) {
            authModeToggle.check(R.id.btn_auth_basic);
        } else {
            authModeToggle.check(R.id.btn_auth_none);
        }
        updateAuthVisibility();

        // Secure fields
        passwordInput.setText(repo.getPassword(profile.id));
        tokenInput.setText(repo.getOAuthTokenEndpoint(profile.id));
        clientIdInput.setText(repo.getOAuthClientId(profile.id));
        clientSecretInput.setText(repo.getOAuthClientSecret(profile.id));
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String host = hostInput.getText().toString().trim();
        String portStr = portInput.getText().toString().trim();
        String endpoint = endpointInput.getText().toString().trim();

        if (name.isEmpty() || host.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, R.string.msg_name_host_port_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (host.contains(" ") || host.contains("://")) {
            Toast.makeText(this, R.string.msg_no_protocol, Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.msg_port_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (port < 1 || port > 65535) {
            Toast.makeText(this, R.string.msg_port_range, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endpoint.isEmpty() && !endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }

        int authCheckedId = authModeToggle.getCheckedButtonId();

        profile.name = name;
        profile.host = host;
        profile.port = port;
        profile.endpointPath = endpoint;
        profile.username = authCheckedId == R.id.btn_auth_basic
                ? usernameInput.getText().toString().trim() : "";
        profile.enableTls = tlsSwitch.isChecked();
        profile.disableSslCheck = sslSwitch.isChecked();
        profile.useConSysClient = profileTypeToggle.getCheckedButtonId() == R.id.btn_type_consys;
        profile.oAuthEnabled = authCheckedId == R.id.btn_auth_oauth;

        repo.save(profile);

        String password = authCheckedId == R.id.btn_auth_basic
                ? passwordInput.getText().toString().trim() : "";
        repo.setPassword(profile.id, password);

        if (profile.oAuthEnabled) {
            repo.setOAuthClientId(profile.id, clientIdInput.getText().toString().trim());
            repo.setOAuthClientSecret(profile.id, clientSecretInput.getText().toString().trim());
            repo.setOAuthTokenEndpoint(profile.id, tokenInput.getText().toString().trim());
        }

        setResult(RESULT_OK);
        finish();
    }
}
