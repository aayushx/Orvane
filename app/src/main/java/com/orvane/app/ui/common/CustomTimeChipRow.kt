@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orvane.app.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Preset-hour chips with a "Custom…" escape hatch that opens a Material time
 * picker (Round 2 / B1: no time-of-day setting may offer only fixed presets).
 *
 * Preset chips apply the currently-configured minute as well; when the current
 * time is a non-preset combination, a selected chip with the exact time is
 * rendered so the configured value is always visible.
 */
@Composable
fun CustomTimeChipRow(
    presetHours: List<Int>,
    currentHour: Int,
    currentMinute: Int,
    onSelect: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val customActive = currentHour !in presetHours || currentMinute != 0

    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (h in presetHours) {
            FilterChip(
                selected = currentHour == h && currentMinute == 0,
                onClick = { onSelect(h, currentMinute) },
                label = { Text("%02d:%02d".format(h, currentMinute)) },
            )
        }
        if (customActive) {
            FilterChip(
                selected = true,
                onClick = { showPicker = true },
                label = { Text("%02d:%02d".format(currentHour, currentMinute)) },
            )
        }
        FilterChip(
            selected = false,
            onClick = { showPicker = true },
            label = { Text("Custom…") },
        )
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pick time") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    onSelect(state.hour, state.minute)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }
}
