package org.lightscout.vatt.presentation.common

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.lightscout.vatt.domain.model.ZonedTime

/**
 * Display formatting. Times are rendered from the **venue** wall-clock ([ZonedTime.local]) — never
 * converted to the device timezone — per the brief.
 */

fun ZonedTime.formatTime(): String {
    val h = local.hour
    val m = local.minute
    val period = if (h < 12) "AM" else "PM"
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val mm = if (m < 10) "0$m" else "$m"
    return "$hour12:$mm $period"
}

fun ZonedTime.formatDateFull(): String =
    "${local.dayOfWeek.shortName()}, ${local.month.shortName()} ${local.dayOfMonth}, ${local.year}"

fun LocalDate.formatDayHeader(): String =
    "${dayOfWeek.shortName()}, ${month.shortName()} ${dayOfMonth}"

fun LocalDate.shortLabel(): String = "${dayOfWeek.shortName3()} ${dayOfMonth}"

private fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
    else -> name
}

private fun DayOfWeek.shortName3(): String = shortName().take(3)

private fun Month.shortName(): String = when (this) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
    else -> name
}
