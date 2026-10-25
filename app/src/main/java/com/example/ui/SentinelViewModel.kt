package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.local.entity.DailyActivityEntity
import com.example.data.local.entity.MonitoringEventEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.remote.ProfileValidationResult
import com.example.data.remote.UserProfileInfo
import com.example.data.remote.VerificationDecision
import com.example.data.repository.StreakSentinelRepository
import com.example.domain.model.EvidenceSource
import com.example.domain.model.StreakStats
import com.example.domain.model.VerificationStatus
import com.example.service.AlarmScheduler
import com.example.service.AlarmService
import com.example.service.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SentinelUiState(
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val todayActivity: DailyActivityEntity? = null,
    val todayDateStr: String = "",
    val alarmSession: AlarmSessionEntity? = null,
    val recentEvents: List<MonitoringEventEntity> = emptyList(),
    val allActivities: List<DailyActivityEntity> = emptyList(),
    val streakStats: StreakStats = StreakStats(),
    val isSyncing: Boolean = false,
    val syncErrorMessage: String? = null,
    val validatedProfile: UserProfileInfo? = null,
    val isValidatingProfile: Boolean = false,
    val validationError: String? = null,
    val minutesUntilCutoff: Long = 0L,
    val hasNotificationPermission: Boolean = true,
    val hasExactAlarmPermission: Boolean = true,
    val isTestAlarmActive: Boolean = false
)

class SentinelViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = AppDatabase.getInstance(context)
    private val remoteProvider = LeetCodeRemoteProvider()
    val repository = StreakSentinelRepository(database, remoteProvider)
    private val notificationHelper = NotificationHelper(context)
    val scheduler = AlarmScheduler(context)

    private val _uiState = MutableStateFlow(SentinelUiState())
    val uiState: StateFlow<SentinelUiState> = _uiState.asStateFlow()

    init {
        notificationHelper.createNotificationChannels()
        observeData()
        refreshTimeCalculations()
    }

    private fun observeData() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            val zone = try {
                ZoneId.of(settings.timezoneId)
            } catch (_: Exception) {
                ZoneId.of("Asia/Kolkata")
            }
            val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)

            combine(
                repository.settingsFlow,
                repository.getTodayActivityFlow(todayStr),
                repository.getAlarmSessionFlow(todayStr),
                repository.recentEventsFlow,
                repository.allActivitiesFlow
            ) { s, todayAct, alarm, events, activities ->
                val currentSettings = s ?: UserSettingsEntity()
                val stats = repository.calculateStreakStats()
                val minutesLeft = calculateMinutesUntilCutoff(currentSettings)

                _uiState.update { state ->
                    state.copy(
                        settings = currentSettings,
                        todayActivity = todayAct,
                        todayDateStr = todayStr,
                        alarmSession = alarm,
                        recentEvents = events,
                        allActivities = activities,
                        streakStats = stats,
                        minutesUntilCutoff = minutesLeft,
                        hasExactAlarmPermission = scheduler.canScheduleExactAlarms()
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        }
    }

    fun refreshTimeCalculations() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            val minutesLeft = calculateMinutesUntilCutoff(settings)
            _uiState.update { it.copy(minutesUntilCutoff = minutesLeft) }
        }
    }

    private fun calculateMinutesUntilCutoff(settings: UserSettingsEntity): Long {
        return try {
            val zone = ZoneId.of(settings.timezoneId)
            val now = ZonedDateTime.now(zone)
            val today = LocalDate.now(zone)
            val cutoff = ZonedDateTime.of(today, LocalTime.of(settings.cutoffHour, settings.cutoffMinute), zone)
            if (now.isBefore(cutoff)) {
                Duration.between(now, cutoff).toMinutes()
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
            val decision = repository.checkAndVerifyToday(isTriggeredByUser = true)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncErrorMessage = if (decision.status == VerificationStatus.VERIFICATION_UNAVAILABLE) decision.message else null
                )
            }
            if (decision.status == VerificationStatus.VERIFIED_ACCEPTED) {
                notificationHelper.showCompletionNotification(
                    decision.acceptedSubmission?.title ?: "LeetCode Daily",
                    isManual = false
                )
            }
        }
    }

    fun validateProfile(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingProfile = true, validationError = null) }
            when (val res = repository.validateLeetCodeProfile(username)) {
                is ProfileValidationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isValidatingProfile = false,
                            validatedProfile = res.profile,
                            validationError = null
                        )
                    }
                }
                is ProfileValidationResult.NotFound -> {
                    _uiState.update {
                        it.copy(
                            isValidatingProfile = false,
                            validatedProfile = null,
                            validationError = "User \"$username\" was not found on LeetCode."
                        )
                    }
                }
                is ProfileValidationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isValidatingProfile = false,
                            validatedProfile = null,
                            validationError = res.message
                        )
                    }
                }
            }
        }
    }

    fun saveSettings(updated: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(updated)
            if (updated.isMonitoringEnabled) {
                scheduler.scheduleDailyCutoffAlarm(updated)
                scheduler.schedulePeriodicWork(updated.checkIntervalMinutes)
            } else {
                scheduler.cancelCutoffAlarm()
                scheduler.cancelPeriodicWork()
            }
            refreshTimeCalculations()
            repository.logEvent("SETTINGS_SAVED", "SUCCESS", "User updated monitoring settings")
        }
    }

    fun markTodayManualComplete(notes: String? = null) {
        viewModelScope.launch {
            repository.markTodayManualComplete(notes)
            notificationHelper.showCompletionNotification("Daily Problem", isManual = true)
        }
    }

    fun simulateOutcome(status: VerificationStatus) {
        viewModelScope.launch {
            repository.simulateTodayOutcome(status)
            if (status == VerificationStatus.VERIFIED_ACCEPTED) {
                notificationHelper.showCompletionNotification("Simulated Two Sum", isManual = false)
            }
        }
    }

    fun resetToday() {
        viewModelScope.launch {
            repository.clearTodayActivity()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }

    fun testRoutineNotification() {
        val settings = _uiState.value.settings
        notificationHelper.showRoutineReminder(
            if (settings.leetcodeUsername.isNotBlank()) settings.leetcodeUsername else "Developer",
            settings.checkIntervalMinutes
        )
    }

    fun testCutoffWarning() {
        notificationHelper.showCutoffWarning(15)
    }

    fun triggerTestAlarm() {
        _uiState.update { it.copy(isTestAlarmActive = true) }
        AlarmService.start(context, isTest = true)
        viewModelScope.launch {
            repository.logEvent("TEST_ALARM", "STARTED", "Triggered 10-second test alarm", isSimulated = true)
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopAlarm()
        }, 10000L)
    }

    fun stopAlarm() {
        AlarmService.stop(context)
        _uiState.update { it.copy(isTestAlarmActive = false) }
    }

    fun snoozeAlarm() {
        AlarmService.stop(context)
        val s = _uiState.value.settings
        scheduler.scheduleSnoozeAlarm(s.snoozeMinutes)
        _uiState.update { it.copy(isTestAlarmActive = false) }
        viewModelScope.launch {
            repository.logEvent("TEST_ALARM", "SNOOZED", "Test alarm snoozed for ${s.snoozeMinutes} min", isSimulated = true)
        }
    }

    fun updatePermissions(notif: Boolean, exact: Boolean) {
        _uiState.update {
            it.copy(
                hasNotificationPermission = notif,
                hasExactAlarmPermission = exact
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(isOnboardingCompleted = true))
            scheduler.scheduleDailyCutoffAlarm(current)
            scheduler.schedulePeriodicWork(current.checkIntervalMinutes)
        }
    }

    fun openLeetCode(username: String? = null) {
        val target = if (!username.isNullOrBlank()) {
            "https://leetcode.com/u/$username/"
        } else {
            "https://leetcode.com/problemset/all/"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
