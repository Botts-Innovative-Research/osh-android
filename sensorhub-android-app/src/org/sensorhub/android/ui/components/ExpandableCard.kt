package org.sensorhub.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sensorhub.android.ui.components.OSHExpandableSwitchCard
import org.sensorhub.android.ui.theme.*

@Composable
fun OSHExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onExpandChange: ((Boolean) -> Unit)? = null,
    status: String? = null,
    collapsedContent: @Composable ColumnScope.() -> Unit = {},
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val toggleExpanded = {
        val newValue = !isExpanded
        if (onExpandChange != null) {
            onExpandChange(newValue)
        } else {
            internalExpanded = newValue
        }
    }

    Card(
        onClick = { toggleExpanded() },
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryContainer,
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                status?.let {
                    StatusDot(status = it)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { toggleExpanded() }) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Filled.KeyboardArrowUp
                        else
                            Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            collapsedContent()

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    expandedContent()
                }
            }
        }
    }
}

@Composable
fun OSHExpandableSwitchCard(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    configHint: String = "",
    expanded: Boolean? = null,
    onExpandChange: ((Boolean) -> Unit)? = null,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val toggleExpanded = {
        val newValue = !isExpanded
        if (onExpandChange != null) {
            onExpandChange(newValue)
        } else {
            internalExpanded = newValue
        }
    }

    Card(
        onClick = { toggleExpanded() },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryContainer,
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                OSHSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }

            if (configHint.isNotBlank() && !isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = configHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    expandedContent()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ExpandableSwitchCardPreview() {
    OSHTheme {
        Column {
            OSHExpandableSwitchCard(
                title = "Polar Heart Monitor",
                subtitle = "Connect to a Polar heart rate sensor over Bluetooth LE",
                checked = true,
                onCheckedChange = {},
                expanded = true,
                expandedContent = {
                }

            )
            Spacer(modifier = Modifier.height(8.dp))
            OSHExpandableSwitchCard(
                title = "Accelerometer",
                subtitle = "Stream real-time accelerometer motion data",
                checked = false,
                configHint = "Tap to configure",
                onCheckedChange = {},
                expandedContent = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ExpandableCardCollapsedPreview() {
    OSHTheme {
        OSHExpandableCard(
            title = "GPS Sensor",
            collapsedContent = {
                Text(
                    text = "Streaming location data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            expandedContent = {
                Text("Latitude: 34.0522")
                Text("Longitude: -118.2437")
                Text("Altitude: 71m")
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ExpandableCardExpandedPreview() {
    OSHTheme {
        OSHExpandableCard(
            title = "Accelerometer",
            expanded = true,
            collapsedContent = {
                Text(
                    text = "Motion sensor active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            expandedContent = {
                Text("X: 0.023 m/s²")
                Text("Y: 9.81 m/s²")
                Text("Z: -0.15 m/s²")
            }
        )
    }
}
