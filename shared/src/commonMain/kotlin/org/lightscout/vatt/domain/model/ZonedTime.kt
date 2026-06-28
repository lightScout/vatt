package org.lightscout.vatt.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset

/**
 * A timestamp that preserves the **venue's** local wall-clock and UTC offset, as returned by the API
 * (ISO-8601 with offset, e.g. `2026-06-22T07:00:00+02:00`).
 *
 * The brief is explicit: render times exactly as the venue reports them — do NOT convert to the device's
 * timezone. So we keep [local] + [offset] for display, and [instant] (the absolute point in time) only for
 * correct comparisons such as the 12-hour cancellation window.
 */
data class ZonedTime(
    val instant: Instant,
    val local: LocalDateTime,
    val offset: UtcOffset,
)
