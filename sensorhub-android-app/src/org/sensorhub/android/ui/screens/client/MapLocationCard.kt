package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import org.sensorhub.android.R
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.data.client.StreamStatus
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.StatusDot
import org.sensorhub.android.ui.screens.maps.MapScreen
import org.sensorhub.android.ui.theme.ControlSurface
import org.sensorhub.android.ui.theme.OSHShapes
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OshDimensions

@Composable
fun MapLocationCard(
    visualization: RemoteVisualization,
    status: StreamStatus,
    position: Pair<Double, Double>?
) {
    val location = stringResource(R.string.system_detail_location)
    OSHCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OshDimensions.screenHorizontal)
    ) {
        Column(Modifier.padding(OshDimensions.cardContent)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status.dotStatus()); Spacer(
                Modifier.width(OshDimensions.titleGap)
            ); Text(
                visualization.name.ifBlank { location },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            ); Icon(Icons.Filled.LocationOn, contentDescription = location)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = OshDimensions.contentGap)
                    .aspectRatio(16f / 9f)
                    .clip(OSHShapes.medium)
                    .background(ControlSurface)
            ) {
                if (position == null) Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(status.message(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else LocationMap(position, visualization.name.ifBlank { location })
            }
        }
    }
}

@Composable
private fun LocationMap(position: Pair<Double, Double>, label: String) {
    val context = LocalContext.current
    val mapView = remember(context) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
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
            icon = ContextCompat.getDrawable(
                context,
                R.drawable.ic_location
            )
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays += this
        }
    }
    var centered by remember(mapView) { mutableStateOf(false) }
    DisposableEffect(mapView) {
        mapView.onResume();
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { map ->
        val point = GeoPoint(position.first, position.second)
        marker.position = point
        marker.title = label
        marker.snippet = "${position.first},${position.second}"
        if (!centered) {
            map.controller.setZoom(16.0)
            map.controller.setCenter(point)
            centered = true
    }
        map.invalidate()
    }
}
