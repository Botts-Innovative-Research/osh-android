package org.sensorhub.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OshDimensions

@Composable
fun OSHCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
            .padding(vertical = OshDimensions.cardOuterVertical),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
    OSHCard(modifier = modifier.fillMaxWidth().padding(horizontal = OshDimensions.screenHorizontal, vertical = OshDimensions.cardOuterVertical)) {
        OSHClickableRowWithIcon(
            title = title,
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier,
            onClick = onClick
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OshDimensions.screenHorizontal, vertical = OshDimensions.cardOuterVertical)
            .then(
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun OSHEditActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    additionalActions: @Composable ColumnScope.() -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val cardModifier = if (onLongClick != null) {
        modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        modifier
    }

    OSHCard(
        modifier = cardModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onClick
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription
                )
            }
        }

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
private fun ServerItemsCard() {
    OSHTheme {
        Column {
            OSHActionCard(
                title = "Local Server",
                subtitle = "https://localhost:8080/sensorhub/api",
                checked = true,
                onCheckedChange = {},
                onClick = {},
                imageVector = Icons.Filled.Edit,
                contentDescription = "Update Server"
            )

            OSHEditActionCard(
                title = "Local Server",
                subtitle = "https://localhost:8080/sensorhub/api",
                onClick = {},
                imageVector = Icons.Filled.Edit,
                contentDescription = "Update Server"
            )
        }
    }
}
