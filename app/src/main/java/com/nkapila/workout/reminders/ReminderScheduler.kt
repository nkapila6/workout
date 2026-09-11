package com.nkapila.workout.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nkapila.workout.data.settings.AppSettings
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    fun schedule(context: Context, settings: AppSettings) {
        if (!settings.remindersEnabled) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel every weekday first so stale PendingIntents for disabled days are removed.
        (1..7).forEach { day ->
            val pending = pendingIntent(context, day)
            alarmManager.cancel(pending)
            pending.cancel()
        }

        settings.reminderDays.forEach { day ->
            val trigger = nextOccurrenceMillis(day, settings.reminderTimeMinutesOfDay)
            val pending = pendingIntent(context, day)
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trigger,
                    pending
                )
            } else {
                // Fallback for users who revoked exact alarm permission on S+.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    trigger,
                    5 * 60 * 1000L,
                    pending
                )
            }
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        (1..7).forEach { day ->
            val pending = pendingIntent(context, day)
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    internal fun scheduleSingleDay(context: Context, day: Int, settings: AppSettings) {
        if (!settings.remindersEnabled || day !in settings.reminderDays) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = nextOccurrenceMillis(day, settings.reminderTimeMinutesOfDay)
        val pending = pendingIntent(context, day)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, trigger, 5 * 60 * 1000L, pending)
        }
    }

    private fun pendingIntent(context: Context, day: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_DAY, day)
        }
        return PendingIntent.getBroadcast(
            context,
            day * 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextOccurrenceMillis(dayOfWeek: Int, minutesOfDay: Int): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val targetTime = minutesOfDayToLocalTime(minutesOfDay)
        var candidate = now
            .with(DayOfWeek.of(mapDayOfWeek(dayOfWeek)))
            .withHour(targetTime.first)
            .withMinute(targetTime.second)
            .withSecond(0)
            .withNano(0)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun minutesOfDayToLocalTime(minutes: Int): Pair<Int, Int> {
        val safe = minutes.coerceIn(0, 24 * 60 - 1)
        return safe / 60 to safe % 60
    }

    // Settings days are 1=Mon..7=Sun; java.time.DayOfWeek is 1=Mon..7=Sun.
    private fun mapDayOfWeek(day: Int): Int = day.coerceIn(1, 7)

    const val EXTRA_DAY = "extra_day"
}
