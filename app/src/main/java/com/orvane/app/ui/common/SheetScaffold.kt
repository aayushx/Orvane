package com.orvane.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Bottom-sheet scaffold (Round 2 / C0/C6): grab handle + 28dp top radius +
 * consistent title row. Content flows in a Column; buttons below content
 * belong to each sheet, keep them right-aligned with `Alignment.End`.
 */
@Composable
fun OrvaneSheet(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = orvaneTokens()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = tokens.corners.sheet,
            topEnd = tokens.corners.sheet,
        ),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth()) {
            GrabHandle(Modifier.align(Alignment.CenterHorizontally))
            if (!title.isNullOrEmpty()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(
                            start = tokens.spacing.lg,
                            end = tokens.spacing.lg,
                            bottom = tokens.spacing.sm,
                        ),
                )
            }
            content()
        }
    }
}

/** Grab handle strip shown at the top of every bottom sheet. */
@Composable
fun GrabHandle(modifier: Modifier = Modifier) {
    val tokens = orvaneTokens()
    Box(modifier.padding(vertical = tokens.spacing.sm)) {
        Box(
            Modifier
                .width(32.dp)
                .height(4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().height(4.dp),
            ) {}
        }
    }
}
