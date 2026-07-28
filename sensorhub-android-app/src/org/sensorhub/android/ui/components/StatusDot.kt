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

@Composable
fun StatusDot(
    status: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    val _status = status.lowercase()
    val color = when {
        _status.contains("started") -> Color(0xFF4CAF50)
        _status.contains("stopped") -> Color(0xFFEF5350)
        _status.contains("initializ" )|| _status.contains("starting") -> Color(0xFFFF9800)
        else -> Color(0xFF757575)
    }
    Box(
        modifier = modifier
            .size(dotSize)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}