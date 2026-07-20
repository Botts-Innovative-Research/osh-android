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

    val color = when (status) {
        "started" -> Color.Green
        "initialized", "starting" -> Color.Yellow
        "stopped" -> Color.Red
        "unknown" -> Color.Gray
        else -> Color.Gray
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