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
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .background(
                color = if (isOnline) Color.Green else Color.Gray,
                shape = CircleShape
            )
    )
}