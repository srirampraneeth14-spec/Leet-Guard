package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.local.entity.DailyActivityEntity
import com.example.data.local.entity.MonitoringEventEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.remote.LeetCodeActivityProvider
import com.example.data.remote.ProfileValidationResult
import com.example.data.remote.VerificationDecision
import com.example.domain.model.EvidenceSource
import com.example.domain.model.StreakStats
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StreakSentinelRepository(
    private val database: AppDatabase,
    private val remoteProvider: LeetCodeActivityProvider
) {
    private val settingsDao = database.userSettingsDao()
    private val activityDao = database.dailyActivityDao()
    private val eventDao = database.monitoringEventDao()
    private val alarmDao = database.alarmSessionDao()

    val settingsFlow: Flow<UserSettingsEntity?> = settingsDao.getSettingsFlow()
    val allActivitiesFlow: Flow<List<DailyActivityEntity>> = activityDao.getAllActivitiesFlow()
    val recentEventsFlow: Flow<List<MonitoringEventEntity>> = eventDao.getRecentEventsFlow(100)

    fun getTodayActivityFlow(dateStr: String): Flow<DailyActivityEntity?> =
        activityDao.getActivityFlow(dateStr)

    fun getAlarmSessionFlow(dateStr: String): Flow<AlarmSessionEntity?> =
        alarmDao.getAlarmSessionFlow(dateStr)

    suspend fun getSettings(): UserSettingsEntity {
        return settingsDao.getSettings() ?: UserSettingsEntity().also {
            settingsDao.insertOrUpdate(it)
        }
    }

    suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun validateLeetCodeProfile(username: String): ProfileValidationResult {
        return remoteProvider.validateProfile(username)
    }

    suspend fun getEffectiveZoneId(): ZoneId {
        val settings = getSettings()
        return try {
            ZoneId.of(settings.timezoneId)
        } catch (_: Exception) {
            ZoneId.of("Asia/Kolkata")
        }
    }

    suspend fun getTodayDate(): LocalDate {
        val zone = getEffectiveZoneId()
        return LocalDate.now(zone)
    }

    suspend fun checkAndVerifyToday(
        isTriggeredByUser: Boolean = false,
        isCutoffCheck: Boolean = false
    ): VerificationDecision = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val zone = getEffectiveZoneId()
        val today = LocalDate.now(zone)
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (settings.leetcodeUsername.isBlank()) {
            val decision = VerificationDecision(
                status = VerificationStatus.PROFILE_INVALID,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API,
                message = "LeetCode username is not configured."
            )
            logEvent(
                eventType = if (isCutoffCheck) "CUTOFF_CHECK" else "PERIODIC_CHECK",
                resultStatus = decision.status.name,
                details = decision.message
            )
            return@withContext decision
        }

        val existingActivity = activityDao.getActivity(todayStr)
        val decision = remoteProvider.determineVerificationStatus(
            username = settings.leetcodeUsername,
            targetDate = today,
            zoneId = zone
        )

        val now = System.currentTimeMillis()
        when (decision.status) {
            VerificationStatus.VERIFIED_ACCEPTED -> {
                val sub = decision.acceptedSubmission
                val updatedActivity = DailyActivityEntity(
                    date = todayStr,
                    verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                    evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                    acceptedSubmissionTimestamp = sub?.timestampSeconds?.let { it * 1000L },
                    problemTitle = sub?.title,
                    submissionId = sub?.id,
                    lastCheckedAt = now,
                    manualConfirmationTimestamp = existingActivity?.manualConfirmationTimestamp,
                    notes = existingActivity?.notes
                )
                activityDao.insertOrUpdate(updatedActivity)
                settingsDao.insertOrUpdate(settings.copy(lastSyncTimestamp = now))
                logEvent(
                    eventType = if (isCutoffCheck) "CUTOFF_VERIFIED" else "CHECK_VERIFIED",
                    resultStatus = decision.status.name,
                    details = "Verified accepted submission: \"${sub?.title}\""
                )
            }
            VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND -> {
                // If already manually confirmed or verified, preserve that completion status
                if (existingActivity?.verificationStatus != VerificationStatus.VERIFIED_ACCEPTED.name &&
                    existingActivity?.verificationStatus != VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN.name
                ) {
                    val isManual = existingActivity?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
                    if (!isManual) {
                        val record = DailyActivityEntity(
                            date = todayStr,
                            verificationStatus = VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND.name,
                            evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                            lastCheckedAt = now
                        )
                        activityDao.insertOrUpdate(record)
                    }
                }
                settingsDao.insertOrUpdate(settings.copy(lastSyncTimestamp = now))
                logEvent(
                    eventType = if (isCutoffCheck) "CUTOFF_UNVERIFIED" else "CHECK_NO_SUBMISSION",
                    resultStatus = decision.status.name,
                    details = decision.message
                )
            }
            VerificationStatus.VERIFICATION_UNAVAILABLE -> {
                // Do NOT mark as unverified/failed streak, just update check time and log event
                if (existingActivity == null) {
                    val record = DailyActivityEntity(
                        date = todayStr,
                        verificationStatus = VerificationStatus.VERIFICATION_UNAVAILABLE.name,
                        evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                        lastCheckedAt = now
                    )
                    activityDao.insertOrUpdate(record)
                } else {
                    activityDao.insertOrUpdate(existingActivity.copy(lastCheckedAt = now))
                }
                logEvent(
                    eventType = "CHECK_ERROR",
                    resultStatus = decision.status.name,
                    details = decision.message
                )
            }
            VerificationStatus.PROFILE_INVALID -> {
                logEvent(
                    eventType = "CHECK_INVALID_PROFILE",
                    resultStatus = decision.status.name,
                    details = decision.message
                )
            }
            VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN -> {
                logEvent(
                    eventType = "CHECK_UNKNOWN_ACCEPTANCE",
                    resultStatus = decision.status.name,
                    details = decision.message
                )
            }
        }

        decision
    }

    suspend fun markTodayManualComplete(notes: String? = null) = withContext(Dispatchers.IO) {
        val zone = getEffectiveZoneId()
        val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val now = System.currentTimeMillis()
        val existing = activityDao.getActivity(todayStr)

        val manualRecord = DailyActivityEntity(
            date = todayStr,
            verificationStatus = VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN.name,
            evidenceSource = EvidenceSource.MANUAL_CONFIRMATION.name,
            lastCheckedAt = now,
            manualConfirmationTimestamp = now,
            notes = notes ?: "Manually confirmed by user"
        )
        activityDao.insertOrUpdate(manualRecord)
        logEvent(
            eventType = "MANUAL_CONFIRMATION",
            resultStatus = manualRecord.verificationStatus,
            details = "User manually marked today's activity complete ($todayStr)"
        )
    }

    suspend fun simulateTodayOutcome(
        status: VerificationStatus,
        problemTitle: String? = "Simulated Problem #128"
    ) = withContext(Dispatchers.IO) {
        val zone = getEffectiveZoneId()
        val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val now = System.currentTimeMillis()

        val record = when (status) {
            VerificationStatus.VERIFIED_ACCEPTED -> DailyActivityEntity(
                date = todayStr,
                verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                evidenceSource = EvidenceSource.SIMULATED_TEST_MODE.name,
                acceptedSubmissionTimestamp = now,
                problemTitle = problemTitle,
                submissionId = "sim-${System.currentTimeMillis()}",
                lastCheckedAt = now,
                notes = "Test mode simulation"
            )
            VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND -> DailyActivityEntity(
                date = todayStr,
                verificationStatus = VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND.name,
                evidenceSource = EvidenceSource.SIMULATED_TEST_MODE.name,
                lastCheckedAt = now,
                notes = "Test mode simulation"
            )
            VerificationStatus.VERIFICATION_UNAVAILABLE -> DailyActivityEntity(
                date = todayStr,
                verificationStatus = VerificationStatus.VERIFICATION_UNAVAILABLE.name,
                evidenceSource = EvidenceSource.SIMULATED_TEST_MODE.name,
                lastCheckedAt = now,
                notes = "Test mode simulation error"
            )
            else -> DailyActivityEntity(
                date = todayStr,
                verificationStatus = status.name,
                evidenceSource = EvidenceSource.SIMULATED_TEST_MODE.name,
                lastCheckedAt = now
            )
        }
        activityDao.insertOrUpdate(record)
        logEvent(
            eventType = "SIMULATION",
            resultStatus = status.name,
            details = "Simulated outcome: $status",
            isSimulated = true
        )
    }

    suspend fun clearTodayActivity() = withContext(Dispatchers.IO) {
        val zone = getEffectiveZoneId()
        val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        activityDao.deleteActivity(todayStr)
        logEvent("RESET_DAY", "CLEARED", "Cleared today's recorded activity ($todayStr)")
    }

    suspend fun calculateStreakStats(): StreakStats = withContext(Dispatchers.IO) {
        val activities = activityDao.getAllActivities()
        val settings = getSettings()
        val zone = getEffectiveZoneId()
        val today = LocalDate.now(zone)

        val completedDates = activities
            .filter {
                it.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name ||
                it.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
            }
            .mapNotNull {
                try {
                    LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    null
                }
            }
            .toSet()

        // Current streak: calculate consecutive days backwards from today or yesterday
        var currentStreak = 0
        val isTodayDone = completedDates.contains(today)
        var checkDate = if (isTodayDone) today else today.minusDays(1)

        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Longest verified streak in recorded history
        val sortedDates = completedDates.sorted()
        var maxStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                tempStreak++
            } else if (date != prevDate) {
                tempStreak = 1
            }
            if (tempStreak > maxStreak) {
                maxStreak = tempStreak
            }
            prevDate = date
        }

        // Weekly completion rate (last 7 days)
        var weeklyDone = 0
        for (i in 0 until 7) {
            if (completedDates.contains(today.minusDays(i.toLong()))) {
                weeklyDone++
            }
        }
        val weeklyRate = (weeklyDone / 7f) * 100f

        // Monthly completion rate (last 30 days)
        var monthlyDone = 0
        for (i in 0 until 30) {
            if (completedDates.contains(today.minusDays(i.toLong()))) {
                monthlyDone++
            }
        }
        val monthlyRate = (monthlyDone / 30f) * 100f

        val totalAccepted = activities.count { it.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name }

        StreakStats(
            currentStreak = currentStreak,
            longestVerifiedStreak = maxStreak,
            unverifiedHistoricalBest = settings.unverifiedPastStreak,
            totalActiveDays = completedDates.size,
            totalAcceptedSubmissions = totalAccepted,
            weeklyCompletionRate = weeklyRate,
            monthlyCompletionRate = monthlyRate
        )
    }

    suspend fun exportHistoryAsCsv(): String = withContext(Dispatchers.IO) {
        val activities = activityDao.getAllActivities()
        val sb = StringBuilder()
        sb.append("Date,VerificationStatus,EvidenceSource,ProblemTitle,SubmissionTimestamp,ManualTimestamp,Notes\n")
        for (act in activities) {
            sb.append("${act.date},")
            sb.append("${act.verificationStatus},")
            sb.append("${act.evidenceSource},")
            sb.append("\"${(act.problemTitle ?: "").replace("\"", "\"\"")}\",")
            sb.append("${act.acceptedSubmissionTimestamp ?: ""},")
            sb.append("${act.manualConfirmationTimestamp ?: ""},")
            sb.append("\"${(act.notes ?: "").replace("\"", "\"\"")}\"\n")
        }
        sb.toString()
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        activityDao.deleteAll()
        eventDao.deleteAll()
        alarmDao.deleteAll()
        val currentSettings = getSettings()
        settingsDao.insertOrUpdate(currentSettings.copy(lastSyncTimestamp = 0L))
        logEvent("DATA_RESET", "SUCCESS", "All history and alarm states cleared by user")
    }

    suspend fun logEvent(
        eventType: String,
        resultStatus: String,
        details: String,
        isSimulated: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val event = MonitoringEventEntity(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            resultStatus = resultStatus,
            details = details,
            isSimulated = isSimulated
        )
        eventDao.insertEvent(event)
    }

    suspend fun getAlarmSession(dateStr: String): AlarmSessionEntity? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmSession(dateStr)
    }

    suspend fun updateAlarmSession(session: AlarmSessionEntity) = withContext(Dispatchers.IO) {
        alarmDao.insertOrUpdate(session)
    }
}
