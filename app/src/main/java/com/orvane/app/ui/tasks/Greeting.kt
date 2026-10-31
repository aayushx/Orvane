package com.orvane.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orvane.app.ui.theme.orvaneTokens
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Time-of-day buckets for the greeting header (pure; unit-tested — Round 2 / C1). */
enum class GreetingPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

/** Maps an hour of day (0–23) to its greeting period. */
fun greetingPeriod(hour: Int): GreetingPeriod = when (hour) {
    in 5..11 -> GreetingPeriod.MORNING
    in 12..16 -> GreetingPeriod.AFTERNOON
    in 17..21 -> GreetingPeriod.EVENING
    else -> GreetingPeriod.NIGHT
}

/** "Good morning" (+ ", Name" when a display name is configured in settings). */
fun greetingText(period: GreetingPeriod, displayName: String?): String {
    val base = when (period) {
        GreetingPeriod.MORNING -> "Good morning"
        GreetingPeriod.AFTERNOON -> "Good afternoon"
        GreetingPeriod.EVENING -> "Good evening"
        GreetingPeriod.NIGHT -> "Working late"
    }
    return if (displayName.isNullOrBlank()) base else "$base, $displayName"
}

/** Human day label for the greeting sub-line, e.g. "Tuesday, October 29". */
fun dayLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

/** Live mini-stats for the greeting card (v1.2.0 / E3). */
data class GreetingStats(
    val dueToday: Int = 0,
    val completedToday: Int = 0,
    val habitsDoneToday: Int = 0,
)

/**
 * Today-page greeting card (v1.2.0 / E3): time-based greeting + full date on a
 * primaryContainer tonal surface (24dp radius) with live mini-stat pills.
 */
@Composable
fun GreetingHeader(
    displayName: String?,
    stats: GreetingStats,
    modifier: Modifier = Modifier,
) {
    val tokens = orvaneTokens()
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val period = greetingPeriod(java.time.LocalTime.now(zone).hour)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
        ),
    ) {
        Column(Modifier.padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
            Text(
                greetingText(period, displayName),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                dayLabel(now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = tokens.spacing.xs),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = tokens.spacing.sm),
            ) {
                GreetingStatPill("${stats.dueToday} due today")
                GreetingStatPill("${stats.completedToday} done")
                GreetingStatPill("${stats.habitsDoneToday} habits")
            }
        }
    }
}

/** One mini-stat pill: on-primary-container tonal on the card's surface. */
@Composable
private fun GreetingStatPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
