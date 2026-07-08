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
import androidx.compose.foundation.layout.height
import org.sensorhub.android.ui.theme.*

private val PillShape = RoundedCornerShape(25)

@Composable
fun OSHButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary
        ),
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
        border = BorderStroke(1.dp, Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary
        ),
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

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ButtonPreview() {
    OSHTheme {
        Column {
            OSHButton(onClick = {}, text = "Primary Button")
            Spacer(modifier = Modifier.height(8.dp))
            OSHOutlinedButton(onClick = {}, text = "Outlined Button")
            Spacer(modifier = Modifier.height(8.dp))
            OSHTextButton(onClick = {}, text = "Text Button")
        }
    }
}
