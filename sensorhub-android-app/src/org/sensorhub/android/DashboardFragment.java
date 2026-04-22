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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import org.sensorhub.impl.service.consys.client.ConSysApiClientModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Flow;

import android.widget.LinearLayout;


public class DashboardFragment extends Fragment implements TextureView.SurfaceTextureListener, Flow.Subscriber<Event>
{
    private TextView videoInfoArea;
    private TextureView textureView;
    private MaterialCardView videoStatusCard;
    private MaterialButton btnToggleVideo;
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

    private void stopHub() {
        Toast.makeText(requireContext(), "Stopping SensorHub", Toast.LENGTH_SHORT).show();
        stopRefreshingStatus();
        provider.stopSensorHub();
        updateFabIcon();
        hideVideoPreview();
        clearTextureView();
        videoStatusCard.setVisibility(View.GONE);
        newStatusMessage("SensorHub Stopped");
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        alert.setTitle("Run Name");
        alert.setMessage("Please enter the name for this run");

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint("Run Name");

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

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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
                    newStatusMessage("Video Config Error: Check Settings");
                } else {
                    Toast.makeText(requireContext(), "Starting SensorHub...", Toast.LENGTH_SHORT).show();
                    newStatusMessage("Starting SensorHub...");
                    provider.getSostClients().clear();
                    provider.getConSysClients().clear();
                    provider.startSensorHub();

                    waitForHubReady();
                }
            }
        });

        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {});
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
                updateFabIcon();
                serverStatusContainer.removeAllViews();
                serverCardViews.clear();
                startRefreshingStatus();
                updateVideoStatusCard();
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
        String message = "Check Video Settings and ensure the resolution for the selected preset has been set.";
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("OpenSensorHub")
            .setMessage(message)
            .setPositiveButton("OK", (dialog, id) -> {})
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
            StringBuffer detailHtml = new StringBuffer();
            boolean hasError = false;

            if (client.getCurrentError() != null) {
                hasError = true;
                Throwable errorObj = client.getCurrentError();
                String errorMsg = errorObj.getMessage() != null ? errorObj.getMessage().trim() : "Unknown error";
                if (!errorMsg.endsWith(".")) errorMsg += ". ";
                if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                    errorMsg += errorObj.getCause().getMessage();
                detailHtml.append("<font color='red'>" + errorMsg + "</font><br/>");
            }
            if (dataStreams.isEmpty() && client.getStatusMessage() != null) {
                detailHtml.append(client.getStatusMessage() + "<br/>");
            }

            long now = System.currentTimeMillis();
            boolean allOk = !hasError && !dataStreams.isEmpty();
            for (Entry<String, StreamInfo> stream : dataStreams.entrySet()) {
                detailHtml.append("<b>" + stream.getKey() + " : </b>");
                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE) {
                    detailHtml.append("<font color='red'>NO OBS</font>");
                    allOk = false;
                } else if (dt > stream.getValue().measPeriodMs) {
                    detailHtml.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                    allOk = false;
                } else {
                    detailHtml.append("<font color='green'>OK (" + dt + "ms ago)</font>");
                }
                if (stream.getValue().errorCount > 0) {
                    detailHtml.append("<font color='red'> (" + stream.getValue().errorCount + ")</font>");
                    allOk = false;
                }
                detailHtml.append("<br/>");
            }

            updateServerCard(clientId, serverName, clientMode, allOk, hasError, detailHtml.toString());
        }

        for (ConSysApiClientModule client : provider.getConSysClients()) {
            String clientId = client.getLocalID();
            activeClientIds.add(clientId);
            String serverName = extractServerName(client.getName(), "Connected Systems");
            String clientMode = "Connected Systems";

            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            StringBuffer detailHtml = new StringBuffer();
            boolean hasError = false;

            if (client.getCurrentError() != null) {
                hasError = true;
                Throwable errorObj = client.getCurrentError();
                String errorMsg = errorObj.getMessage() != null ? errorObj.getMessage().trim() : "Unknown error";
                if (!errorMsg.endsWith(".")) errorMsg += ". ";
                if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                    errorMsg += errorObj.getCause().getMessage();
                detailHtml.append("<font color='red'>" + errorMsg + "</font><br/>");
            }
            if (dataStreams.isEmpty() && client.getStatusMessage() != null) {
                detailHtml.append(client.getStatusMessage() + "<br/>");
            }

            long now = System.currentTimeMillis();
            boolean allOk = !hasError && !dataStreams.isEmpty();
            for (Entry<String, ConSysApiClientModule.StreamInfo> stream : dataStreams.entrySet()) {
                detailHtml.append("<b>" + stream.getKey() + " : </b>");
                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE) {
                    detailHtml.append("<font color='red'>NO OBS</font>");
                    allOk = false;
                } else if (dt > stream.getValue().measPeriodMs) {
                    detailHtml.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                    allOk = false;
                } else {
                    detailHtml.append("<font color='green'>OK (" + dt + "ms ago)</font>");
                }
                if (stream.getValue().errorCount > 0) {
                    detailHtml.append("<font color='red'> (" + stream.getValue().errorCount + ")</font>");
                    allOk = false;
                }
                detailHtml.append("<br/>");
            }

            updateServerCard(clientId, serverName, clientMode, allOk, hasError, detailHtml.toString());
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
                tv.setText("No Sensors Set to Push Remotely");
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

    private String extractServerName(String clientName, String fallback) {
        if (clientName != null && clientName.contains(" -> ")) {
            return clientName.substring(clientName.lastIndexOf(" -> ") + 4);
        }
        return fallback;
    }

    private void updateServerCard(String clientId, String serverName, String clientMode,
                                  boolean allOk, boolean hasError, String detailHtml) {
        View card = serverCardViews.get(clientId);

        if (card == null) {
            card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_server_status, serverStatusContainer, false);
            serverCardViews.put(clientId, card);
            serverStatusContainer.addView(card);

            final View cardRef = card;
            final String idRef = clientId;
            View header = card.findViewById(R.id.server_status_header);
            header.setOnClickListener(v -> {
                boolean expanded = expandedServers.contains(idRef);
                TextView details = cardRef.findViewById(R.id.server_status_details);
                ImageButton toggle = cardRef.findViewById(R.id.btn_toggle_server_details);
                if (expanded) {
                    expandedServers.remove(idRef);
                    details.setVisibility(View.GONE);
                    toggle.setImageResource(R.drawable.ic_expand_more);
                } else {
                    expandedServers.add(idRef);
                    details.setVisibility(View.VISIBLE);
                    toggle.setImageResource(R.drawable.ic_expand_less);
                }
            });
        }

        TextView nameView = card.findViewById(R.id.server_status_name);
        TextView modeView = card.findViewById(R.id.server_status_mode);
        nameView.setText(serverName);
        modeView.setText(clientMode);

        View dot = card.findViewById(R.id.server_status_dot);
        if (dot.getBackground() instanceof GradientDrawable) {
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            int colorRes;
            if (hasError) colorRes = R.color.status_stopped;
            else if (allOk) colorRes = R.color.status_started;
            else colorRes = R.color.status_initializing;
            bg.setColor(ContextCompat.getColor(requireContext(), colorRes));
        }

        if (card instanceof MaterialCardView) {
            int strokeColorRes;
            if (hasError) strokeColorRes = R.color.status_stopped;
            else if (allOk) strokeColorRes = R.color.status_started;
            else strokeColorRes = R.color.md_theme_outline;
            ((MaterialCardView) card).setStrokeColor(
                    ContextCompat.getColor(requireContext(), strokeColorRes));
        }

        TextView details = card.findViewById(R.id.server_status_details);
        details.setText(Html.fromHtml(detailHtml));
        boolean expanded = expandedServers.contains(clientId);
        details.setVisibility(expanded ? View.VISIBLE : View.GONE);

        ImageButton toggle = card.findViewById(R.id.btn_toggle_server_details);
        toggle.setImageResource(expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
    }

    private void updateVideoStatusCard() {
        SensorHubService service = provider.getBoundService();
        boolean hasVideo = service != null && service.hasVideo();

        videoStatusCard.setVisibility(hasVideo ? View.VISIBLE : View.GONE);

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
            btnToggleVideo.setText("Hide");
            serverStatusContainer.setBackgroundColor(getResources().getColor(R.color.overlay_light, requireActivity().getTheme()));
            showVideo();
        } else {
            hideVideoPreview();
        }
    }

    private void hideVideoPreview() {
        videoPreviewVisible = false;
        textureView.setVisibility(View.GONE);
        if (btnToggleVideo != null) btnToggleVideo.setText("Show");
        serverStatusContainer.setBackgroundColor(0x00000000);
    }

    protected void showVideo() {
        SensorHubService service = provider.getBoundService();
        if (service != null && service.getVideoTexture() != null && !service.getVideoTexture().isReleased()) {
            if (textureView.getSurfaceTexture() != service.getVideoTexture())
                textureView.setSurfaceTexture(service.getVideoTexture());
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
                requireActivity().runOnUiThread(this::updateFabIcon);
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
