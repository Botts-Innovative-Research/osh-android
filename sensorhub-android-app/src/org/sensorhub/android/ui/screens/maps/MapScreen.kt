package org.sensorhub.android.ui.screens.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.sensorhub.android.R
import org.sensorhub.android.data.client.RemoteTrack
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

private data class TrackOverlays(
    val marker: Marker,
    var trail: Polyline? = null,
)

@Composable
fun MapScreen(
    onNavigateToSettings : () -> Unit,
    tracks: StateFlow<Map<String, RemoteTrack>>,
) {
    val context = LocalContext.current
    val remoteTracks by tracks.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(context, lifecycle) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setDestroyMode(false)
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(33.4484, -86.7987))
        }
    }
    val trackOverlays = remember(mapView) { mutableMapOf<String, TrackOverlays>() }

    LaunchedEffect(remoteTracks.keys) {
        remoteTracks.values.firstOrNull()?.let { track ->
            mapView.controller.animateTo(GeoPoint(track.latitude, track.longitude))
        }
    }

    DisposableEffect(mapView, lifecycle) {
        var resumed = false
        fun setResumed(value: Boolean) {
            if (resumed == value) return
            resumed = value
            if (value) mapView.onResume() else mapView.onPause()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> setResumed(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> setResumed(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        setResumed(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycle.removeObserver(observer)
            setResumed(false)
            mapView.onDetach()
        }
    }


    Scaffold(
        topBar = {
            OSHTopAppBarWithLogo(
                title = stringResource(R.string.tab_map),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                },
            )
        },
        containerColor = Background
    ) { padding ->
        key(mapView) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                factory = { mapView },
                update = { map ->
                    (trackOverlays.keys - remoteTracks.keys).forEach { streamId ->
                        trackOverlays.remove(streamId)?.let { overlays ->
                            map.overlays.remove(overlays.marker)
                            overlays.trail?.let(map.overlays::remove)
                        }
                    }
                    remoteTracks.values.forEach { track ->
                        val overlays = trackOverlays.getOrPut(track.streamId) {
                            TrackOverlays(
                                marker = Marker(map).apply {
                                    icon = ContextCompat.getDrawable(context, R.drawable.ic_location)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    map.overlays += this
                                },
                            )
                        }
                        overlays.marker.position = GeoPoint(track.latitude, track.longitude)
                        overlays.marker.title = track.label
                        overlays.marker.snippet = "${track.latitude}, ${track.longitude}"
                        if (track.trail.size > 1) {
                            val trail = overlays.trail ?: Polyline(map).also {
                                it.outlinePaint.color = android.graphics.Color.rgb(255, 145, 0)
                                it.outlinePaint.strokeWidth = 6f
                                map.overlays += it
                                overlays.trail = it
                            }
                            trail.setPoints(track.trail.map { (lat, lon) -> GeoPoint(lat, lon) })
                        } else {
                            overlays.trail?.let(map.overlays::remove)
                            overlays.trail = null
                        }
                    }
                    map.invalidate()
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun MapScreenPreview() {
    OSHTheme {
        MapScreen(
            onNavigateToSettings = {},
            tracks = remember { MutableStateFlow(emptyMap()) },
        )
    }
}
