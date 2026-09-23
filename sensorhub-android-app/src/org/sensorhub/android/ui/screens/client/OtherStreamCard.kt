package org.sensorhub.android.ui.screens.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.sensorhub.android.R
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.data.client.StreamStatus
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.StatusDot
import org.sensorhub.android.ui.theme.OshDimensions

@Composable
fun OtherStreamCard(
    visualization: RemoteVisualization,
    values: Map<String, String>,
    status: StreamStatus
) {
    OSHCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(OshDimensions.cardContent)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status.dotStatus()); Spacer(Modifier.width(OshDimensions.titleGap))
                Text(
                    visualization.name.ifBlank { stringResource(R.string.system_detail_datastream) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (values.isEmpty()) Text(
                status.message(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = OshDimensions.compactGap)
            )
            else {
                Text(
                    pluralStringResource(
                        R.plurals.system_detail_value_count,
                        values.size,
                        values.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = OshDimensions.compactGap)
                )
                values.toSortedMap().forEach { (field, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = OshDimensions.compactGap),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            field,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

fun StreamStatus.dotStatus() = when (this) {
    StreamStatus.RECEIVING -> "started"; StreamStatus.DISCONNECTED -> "error"; else -> "unknown"
}

@Composable
fun StreamStatus.message() = when (this) {
    StreamStatus.CONNECTING -> stringResource(R.string.system_detail_waiting_for_observations)
    StreamStatus.RECEIVING -> stringResource(R.string.system_detail_receiving_observations)
    StreamStatus.DISCONNECTED -> stringResource(R.string.system_detail_disconnected)
    StreamStatus.PAUSED -> stringResource(R.string.system_detail_paused)
}
