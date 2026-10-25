package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.repository.StreakSentinelRepository
import com.example.domain.model.EvidenceSource
import com.example.domain.model.FailSafePolicy
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "AlarmReceiver triggered with action: $action")

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
                val today = LocalDate.now(zone)
                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (!settings.isMonitoringEnabled) {
                    Log.d(TAG, "Monitoring is disabled, ignoring alarm")
                    return@launch
                }

                // Check existing status
                val existing = database.dailyActivityDao().getActivity(todayStr)
                if (existing?.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name ||
                    existing?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
                ) {
                    Log.d(TAG, "Today is already verified or confirmed ($todayStr). Suppressing alarm.")
                    notificationHelper.cancelOutstandingReminders()
                    scheduler.scheduleDailyCutoffAlarm(settings) // schedule for tomorrow
                    return@launch
                }

                if (action == AlarmScheduler.ACTION_SNOOZE_ALARM) {
                    // Snooze interval expired, re-trigger alarm
                    val session = repository.getAlarmSession(todayStr)
                    val newSnoozeCount = (session?.snoozeCount ?: 0) + 1
                    repository.updateAlarmSession(
                        (session ?: AlarmSessionEntity(date = todayStr)).copy(
                            status = "ACTIVE",
                            escalationStage = 4,
                            snoozeCount = newSnoozeCount,
                            lastTriggeredAt = System.currentTimeMillis()
                        )
                    )
                    AlarmService.start(context, isTest = false)
                    return@launch
                }

                // Cutoff reached: perform immediate verification re-check
                repository.logEvent(
                    eventType = "CUTOFF_REACHED",
                    resultStatus = "CHECKING",
                    details = "Cutoff time reached. Performing pre-alarm verification check."
                )

                val decision = repository.checkAndVerifyToday(isTriggeredByUser = false, isCutoffCheck = true)

                when (decision.status) {
                    VerificationStatus.VERIFIED_ACCEPTED -> {
                        Log.d(TAG, "Verified accepted problem just before alarm: ${decision.acceptedSubmission?.title}")
                        notificationHelper.showCompletionNotification(
                            decision.acceptedSubmission?.title ?: "LeetCode Problem",
                            isManual = false
                        )
                        scheduler.scheduleDailyCutoffAlarm(settings)
                    }

                    VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND -> {
                        Log.d(TAG, "No qualifying submission found at cutoff. Triggering alarm sequence.")
                        val session = repository.getAlarmSession(todayStr) ?: AlarmSessionEntity(date = todayStr)
                        repository.updateAlarmSession(
                            session.copy(
                                status = "ACTIVE",
                                escalationStage = 4,
                                startedAt = System.currentTimeMillis(),
                                lastTriggeredAt = System.currentTimeMillis()
                            )
                        )
                        AlarmService.start(context, isTest = false)
                        scheduler.scheduleDailyCutoffAlarm(settings)
                    }

                    VerificationStatus.VERIFICATION_UNAVAILABLE -> {
                        val policy = try {
                            FailSafePolicy.valueOf(settings.failSafePolicy)
                        } catch (_: Exception) {
                            FailSafePolicy.PROTECTIVE
                        }

                        when (policy) {
                            FailSafePolicy.PROTECTIVE -> {
                                Log.w(TAG, "Verification unavailable at cutoff. Policy=PROTECTIVE: Triggering alarm.")
                                val session = repository.getAlarmSession(todayStr) ?: AlarmSessionEntity(date = todayStr)
                                repository.updateAlarmSession(
                                    session.copy(
                                        status = "ACTIVE",
                                        escalationStage = 4,
                                        startedAt = System.currentTimeMillis(),
                                        lastTriggeredAt = System.currentTimeMillis()
                                    )
                                )
                                AlarmService.start(context, isTest = false)
                            }
                            FailSafePolicy.CONSERVATIVE -> {
                                Log.w(TAG, "Verification unavailable at cutoff. Policy=CONSERVATIVE: Notifying error without loud alarm.")
                                notificationHelper.showCutoffWarning(0)
                            }
                            FailSafePolicy.MANUAL_OVERRIDE -> {
                                notificationHelper.showCutoffWarning(0)
                            }
                        }
                        scheduler.scheduleDailyCutoffAlarm(settings)
                    }

                    else -> {
                        AlarmService.start(context, isTest = false)
                        scheduler.scheduleDailyCutoffAlarm(settings)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
