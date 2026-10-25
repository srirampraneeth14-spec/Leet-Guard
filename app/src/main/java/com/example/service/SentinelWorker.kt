package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.repository.StreakSentinelRepository
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SentinelWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SentinelWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SentinelWorker starting periodic background execution")
        return try {
            val database = AppDatabase.getInstance(context)
            val remote = LeetCodeRemoteProvider()
            val repository = StreakSentinelRepository(database, remote)
            val notificationHelper = NotificationHelper(context)
            val settings = repository.getSettings()

            if (!settings.isMonitoringEnabled) {
                Log.d(TAG, "Monitoring is disabled in settings. Skipping work.")
                return Result.success()
            }

            if (settings.leetcodeUsername.isBlank()) {
                Log.d(TAG, "No LeetCode username configured. Skipping work.")
                return Result.success()
            }

            val zone = repository.getEffectiveZoneId()
            val now = ZonedDateTime.now(zone)
            val today = LocalDate.now(zone)
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Check if current time is within active monitoring window
            val startTime = LocalTime.of(settings.startHour, settings.startMinute)
            val cutoffTime = LocalTime.of(settings.cutoffHour, settings.cutoffMinute)
            val currentLocalTime = now.toLocalTime()

            // Handle start time: if before start time, wait
            if (currentLocalTime.isBefore(startTime)) {
                Log.d(TAG, "Current time $currentLocalTime is before start time $startTime. Skipping.")
                return Result.success()
            }

            // Check if today is already verified or manually confirmed
            val existingActivity = database.dailyActivityDao().getActivity(todayStr)
            if (existingActivity?.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name ||
                existingActivity?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
            ) {
                Log.d(TAG, "Today is already verified/confirmed complete. Cancelling reminders.")
                notificationHelper.cancelOutstandingReminders()
                return Result.success()
            }

            // Perform verification check
            val decision = repository.checkAndVerifyToday(isTriggeredByUser = false)

            when (decision.status) {
                VerificationStatus.VERIFIED_ACCEPTED -> {
                    notificationHelper.showCompletionNotification(
                        decision.acceptedSubmission?.title ?: "LeetCode Problem",
                        isManual = false
                    )
                }

                VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND -> {
                    // Check minutes until cutoff
                    val cutoffZoned = ZonedDateTime.of(today, cutoffTime, zone)
                    if (now.isBefore(cutoffZoned)) {
                        val minutesRemaining = Duration.between(now, cutoffZoned).toMinutes()
                        if (minutesRemaining in 1..60) {
                            notificationHelper.showCutoffWarning(minutesRemaining)
                        } else {
                            notificationHelper.showRoutineReminder(
                                settings.leetcodeUsername,
                                settings.checkIntervalMinutes
                            )
                        }
                    }
                }

                VerificationStatus.VERIFICATION_UNAVAILABLE -> {
                    Log.w(TAG, "Verification check failed: ${decision.message}")
                }

                else -> {}
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SentinelWorker", e)
            Result.retry()
        }
    }
}
