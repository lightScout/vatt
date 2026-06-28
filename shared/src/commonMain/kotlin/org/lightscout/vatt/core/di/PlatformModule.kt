package org.lightscout.vatt.core.di

import org.koin.core.module.Module

/**
 * Provides platform-specific bindings (currently the [org.lightscout.vatt.domain.reminder.ReminderScheduler]):
 * a real implementation on Android, a stub on iOS.
 */
expect fun platformModule(): Module
