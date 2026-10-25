package com.tasky.app.ui.quickadd

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasky.app.core.nl.NaturalLanguageDateParser
import com.tasky.app.ui.common.Format
import com.tasky.app.ui.tasks.TasksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    vm: QuickAddViewModel,
) {
    val text by vm.text.collectAsState()
    val parsed by vm.parsed.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Quick add", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Try “Pay rent tomorrow 5pm !High #work”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { vm.text.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("New task") },
                maxLines = 3,
            )
            Spacer(Modifier.height(10.dp))
            ParsePreviewRow(parsed)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { vm.save(onSaved = onDismiss) },
                    enabled = parsed?.title?.isBlank() == false,
                ) { Text("Add task") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Live NL parsing preview while typing. */
@Composable
private fun ParsePreviewRow(parsed: NaturalLanguageDateParser.Parsed?) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val title = parsed?.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            AssistChip(onClick = {}, label = { Text("\u270E " + title.take(24)) })
            Spacer(Modifier.width(6.dp))
        }
        val due = parsed?.due
        if (due != null) {
            val allDay = parsed.isAllDay
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "\uD83D\uDCC5 " +
                            Format.dueChip(due.toInstant().toEpochMilli(), !allDay),
                    )
                },
            )
            Spacer(Modifier.width(6.dp))
        }
        if (parsed != null && parsed.priority > 0) {
            AssistChip(onClick = {}, label = { Text("\uD83D\uDEA9 " + Format.priorityName(parsed.priority)) })
            Spacer(Modifier.width(6.dp))
        }
        for (tag in parsed?.tags ?: emptyList()) {
            AssistChip(onClick = {}, label = { Text("#$tag") })
            Spacer(Modifier.width(6.dp))
        }
    }
}
