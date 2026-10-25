package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import com.example.ui.SentinelUiState
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.SentinelCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelGreenGlow
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextMuted
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    state: SentinelUiState,
    onSyncNow: () -> Unit,
    onOpenLeetCode: () -> Unit,
    onMarkManualComplete: (notes: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualConfirmDialog by remember { mutableStateOf(false) }

    val todayAct = state.todayActivity
    val isVerified = todayAct?.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name
    val isManual = todayAct?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name
    val isDone = isVerified || isManual

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Active Cutoff Alarm Banner if triggered
        AnimatedVisibility(visible = state.alarmSession?.status == "ACTIVE" || state.isTestAlarmActive) {
            Surface(
                color = SentinelRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SentinelRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = SentinelRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isTestAlarmActive) "TEST ALARM RUNNING" else "CUTOFF ALARM ACTIVE",
                            fontWeight = FontWeight.Bold,
                            color = SentinelRed,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Solve today's LeetCode problem or stop the alarm.",
                            color = SentinelTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Hero Leet Guard Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF132035),
                            Color(0xFF0D1424)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isDone) SentinelGreen.copy(alpha = 0.5f) else SentinelBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isDone) SentinelGreen else SentinelAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.streakStats.currentStreak}",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDone) SentinelGreen else SentinelTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.streakStats.currentStreak == 1) "DAY" else "DAYS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SentinelTextSecondary,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }

                Text(
                    text = if (isDone) "STREAK SECURE FOR TODAY" else "DAILY STREAK IN JEOPARDY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) SentinelGreen else SentinelAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Lost Streak Context Tag
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target Record: ${state.streakStats.unverifiedHistoricalBest}d past personal best",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today's Status & Cutoff Countdown
        SentinelCard(
            title = "Today's Verification Status",
            icon = Icons.Default.CheckCircle
        ) {
            val status = todayAct?.verificationStatus ?: VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND.name
            val source = todayAct?.evidenceSource

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = status, evidenceSource = source)

                // Cutoff Countdown
                val hours = state.minutesUntilCutoff / 60
                val mins = state.minutesUntilCutoff % 60
                val cutoffStr = String.format("%02d:%02d", state.settings.cutoffHour, state.settings.cutoffMinute)

                Surface(
                    color = if (isDone) Color(0xFF1E293B) else if (state.minutesUntilCutoff in 1..60) Color(0xFF451A03) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (isDone) SentinelTextSecondary else if (state.minutesUntilCutoff in 1..60) SentinelAmber else SentinelTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDone) "Cutoff: $cutoffStr" else if (state.minutesUntilCutoff > 0) "${hours}h ${mins}m to cutoff" else "Cutoff Passed ($cutoffStr)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isDone) SentinelTextSecondary else if (state.minutesUntilCutoff in 1..60) SentinelAmber else SentinelTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isVerified && todayAct?.problemTitle != null) {
                Surface(
                    color = Color(0xFF064E3B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = SentinelGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = todayAct.problemTitle,
                                fontWeight = FontWeight.Bold,
                                color = SentinelGreen,
                                fontSize = 13.sp
                            )
                            val ts = todayAct.acceptedSubmissionTimestamp
                            if (ts != null && ts > 0) {
                                val timeStr = Instant.ofEpochMilli(ts)
                                    .atZone(ZoneId.of(state.settings.timezoneId))
                                    .format(DateTimeFormatter.ofPattern("hh:mm a"))
                                Text(
                                    text = "Accepted at $timeStr (${state.settings.timezoneId})",
                                    color = SentinelTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else if (isManual) {
                Surface(
                    color = Color(0xFF78350F).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SentinelAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Marked complete via manual confirmation",
                                fontWeight = FontWeight.Bold,
                                color = SentinelAmber,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Automatic sync will verify accepted submissions when available.",
                                color = SentinelTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No qualifying accepted submission found for ${state.todayDateStr}. Solve and submit your daily problem on LeetCode to avoid the alarm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SentinelTextSecondary
                )
            }

            // Sync error notice if any
            if (state.syncErrorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SentinelRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.syncErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !state.isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_now_button")
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checking...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onOpenLeetCode,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_leetcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LeetCode", color = SentinelTextPrimary, fontSize = 13.sp)
                }
            }

            // Manual confirmation trigger if not yet completed
            if (!isDone) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showManualConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = SentinelAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark today's activity complete (Manual Fallback)",
                        color = SentinelAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mini Recent Activity Heatmap (Last 14 days)
        SentinelCard(
            title = "Recent 14 Days Activity",
            subtitle = "Verified accepted vs manual confirmation",
            icon = Icons.Default.Shield
        ) {
            val today = try {
                LocalDate.parse(state.todayDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                LocalDate.now()
            }

            val activitiesMap = state.allActivities.associateBy { it.date }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 13 downTo 0) {
                    val date = today.minusDays(i.toLong())
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val record = activitiesMap[dateStr]

                    val (color, borderColor) = when {
                        record?.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name -> {
                            Pair(SentinelGreen, SentinelGreenGlow)
                        }
                        record?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name -> {
                            Pair(SentinelAmber, Color(0xFFFBBF24))
                        }
                        date == today -> {
                            Pair(Color(0xFF1E293B), SentinelAmber)
                        }
                        else -> {
                            Pair(Color(0xFF1E293B), SentinelBorder)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${date.dayOfMonth}",
                            fontSize = 9.sp,
                            color = SentinelTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SentinelGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verified", fontSize = 11.sp, color = SentinelTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SentinelAmber))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 11.sp, color = SentinelTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1E293B)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending/None", fontSize = 11.sp, color = SentinelTextSecondary)
                }
            }
        }
    }

    // Manual Confirmation Dialog
    if (showManualConfirmDialog) {
        ConfirmationDialog(
            title = "Record Manual Completion",
            message = "Marking today complete manually registers completion as MANUAL_CONFIRMATION without claiming LeetCode verification. If LeetCode later verifies an accepted submission, your record will automatically upgrade with the problem evidence. Confirm?",
            confirmText = "Mark Complete",
            onConfirm = {
                showManualConfirmDialog = false
                onMarkManualComplete("Confirmed manually via Dashboard")
            },
            onDismiss = { showManualConfirmDialog = false }
        )
    }
}
