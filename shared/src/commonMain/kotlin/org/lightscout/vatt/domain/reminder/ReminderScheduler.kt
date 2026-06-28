package org.lightscout.vatt.domain.reminder

import org.lightscout.vatt.domain.model.BookingResult

/**
 * Schedules a **local, device-only** reminder for a booking (and optionally adds it to the calendar).
 * This never touches the API. Implementations are platform-specific (Android: notification + calendar
 * intent; iOS: stub for this assessment), provided via the DI [platformModule].
 */
interface ReminderScheduler {
    suspend fun scheduleReminder(request: ReminderRequest): ReminderOutcome
    suspend fun addToCalendar(request: ReminderRequest): ReminderOutcome
}

/** Everything needed to build a reminder, derived from a confirmed [BookingResult]. */
data class ReminderRequest(
    val title: String,
    val location: String?,
    /** Class start as epoch milliseconds (absolute instant). */
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    /** How long before the class the reminder should fire. */
    val minutesBefore: Int = 60,
) {
    companion object {
        fun fromBooking(booking: BookingResult, clubName: String?): ReminderRequest = ReminderRequest(
            title = booking.classSession.title,
            location = clubName,
            startEpochMillis = booking.classSession.startsAt.instant.toEpochMilliseconds(),
            endEpochMillis = booking.classSession.endsAt.instant.toEpochMilliseconds(),
        )
    }
}

sealed interface ReminderOutcome {
    data object Scheduled : ReminderOutcome
    /** The platform doesn't implement this (e.g. iOS stub in this assessment). */
    data object NotSupported : ReminderOutcome
    data object PermissionDenied : ReminderOutcome
    data class Failed(val reason: String?) : ReminderOutcome
}
