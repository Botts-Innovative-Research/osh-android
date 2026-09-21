package org.sensorhub.android.ui.screens.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

data class Location(
    val latitude: Double,
    val longitude: Double
)

@Composable
fun MapScreen(
    onNavigateToSettings : () -> Unit,
) {
    val context = LocalContext.current

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

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_location)
            title = context.getString(R.string.map_this_device)
        }
    }
    var centeredOnDevice by remember(mapView) { mutableStateOf(false) }
    val fix = Location(
        latitude = 34.58388,
        longitude = -86.464493
    )

    LaunchedEffect(mapView) {
        if (fix == null) {
            marker.closeInfoWindow()
            mapView.overlays.remove(marker)
            centeredOnDevice = false
        } else {
            marker.position = GeoPoint(fix.latitude, fix.longitude)
            marker.alpha = 1f
            marker.snippet = "${fix.latitude}, ${fix.longitude}"
            if (!mapView.overlays.contains(marker)) mapView.overlays.add(marker)
            if (!centeredOnDevice) {
                mapView.controller.setCenter(marker.position)
                centeredOnDevice = true
            }
        }
        mapView.invalidate()
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            key(mapView) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                tonalElevation = 3.dp,
            ) {
                IconButton(
                    enabled = fix != null,
                    onClick = {
                        fix?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
                    },
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_recenter))
                }
            }
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
