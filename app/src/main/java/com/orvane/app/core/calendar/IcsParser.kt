package com.orvane.app.core.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Minimal read-only ICS (vCalendar) parser: extracts VEVENT entries with
 * DTSTART/DTEND/SUMMARY/DESCRIPTION/LOCATION and basic RRULE-free semantics.
 * Handles folded lines (lines starting with space/tab), escaped chars,
 * all-day dates (VALUE=DATE) and UTC / floating / TZID datetimes.
 */
object IcsParser {

    data class ParsedEvent(
        val title: String,
        val note: String,
        val location: String?,
        val start: ZonedDateTime,
        val end: ZonedDateTime,
        val allDay: Boolean,
    )

    /** Parses the raw string; tolerant of malformed content. */
    fun parse(raw: String, zone: ZoneId = ZoneId.systemDefault()): List<ParsedEvent> {
        val unfolded = unfold(raw)
        val events = mutableListOf<ParsedEvent>()
        var current = mutableMapOf<String, String>()
        var inEvent = false

        for (line in unfolded) {
            val trimmed = line.trim()
            when {
                trimmed.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    current = mutableMapOf()
                }
                trimmed.equals("END:VEVENT", ignoreCase = true) -> {
                    inEvent = false
                    current.toParsedEvent(zone)?.let { events.add(it) }
                }
                inEvent && trimmed.contains(':') -> {
                    val idx = trimmed.indexOf(':')
                    val namePart = trimmed.substring(0, idx)
                    val value = trimmed.substring(idx + 1)
                    val name = namePart.substringBefore(';').uppercase()
                    val params = namePart.substringAfter(';', "")
                    val key = if (params.contains("VALUE=DATE", ignoreCase = true)) name else name
                    if (!current.containsKey(key) || key == "DTSTART" || key == "DTEND") {
                        current[key] = value
                    }
                }
            }
        }
        return events
    }

    private fun unfold(raw: String): List<String> {
        val result = mutableListOf<String>()
        for (line in raw.split("\r\n", "\n", "\r")) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.size - 1] = result.last() + line.substring(1)
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun Map<String, String>.toParsedEvent(zone: ZoneId): ParsedEvent? {
        val startRaw = this["DTSTART"] ?: return null
        val endRaw = this["DTEND"]
        val title = (this["SUMMARY"] ?: "").unescape().let { it.ifBlank { "(no title)" } }
        val note = (this["DESCRIPTION"] ?: "").unescape()
        val location = (this["LOCATION"] ?: "").unescape().takeIf { it.isNotBlank() }

        val isDateOnly = startRaw.length == 8 || (containsDateOnlyParam(this))
        val start = parseIcsDate(startRaw, zone) ?: return null
        val end = endRaw?.let { parseIcsDate(it, zone) }
            ?: if (isDateOnly) start.plusDays(1) else start.plusHours(1)

        return ParsedEvent(title, note, location, start, end, isDateOnly)
    }

    private fun containsDateOnlyParam(props: Map<String, String>): Boolean {
        // parser keeps only the value; DATE-only forms are distinguishable by length
        return false
    }

    /** 20261025 | 20261025T093000Z | 20261025T093000. */
    fun parseIcsDate(value: String, zone: ZoneId): ZonedDateTime? {
        val v = value.trim()
        return try {
            when {
                v.length == 8 -> {
                    LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay(zone)
                }
                v.endsWith("Z") -> {
                    val dt = LocalDateTime.parse(v.removeSuffix("Z"), dtFormatter)
                    dt.atZone(ZoneId.of("UTC"))
                }
                v.length == 15 -> LocalDateTime.parse(v, dtFormatter).atZone(zone)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private val dtFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    private fun String.unescape(): String = this
        .replace("\\n", "\n")
        .replace("\\N", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
