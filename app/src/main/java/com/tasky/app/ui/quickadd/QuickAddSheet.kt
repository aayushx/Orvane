package com.tasky.app.ui.quickadd

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasky.app.core.ai.AiClient
import com.tasky.app.core.nl.NaturalLanguageDateParser
import com.tasky.app.ui.common.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    vm: QuickAddViewModel = hiltViewModel(),
) {
    val text by vm.text.collectAsState()
    val parsed by vm.parsed.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val aiMessage by vm.aiMessage.collectAsState()
    val aiSuggestion by vm.aiSuggestion.collectAsState()
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) vm.text.value = spoken
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Quick add", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Try “Pay rent tomorrow 5pm !High #work”. Tap the mic to add by voice.",
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
                trailingIcon = {
                    // Voice quick-add via the system speech recognizer.
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your task…")
                    }
                    androidx.compose.material3.IconButton(
                        onClick = {
                            runCatching { voiceLauncher.launch(intent) }
                                .onFailure {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Voice input unavailable on this device",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                        },
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Mic,
                            contentDescription = "Add task by voice",
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            ParsePreviewRow(parsed)
            Spacer(Modifier.height(10.dp))
            aiMessage?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            aiSuggestion?.let { suggestion ->
                Column {
                    Text(
                        "AI suggestion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                    for (sub in suggestion.subtasks) {
                        Text("• $sub", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            vm.applySuggestion(suggestion.title, suggestion.subtasks)
                            vm.clearAi()
                        }) { Text("Use suggestion") }
                        TextButton(onClick = { vm.clearAi() }) { Text("Ignore") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    enabled = text.isNotBlank() && !aiBusy,
                    onClick = { vm.askAi() },
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("AI assist")
                }
                if (aiBusy) {
                    CircularProgressIndicator(Modifier.width(16.dp).height(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { vm.save(onSaved = onDismiss) },
                    enabled = parsed?.title?.isBlank() == false,
                ) { Text("Add task") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun Modifier.semanticsVoiceDescription(): Modifier = this

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
