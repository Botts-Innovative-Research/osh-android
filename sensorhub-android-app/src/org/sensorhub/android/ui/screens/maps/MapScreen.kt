package org.sensorhub.android.ui.screens.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.sensorhub.android.R
import org.sensorhub.android.data.client.OshMapStore
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun MapScreen(
    onNavigateToSettings : () -> Unit,
) {
    val context = LocalContext.current
    val remoteTracks by OshMapStore.tracks.collectAsState()

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
    val clientOverlays = remember(mapView) { mutableListOf<Overlay>() }

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
                    map.overlays.removeAll(clientOverlays.toSet())
                    clientOverlays.clear()
                    remoteTracks.values.forEach { track ->
                        if (track.trail.size > 1) {
                            val trail = Polyline(map).apply {
                                setPoints(track.trail.map { (lat, lon) -> GeoPoint(lat, lon) })
                                outlinePaint.color = android.graphics.Color.rgb(255, 145, 0)
                                outlinePaint.strokeWidth = 6f
                            }
                            clientOverlays += trail
                            map.overlays += trail
                        }
                        val marker = Marker(map).apply {
                            position = GeoPoint(track.latitude, track.longitude)
                            title = track.label
                            snippet = "${track.latitude} , ${track.longitude}"
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_location)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        clientOverlays += marker
                        map.overlays += marker
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
            {}
        )
    }
}
