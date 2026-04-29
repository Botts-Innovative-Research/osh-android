package org.sensorhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

    public interface Listener {
        void onEditClicked(ServerProfile profile);
        void onEnabledToggled(ServerProfile profile, boolean enabled);
        void onDeleteRequested(ServerProfile profile);
    }

    private final List<ServerProfile> servers;
    private final Listener listener;

    public ServerAdapter(List<ServerProfile> servers, Listener listener) {
        this.servers = servers;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, summary, mode;
        MaterialSwitch enabledSwitch;
        ImageButton editButton;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.profile_name);
            summary = view.findViewById(R.id.profile_summary);
            mode = view.findViewById(R.id.profile_mode);
            enabledSwitch = view.findViewById(R.id.profile_enabled_switch);
            editButton = view.findViewById(R.id.btn_edit_profile);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerProfile p = servers.get(position);

        holder.name.setText(p.name);
        holder.summary.setText(p.getDisplaySummary());
        holder.mode.setText(p.getClientModeLabel());

        holder.enabledSwitch.setOnCheckedChangeListener(null);
        holder.enabledSwitch.setChecked(p.enabled);
        holder.enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onEnabledToggled(p, isChecked));

        holder.editButton.setOnClickListener(v -> listener.onEditClicked(p));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteRequested(p);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }
}
