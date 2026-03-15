package dev.o3000y.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> LabeledDropdown(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box {
            Text(
                options.firstOrNull { it.first == selected }?.second ?: "",
                fontSize = 13.sp,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text, fontSize = 13.sp) },
                        onClick = { onSelect(value); expanded = false },
                    )
                }
            }
        }
    }
}
