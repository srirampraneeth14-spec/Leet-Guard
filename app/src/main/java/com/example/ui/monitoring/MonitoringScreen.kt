package com.example.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VerificationStatus
import com.example.ui.SentinelUiState
import com.example.ui.components.SentinelCard
import com.example.ui.theme.SentinelAmber
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
fun MonitoringScreen(
    state: SentinelUiState,
    onToggleMonitoring: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onSimulate: (VerificationStatus) -> Unit,
    onResetToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Monitoring Master Switch Card
        SentinelCard(
            title = "Sentinel Monitoring Engine",
            subtitle = "Battery-aware background polling via WorkManager",
            icon = Icons.Default.Sync
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.settings.isMonitoringEnabled) "Monitoring ACTIVE" else "Monitoring PAUSED",
                        fontWeight = FontWeight.Bold,
                        color = if (state.settings.isMonitoringEnabled) SentinelGreen else SentinelTextSecondary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (state.settings.isMonitoringEnabled)
                            "Checking every ${state.settings.checkIntervalMinutes} min between ${String.format("%02d:%02d", state.settings.startHour, state.settings.startMinute)} and ${String.format("%02d:%02d", state.settings.cutoffHour, state.settings.cutoffMinute)}"
                        else "No background checks or alarms will run",
                        color = SentinelTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = state.settings.isMonitoringEnabled,
                    onCheckedChange = onToggleMonitoring,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SentinelGreen,
                        uncheckedThumbColor = SentinelTextSecondary,
                        uncheckedTrackColor = SentinelSurfaceVariant
                    ),
                    modifier = Modifier.testTag("monitoring_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scheduling and Timing Details Card
        SentinelCard(
            title = "Scheduling & Execution Telemetry",
            icon = Icons.Default.Schedule
        ) {
            val syncTimeStr = if (state.settings.lastSyncTimestamp > 0) {
                Instant.ofEpochMilli(state.settings.lastSyncTimestamp)
                    .atZone(ZoneId.of(state.settings.timezoneId))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"))
            } else {
                "No sync executed yet"
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryRow(label = "Target LeetCode Profile", value = if (state.settings.leetcodeUsername.isNotBlank()) state.settings.leetcodeUsername else "Not set")
                TelemetryRow(label = "Configured Timezone", value = state.settings.timezoneId)
                TelemetryRow(label = "Monitoring Window", value = "${String.format("%02d:%02d", state.settings.startHour, state.settings.startMinute)} to ${String.format("%02d:%02d", state.settings.cutoffHour, state.settings.cutoffMinute)}")
                TelemetryRow(label = "Polling Interval", value = "${state.settings.checkIntervalMinutes} minutes (WorkManager batched)")
                TelemetryRow(label = "Last Successful Sync", value = syncTimeStr)
                TelemetryRow(
                    label = "Exact Alarm Permission",
                    value = if (state.hasExactAlarmPermission) "Granted (Exact timing)" else "Standard (Approximate timing)"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSyncNow,
                enabled = !state.isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monitoring_trigger_check_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute Immediate Verification", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Test & Simulation Mode
        SentinelCard(
            title = "Developer Simulation Mode",
            subtitle = "Test verification branches without touching real streak stats",
            icon = Icons.Default.BugReport
        ) {
            Text(
                text = "Use these simulated triggers to test app behavior across all potential verification outcomes. Simulated states are tagged as test records in diagnostics.",
                color = SentinelTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulate(VerificationStatus.VERIFIED_ACCEPTED) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_accepted_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SentinelGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accepted", color = SentinelGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSimulate(VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelAmber.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_none_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = SentinelAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending", color = SentinelAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSimulate(VerificationStatus.VERIFICATION_UNAVAILABLE) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_error_button")
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = SentinelRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Network Err", color = SentinelRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onResetToday,
                border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_reset_button")
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = SentinelTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Today's Recorded Status", color = SentinelTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SentinelTextSecondary, fontSize = 13.sp)
        Text(text = value, color = SentinelTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
