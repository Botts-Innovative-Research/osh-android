package org.sensorhub.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OnPrimary
import org.sensorhub.android.ui.theme.Secondary
import org.sensorhub.android.ui.theme.SecondaryContainer


@Composable
fun OSHFAB(
    onClick: ((Boolean) -> Unit)? = null,
    started: Boolean? = null,
    ) {
    var internalStarted by remember { mutableStateOf(false) }
    val isStarted = started ?: internalStarted
    val toggleStart = {
        val newValue = !isStarted
        if (onClick != null) {
            onClick(newValue)
        } else {
            internalStarted = newValue
        }
    }

    FloatingActionButton(
        onClick = { toggleStart() },
        containerColor = SecondaryContainer,
        contentColor = OnPrimary

    ) {

        Icon(
            imageVector = if (isStarted)
                Icons.Filled.Stop
            else
                Icons.Filled.PlayArrow,
            contentDescription = if (isStarted) "Stop SmartHub" else "Start SmartHub"
        )

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerItemsCard() {
    OSHTheme {
        Column {
            OSHFAB()
        }
    }
}