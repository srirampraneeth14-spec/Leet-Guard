package com.example.ui.stats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import com.example.ui.SentinelUiState
import com.example.ui.components.SentinelCard
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBlue
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelGreenGlow
import com.example.ui.theme.SentinelPurple
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextMuted
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsScreen(
    state: SentinelUiState,
    onExportCsv: suspend () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats = state.streakStats

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Summary Streak Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsMetricCard(
                title = "Current Streak",
                value = "${stats.currentStreak}d",
                color = SentinelGreen,
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f)
            )
            StatsMetricCard(
                title = "Verified Best",
                value = "${stats.longestVerifiedStreak}d",
                color = SentinelBlue,
                icon = Icons.Default.Shield,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsMetricCard(
                title = "Total Active Days",
                value = "${stats.totalActiveDays}d",
                color = SentinelPurple,
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
            StatsMetricCard(
                title = "Target Record",
                value = "${stats.unverifiedHistoricalBest}d",
                color = SentinelAmber,
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contribution Heatmap Card (Last 10 Weeks)
        SentinelCard(
            title = "Daily Contribution Heatmap",
            subtitle = "Recent 10 weeks of problem-solving history",
            icon = Icons.Default.CalendarMonth
        ) {
            val today = try {
                LocalDate.parse(state.todayDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                LocalDate.now()
            }

            val activitiesMap = state.allActivities.associateBy { it.date }

            // 10 weeks * 7 days = 70 days
            val totalWeeks = 10
            val totalDays = totalWeeks * 7
            val startDate = today.minusDays((totalDays - 1).toLong())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (weekIndex in 0 until totalWeeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dayIndex in 0 until 7) {
                            val cellDate = startDate.plusDays((weekIndex * 7 + dayIndex).toLong())
                            val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val record = activitiesMap[dateStr]

                            val color = when {
                                record?.verificationStatus == VerificationStatus.VERIFIED_ACCEPTED.name -> SentinelGreen
                                record?.evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name -> SentinelAmber
                                cellDate.isAfter(today) -> Color.Transparent
                                cellDate == today -> Color(0xFF1E293B)
                                else -> Color(0xFF161E2E)
                            }

                            val border = if (cellDate == today) SentinelAmber else SentinelBorder.copy(alpha = 0.5f)

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                                    .border(1.dp, border, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SentinelGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verified Accepted", fontSize = 11.sp, color = SentinelTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SentinelAmber))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Complete", fontSize = 11.sp, color = SentinelTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF161E2E)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("None / Missed", fontSize = 11.sp, color = SentinelTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Completion Rates Card
        SentinelCard(
            title = "Consistency Metrics",
            icon = Icons.Default.BarChart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RateProgressBar(label = "7-Day Completion Rate", percentage = stats.weeklyCompletionRate, color = SentinelGreen)
                RateProgressBar(label = "30-Day Completion Rate", percentage = stats.monthlyCompletionRate, color = SentinelBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CSV Export Card
        SentinelCard(
            title = "Export Activity Records",
            subtitle = "Download or share your full verification history as CSV",
            icon = Icons.Default.Download
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val csv = onExportCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Leet Guard CSV", csv))
                            Toast.makeText(context, "CSV copied to clipboard (${state.allActivities.size} records)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.weight(1f).testTag("copy_csv_button")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = SentinelGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy CSV", color = SentinelGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = onExportCsv()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csv)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Leet Guard CSV"))
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                    modifier = Modifier.weight(1f).testTag("share_csv_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SentinelBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV", color = SentinelTextPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatsMetricCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = SentinelTextSecondary, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun RateProgressBar(label: String, percentage: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = SentinelTextSecondary, fontSize = 12.sp)
            Text(String.format("%.1f%%", percentage), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
