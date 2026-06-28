package org.lightscout.vatt.domain.usecase

import org.lightscout.vatt.core.session.UserSession
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.reminder.ReminderOutcome
import org.lightscout.vatt.domain.reminder.ReminderRequest
import org.lightscout.vatt.domain.reminder.ReminderScheduler

/**
 * Sets a local device reminder, or adds the booking to the calendar. Builds the [ReminderRequest] from a
 * confirmed [BookingResult], enriching it with the user's club name for context.
 */
class SetReminderUseCase(
    private val scheduler: ReminderScheduler,
    private val userSession: UserSession,
) {
    suspend fun reminder(booking: BookingResult): ReminderOutcome =
        scheduler.scheduleReminder(ReminderRequest.fromBooking(booking, userSession.homeClubName))

    suspend fun calendar(booking: BookingResult): ReminderOutcome =
        scheduler.addToCalendar(ReminderRequest.fromBooking(booking, userSession.homeClubName))
}
