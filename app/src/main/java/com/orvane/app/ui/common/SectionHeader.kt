package com.orvane.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orvane.app.ui.theme.orvaneTokens

/**
 * Section header with optional count and trailing action slot (Round 2 / C0).
 * "Today · 6" pattern; pass [countTint] (e.g. error) for overdue counts.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    countTint: Color? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = orvaneTokens()
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                start = tokens.spacing.md,
                end = tokens.spacing.md,
                top = tokens.spacing.lg,
                bottom = tokens.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count != null) {
            Spacer(Modifier.width(tokens.spacing.sm))
            Text(
                "· $count",
                style = MaterialTheme.typography.titleSmall,
                color = countTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}
