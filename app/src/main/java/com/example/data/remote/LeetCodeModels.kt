package com.example.data.remote

import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import java.time.LocalDate

data class UserProfileInfo(
    val username: String,
    val realName: String?,
    val avatarUrl: String?,
    val ranking: Int?,
    val totalSolved: Int,
    val easySolved: Int,
    val mediumSolved: Int,
    val hardSolved: Int
)

data class RecentSubmission(
    val id: String,
    val title: String,
    val titleSlug: String,
    val timestampSeconds: Long,
    val statusDisplay: String = "Accepted"
)

sealed class ProfileValidationResult {
    data class Success(val profile: UserProfileInfo) : ProfileValidationResult()
    object NotFound : ProfileValidationResult()
    data class Error(val message: String) : ProfileValidationResult()
}

data class VerificationDecision(
    val status: VerificationStatus,
    val evidenceSource: EvidenceSource,
    val acceptedSubmission: RecentSubmission? = null,
    val message: String,
    val errorDetails: String? = null
)

interface LeetCodeActivityProvider {
    suspend fun validateProfile(username: String): ProfileValidationResult
    suspend fun fetchRecentSubmissions(username: String, limit: Int = 20): Result<List<RecentSubmission>>
    suspend fun determineVerificationStatus(
        username: String,
        targetDate: LocalDate,
        zoneId: java.time.ZoneId
    ): VerificationDecision
}
