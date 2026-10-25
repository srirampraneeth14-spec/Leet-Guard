package com.example.domain.model

enum class VerificationStatus {
    VERIFIED_ACCEPTED,
    ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN,
    NO_QUALIFYING_SUBMISSION_FOUND,
    VERIFICATION_UNAVAILABLE,
    PROFILE_INVALID
}

enum class EvidenceSource {
    AUTOMATIC_LEETCODE_API,
    AUTOMATIC_CALENDAR_BACKFILL,
    MANUAL_CONFIRMATION,
    SIMULATED_TEST_MODE
}

enum class DailyState {
    INITIALIZING,
    MONITORING,
    REMINDER_PENDING,
    REMINDER_SENT,
    CUTOFF_PENDING,
    ALARM_ACTIVE,
    COMPLETED_VERIFIED,
    COMPLETED_MANUAL,
    VERIFICATION_ERROR,
    DISABLED
}

enum class FailSafePolicy {
    CONSERVATIVE, // Warn with verification error, retry, don't trigger alarm
    PROTECTIVE,   // Trigger unverified alarm at cutoff to prevent streak loss
    MANUAL_OVERRIDE // Allow user to confirm uncertainty manually
}

enum class AlarmTone {
    DEFAULT_ALARM,
    EMERGENCY_SIREN,
    DIGITAL_PULSE,
    GENTLE_CHIME
}

data class StreakStats(
    val currentStreak: Int = 0,
    val longestVerifiedStreak: Int = 0,
    val unverifiedHistoricalBest: Int = 0,
    val totalActiveDays: Int = 0,
    val totalAcceptedSubmissions: Int = 0,
    val weeklyCompletionRate: Float = 0f,
    val monthlyCompletionRate: Float = 0f
)
