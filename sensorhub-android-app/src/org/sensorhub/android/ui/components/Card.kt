package org.sensorhub.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.SecondaryContainer
import org.sensorhub.android.ui.theme.TextPrimary

@Composable
fun OSHCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryContainer,
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
        ),
        content = content
    )
}


@Composable
fun OSHSensorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryContainer,
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
        ),
        content = content
    )
}
@Composable
fun OSHClickableCardWithIcon(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String
) {
    OSHCard()
     {
        OSHClickableRowWithIcon(
            title = title,
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            onClick = onClick
        )
    }
}

@Composable
fun OSHClickableCard(
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    OSHCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(30.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))

            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}


@Composable
fun OSHSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OSHCard(modifier = modifier) {
        OSHSwitchRow(
            title = title,
            subtitle = subtitle,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun OSHButtonCard(
    title: String,
    buttonText: String,
    onClick: () -> Unit,
    status: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
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
       OSHStatusButtonRow(
           title = title,
           buttonText = buttonText,
           onClick = onClick,
           status = status,
           enabled = enabled
       )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun OSHActionCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    additionalActions: @Composable ColumnScope.() -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    OSHCard(
        modifier = modifier.then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
            } else Modifier
        )
    ) {
        OSHSwitchRow(
            title = title,
            subtitle = subtitle,
            checked = checked,
            onCheckedChange = onCheckedChange,
            trailingContent = {
                IconButton(onClick = { onClick() }) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = contentDescription
                    )
                }
            }
        )
        additionalActions()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun DeviceInfoCardPreview() {
    OSHTheme {
        OSHCard {
            OSHInfoRow(
                label = "Device Name",
                value = "MyDevice",
                onClick = {}
            )
            OSHInfoRow(
                label = "Device IP Address",
                value = "192.168.1.42"
            )
            OSHInfoRow(
                label = "Version",
                value = "1.0.0"
            )
            OSHClickableRowWithIcon(
                title = "Language",
                imageVector = Icons.Default.Language,
                contentDescription = "Select in app language"
            )

            OSHStatusRow(
                title = "SOS Service Status",
                subtitle = "Connected"
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CardPreview() {
    OSHTheme {
        Column {
            OSHCard {
                Text(
                    text = "Card content goes here",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SwitchCardPreview() {
    OSHTheme {
        Column {
            OSHSwitchCard(
                title = "Accelerometer",
                subtitle = "Stream real-time accelerometer motion data",
                checked = true,
                onCheckedChange = {}
            )
            OSHSwitchCard(
                title = "GPS",
                subtitle = "Stream location data",
                checked = false,
                onCheckedChange = {}
            )
            OSHSwitchCard(
                title = "Connected Systems API",
                subtitle = "OGC Standard REST API service this devices sensor data",
                checked = true,
                onCheckedChange = {}
            )
            OSHSwitchCard(
                title = "Discovery Service",
                subtitle = "Service providfcing discovery based on definable rulesets",
                checked = false,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SettingsItemPreview() {
    OSHTheme {
        Column {
            OSHClickableCard(
                onClick = {},
                title = "Manage Sensors",
                subtitle = "1 of 2 server(s) enabled",
            )
            OSHClickableCardWithIcon (
                onClick = {},
                title = "Language",
                imageVector = Icons.Default.Language,
                contentDescription = "Select in app language"
            )
            OSHButtonCard(
                onClick = {},
                title = "SmartHub",
                buttonText = "Start Streaming",
                status = "Connected",
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerItemsCard() {
    OSHTheme {
        Column {
            OSHActionCard(
                title = "Local Server",
                subtitle = "http:localhost:8080/sensorhub/api",
                checked = true,
                onCheckedChange = {},
                 onClick = {},
                imageVector = Icons.Filled.Edit,
                contentDescription = "Update Server"
            )
        }
    }
}
