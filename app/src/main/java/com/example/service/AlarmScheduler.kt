package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.entity.UserSettingsEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_CUTOFF_ALARM = "com.example.streaksentinel.ACTION_CUTOFF_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.streaksentinel.ACTION_SNOOZE_ALARM"
        const val WORK_NAME_PERIODIC = "SentinelPeriodicWork"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleDailyCutoffAlarm(settings: UserSettingsEntity) {
        if (!settings.isMonitoringEnabled || !settings.isEscalationEnabled) {
            cancelCutoffAlarm()
            return
        }

        val zone = try {
            ZoneId.of(settings.timezoneId)
        } catch (_: Exception) {
            ZoneId.of("Asia/Kolkata")
        }

        val now = ZonedDateTime.now(zone)
        var targetCutoff = ZonedDateTime.of(
            LocalDate.now(zone),
            LocalTime.of(settings.cutoffHour, settings.cutoffMinute),
            zone
        )

        // If cutoff has already passed today, schedule for tomorrow
        if (targetCutoff.isBefore(now) || targetCutoff.isEqual(now)) {
            targetCutoff = targetCutoff.plusDays(1)
        }

        val triggerMillis = targetCutoff.toInstant().toEpochMilli()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_CUTOFF_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled cutoff alarm for: $targetCutoff ($triggerMillis)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact cutoff alarm", e)
        }
    }

    fun scheduleSnoozeAlarm(snoozeMinutes: Int) {
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled snooze alarm in $snoozeMinutes minutes")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling snooze alarm", e)
        }
    }

    fun cancelCutoffAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_CUTOFF_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelSnoozeAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun schedulePeriodicWork(intervalMinutes: Int) {
        val effectiveInterval = intervalMinutes.coerceAtLeast(15).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SentinelWorker>(
            effectiveInterval,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
        Log.d(TAG, "Enqueued periodic WorkManager check with interval $effectiveInterval min")
    }

    fun cancelPeriodicWork() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
    }
}
