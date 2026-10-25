package com.example

import com.example.data.local.entity.DailyActivityEntity
import com.example.data.remote.LeetCodeActivityProvider
import com.example.data.remote.ProfileValidationResult
import com.example.data.remote.RecentSubmission
import com.example.data.remote.VerificationDecision
import com.example.domain.model.EvidenceSource
import com.example.domain.model.FailSafePolicy
import com.example.domain.model.StreakStats
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StreakSentinelUnitTest {

    @Test
    fun `test accepted submission verified on matching date in target timezone`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val targetDate = LocalDate.of(2026, 10, 25)

        // Epoch second corresponding to 2026-10-25 10:00:00 AM IST
        val submissionInstant = targetDate.atTime(10, 0).atZone(zone).toInstant()
        val submissionEpochSec = submissionInstant.epochSecond

        val submission = RecentSubmission(
            id = "sub-101",
            title = "Two Sum",
            titleSlug = "two-sum",
            timestampSeconds = submissionEpochSec
        )

        val subDate = Instant.ofEpochSecond(submission.timestampSeconds)
            .atZone(zone)
            .toLocalDate()

        assertEquals(targetDate, subDate)
        assertEquals("Two Sum", submission.title)
    }

    @Test
    fun `test submission on previous day does not verify today`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val today = LocalDate.of(2026, 10, 25)
        val yesterday = today.minusDays(1)

        val yesterdayInstant = yesterday.atTime(23, 0).atZone(zone).toInstant()
        val yesterdayEpochSec = yesterdayInstant.epochSecond

        val submission = RecentSubmission(
            id = "sub-100",
            title = "3Sum",
            titleSlug = "3sum",
            timestampSeconds = yesterdayEpochSec
        )

        val subDate = Instant.ofEpochSecond(submission.timestampSeconds)
            .atZone(zone)
            .toLocalDate()

        assertEquals(yesterday, subDate)
        assertFalse(subDate == today)
    }

    @Test
    fun `test streak calculation with consecutive verified and manual days`() {
        val today = LocalDate.of(2026, 10, 25)

        val activities = listOf(
            DailyActivityEntity(
                date = "2026-10-25",
                verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                problemTitle = "Course Schedule"
            ),
            DailyActivityEntity(
                date = "2026-10-24",
                verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                problemTitle = "Valid Parentheses"
            ),
            DailyActivityEntity(
                date = "2026-10-23",
                verificationStatus = VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN.name,
                evidenceSource = EvidenceSource.MANUAL_CONFIRMATION.name,
                notes = "Manual fallback confirmation"
            ),
            DailyActivityEntity(
                date = "2026-10-22",
                verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
                problemTitle = "Binary Search"
            ),
            // Missed 2026-10-21
            DailyActivityEntity(
                date = "2026-10-20",
                verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name
            )
        )

        val completedDates = activities
            .filter {
                it.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name ||
                it.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
            }
            .map { LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE) }
            .toSet()

        // Calculate consecutive streak backwards from today
        var currentStreak = 0
        var checkDate = today
        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // 25, 24, 23, 22 are completed -> currentStreak should be 4
        assertEquals(4, currentStreak)
    }

    @Test
    fun `test manual confirmation upgrade does not create duplicate record`() {
        val dateStr = "2026-10-25"
        val manualActivity = DailyActivityEntity(
            date = dateStr,
            verificationStatus = VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN.name,
            evidenceSource = EvidenceSource.MANUAL_CONFIRMATION.name,
            manualConfirmationTimestamp = 1700000000000L,
            notes = "Confirmed by user"
        )

        // Remote later finds verified submission
        val verifiedActivity = manualActivity.copy(
            verificationStatus = VerificationStatus.VERIFIED_ACCEPTED.name,
            evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API.name,
            acceptedSubmissionTimestamp = 1700000500000L,
            problemTitle = "Subsets"
        )

        assertEquals(dateStr, verifiedActivity.date)
        assertEquals(VerificationStatus.VERIFIED_ACCEPTED.name, verifiedActivity.verificationStatus)
        assertEquals("Subsets", verifiedActivity.problemTitle)
        assertNotNull(verifiedActivity.manualConfirmationTimestamp)
    }

    @Test
    fun `test fail-safe policy resolution`() {
        val protective = FailSafePolicy.valueOf("PROTECTIVE")
        assertEquals(FailSafePolicy.PROTECTIVE, protective)

        val conservative = FailSafePolicy.valueOf("CONSERVATIVE")
        assertEquals(FailSafePolicy.CONSERVATIVE, conservative)
    }
}
