package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.repository.StreakSentinelRepository
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRecv"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received notification action: $action")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val database = AppDatabase.getInstance(context)
                val remote = LeetCodeRemoteProvider()
                val repository = StreakSentinelRepository(database, remote)
                val notificationHelper = NotificationHelper(context)
                val scheduler = AlarmScheduler(context)

                val settings = repository.getSettings()
                val zone = repository.getEffectiveZoneId()
                val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)

                when (action) {
                    NotificationHelper.ACTION_CHECK_NOW -> {
                        val decision = repository.checkAndVerifyToday(isTriggeredByUser = true)
                        if (decision.status == VerificationStatus.VERIFIED_ACCEPTED) {
                            notificationHelper.showCompletionNotification(
                                decision.acceptedSubmission?.title ?: "LeetCode Problem",
                                isManual = false
                            )
                            AlarmService.stop(context)
                        }
                    }

                    NotificationHelper.ACTION_OPEN_LEETCODE -> {
                        val url = if (settings.leetcodeUsername.isNotBlank()) {
                            "https://leetcode.com/u/${settings.leetcodeUsername}/"
                        } else {
                            "https://leetcode.com/problemset/all/"
                        }
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    }

                    NotificationHelper.ACTION_MARK_COMPLETE -> {
                        repository.markTodayManualComplete("Confirmed via notification action")
                        notificationHelper.showCompletionNotification("Daily Problem", isManual = true)
                        AlarmService.stop(context)
                    }

                    NotificationHelper.ACTION_SNOOZE -> {
                        AlarmService.stop(context)
                        val session = repository.getAlarmSession(todayStr) ?: AlarmSessionEntity(date = todayStr)
                        val newCount = session.snoozeCount + 1
                        repository.updateAlarmSession(
                            session.copy(
                                status = "SNOOZED",
                                snoozeCount = newCount,
                                stoppedAt = System.currentTimeMillis()
                            )
                        )
                        scheduler.scheduleSnoozeAlarm(settings.snoozeMinutes)
                        repository.logEvent(
                            eventType = "ALARM_SNOOZED",
                            resultStatus = "SNOOZED",
                            details = "Alarm snoozed for ${settings.snoozeMinutes} min (Count: $newCount/${settings.maxSnoozes})"
                        )
                    }

                    NotificationHelper.ACTION_DISMISS_ALARM -> {
                        AlarmService.stop(context)
                        val session = repository.getAlarmSession(todayStr) ?: AlarmSessionEntity(date = todayStr)
                        repository.updateAlarmSession(
                            session.copy(
                                status = "DISMISSED",
                                stoppedAt = System.currentTimeMillis()
                            )
                        )
                        repository.logEvent(
                            eventType = "ALARM_STOPPED",
                            resultStatus = "STOPPED",
                            details = "User stopped the cutoff alarm. Daily status remains pending."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
