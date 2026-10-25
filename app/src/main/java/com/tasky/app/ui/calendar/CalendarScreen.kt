package com.tasky.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tasky.app.core.util.Time
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month overview (Phase 2 will add task dots per day, the week time grid and
 * drag-to-schedule). Fully navigable month grid with today / selection.
 */
@Composable
fun CalendarScreen() {
    var month by remember { mutableStateOf(YearMonth.now(Time.zone())) }
    var selected by remember { mutableStateOf(LocalDate.now(Time.zone())) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.year,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous month")
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            for (dow in listOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY,
                java.time.DayOfWeek.SATURDAY,
                java.time.DayOfWeek.SUNDAY,
            )) {
                Text(
                    dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val today = LocalDate.now(Time.zone())
        MonthGrid(month, today, selected, onSelected = { selected = it })
        Spacer(Modifier.height(12.dp))
        Text(
            "${selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, ${selected.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${selected.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Schedule blocks and calendar sync arrive with Phase 2 — task dots will appear on this calendar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekRow(days: List<LocalDate?>, today: LocalDate, selected: LocalDate, onSelected: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp)) {
        for (i in 0 until 7) {
            val day = days.getOrNull(i)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (day != null) DayCell(day, today, selected, onSelected)
            }
        }
    }
}

@Composable
fun MonthGrid(month: YearMonth, today: LocalDate, selected: LocalDate, onSelected: (LocalDate) -> Unit) {
    val firstDow = month.atDay(1).dayOfWeek.value - 1
    val rows = mutableListOf<List<LocalDate?>>()
    var current = mutableListOf<LocalDate?>()
    repeat(firstDow) { current.add(null) }
    for (d in 1..month.lengthOfMonth()) {
        current.add(month.atDay(d))
        if (current.size == 7) {
            rows.add(current)
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) {
        while (current.size < 7) current.add(null)
        rows.add(current)
    }
    Column {
        for (row in rows) {
            WeekRow(row, today, selected, onSelected)
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, today: LocalDate, selected: LocalDate, onSelected: (LocalDate) -> Unit) {
    val isToday = day == today
    val isSelected = day == selected
    Box(
        Modifier
            .padding(2.dp)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .size(32.dp)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                CircleShape,
            )
            .clickable { onSelected(day) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
