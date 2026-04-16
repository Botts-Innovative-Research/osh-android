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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Flow;


public class DashboardFragment extends Fragment implements TextureView.SurfaceTextureListener, Flow.Subscriber<Event>
{
    private TextView mainInfoArea;
    private TextView videoInfoArea;
    private TextureView textureView;
    private MaterialCardView videoStatusCard;
    private MaterialButton btnToggleVideo;
    private View videoStatusDot;
    private FloatingActionButton fab;
    private ImageButton btnToggleStatus;
    private View mainInfoScroll;
    private Handler displayHandler;
    private Runnable displayCallback;
    private StringBuffer mainInfoText = new StringBuffer();
    private StringBuffer videoInfoText = new StringBuffer();
    private Flow.Subscription subscription;
    private SensorHubServiceProvider provider;
    private boolean videoPreviewVisible = false;
    private boolean statusExpanded = true;

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

        mainInfoArea = view.findViewById(R.id.main_info);
        videoInfoArea = view.findViewById(R.id.video_info);

        textureView = view.findViewById(R.id.video);
        textureView.setSurfaceTextureListener(this);

        videoStatusCard = view.findViewById(R.id.video_status_card);
        btnToggleVideo = view.findViewById(R.id.btn_toggle_video);
        videoStatusDot = view.findViewById(R.id.video_status_dot);

        btnToggleVideo.setOnClickListener(v -> toggleVideoPreview());

        btnToggleStatus = view.findViewById(R.id.btn_toggle_status);
        btnToggleStatus.setOnClickListener(v -> toggleStatusExpanded());
        mainInfoScroll = view.findViewById(R.id.main_info_scroll);

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
                mainInfoArea.setText(Html.fromHtml(mainInfoText.toString()));
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
        mainInfoText.setLength(0);

        // SOST Client errors/status
        for (SOSTClient client : provider.getSostClients()) {
            Map<String, StreamInfo> dataStreams = client.getDataStreams();
            boolean showError = (client.getCurrentError() != null);
            boolean showMsg = (dataStreams.isEmpty()) && (client.getStatusMessage() != null);

            if (showError || showMsg) {
                mainInfoText.append("<p>" + client.getName() + ":<br/>");
                if (showMsg)
                    mainInfoText.append(client.getStatusMessage() + "<br/>");
                if (showError) {
                    Throwable errorObj = client.getCurrentError();
                    String errorMsg = errorObj.getMessage().trim();
                    if (!errorMsg.endsWith("."))
                        errorMsg += ". ";
                    if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                        errorMsg += errorObj.getCause().getMessage();
                    mainInfoText.append("<font color='red'>" + errorMsg + "</font>");
                }
                mainInfoText.append("</p>");
            }
        }

        // ConSys Client errors/status
        for (ConSysApiClientModule client : provider.getConSysClients()) {
            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            boolean showError = (client.getCurrentError() != null);
            boolean showMsg = (dataStreams.isEmpty()) && (client.getStatusMessage() != null);

            if (showError || showMsg) {
                mainInfoText.append("<p>" + client.getName() + ":<br/>");
                if (showMsg)
                    mainInfoText.append(client.getStatusMessage() + "<br/>");
                if (showError) {
                    Throwable errorObj = client.getCurrentError();
                    String errorMsg = errorObj.getMessage().trim();
                    if (!errorMsg.endsWith("."))
                        errorMsg += ". ";
                    if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                        errorMsg += errorObj.getCause().getMessage();
                    mainInfoText.append("<font color='red'>" + errorMsg + "</font>");
                }
                mainInfoText.append("</p>");
            }
        }

        mainInfoText.append("<p>");
        for (SOSTClient client : provider.getSostClients()) {
            mainInfoText.append("SOS-T Client<p>");
            Map<String, StreamInfo> dataStreams = client.getDataStreams();
            long now = System.currentTimeMillis();
            for (Entry<String, StreamInfo> stream : dataStreams.entrySet()) {
                mainInfoText.append("<b>" + stream.getKey() + " : </b>");
                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE)
                    mainInfoText.append("<font color='red'>NO OBS</font>");
                else if (dt > stream.getValue().measPeriodMs)
                    mainInfoText.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                else
                    mainInfoText.append("<font color='green'>OK (" + dt + "ms ago)</font>");
                if (stream.getValue().errorCount > 0) {
                    mainInfoText.append("<font color='red'> (");
                    mainInfoText.append(stream.getValue().errorCount);
                    mainInfoText.append(")</font>");
                }
                mainInfoText.append("<br/>");
            }
        }

        for (ConSysApiClientModule client : provider.getConSysClients()) {
            mainInfoText.append("ConSysApi Client<p>");
            Map<String, ConSysApiClientModule.StreamInfo> dataStreams = client.getDataStreams();
            long now = System.currentTimeMillis();
            for (Entry<String, ConSysApiClientModule.StreamInfo> stream : dataStreams.entrySet()) {
                mainInfoText.append("<b>" + stream.getKey() + " : </b>");
                long lastEventTime = stream.getValue().lastEventTime;
                long dt = now - lastEventTime;
                if (lastEventTime == Long.MIN_VALUE)
                    mainInfoText.append("<font color='red'>NO OBS</font>");
                else if (dt > stream.getValue().measPeriodMs)
                    mainInfoText.append("<font color='red'>NOK (" + dt + "ms ago)</font>");
                else
                    mainInfoText.append("<font color='green'>OK (" + dt + "ms ago)</font>");
                if (stream.getValue().errorCount > 0) {
                    mainInfoText.append("<font color='red'> (");
                    mainInfoText.append(stream.getValue().errorCount);
                    mainInfoText.append(")</font>");
                }
                mainInfoText.append("<br/>");
            }
        }
        mainInfoText.append("<p>");

        if (mainInfoText.length() > 5)
            mainInfoText.setLength(mainInfoText.length() - 5);
        mainInfoText.append("</p>");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        MainActivity activity = (MainActivity) requireActivity();
        boolean serveOrStore = activity.shouldServe(prefs) || activity.shouldStore(prefs);
        if (provider.getSostClients().isEmpty() && serveOrStore) {
            mainInfoText.append("No Sensors Set to Push Remotely");
        }
        if (provider.getConSysClients().isEmpty() && serveOrStore) {
            mainInfoText.append("No Sensors Set to Push Remotely");
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
        mainInfoText.setLength(0);
        mainInfoText.append(msg);
        displayHandler.post(() -> mainInfoArea.setText(mainInfoText.toString()));
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

    private void toggleStatusExpanded() {
        statusExpanded = !statusExpanded;
        mainInfoScroll.setVisibility(statusExpanded ? View.VISIBLE : View.GONE);
        btnToggleStatus.setImageResource(statusExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
    }

    private void toggleVideoPreview() {
        videoPreviewVisible = !videoPreviewVisible;
        if (videoPreviewVisible) {
            textureView.setVisibility(View.VISIBLE);
            btnToggleVideo.setText("Hide");
            mainInfoArea.setBackgroundColor(getResources().getColor(R.color.overlay_light, requireActivity().getTheme()));
            showVideo();
        } else {
            hideVideoPreview();
        }
    }

    private void hideVideoPreview() {
        videoPreviewVisible = false;
        textureView.setVisibility(View.GONE);
        if (btnToggleVideo != null) btnToggleVideo.setText("Show");
        mainInfoArea.setBackgroundColor(0x00000000);
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

    // ==================== Event Subscriber ====================

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
