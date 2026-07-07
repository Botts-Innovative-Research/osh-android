package org.sensorhub.android;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import net.opengis.swe.v20.DataBlock;

import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClient.StreamInfo;
import org.sensorhub.impl.event.EventBus;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig;
import org.sensorhub.impl.sensor.android.video.VideoEncoderConfig.VideoPreset;
import org.sensorhub.impl.sensor.meshtastic.MeshtasticSensor;
import org.sensorhub.impl.sensor.meshtastic.control.TextMessageControl;
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule;

import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.LinearLayout;


public class DashboardFragment extends Fragment implements TextureView.SurfaceTextureListener, Flow.Subscriber<Event>
{
    private TextView videoInfoArea;
    private TextureView textureView;
    private MaterialCardView videoStatusCard;
    private MaterialButton btnToggleVideo;
    private MaterialButton btnFlipCamera;
    private MaterialButton btnZoomIn;
    private MaterialButton btnZoomOut;
    private LinearLayout videoControlsOverlay;
    private int currentZoomLevel = 0;
    private MaterialCardView meshtasticCard;
    private View videoStatusDot;
    private FloatingActionButton fab;
    private LinearLayout serverStatusContainer;
    private Handler displayHandler;
    private Runnable displayCallback;
    private StringBuffer videoInfoText = new StringBuffer();
    private Flow.Subscription subscription;
    private SensorHubServiceProvider provider;
    private boolean videoPreviewVisible = false;

    private final Map<String, View> serverCardViews = new HashMap<>();
    private final Set<String> expandedServers = new HashSet<>();
    private final Set<String> expandedSensors = new HashSet<>();

    private static class DataStreamStatus {
        final String outputName;
        final String statusText;
        final int statusColor;
        final boolean isOk;

        DataStreamStatus(String outputName, String statusText, int statusColor, boolean isOk) {
            this.outputName = outputName;
            this.statusText = statusText;
            this.statusColor = statusColor;
            this.isOk = isOk;
        }
    }

    private static class SensorGroupInfo {
        final String sensorId;
        final String sensorName;
        final java.util.List<DataStreamStatus> streams = new java.util.ArrayList<>();
        boolean allOk = true;
        boolean hasError = false;

        SensorGroupInfo(String sensorId, String sensorName) {
            this.sensorId = sensorId;
            this.sensorName = sensorName;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        provider = (SensorHubServiceProvider) requireActivity();
        displayHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoInfoArea = view.findViewById(R.id.video_info);

        textureView = view.findViewById(R.id.video);
        textureView.setSurfaceTextureListener(this);

        videoStatusCard = view.findViewById(R.id.video_status_card);
        btnToggleVideo = view.findViewById(R.id.btn_toggle_video);
        videoStatusDot = view.findViewById(R.id.video_status_dot);

        btnToggleVideo.setOnClickListener(v -> toggleVideoPreview());

        videoControlsOverlay = view.findViewById(R.id.video_controls_overlay);

        btnFlipCamera = view.findViewById(R.id.btn_flip_camera);
        btnFlipCamera.setOnClickListener(v -> flipCamera());

        btnZoomIn = view.findViewById(R.id.btn_zoom_in);
        btnZoomIn.setOnClickListener(v -> adjustZoom(1));
        btnZoomOut = view.findViewById(R.id.btn_zoom_out);
        btnZoomOut.setOnClickListener(v -> adjustZoom(-1));

        meshtasticCard = view.findViewById(R.id.meshtastic_card);
        view.findViewById(R.id.btn_meshtastic_msg).setOnClickListener(v -> showMeshtasticDialog());

        serverStatusContainer = view.findViewById(R.id.server_status_container);

        fab = view.findViewById(R.id.fab_toggle);
        fab.setOnClickListener(v -> {
            if (!provider.isOshStarted()) {
                if (provider.getBoundService() != null)
                    showRunNamePopup();
            } else {
                stopHub();
            }
        });

        updateFabIcon();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (provider.isOshStarted()) {
            startRefreshingStatus();
            updateVideoStatusCard();
            updateMeshtasticCard();
        }
    }

    @Override
    public void onPause() {
        stopRefreshingStatus();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopRefreshingStatus();
        displayHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void updateFabIcon() {
        if (fab == null) return;
        if (provider.isOshStarted()) {
            fab.setImageResource(R.drawable.ic_stop);
        } else {
            fab.setImageResource(R.drawable.ic_play);
        }
    }

    private void showFabProgress() {
        if (fab == null) return;
        fab.setImageResource(R.drawable.ic_loading_animated);
        fab.setEnabled(false);
    }

    private void hideFabProgress() {
        if (fab == null) return;
        fab.setEnabled(true);
        updateFabIcon();
    }

    private void stopHub() {
        Toast.makeText(requireContext(), "Stopping SensorHub", Toast.LENGTH_SHORT).show();
        showFabProgress();
        newStatusMessage(getString(R.string.stopping_sensorhub));

        stopRefreshingStatus();
        hideVideoPreview();
        clearTextureView();
        videoStatusCard.setVisibility(View.GONE);
        if (videoControlsOverlay != null) videoControlsOverlay.setVisibility(View.GONE);
        currentZoomLevel = 0;
        if (btnFlipCamera != null) btnFlipCamera.setVisibility(View.GONE);
        if (meshtasticCard != null) meshtasticCard.setVisibility(View.GONE);
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (meshtasticCard != null) meshtasticCard.setVisibility(View.GONE);
        provider.stopSensorHub();
        displayHandler.postDelayed(() -> {
            if (!isAdded()) return;
            hideFabProgress();
            updateFabIcon();
            newStatusMessage(getString(R.string.sensorhub_stopped));
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }, 1000);
    }

    private void clearTextureView() {
        if (textureView == null || textureView.getSurfaceTexture() == null) return;
        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            textureView.unlockCanvasAndPost(canvas);
        }
    }


    protected synchronized void showRunNamePopup() {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(requireContext());
        alert.setTitle(R.string.title_run_name);
        alert.setMessage(getString(R.string.msg_enter_run_name));

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint(getString(R.string.title_run_name));

        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.getText().append("Run-");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        input.getText().append(formatter.format(new Date()));
        inputLayout.addView(input);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(padding, 0, padding, 0);
        container.addView(inputLayout);
        alert.setView(container);

        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                String runName = input.getText().toString();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                provider.updateConfig(prefs, runName);

                AndroidSensorsConfig androidSensorConfig = (AndroidSensorsConfig) provider.getSensorhubConfig().get("ANDROID_SENSORS");
                VideoEncoderConfig videoConfig = androidSensorConfig.videoConfig;

                boolean cameraInUse = (androidSensorConfig.activateBackCamera || androidSensorConfig.activateFrontCamera);
                boolean improperVideoSettings = (videoConfig.selectedPreset < 0 || videoConfig.selectedPreset >= videoConfig.presets.length);

                if (cameraInUse && improperVideoSettings) {
                    showVideoConfigErrorPopup();
                    newStatusMessage(getString(R.string.video_config_error));
                } else {
                    showFabProgress();
                    newStatusMessage(getString(R.string.starting_sensorhub));
                    provider.getSostClients().clear();
                    provider.getConSysClients().clear();
                    provider.startSensorHub();

                    waitForHubReady();
                }
            }
        });

        alert.setNegativeButton(R.string.btn_cancel, (dialog, whichButton) -> {});
        alert.show();
    }

    private static final int HUB_POLL_INTERVAL_MS = 200;
    private static final int HUB_POLL_MAX_ATTEMPTS = 150;
    private int hubPollAttempts = 0;

    private void waitForHubReady() {
        hubPollAttempts = 0;
        displayHandler.post(this::pollHubReady);
    }

    private void pollHubReady() {
        if (!isAdded()) return;

        SensorHubService service = provider.getBoundService();
        hubPollAttempts++;

        if (service != null && service.getSensorHub() != null && service.getSensorHub().getEventBus() != null) {
            EventBus shEvtBus = (EventBus) service.getSensorHub().getEventBus();
            shEvtBus.newSubscription()
                    .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
                    .subscribe(DashboardFragment.this);

            ModuleRegistry registry = (ModuleRegistry) service.getSensorHub().getModuleRegistry();
            for (IModule<?> module : registry.getLoadedModules()) {
                if (module instanceof SOSTClient) {
                    provider.getSostClients().add((SOSTClient) module);
                } else if (module instanceof ConSysApiClientModule) {
                    provider.getConSysClients().add((ConSysApiClientModule) module);
                } else if (module instanceof AndroidSensorsDriver) {
                    provider.setAndroidSensors((AndroidSensorsDriver) module);
                }
            }

            if (!provider.isOshStarted()) {
                provider.setOshStarted(true);
                hideFabProgress();
                serverStatusContainer.removeAllViews();
                serverCardViews.clear();
                startRefreshingStatus();
                updateVideoStatusCard();
                updateMeshtasticCard();
                if (videoPreviewVisible)
                    showVideo();
            }
        } else if (hubPollAttempts < HUB_POLL_MAX_ATTEMPTS) {
            displayHandler.postDelayed(this::pollHubReady, HUB_POLL_INTERVAL_MS);
        } else {
            newStatusMessage("SensorHub failed to start");
            updateFabIcon();
        }
    }

    protected void showVideoConfigErrorPopup() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.app_name)
            .setMessage(R.string.video_config_error_msg)
            .setPositiveButton(R.string.btn_ok, (dialog, id) -> {})
            .show();
    }

    protected void startRefreshingStatus() {
        if (displayCallback != null) return;

        displayCallback = new Runnable() {
            public void run() {
                displayStatus();
                videoInfoArea.setText(Html.fromHtml(videoInfoText.toString()));
                displayHandler.postDelayed(this, 1000);
            }
        };
        displayHandler.post(displayCallback);
    }

    protected void stopRefreshingStatus() {
        if (displayCallback != null) {
            displayHandler.removeCallbacks(displayCallback);
            displayCallback = null;
        }
    }

    protected synchronized void displayStatus() {
        Set<String> activeClientIds = new HashSet<>();

        for (SOSTClient client : provider.getSostClients()) {
            String clientId = client.getLocalID();
            activeClientIds.add(clientId);
            String serverName = extractServerName(client.getName(), "SOS-T");
            String clientMode = "SOS-T";

            Map<String, StreamInfo> dataStreams = client.getDataStreams();
            String errorText = null;
            String statusMsg = null;
            boolean hasError = false;

            if (client.getCurrentError() != null) {
                hasError = true;
                Throwable errorObj = client.getCurrentError();
                String errorMsg = errorObj.getMessage() != null ? errorObj.getMessage().trim() : "Unknown error";
                if (!errorMsg.endsWith(".")) errorMsg += ". ";
                if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                    errorMsg += errorObj.getCause().getMessage();
                errorText = errorMsg;
            }
            if (dataStreams.isEmpty() && client.getStatusMessage() != null) {
                statusMsg = client.getStatusMessage();
            }

            long now = System.currentTimeMillis();
            boolean allOk = !hasError && !dataStreams.isEmpty();
            java.util.List<SensorGroupInfo> sensorGroups = buildSensorGroups(dataStreams, now);
            for (SensorGroupInfo g : sensorGroups) { if (!g.allOk) allOk = false; if (g.hasError) hasError = true; }

            updateServerCard(clientId, serverName, clientMode, allOk, hasError, errorText, statusMsg, sensorGroups);
        }

        for (ConSysApiClientModule client : provider.getConSysClients()) {
            String clientId = client.getLocalID();
            activeClientIds.add(clientId);
            String serverName = extractServerName(client.getName(), "Connected Systems");
            String clientMode = "Connected Systems";

            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            String errorText = null;
            String statusMsg = null;
            boolean hasError = false;

            if (client.getCurrentError() != null) {
                hasError = true;
                Throwable errorObj = client.getCurrentError();
                String errorMsg = errorObj.getMessage() != null ? errorObj.getMessage().trim() : "Unknown error";
                if (!errorMsg.endsWith(".")) errorMsg += ". ";
                if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                    errorMsg += errorObj.getCause().getMessage();
                errorText = errorMsg;
            }
            if (dataStreams.isEmpty() && client.getStatusMessage() != null) {
                statusMsg = client.getStatusMessage();
            }

            long now = System.currentTimeMillis();
            boolean allOk = !hasError && !dataStreams.isEmpty();
            java.util.List<SensorGroupInfo> sensorGroups = buildSensorGroups(dataStreams, now);
            for (SensorGroupInfo g : sensorGroups) { if (!g.allOk) allOk = false; if (g.hasError) hasError = true; }

            updateServerCard(clientId, serverName, clientMode, allOk, hasError, errorText, statusMsg, sensorGroups);
        }

        Set<String> staleIds = new HashSet<>(serverCardViews.keySet());
        staleIds.removeAll(activeClientIds);
        for (String id : staleIds) {
            View card = serverCardViews.remove(id);
            if (card != null) serverStatusContainer.removeView(card);
            expandedServers.remove(id);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        MainActivity activity = (MainActivity) requireActivity();
        boolean serveOrStore = activity.shouldServe(prefs) || activity.shouldStore(prefs);
        boolean noClients = provider.getSostClients().isEmpty() && provider.getConSysClients().isEmpty();

        View emptyView = serverStatusContainer.findViewWithTag("empty_status");
        if (noClients && serveOrStore) {
            if (emptyView == null) {
                TextView tv = new TextView(requireContext());
                tv.setTag("empty_status");
                tv.setText(R.string.no_sensors_push);
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
                tv.setTextSize(14);
                tv.setGravity(android.view.Gravity.CENTER);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                tv.setPadding(pad, pad, pad, pad);
                serverStatusContainer.addView(tv);
            }
        } else if (emptyView != null) {
            serverStatusContainer.removeView(emptyView);
        }

        AndroidSensorsDriver sensors = provider.getAndroidSensors();
        SensorHubService service = provider.getBoundService();
        if (sensors != null && service != null && service.hasVideo()) {
            try {
                VideoEncoderConfig config = sensors.getConfiguration().videoConfig;
                VideoPreset preset = config.presets[config.selectedPreset];
                videoInfoText.setLength(0);
                videoInfoText.append(config.codec).append(", ")
                        .append(preset.width).append("x").append(preset.height).append(", ")
                        .append(config.frameRate).append(" fps, ")
                        .append(preset.selectedBitrate).append(" kbits/s");
            } catch (Exception e) {
                // ignore display errors
            }
            updateVideoStatusCard();
            if (videoPreviewVisible)
                showVideo();
        }
    }

    protected synchronized void newStatusMessage(String msg) {
        displayHandler.post(() -> {
            serverStatusContainer.removeAllViews();
            serverCardViews.clear();
            TextView tv = new TextView(requireContext());
            tv.setText(msg);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
            tv.setTextSize(14);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
            serverStatusContainer.addView(tv);
        });
    }

    private String extractSensorId(String streamKey) {
        Matcher m = Pattern.compile("systems/([^/]+)/").matcher(streamKey);
        if (m.find()) return m.group(1);
        return streamKey;
    }

    private String formatSensorName(String sensorId) {
        String[] parts = sensorId.replace("urn:", "").split(":");
        String name;
        if (parts.length >= 3) {
            name = parts[parts.length - 2];
        } else if (parts.length == 2) {
            name = parts[0];
        } else {
            name = sensorId;
        }
        name = name.replace("_", " ").replace("-", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1); //capitalizes first letter
    }

    private String formatOutputName(String streamKey) {
        Matcher m = Pattern.compile("outputs/([^/]+)").matcher(streamKey);
        String raw;
        if (m.find()) {
            raw = m.group(1);
        } else {
            raw = streamKey;
        }
        raw = raw.replaceAll("(?i)_data$", "");
        raw = raw.replace("_", " ").replace("-", " ");
        return raw.substring(0, 1).toUpperCase() + raw.substring(1); //capitalizes first letter
    }


    private interface StreamAccessor<T> {
        long getLastEventTime(T info);
        long getMeasPeriodMs(T info);
        int getErrorCount(T info);
    }

    private <T> java.util.List<SensorGroupInfo> buildSensorGroups(Map<String, T> dataStreams, long now) {
        StreamAccessor<Object> accessor;
        if (!dataStreams.isEmpty()) {
            Object first = dataStreams.values().iterator().next();
            if (first instanceof StreamInfo) {
                accessor = new StreamAccessor<Object>() {
                    public long getLastEventTime(Object o) { return ((StreamInfo)o).lastEventTime; }
                    public long getMeasPeriodMs(Object o) { return ((StreamInfo)o).measPeriodMs; }
                    public int getErrorCount(Object o) { return ((StreamInfo)o).errorCount; }
                };
            } else {
                accessor = new StreamAccessor<Object>() {
                    public long getLastEventTime(Object o) { return ((ConSysApiClientModule.StreamInfo)o).lastEventTime; }
                    public long getMeasPeriodMs(Object o) { return ((ConSysApiClientModule.StreamInfo)o).measPeriodMs; }
                    public int getErrorCount(Object o) { return ((ConSysApiClientModule.StreamInfo)o).errorCount; }
                };
            }
        } else {
            return new java.util.ArrayList<>();
        }

        Map<String, SensorGroupInfo> grouped = new java.util.LinkedHashMap<>();
        for (Entry<String, T> stream : dataStreams.entrySet()) {
            String sensorId = extractSensorId(stream.getKey());
            SensorGroupInfo group = grouped.computeIfAbsent(sensorId,
                    k -> new SensorGroupInfo(k, formatSensorName(k)));

            String outputName = formatOutputName(stream.getKey());
            long lastEventTime = accessor.getLastEventTime(stream.getValue());
            long dt = now - lastEventTime;
            long measPeriod = accessor.getMeasPeriodMs(stream.getValue());
            int errorCount = accessor.getErrorCount(stream.getValue());

            String statusText;
            int statusColor;
            boolean isOk;
            if (lastEventTime == Long.MIN_VALUE) {
                statusText = "NO OBS";
                statusColor = R.color.status_stopped;
                isOk = false;
            } else if (dt > measPeriod) {
                statusText = "NOK (" + dt + "ms)";
                statusColor = R.color.status_stopped;
                isOk = false;
            } else {
                statusText = "OK (" + dt + "ms)";
                statusColor = R.color.status_started;
                isOk = true;
            }
            if (errorCount > 0) {
                statusText += " (" + errorCount + ")";
                statusColor = R.color.status_stopped;
                isOk = false;
            }
            if (!isOk) group.allOk = false;

            group.streams.add(new DataStreamStatus(outputName, statusText, statusColor, isOk));
        }
        return new java.util.ArrayList<>(grouped.values());
    }

    private String extractServerName(String clientName, String fallback) {
        if (clientName != null && clientName.contains(" -> ")) {
            return clientName.substring(clientName.lastIndexOf(" -> ") + 4);
        }
        return fallback;
    }

    private void updateServerCard(String clientId, String serverName, String clientMode,
                                  boolean allOk, boolean hasError,
                                  String errorText, String statusMsg,
                                  java.util.List<SensorGroupInfo> sensorGroups) {
        View card = serverCardViews.get(clientId);

        if (card == null) {
            card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_server_status, serverStatusContainer, false);
            serverCardViews.put(clientId, card);
            serverStatusContainer.addView(card);

            final View cardRef = card;
            final String idRef = clientId;
            ImageButton toggleBtn = card.findViewById(R.id.btn_toggle_server_details);
            toggleBtn.setOnClickListener(v -> {
                LinearLayout details = cardRef.findViewById(R.id.server_status_details);
                View divider = cardRef.findViewById(R.id.server_divider);
                ImageButton arrow = cardRef.findViewById(R.id.btn_toggle_server_details);
                boolean expanded = details.getVisibility() == View.VISIBLE;

                TransitionManager.beginDelayedTransition(
                        (ViewGroup) cardRef,
                        new AutoTransition().setDuration(200)
                );

                details.setVisibility(expanded ? View.GONE : View.VISIBLE);
                divider.setVisibility(expanded ? View.GONE : View.VISIBLE);

                arrow.animate()
                        .rotation(expanded ? 0f : 90f)
                        .setDuration(200)
                        .start();

                if (expanded) {
                    expandedServers.remove(idRef);
                } else {
                    expandedServers.add(idRef);
                }
            });
        }

        TextView nameView = card.findViewById(R.id.server_status_name);
        nameView.setText(serverName);

        TextView subtitleView = card.findViewById(R.id.server_status_subtitle);
        subtitleView.setText(clientMode);

        View serverDot = card.findViewById(R.id.server_overall_status_dot);
        if (serverDot.getBackground() instanceof GradientDrawable) {
            GradientDrawable dotBg = (GradientDrawable) serverDot.getBackground();
            boolean serverOk = errorText == null;
            if (serverOk) {
                for (SensorGroupInfo g : sensorGroups) {
                    if (!g.allOk) { serverOk = false; break; }
                }
            }
            int dotColor = serverOk ? R.color.status_started : R.color.status_stopped;
            dotBg.setColor(ContextCompat.getColor(requireContext(), dotColor));
        }

        LinearLayout detailsContainer = card.findViewById(R.id.server_status_details);
        detailsContainer.removeAllViews();

        if (errorText != null) {
            TextView errorView = new TextView(requireContext());
            errorView.setText(errorText);
            errorView.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_stopped));
            errorView.setTextSize(12);
            errorView.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));
            detailsContainer.addView(errorView);
        }

        if (statusMsg != null) {
            TextView statusView = new TextView(requireContext());
            statusView.setText(statusMsg);
            statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            statusView.setTextSize(12);
            statusView.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));
            detailsContainer.addView(statusView);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (SensorGroupInfo group : sensorGroups) {
            View groupView = inflater.inflate(R.layout.item_sensor_group, detailsContainer, false);
            String sensorKey = clientId + ":" + group.sensorId;

            TextView sensorName = groupView.findViewById(R.id.sensor_group_name);
            ImageView sensorArrow = groupView.findViewById(R.id.sensor_group_arrow);
            View serverStatusDot = groupView.findViewById(R.id.server_status_dot);
            LinearLayout streamsContainer = groupView.findViewById(R.id.sensor_group_streams);
            View sensorHeader = groupView.findViewById(R.id.sensor_group_header);

            sensorName.setText(group.sensorName);

            if (serverStatusDot.getBackground() instanceof GradientDrawable) {
                GradientDrawable dotBg = (GradientDrawable) serverStatusDot.getBackground();
                int dotColor = group.allOk ? R.color.status_started : R.color.status_stopped;
                dotBg.setColor(ContextCompat.getColor(requireContext(), dotColor));
            }

            for (DataStreamStatus stream : group.streams) {
                View streamView = inflater.inflate(R.layout.item_datastream, streamsContainer, false);
                TextView streamName = streamView.findViewById(R.id.datastream_name);
                TextView streamStatus = streamView.findViewById(R.id.datastream_status);
                streamName.setText(stream.outputName);
                streamStatus.setText(stream.statusText);
                streamStatus.setTextColor(ContextCompat.getColor(requireContext(), stream.statusColor));
                streamsContainer.addView(streamView);
            }

            boolean sensorExpanded = expandedSensors.contains(sensorKey);
            streamsContainer.setVisibility(sensorExpanded ? View.VISIBLE : View.GONE);
            sensorArrow.setRotation(sensorExpanded ? 90f : 0f);

            sensorHeader.setOnClickListener(v -> {
                boolean exp = expandedSensors.contains(sensorKey);
                if (exp) {
                    expandedSensors.remove(sensorKey);
                    streamsContainer.setVisibility(View.GONE);
                    sensorArrow.animate().rotation(0f).setDuration(200).start();
                } else {
                    expandedSensors.add(sensorKey);
                    streamsContainer.setVisibility(View.VISIBLE);
                    sensorArrow.animate().rotation(90f).setDuration(200).start();
                }
            });

            detailsContainer.addView(groupView);
        }

        boolean expanded = expandedServers.contains(clientId);
        detailsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);

        View divider = card.findViewById(R.id.server_divider);
        divider.setVisibility(expanded ? View.VISIBLE : View.GONE);

        ImageButton toggle = card.findViewById(R.id.btn_toggle_server_details);
        toggle.setRotation(expanded ? 90f : 0f);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateVideoStatusCard() {
        SensorHubService service = provider.getBoundService();
        boolean hasVideo = service != null && service.hasVideo();

        videoStatusCard.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
        updateVideoControlsVisibility();

        if (hasVideo && videoInfoText.length() > 0) {
            videoInfoArea.setText(videoInfoText.toString());
        }

        if (videoStatusDot != null && videoStatusDot.getBackground() instanceof GradientDrawable) {
            GradientDrawable dot = (GradientDrawable) videoStatusDot.getBackground();
            int color = ContextCompat.getColor(requireContext(),
                    hasVideo ? R.color.status_started : R.color.status_unknown);
            dot.setColor(color);
        }
    }

    private void toggleVideoPreview() {
        videoPreviewVisible = !videoPreviewVisible;
        if (videoPreviewVisible) {
            textureView.setVisibility(View.VISIBLE);
            btnToggleVideo.setText(R.string.btn_hide);
            serverStatusContainer.setBackgroundColor(getResources().getColor(R.color.overlay_light, requireActivity().getTheme()));
            updateVideoControlsVisibility();
            showVideo();
        } else {
            hideVideoPreview();
        }
    }

    @SuppressWarnings("deprecation")
    private void updateVideoControlsVisibility() {
        SensorHubService service = provider.getBoundService();
        boolean hasVideo = service != null && service.hasVideo();

        if (videoControlsOverlay != null)
            videoControlsOverlay.setVisibility(hasVideo && videoPreviewVisible ? View.VISIBLE : View.GONE);

        if (btnFlipCamera != null) {
            boolean showFlip = hasVideo && android.hardware.Camera.getNumberOfCameras() > 1;
            btnFlipCamera.setVisibility(showFlip ? View.VISIBLE : View.GONE);
        }

        boolean isBackCamera = isBackCameraActive();
        if (btnZoomIn != null) btnZoomIn.setVisibility(isBackCamera ? View.VISIBLE : View.GONE);
        if (btnZoomOut != null) btnZoomOut.setVisibility(isBackCamera ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("deprecation")
    private boolean isBackCameraActive() {
        AndroidSensorsDriver sensors = provider.getAndroidSensors();
        if (sensors == null) return true;
        try {
            int cameraId = sensors.getConfiguration().selectedCameraId;
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            return info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    private void flipCamera() {
        AndroidSensorsDriver sensors = provider.getAndroidSensors();
        if (sensors == null) return;

        try {
            int currentId = sensors.getConfiguration().selectedCameraId;
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(currentId, info);

            String targetFacing = (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)
                    ? "FRONT" : "BACK";

            int targetId = -1;
            int targetFacingInt = "FRONT".equals(targetFacing)
                    ? android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
                    : android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

            for (int i = 0; i < android.hardware.Camera.getNumberOfCameras(); i++) {
                android.hardware.Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(i, camInfo);
                if (camInfo.facing == targetFacingInt) {
                    targetId = i;
                    break;
                }
            }

            if (targetId >= 0) {
                sensors.switchCamera(targetId);
                currentZoomLevel = 0;
                Toast.makeText(requireContext(), "Switched to " + targetFacing.toLowerCase() + " camera", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to switch camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustZoom(int direction) {
        AndroidSensorsDriver sensors = provider.getAndroidSensors();
        if (sensors == null) return;

        try {
            currentZoomLevel = Math.max(0, currentZoomLevel + direction);
            sensors.setCameraZoom(currentZoomLevel);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Zoom not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideVideoPreview() {
        videoPreviewVisible = false;
        textureView.setVisibility(View.GONE);
        if (videoControlsOverlay != null) videoControlsOverlay.setVisibility(View.GONE);
        if (btnToggleVideo != null) btnToggleVideo.setText(R.string.btn_show);
        serverStatusContainer.setBackgroundColor(0x00000000);
    }

    protected void showVideo() {
        SensorHubService service = provider.getBoundService();
        if (service != null && service.getVideoTexture() != null && !service.getVideoTexture().isReleased()) {
            if (textureView.getSurfaceTexture() != service.getVideoTexture())
                textureView.setSurfaceTexture(service.getVideoTexture());
        }
    }

    private void updateMeshtasticCard() {
        if (meshtasticCard == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean meshtasticEnabled = prefs.getBoolean("meshtastic_enabled", false);
        boolean show = meshtasticEnabled && provider.isOshStarted();
        meshtasticCard.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            View dot = meshtasticCard.findViewById(R.id.meshtastic_status_dot);
            if (dot != null && dot.getBackground() instanceof GradientDrawable) {
                GradientDrawable bg = (GradientDrawable) dot.getBackground();
                bg.setColor(ContextCompat.getColor(requireContext(), R.color.status_started));
            }
        }
    }

    private void showMeshtasticDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_meshtastic, null);

        EditText messageInput = dialogView.findViewById(R.id.msg_input);
        EditText destinationIdText = dialogView.findViewById(R.id.destination_nodeId);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_send_meshtastic)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_send, (dialog, id) -> {
                    String msg = messageInput.getText().toString();
                    String destinationId = destinationIdText.getText().toString();
                    sendMeshtasticMessage(msg, destinationId);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void sendMeshtasticMessage(String message, String nodeId) {
        SensorHubService service = provider.getBoundService();
        if (service == null || service.getSensorHub() == null) {
            Toast.makeText(requireContext(), "SensorHub not running", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ModuleRegistry reg = (ModuleRegistry) service.getSensorHub().getModuleRegistry();
            MeshtasticSensor meshy = reg.getModuleByType(MeshtasticSensor.class);

            IStreamingControlInterface textMessageControl =
                    meshy.getCommandInputs().get(TextMessageControl.NAME);

            DataBlock cmdData = textMessageControl.getCommandDescription().createDataBlock();
            cmdData.setStringValue(0, message);
            cmdData.setIntValue(1, Integer.parseInt(nodeId));

            String deviceID = android.provider.Settings.Secure.getString(
                    requireContext().getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);

            var cmd = new CommandData.Builder()
                    .withCommandStream(BigId.NONE)
                    .withSender(deviceID)
                    .withParams(cmdData)
                    .build();

            textMessageControl.submitCommand(cmd);
            Toast.makeText(requireContext(), "Message sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if (videoPreviewVisible) showVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}


    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(10);
    }

    @Override
    public void onNext(Event e) {
        if (e instanceof ModuleEvent) {
            if (!provider.isOshStarted() && ((ModuleEvent) e).getType() == ModuleEvent.Type.LOADED) {
                provider.setOshStarted(true);
                requireActivity().runOnUiThread(this::hideFabProgress);
                startRefreshingStatus();
                subscription.request(10);
                return;
            }
            else if (e.getSource() instanceof AndroidSensorsDriver) {
                provider.setAndroidSensors((AndroidSensorsDriver) e.getSource());
            }
            else if (e.getSource() instanceof SOSTClient && ((ModuleEvent) e).getType() == ModuleEvent.Type.STATE_CHANGED) {
                if (((ModuleEvent) e).getNewState() == org.sensorhub.api.module.ModuleEvent.ModuleState.INITIALIZING) {
                    provider.getSostClients().add((SOSTClient) e.getSource());
                }
            }
            else if (e.getSource() instanceof ConSysApiClientModule && ((ModuleEvent) e).getType() == ModuleEvent.Type.STATE_CHANGED) {
                if (((ModuleEvent) e).getNewState() == org.sensorhub.api.module.ModuleEvent.ModuleState.INITIALIZING) {
                    provider.getConSysClients().add((ConSysApiClientModule) e.getSource());
                }
            }
        }
        subscription.request(10);
    }

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}
}
