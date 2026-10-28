package com.orvane.app.ui.quickadd

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvane.app.core.ai.AiClient
import com.orvane.app.core.nl.NaturalLanguageDateParser
import com.orvane.app.ui.common.Format
import com.orvane.app.ui.common.GrabHandle
import com.orvane.app.ui.theme.orvaneTokens
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    vm: QuickAddViewModel = hiltViewModel(),
) {
    val tokens = orvaneTokens()
    val text by vm.text.collectAsState()
    val parsed by vm.parsed.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val aiMessage by vm.aiMessage.collectAsState()
    val aiSuggestion by vm.aiSuggestion.collectAsState()
    val aiConfigured by vm.aiConfigured.collectAsState()
    val context = LocalContext.current
    var voiceError by remember { mutableStateOf<String?>(null) }

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

    val launchRecognizer: () -> Unit = launch@{
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your task…")
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure {
                android.widget.Toast.makeText(
                    context,
                    "Voice input unavailable on this device",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
    }

    // Request RECORD_AUDIO in-app BEFORE the recognizer: with the mic off, the
    // system sheet misattributes the denial to a listening failure ("Didn't
    // catch that") — QA finding. Ask first; if still denied, say exactly why.
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchRecognizer()
        } else {
            voiceError = "Microphone permission is off — enable it in Settings to add tasks by voice"
        }
    }

    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = tokens.corners.sheet, topEnd = tokens.corners.sheet)) {
        GrabHandle(Modifier.align(Alignment.CenterHorizontally))
        Column(Modifier.padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.sm)) {
            Text("Quick add", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(tokens.spacing.xs))
            Text(
                "Try “Pay rent tomorrow 5pm !High #work”. Tap the mic to add by voice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(tokens.spacing.md))
            OutlinedTextField(
                value = text,
                onValueChange = { vm.text.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_input")
                    .focusRequester(focusRequester),
                placeholder = { Text("New task") },
                maxLines = 3,
                trailingIcon = {
                    // Voice quick-add via the system speech recognizer; mic
                    // permission is requested in-app first (see micPermission).
                    androidx.compose.material3.IconButton(
                        onClick = {
                            voiceError = null
                            if (
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                launchRecognizer()
                            } else {
                                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
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
            // ModalBottomSheet content lives in a separate dialog window that attaches
            // after the caller composes; this effect only runs once this content is
            // actually in the tree, so the focus target exists by then.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Spacer(Modifier.height(tokens.spacing.lg))
            ParsePreviewRow(parsed)
            Spacer(Modifier.height(tokens.spacing.lg))
            aiMessage?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            voiceError?.let { msg ->
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
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        TextButton(onClick = {
                            vm.applySuggestion(suggestion.title, suggestion.subtasks)
                            vm.clearAi()
                        }) { Text("Use suggestion") }
                        TextButton(onClick = { vm.clearAi() }) { Text("Ignore") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Hidden entirely when unconfigured — never render a button that
                // can only error ("when configured" is the Settings promise).
                if (aiConfigured) {
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
                }
                if (aiBusy) {
                    CircularProgressIndicator(Modifier.width(16.dp).height(16.dp))
                }
                Spacer(Modifier.width(tokens.spacing.sm))
                Button(
                    onClick = { vm.save(onSaved = onDismiss) },
                    enabled = parsed?.title?.isBlank() == false,
                ) { Text("Add task") }
            }
            Spacer(Modifier.height(tokens.spacing.lg))
        }
    }
}

private fun Modifier.semanticsVoiceDescription(): Modifier = this

/** Live NL parsing preview while typing. */
@Composable
private fun ParsePreviewRow(parsed: NaturalLanguageDateParser.Parsed?) {
    val tokens = orvaneTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val title = parsed?.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            AssistChip(onClick = {}, label = { Text("\u270E " + title.take(24)) })
            Spacer(Modifier.width(tokens.spacing.xs))
        }
        val due = parsed?.due
        if (due != null) {
            val allDay = parsed.isAllDay
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "\uD83D\uDCC5 " +
                            Format.dueChip(due.toInstant().toEpochMilli(), allDay),
                    )
                },
            )
            Spacer(Modifier.width(tokens.spacing.xs))
        }
        if (parsed != null && parsed.priority > 0) {
            AssistChip(onClick = {}, label = { Text("\uD83D\uDEA9 " + Format.priorityName(parsed.priority)) })
            Spacer(Modifier.width(tokens.spacing.xs))
        }
        for (tag in parsed?.tags ?: emptyList()) {
            AssistChip(onClick = {}, label = { Text("#$tag") })
            Spacer(Modifier.width(tokens.spacing.xs))
        }
    }
}
