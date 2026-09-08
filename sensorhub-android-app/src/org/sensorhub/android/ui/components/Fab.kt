package org.sensorhub.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.OnPrimary
import org.sensorhub.android.ui.theme.SecondaryContainer


@Composable
fun OSHFAB(
    started: Boolean,
    onClick: () -> Unit,
    actionDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    FloatingActionButton(
        onClick = { if (enabled && !loading) onClick() },
        modifier = modifier.semantics {
            if (!enabled || loading) disabled()
            contentDescription = actionDescription
        },
        containerColor = SecondaryContainer,
        contentColor = OnPrimary

    ) {

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = OnPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (started) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null
            )
        }

    }
}

@Composable
fun OSHAddFAB(
    onClick: () -> Unit = {},
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = SecondaryContainer,
        contentColor = OnPrimary

    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add new item"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerItemsCard() {
    OSHTheme {
        Column {
            OSHFAB(started = false, onClick = {}, actionDescription = "Start Smart Hub")
            Spacer(modifier = Modifier.width(10.dp))
            OSHAddFAB()
        }
    }
}