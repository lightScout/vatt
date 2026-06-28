package org.lightscout.vatt.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.lightscout.vatt.domain.reminder.ReminderOutcome
import org.lightscout.vatt.domain.reminder.ReminderRequest
import org.lightscout.vatt.domain.reminder.ReminderScheduler

/**
 * iOS is a reference target for this assessment, so reminders are stubbed rather than wired to EventKit /
 * UserNotifications. The contract is satisfied (the app compiles and runs on iOS); the UI surfaces a
 * "not available on this platform yet" message. Real implementation noted as next-step work.
 */
private class IosReminderSchedulerStub : ReminderScheduler {
    override suspend fun scheduleReminder(request: ReminderRequest): ReminderOutcome =
        ReminderOutcome.NotSupported

    override suspend fun addToCalendar(request: ReminderRequest): ReminderOutcome =
        ReminderOutcome.NotSupported
}

actual fun platformModule(): Module = module {
    single<ReminderScheduler> { IosReminderSchedulerStub() }
}
