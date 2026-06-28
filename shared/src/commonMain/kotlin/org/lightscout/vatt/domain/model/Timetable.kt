package org.lightscout.vatt.domain.model

import kotlinx.datetime.LocalDate

/** A week of classes at a club, grouped by day — the shape the timetable screen renders. */
data class Timetable(
    val clubId: String,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val selectedDate: LocalDate,
    val days: List<TimetableDay>,
)

data class TimetableDay(
    val date: LocalDate,
    val classes: List<ClassSession>,
)
