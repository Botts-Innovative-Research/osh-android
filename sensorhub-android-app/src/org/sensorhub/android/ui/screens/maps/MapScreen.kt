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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHTopAppBarWithLogo
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

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
