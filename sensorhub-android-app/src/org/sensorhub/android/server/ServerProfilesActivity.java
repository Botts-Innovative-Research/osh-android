package org.sensorhub.android.server;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.sensorhub.android.R;

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

        repo = ServerProfileRepository.getInstance(this);

        recyclerView = findViewById(R.id.server_list);
        emptyText = findViewById(R.id.empty_text);

        FloatingActionButton fab = findViewById(R.id.fab_add_server);
        fab.setOnClickListener(v -> {
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
                .setTitle(R.string.title_delete_server)
                .setMessage(getString(R.string.msg_delete_server, profile.name))
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    repo.delete(profile.id);
                    refreshList();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void refreshList() {
        servers.clear();
        servers.addAll(repo.getAll());
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(servers.isEmpty() ? getString(R.string.empty_server_profiles) : "");
    }
}
