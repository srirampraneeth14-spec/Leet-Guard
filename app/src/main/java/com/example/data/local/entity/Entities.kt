package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val leetcodeUsername: String = "",
    val isMonitoringEnabled: Boolean = true,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val cutoffHour: Int = 22,
    val cutoffMinute: Int = 30,
    val checkIntervalMinutes: Int = 30,
    val timezoneId: String = "Asia/Kolkata",
    val snoozeMinutes: Int = 5,
    val maxSnoozes: Int = 3,
    val alarmTone: String = "EMERGENCY_SIREN",
    val isVibrationEnabled: Boolean = true,
    val isEscalationEnabled: Boolean = true,
    val failSafePolicy: String = "PROTECTIVE",
    val unverifiedPastStreak: Int = 285,
    val isOnboardingCompleted: Boolean = false,
    val lastSyncTimestamp: Long = 0L
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val verificationStatus: String, // from VerificationStatus enum
    val evidenceSource: String, // from EvidenceSource enum
    val acceptedSubmissionTimestamp: Long? = null,
    val problemTitle: String? = null,
    val submissionId: String? = null,
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val manualConfirmationTimestamp: Long? = null,
    val notes: String? = null
)

@Entity(tableName = "monitoring_events")
data class MonitoringEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val resultStatus: String,
    val details: String,
    val isSimulated: Boolean = false
)

@Entity(tableName = "alarm_sessions")
data class AlarmSessionEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val status: String = "IDLE", // IDLE, ACTIVE, SNOOZED, DISMISSED, CANCELLED_VERIFIED
    val escalationStage: Int = 1, // 1: Routine, 2: Approaching, 3: Cutoff, 4: Active Alarm
    val snoozeCount: Int = 0,
    val startedAt: Long? = null,
    val lastTriggeredAt: Long? = null,
    val stoppedAt: Long? = null
)
