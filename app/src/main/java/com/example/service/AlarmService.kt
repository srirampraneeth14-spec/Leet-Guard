package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.repository.StreakSentinelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.example.streaksentinel.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.streaksentinel.STOP_ALARM"
        const val EXTRA_IS_TEST = "extra_is_test"
        private const val NOTIFICATION_ID = 9999

        fun start(context: Context, isTest: Boolean = false) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_IS_TEST, isTest)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var soundPlayer: AlarmSoundPlayer
    private lateinit var repository: StreakSentinelRepository

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AlarmSoundPlayer(this)
        val database = AppDatabase.getInstance(this)
        val remote = LeetCodeRemoteProvider()
        repository = StreakSentinelRepository(database, remote)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false

        if (action == ACTION_STOP_ALARM) {
            stopAlarmInternal()
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val settings = repository.getSettings()
            soundPlayer.startAlarm(settings.alarmTone, settings.isVibrationEnabled)
        }

        val notification = createAlarmNotification(isTest)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun createAlarmNotification(isTest: Boolean): Notification {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_alarm", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            91,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            92,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            93,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openLeetCodeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_OPEN_LEETCODE
        }
        val openLeetCodePendingIntent = PendingIntent.getBroadcast(
            this,
            94,
            openLeetCodeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTest) "🚨 Test Alarm: Leet Guard" else "🚨 STREAK CUTOFF ALARM REACHED!"
        val message = if (isTest) {
            "This is a test alarm. Your daily streak status has NOT been changed."
        } else {
            "Today's LeetCode submission has NOT been verified. Solve your problem now!"
        }

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 5m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open LeetCode", openLeetCodePendingIntent)
            .build()
    }

    private fun stopAlarmInternal() {
        soundPlayer.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopAlarmInternal()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
