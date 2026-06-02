package org.sensorhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ServerProfilesActivity extends AppCompatActivity implements ServerAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ServerAdapter adapter;
    private final List<ServerProfile> servers = new ArrayList<>();
    private ServerProfileRepository repo;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                refreshList();
            });

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

        MaterialButton addButton = findViewById(R.id.btn_add_server);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditServerProfileActivity.class);
            editLauncher.launch(intent);
        });

        adapter = new ServerAdapter(servers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        refreshList();
    }

    @Override
    public void onEditClicked(ServerProfile profile) {
        Intent intent = new Intent(this, EditServerProfileActivity.class);
        intent.putExtra(EditServerProfileActivity.EXTRA_PROFILE_ID, profile.id);
        editLauncher.launch(intent);
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

    private void refreshList() {
        servers.clear();
        servers.addAll(repo.getAll());
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(servers.isEmpty() ? "No server profiles configured.\nTap Add to create one." : "");
    }
}
