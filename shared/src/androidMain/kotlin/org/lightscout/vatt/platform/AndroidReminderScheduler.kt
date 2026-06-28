package org.lightscout.vatt.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import kotlin.time.Clock
import org.lightscout.vatt.domain.reminder.ReminderOutcome
import org.lightscout.vatt.domain.reminder.ReminderRequest
import org.lightscout.vatt.domain.reminder.ReminderScheduler

/**
 * Real Android implementation: schedules a local notification via [AlarmManager] and offers a calendar
 * "add event" intent. Both are device-local and never touch the API.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    override suspend fun scheduleReminder(request: ReminderRequest): ReminderOutcome {
        ReminderReceiver.ensureChannel(context)
        val triggerAt = computeTriggerTime(request)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, request.title)
            putExtra(
                ReminderReceiver.EXTRA_TEXT,
                buildString {
                    append("Your class is coming up")
                    request.location?.let { append(" at ").append(it) }
                    append('.')
                },
            )
            putExtra(ReminderReceiver.EXTRA_ID, request.title.hashCode())
        }
        val pending = PendingIntent.getBroadcast(
            context,
            request.title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return try {
            // Inexact alarm: needs no special permission and is plenty for a class reminder.
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            ReminderOutcome.Scheduled
        } catch (e: SecurityException) {
            ReminderOutcome.PermissionDenied
        } catch (e: Exception) {
            ReminderOutcome.Failed(e.message)
        }
    }

    override suspend fun addToCalendar(request: ReminderRequest): ReminderOutcome {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, request.title)
                request.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, request.startEpochMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, request.endEpochMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // launched from a non-Activity context
            }
            context.startActivity(intent)
            ReminderOutcome.Scheduled
        } catch (e: Exception) {
            ReminderOutcome.Failed(e.message)
        }
    }

    /** Reminder fires [ReminderRequest.minutesBefore] before the class — or shortly from now if that's past. */
    private fun computeTriggerTime(request: ReminderRequest): Long {
        val desired = request.startEpochMillis - request.minutesBefore * 60_000L
        val soon = Clock.System.now().toEpochMilliseconds() + 5_000L
        return maxOf(desired, soon)
    }
}
