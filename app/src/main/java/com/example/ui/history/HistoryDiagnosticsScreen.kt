package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DailyActivityEntity
import com.example.data.local.entity.MonitoringEventEntity
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import com.example.ui.SentinelUiState
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.SentinelCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBlue
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextMuted
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryDiagnosticsScreen(
    state: SentinelUiState,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = SentinelGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SentinelGreen
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Daily History (${state.allActivities.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Event Audit Log (${state.recentEvents.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("System Diagnostics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Daily Activities List
                if (state.allActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded activity yet. Trigger a sync from the dashboard to initialize records.",
                            color = SentinelTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.allActivities, key = { it.date }) { activity ->
                            DailyActivityItem(activity, state.settings.timezoneId)
                        }
                    }
                }
            }

            1 -> {
                // Monitoring Events Audit Log
                if (state.recentEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No monitoring events recorded yet.",
                            color = SentinelTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.recentEvents, key = { it.id }) { event ->
                            MonitoringEventItem(event, state.settings.timezoneId)
                        }
                    }
                }
            }

            2 -> {
                // System Diagnostics & Factory Reset
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SentinelCard(
                        title = "Android Platform & Subsystem Health",
                        icon = Icons.Default.Memory
                    ) {
                        DiagnosticHealthRow(
                            name = "Notification Permission",
                            isHealthy = state.hasNotificationPermission,
                            detail = if (state.hasNotificationPermission) "Enabled (POST_NOTIFICATIONS)" else "Blocked by OS"
                        )
                        DiagnosticHealthRow(
                            name = "Exact Alarm Scheduling",
                            isHealthy = state.hasExactAlarmPermission,
                            detail = if (state.hasExactAlarmPermission) "Granted (Exact wakeups allowed)" else "Standard (OS may delay ~5m)"
                        )
                        DiagnosticHealthRow(
                            name = "WorkManager Polling Engine",
                            isHealthy = state.settings.isMonitoringEnabled,
                            detail = if (state.settings.isMonitoringEnabled) "Registered periodic work (${state.settings.checkIntervalMinutes}m)" else "Stopped"
                        )
                        DiagnosticHealthRow(
                            name = "LeetCode Integration Mode",
                            isHealthy = state.settings.leetcodeUsername.isNotBlank(),
                            detail = if (state.settings.leetcodeUsername.isNotBlank()) "Target: ${state.settings.leetcodeUsername}" else "No username configured"
                        )
                    }

                    SentinelCard(
                        title = "Android Battery Optimization Notice",
                        icon = Icons.Default.Info
                    ) {
                        Text(
                            text = "To guarantee alarms ring exactly at your cutoff time regardless of device sleep or Doze state, ensure Leet Guard is exempted from aggressive OEM battery optimization in your device Settings > Apps > Leet Guard > Battery > Unrestricted.",
                            color = SentinelTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_all_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All Data & History", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        ConfirmationDialog(
            title = "Confirm Factory Reset",
            message = "This action will permanently delete all daily records, streak calculations, monitoring event audit logs, and alarm session history. This cannot be undone.",
            confirmText = "Delete Everything",
            onConfirm = {
                showResetConfirmDialog = false
                onResetAllData()
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}

@Composable
fun DailyActivityItem(activity: DailyActivityEntity, timezoneId: String) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.date,
                    fontWeight = FontWeight.Bold,
                    color = SentinelTextPrimary,
                    fontSize = 14.sp
                )
                StatusBadge(status = activity.verificationStatus, evidenceSource = activity.evidenceSource)
            }

            if (activity.problemTitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Problem: ${activity.problemTitle}",
                    color = SentinelGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (activity.notes != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Notes: ${activity.notes}",
                    color = SentinelTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MonitoringEventItem(event: MonitoringEventEntity, timezoneId: String) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeStr = try {
                Instant.ofEpochMilli(event.timestamp)
                    .atZone(ZoneId.of(timezoneId))
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            } catch (_: Exception) {
                ""
            }

            Text(
                text = timeStr,
                color = SentinelTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.width(55.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.eventType,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isSimulated) SentinelAmber else SentinelBlue,
                        fontSize = 12.sp
                    )
                    if (event.isSimulated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[SIMULATED]",
                            color = SentinelAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = event.details,
                    color = SentinelTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun DiagnosticHealthRow(name: String, isHealthy: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = SentinelTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = SentinelTextSecondary, fontSize = 11.sp)
        }
        Icon(
            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isHealthy) SentinelGreen else SentinelAmber,
            modifier = Modifier.size(18.dp)
        )
    }
}
