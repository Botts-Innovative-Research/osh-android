package org.sensorhub.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun <T> OSHDropDown(
    title: String,
    summary: String,
    items: List<T>,
    selectedIds: Set<String>,
    itemId: (T) -> String,
    itemLabel: (T) -> String,
    itemSummary: (T) -> String,
    onSelectionChange: (String, Boolean) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val menuWidth = with(LocalDensity.current) { anchorWidth.toDp() }

    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().onSizeChanged { anchorWidth = it.width }) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(summary, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(menuWidth)
            ) {
                if (items.isEmpty()) {
                    DropdownMenuItem(text = { Text(emptyText) }, onClick = {}, enabled = false)
                }
                items.forEach { item ->
                    val id = itemId(item)
                    val checked = id in selectedIds
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemLabel(item),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = itemSummary(item),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                        modifier = Modifier.semantics { selected = checked },
                        onClick = { onSelectionChange(id, !checked) }
                    )
                }
            }
        }
    }
}
