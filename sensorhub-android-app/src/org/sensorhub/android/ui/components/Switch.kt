package org.sensorhub.android.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.sensorhub.android.ui.theme.*


@Composable
fun OSHSwitch(
    enabled: Boolean = true,
    checked: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = OnPrimary,
            checkedTrackColor = PrimaryContainer,
            uncheckedThumbColor = Secondary,
            uncheckedTrackColor = SecondaryContainer,
        )    )
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SwitchPreview() {
    var checked by remember { mutableStateOf(true) }

    OSHTheme {
        Column {
            OSHSwitch(true, checked,  onCheckedChange = {
                checked = it
            })
        }
    }
}
