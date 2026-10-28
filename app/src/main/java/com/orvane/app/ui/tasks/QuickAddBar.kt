package com.orvane.app.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvane.app.core.nl.NaturalLanguageDateParser
import com.orvane.app.ui.common.Format
import com.orvane.app.ui.quickadd.QuickAddViewModel
import com.orvane.app.ui.theme.orvaneTokens

/**
 * Persistent quick-add bar (Round 2 / C1): sits above the bottom navigation,
 * parses natural language as you type ("→ tomorrow 17:00 · !High · #work"),
 * saves on Enter or send. The FAB still opens the full editor sheet.
 */
@Composable
internal fun QuickAddBar() {
    val tokens = orvaneTokens()
    val vm: QuickAddViewModel = hiltViewModel()
    val text by vm.text.collectAsState()
    val parsed by vm.parsed.collectAsState()
    val canSave = parsed?.title?.isBlank() == false

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = tokens.spacing.md,
                end = tokens.spacing.md,
                top = tokens.spacing.sm,
                bottom = tokens.spacing.md,
            ),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { vm.text.value = it },
            placeholder = {
                Text(
                    "Add task — try “tomorrow 5pm !High #work”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(50),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canSave) vm.save(onSaved = {}) }),
            trailingIcon = {
                IconButton(
                    enabled = canSave,
                    onClick = { vm.save(onSaved = {}) },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Add task",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (parsed != null) {
            previewLine(parsed)?.let { preview ->
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = tokens.spacing.lg,
                        top = tokens.spacing.xs,
                    ),
                )
            }
        }
    }
}

/** "→ tomorrow 17:00 · !High · #work" for the live parse; null when nothing parsed. */
private fun previewLine(parsed: NaturalLanguageDateParser.Parsed?): String? {
    if (parsed == null) return null
    val parts = mutableListOf<String>()
    parsed.due?.let {
        parts.add("→ " + Format.dueChip(it.toInstant().toEpochMilli(), parsed.isAllDay))
    }
    if (parsed.priority > 0) parts.add("!${Format.priorityName(parsed.priority)}")
    for (tag in parsed.tags) parts.add("#$tag")
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
}
