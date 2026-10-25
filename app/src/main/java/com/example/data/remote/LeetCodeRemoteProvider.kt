package com.example.data.remote

import android.util.Log
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class LeetCodeRemoteProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : LeetCodeActivityProvider {

    companion object {
        private const val TAG = "LeetCodeRemoteProvider"
        private const val GRAPHQL_URL = "https://leetcode.com/graphql"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 LeetGuard/1.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun validateProfile(username: String): ProfileValidationResult = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank()) {
            return@withContext ProfileValidationResult.Error("Username cannot be empty")
        }

        val query = """
            query getUserProfile(${'$'}username: String!) {
                matchedUser(username: ${'$'}username) {
                    username
                    profile {
                        realName
                        userAvatar
                        ranking
                    }
                    submitStats {
                        acSubmissionNum {
                            difficulty
                            count
                        }
                    }
                }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("username", cleanUsername)
            })
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://leetcode.com/$cleanUsername/")
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 429) {
                        return@withContext ProfileValidationResult.Error("LeetCode rate limit reached (HTTP 429). Please wait a moment.")
                    }
                    return@withContext ProfileValidationResult.Error("LeetCode responded with HTTP $code")
                }

                val responseString = response.body?.string() ?: return@withContext ProfileValidationResult.Error("Empty response from LeetCode")
                val rootJson = JSONObject(responseString)

                if (rootJson.has("errors")) {
                    val errors = rootJson.getJSONArray("errors")
                    if (errors.length() > 0) {
                        val errMsg = errors.getJSONObject(0).optString("message", "Unknown GraphQL error")
                        return@withContext ProfileValidationResult.Error(errMsg)
                    }
                }

                val data = rootJson.optJSONObject("data")
                val matchedUser = data?.optJSONObject("matchedUser")

                if (matchedUser == null || matchedUser.isNull("username")) {
                    return@withContext ProfileValidationResult.NotFound
                }

                val profileObj = matchedUser.optJSONObject("profile")
                val submitStats = matchedUser.optJSONObject("submitStats")
                val acSubmissions = submitStats?.optJSONArray("acSubmissionNum")

                var totalSolved = 0
                var easy = 0
                var medium = 0
                var hard = 0

                if (acSubmissions != null) {
                    for (i in 0 until acSubmissions.length()) {
                        val item = acSubmissions.getJSONObject(i)
                        val diff = item.optString("difficulty")
                        val count = item.optInt("count", 0)
                        when (diff.lowercase()) {
                            "all" -> totalSolved = count
                            "easy" -> easy = count
                            "medium" -> medium = count
                            "hard" -> hard = count
                        }
                    }
                }

                val profileInfo = UserProfileInfo(
                    username = matchedUser.getString("username"),
                    realName = profileObj?.optString("realName"),
                    avatarUrl = profileObj?.optString("userAvatar"),
                    ranking = profileObj?.optInt("ranking").takeIf { it != null && it > 0 },
                    totalSolved = totalSolved,
                    easySolved = easy,
                    mediumSolved = medium,
                    hardSolved = hard
                )

                ProfileValidationResult.Success(profileInfo)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error validating profile", e)
            ProfileValidationResult.Error("Network error: ${e.localizedMessage ?: "Unable to contact LeetCode"}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error validating profile", e)
            ProfileValidationResult.Error("Failed to parse LeetCode profile: ${e.localizedMessage}")
        }
    }

    override suspend fun fetchRecentSubmissions(
        username: String,
        limit: Int
    ): Result<List<RecentSubmission>> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        val query = """
            query getRecentSubmissions(${'$'}username: String!, ${'$'}limit: Int!) {
                recentAcSubmissionList(username: ${'$'}username, limit: ${'$'}limit) {
                    id
                    title
                    titleSlug
                    timestamp
                }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("username", cleanUsername)
                put("limit", limit)
            })
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://leetcode.com/$cleanUsername/")
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("LeetCode HTTP ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                val root = JSONObject(bodyStr)
                val data = root.optJSONObject("data")
                val list = data?.optJSONArray("recentAcSubmissionList")
                    ?: return@withContext Result.success(emptyList())

                val submissions = mutableListOf<RecentSubmission>()
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    val rawTs = item.optLong("timestamp", 0L)
                    val submission = RecentSubmission(
                        id = item.optString("id", "${item.optString("titleSlug")}-$rawTs"),
                        title = item.optString("title", "Unknown problem"),
                        titleSlug = item.optString("titleSlug", ""),
                        timestampSeconds = rawTs,
                        statusDisplay = "Accepted"
                    )
                    submissions.add(submission)
                }

                Result.success(submissions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent submissions for $cleanUsername", e)
            Result.failure(e)
        }
    }

    override suspend fun determineVerificationStatus(
        username: String,
        targetDate: LocalDate,
        zoneId: ZoneId
    ): VerificationDecision = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank()) {
            return@withContext VerificationDecision(
                status = VerificationStatus.PROFILE_INVALID,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API,
                message = "Username is not configured"
            )
        }

        // Fetch recent accepted submissions
        val submissionsResult = fetchRecentSubmissions(cleanUsername, limit = 25)

        if (submissionsResult.isFailure) {
            val err = submissionsResult.exceptionOrNull()
            Log.w(TAG, "Verification unavailable: ${err?.message}")
            return@withContext VerificationDecision(
                status = VerificationStatus.VERIFICATION_UNAVAILABLE,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API,
                message = "Unable to contact LeetCode (${err?.localizedMessage ?: "Network error"}). Will retry automatically.",
                errorDetails = err?.localizedMessage
            )
        }

        val submissions = submissionsResult.getOrNull() ?: emptyList()

        // Check if any submission matches targetDate in the given ZoneId
        val matchingSubmission = submissions.firstOrNull { sub ->
            if (sub.timestampSeconds <= 0L) return@firstOrNull false
            val subDate = Instant.ofEpochSecond(sub.timestampSeconds)
                .atZone(zoneId)
                .toLocalDate()
            subDate == targetDate
        }

        if (matchingSubmission != null) {
            return@withContext VerificationDecision(
                status = VerificationStatus.VERIFIED_ACCEPTED,
                evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API,
                acceptedSubmission = matchingSubmission,
                message = "Accepted submission verified: \"${matchingSubmission.title}\""
            )
        }

        // No qualifying submission found for today
        VerificationDecision(
            status = VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND,
            evidenceSource = EvidenceSource.AUTOMATIC_LEETCODE_API,
            message = "No accepted submissions recorded on LeetCode for $targetDate ($zoneId)."
        )
    }
}
