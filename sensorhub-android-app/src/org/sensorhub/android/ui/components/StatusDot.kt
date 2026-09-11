package org.sensorhub.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.sensorhub.android.R

@Composable
fun StatusDot(
    status: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    val _status = status.trim().lowercase()
    val color = when {
        _status == "ok" || _status == "started" -> Color(0xFF4CAF50)
        _status == "nok" -> Color(0xFFFF9800)
        _status.contains("error") -> Color(0xFFEF5350)
        _status.contains("starting") || _status.contains("initializ") -> Color(0xFFFF9800)
        else -> Color(0xFF757575)
    }
    val description = stringResource(when {
        _status == "ok" || _status == "started" -> R.string.health_active
        _status == "nok" -> R.string.health_attention
        _status.contains("error") -> R.string.health_error
        _status.contains("stopping") -> R.string.health_stopping
        _status.contains("stop") -> R.string.health_stopped
        _status.contains("starting") || _status.contains("initializ") -> R.string.health_starting
        else -> R.string.health_unknown
    })
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .size(dotSize)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}
