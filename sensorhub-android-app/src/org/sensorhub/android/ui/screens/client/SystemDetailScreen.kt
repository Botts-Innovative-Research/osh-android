package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.sensorhub.android.R
import org.sensorhub.android.data.client.OshMapStore
import org.sensorhub.android.data.client.RemoteControlStream
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.components.StatusDot
import org.sensorhub.android.ui.theme.Background
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SystemDetailScreen(
    profileId: String,
    systemId: String,
    onBack: () -> Unit,
    viewModel: OshClientViewModel,
) {
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val enabledLocations by viewModel.enabledLocations.collectAsStateWithLifecycle()
    val enabledVideos by viewModel.enabledVideos.collectAsStateWithLifecycle()
    val videoErrors by viewModel.videoErrors.collectAsStateWithLifecycle()
    val otherValues by viewModel.otherValues.collectAsStateWithLifecycle()
    val remoteTracks by OshMapStore.tracks.collectAsStateWithLifecycle()
    val system = nodes.firstOrNull { it.profileId == profileId }
        ?.systems
        ?.firstOrNull { it.id == systemId }

    system?.let { selectedSystem ->
        DisposableEffect(profileId, selectedSystem.id) {
            viewModel.startSystemLocationStreams(profileId, selectedSystem)
            viewModel.startSystemOtherStreams(profileId, selectedSystem)
            onDispose {
                viewModel.stopSystemVideos(profileId, selectedSystem)
                viewModel.stopSystemLocationStreams(profileId, selectedSystem)
                viewModel.stopSystemOtherStreams(profileId, selectedSystem)
            }
        }
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = system?.name ?: "System",
                onBackClick = onBack,
            )
        },
        containerColor = Background,
    ) { padding ->
        if (system == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("This system is no longer available.")
            }
        } else {
            val videoStreams =
                system.visualizations.filter { it.kind == RemoteVisualization.Kind.VIDEO }
            val locationStreams =
                system.visualizations.filter { it.kind == RemoteVisualization.Kind.LOCATION }
            val otherStreams =
                system.visualizations.filter { it.kind == RemoteVisualization.Kind.OTHER }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (videoStreams.isNotEmpty()) {
                    items(videoStreams, key = { it.dataStreamId }) { visualization ->
                        VideoStreamCard(
                            visualization = visualization,
                            playing = visualization.dataStreamId in enabledVideos,
                            error = videoErrors[visualization.dataStreamId],
                            renderer = remember(visualization.dataStreamId) {
                                viewModel.videoRenderer(system, visualization)
                            },
                            onPlayPause = { playing ->
                                viewModel.setVideoEnabled(profileId, visualization, !playing)
                            },
                        )
                    }
                }
                if (locationStreams.isNotEmpty()) {
                    items(locationStreams, key = { it.dataStreamId }) { visualization ->
                        MapLocationCard(
                            visualization = visualization,
                            enabled = visualization.dataStreamId in enabledLocations,
                            position = remoteTracks[visualization.dataStreamId]
                                ?.let { it.latitude to it.longitude }
                                ?: system.location,
                        )
                    }
                }
                if (otherStreams.isNotEmpty()) {
                    items(otherStreams, key = { it.dataStreamId }) { visualization ->
                        OtherStreamCard(
                            visualization = visualization,
                            values = otherValues[visualization.dataStreamId].orEmpty(),
                        )
                    }
                }
                if (system.visualizations.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No datastreams are available for this system.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                system.ptz?.let { ptz ->
                    item(key = "ptz") {
                        PTZCommandCard(
                            controlStream = ptz,
                            onPtz = { command ->
                                viewModel.sendPtzCommand(
                                    profileId,
                                    ptz,
                                    mapOf(command.item to command.delta),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun OtherStreamCard(
    visualization: RemoteVisualization,
    values: Map<String, String>,
) {
    OSHCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SsidChart, contentDescription = "Datastream")
                Spacer(Modifier.width(10.dp))
                Text(
                    visualization.name.ifBlank { "Datastream" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (values.isEmpty()) {
                Text(
                    "Waiting for observations…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                values.toSortedMap().forEach { (field, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(field, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PTZCommandCard(
    controlStream: RemoteControlStream,
    onPtz: (PtzCommand) -> Unit,
) {
    OSHCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status = "started")
                Spacer(Modifier.width(10.dp))
                Text(
                    "PTZ controls",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ControlCamera, contentDescription = "PTZ Control")
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A)),
            ) {
                Column(
                    Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterStart),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        PtzButton(
                            Icons.Default.ZoomOut,
                            "Zoom out",
                            enabled = controlStream.supports(PtzCommand.ZOOM_OUT),
                        ) {
                            onPtz(PtzCommand.ZOOM_OUT)
                        }
                        PtzButton(
                            Icons.Default.ZoomIn,
                            "Zoom in",
                            enabled = controlStream.supports(PtzCommand.ZOOM_IN),
                        ) {
                            onPtz(PtzCommand.ZOOM_IN)
                        }
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        Spacer(Modifier.size(48.dp))
                        PtzButton(
                            Icons.Default.KeyboardArrowUp,
                            "Tilt up",
                            enabled = controlStream.supports(PtzCommand.TILT_UP),
                        ) {
                            onPtz(PtzCommand.TILT_UP)
                        }
                        Spacer(Modifier.size(48.dp))
                    }
                    Row {
                        PtzButton(
                            Icons.Default.KeyboardArrowLeft,
                            "Pan left",
                            enabled = controlStream.supports(PtzCommand.PAN_LEFT),
                        ) {
                            onPtz(PtzCommand.PAN_LEFT)
                        }

                        Spacer(Modifier.size(48.dp))

                        PtzButton(
                            Icons.Default.KeyboardArrowRight,
                            "Pan right",
                            enabled = controlStream.supports(PtzCommand.PAN_RIGHT),
                        ) {
                            onPtz(PtzCommand.PAN_RIGHT)
                        }
                    }
                    Row {
                        Spacer(Modifier.size(48.dp))
                        PtzButton(
                            Icons.Default.KeyboardArrowDown,
                            "Tilt down",
                            enabled = controlStream.supports(PtzCommand.TILT_DOWN),
                        ) {
                            onPtz(PtzCommand.TILT_DOWN)
                        }
                        Spacer(Modifier.size(48.dp))
                    }
                }

            }
        }
    }
}

@Composable
private fun MapLocationCard(
    visualization: RemoteVisualization,
    enabled: Boolean,
    position: Pair<Double, Double>?,
) {
    OSHCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status = if (enabled) "started" else "unknown")
                Spacer(Modifier.width(10.dp))
                Text(
                    visualization.name.ifBlank { "Location" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.LocationOn, contentDescription = "Location")
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A)),
            ) {
                if (position == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Waiting for a location update",
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                } else {
                    LocationMap(position = position, label = visualization.name.ifBlank { "Location" })
                }
            }
        }
    }
}

@Composable
private fun LocationMap(
    position: Pair<Double, Double>,
    label: String
) {
    val context = LocalContext.current
    val mapView = remember(context) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE),
        )
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            isClickable = false
        }
    }
    val marker = remember(mapView) {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_location)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays += this
        }
    }
    var hasCentered by remember(mapView) { mutableStateOf(false) }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            val point = GeoPoint(position.first, position.second)
            marker.position = point
            marker.title = label
            if (!hasCentered) {
                map.controller.setZoom(16.0)
                map.controller.setCenter(point)
                hasCentered = true
            }
            map.invalidate()
        },
    )
}

@Composable
private fun VideoStreamCard(
    visualization: RemoteVisualization,
    playing: Boolean,
    error: String?,
    renderer: VideoStream,
    onPlayPause: (playing: Boolean) -> Unit,
) {
    OSHCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(
                    status = when {
                        playing -> "started"
                        error != null -> "error"
                        else -> "unknown"
                    },
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    visualization.name.ifBlank { "Video" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.Videocam, contentDescription = "Video stream")
            }
            Text(
                text = when {
                    playing -> "Live"
                    error != null -> "Disconnected: $error"
                    else -> "Paused"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A)),
            ) {
                VideoFrame(
                    renderer = renderer,
                    onSurfaceReady = {},
                    modifier = Modifier.fillMaxSize(),
                )
                if (!playing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC1A1A1A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Paused", color = Color.White.copy(alpha = 0.35f))
                    }
                }
                IconButton(
                    onClick = { onPlayPause(playing) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9000000)),
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause video" else "Play video",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun PtzButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha = if (enabled) 1f else .5f
    val fill = if (pressed) Brush.linearGradient(
        listOf(
            Color(0xCC1B6EC2),
            Color(0xCC1B6EC2)
        )
    ) else Brush.verticalGradient(listOf(Color(0xD92E343D), Color(0xD914171C)))
    Box(
        modifier
            .padding(2.dp)
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(fill, CircleShape)
            .border(1.dp, Color.White.copy(alpha = .33f * alpha), CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            description,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
    }
}
