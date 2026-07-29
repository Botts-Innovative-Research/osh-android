package org.sensorhub.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.sensorhub.android.ui.theme.*
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton


private val PillShape = RoundedCornerShape(25)

@Composable
fun OSHButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary
        ),

    ) {
        Text(text)
    }
}

@Composable
fun OSHTonalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp,
            disabledElevation = 0.dp
        )
    ) {
        Text(text)
    }
}

@Composable
fun OSHOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
    ) {
        Text(text)
    }
}

@Composable
fun OSHElevatedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary
        ),
    ) {
        Text(text)
    }
}

@Composable
fun OSHTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = TextSecondary
        )
    ) {
        Text(text)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSHSegmentedButton(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndex: Int = 0,
    onOptionSelected: (Int) -> Unit = {}
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                modifier = Modifier.weight(1f),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = RoundedCornerShape(25)
                ),
                onClick = { onOptionSelected(index) },
                selected = index == selectedIndex,
                label = { Text(label, maxLines = 1) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Primary,
                    activeContentColor = OnPrimary,
                    activeBorderColor = Primary
                ),
                icon = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ButtonPreview() {
    OSHTheme {
        Column {
            OSHButton(onClick = {}, text = "Primary Button")
            OSHTonalButton(onClick = {}, text = "Secondary Button")
            Spacer(modifier = Modifier.height(8.dp))
            OSHOutlinedButton(onClick = {}, text = "Outlined Button")
            Spacer(modifier = Modifier.height(8.dp))
            OSHTextButton(onClick = {}, text = "Text Button")
            Spacer(modifier = Modifier.height(8.dp))
            OSHElevatedButton(onClick = {}, text = "Elevated Button")
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SegmentedButtonPreview() {
    OSHTheme {
        OSHSegmentedButton(
            options = listOf("CS API Client", "SOS-T Client")
        )
    }
}
