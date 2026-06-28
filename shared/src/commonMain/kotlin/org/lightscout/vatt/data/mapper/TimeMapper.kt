package org.lightscout.vatt.data.mapper

import kotlinx.datetime.format.DateTimeComponents
import org.lightscout.vatt.domain.model.ZonedTime

/**
 * Parses an ISO-8601 datetime **with offset** (e.g. `2026-06-22T07:00:00+02:00`) into a [ZonedTime] that
 * keeps the venue wall-clock and offset for display, plus the absolute instant for comparisons.
 * Returns null on unparseable input so callers can defensively drop the item rather than crash.
 */
fun parseZonedTimeOrNull(raw: String?): ZonedTime? {
    if (raw.isNullOrBlank()) return null
    return try {
        val parts = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(raw)
        ZonedTime(
            instant = parts.toInstantUsingOffset(),
            local = parts.toLocalDateTime(),   // exactly the wall-clock written in the string
            offset = parts.toUtcOffset(),
        )
    } catch (e: Exception) {
        null
    }
}
