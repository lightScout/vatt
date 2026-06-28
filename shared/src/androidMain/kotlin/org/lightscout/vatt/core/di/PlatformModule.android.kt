package org.lightscout.vatt.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.lightscout.vatt.domain.reminder.ReminderScheduler
import org.lightscout.vatt.platform.AndroidAppContext
import org.lightscout.vatt.platform.AndroidReminderScheduler

actual fun platformModule(): Module = module {
    single<ReminderScheduler> { AndroidReminderScheduler(AndroidAppContext.get()) }
}
