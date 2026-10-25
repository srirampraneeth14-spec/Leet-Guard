package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ROUTINE = "channel_routine_reminders"
        const val CHANNEL_CUTOFF = "channel_cutoff_warnings"
        const val CHANNEL_ALARM = "channel_urgent_alarms"
        const val CHANNEL_COMPLETION = "channel_completion_status"

        const val NOTIF_ID_ROUTINE = 1001
        const val NOTIF_ID_CUTOFF = 1002
        const val NOTIF_ID_ALARM = 1003
        const val NOTIF_ID_COMPLETION = 1004

        const val ACTION_CHECK_NOW = "com.example.streaksentinel.ACTION_CHECK_NOW"
        const val ACTION_OPEN_LEETCODE = "com.example.streaksentinel.ACTION_OPEN_LEETCODE"
        const val ACTION_MARK_COMPLETE = "com.example.streaksentinel.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "com.example.streaksentinel.ACTION_SNOOZE"
        const val ACTION_DISMISS_ALARM = "com.example.streaksentinel.ACTION_DISMISS_ALARM"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val defaultAlarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // 1. Routine Reminders
            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINE,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders during your monitoring window"
                enableVibration(true)
            }

            // 2. Cutoff Warnings
            val cutoffChannel = NotificationChannel(
                CHANNEL_CUTOFF,
                "Cutoff Approaching Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when the cutoff time is near"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }

            // 3. Urgent Alarms
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Cutoff Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud escalating alarm after cutoff time"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 1000)
                setSound(defaultAlarmUri, audioAttributes)
            }

            // 4. Completion Status
            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION,
                "Completion & Verification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Confirmation when today's submission is verified"
            }

            notificationManager.createNotificationChannels(
                listOf(routineChannel, cutoffChannel, alarmChannel, completionChannel)
            )
        }
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getActionPendingIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showRoutineReminder(username: String, intervalMinutes: Int) {
        val checkIntent = getActionPendingIntent(ACTION_CHECK_NOW, 11)
        val openIntent = getActionPendingIntent(ACTION_OPEN_LEETCODE, 12)
        val markIntent = getActionPendingIntent(ACTION_MARK_COMPLETE, 13)

        val notif = NotificationCompat.Builder(context, CHANNEL_ROUTINE)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Leet Guard: Daily Reminder")
            .setContentText("No LeetCode submission verified yet for $username today.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "You haven't solved today's LeetCode problem yet! Keep your streak safe before the daily cutoff."
            ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Check Now", checkIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open LeetCode", openIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", markIntent)
            .build()

        notificationManager.notify(NOTIF_ID_ROUTINE, notif)
    }

    fun showCutoffWarning(minutesRemaining: Long) {
        val checkIntent = getActionPendingIntent(ACTION_CHECK_NOW, 21)
        val openIntent = getActionPendingIntent(ACTION_OPEN_LEETCODE, 22)

        val notif = NotificationCompat.Builder(context, CHANNEL_CUTOFF)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Leet Guard: Cutoff in $minutesRemaining Minutes!")
            .setContentText("Your cutoff is approaching soon. Solve a problem now to protect your streak!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Warning: Only $minutesRemaining minutes remain until your configured cutoff time. Solve and submit on LeetCode now to avoid the alarm!"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Check Now", checkIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open LeetCode", openIntent)
            .build()

        notificationManager.notify(NOTIF_ID_CUTOFF, notif)
    }

    fun showCompletionNotification(problemTitle: String, isManual: Boolean = false) {
        // Cancel outstanding reminder / alarm notifications
        cancelOutstandingReminders()

        val title = if (isManual) "Activity Confirmed (Manual)" else "Streak Verified! 🔥"
        val body = if (isManual) {
            "You marked today's problem complete. Automatic sync will update evidence when available."
        } else {
            "Accepted submission verified: \"$problemTitle\". Your streak is secure for today!"
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_COMPLETION, notif)
    }

    fun cancelOutstandingReminders() {
        notificationManager.cancel(NOTIF_ID_ROUTINE)
        notificationManager.cancel(NOTIF_ID_CUTOFF)
        notificationManager.cancel(NOTIF_ID_ALARM)
    }
}
