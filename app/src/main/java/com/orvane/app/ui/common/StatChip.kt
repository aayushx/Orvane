package com.orvane.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orvane.app.ui.theme.orvaneTokens

/**
 * Small tonal stat pill ("12", "day streak") used in hero rows (Round 2 / C0/C2).
 */
@Composable
fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = orvaneTokens()
    Surface(
        shape = RoundedCornerShape(tokens.corners.card),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(
                horizontal = tokens.spacing.md,
                vertical = tokens.spacing.sm,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Convenience row laying out equal chips with consistent gaps. */
@Composable
fun StatChipRow(
    chips: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    val tokens = orvaneTokens()
    Row(
        modifier.padding(horizontal = tokens.spacing.md),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        chips.forEachIndexed { i, chip ->
            if (i > 0) androidx.compose.foundation.layout.Spacer(Modifier.width(tokens.spacing.sm))
            chip()
        }
    }
}
