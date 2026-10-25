package com.example.ui.alarm

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserSettingsEntity
import com.example.domain.model.AlarmTone
import com.example.domain.model.FailSafePolicy
import com.example.ui.SentinelUiState
import com.example.ui.components.SentinelCard
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    state: SentinelUiState,
    onSaveSettings: (UserSettingsEntity) -> Unit,
    onTestRoutineNotif: () -> Unit,
    onTestCutoffWarning: () -> Unit,
    onTriggerTestAlarm: () -> Unit,
    onStopAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var settings by remember(state.settings) { mutableStateOf(state.settings) }

    val timezoneOptions = listOf(
        "Asia/Kolkata",
        "UTC",
        "America/New_York",
        "America/Los_Angeles",
        "America/Chicago",
        "Europe/London",
        "Europe/Berlin",
        "Asia/Tokyo",
        "Asia/Singapore"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Exact Alarm Permission Warning if not granted
        if (!state.hasExactAlarmPermission) {
            Surface(
                color = SentinelAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SentinelAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SentinelAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exact Alarm Access Recommended", fontWeight = FontWeight.Bold, color = SentinelAmber)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "On Android 12+, exact alarm scheduling allows Leet Guard to trigger the cutoff alarm at the exact minute even when your device is in Doze mode.",
                        color = SentinelTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelAmber),
                        modifier = Modifier.testTag("request_exact_alarm_button")
                    ) {
                        Text("Open Alarm Permission Settings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Cutoff Schedule Card
        SentinelCard(
            title = "Cutoff & Schedule Timing",
            subtitle = "When your daily problem must be verified by",
            icon = Icons.Default.Alarm
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cutoff Hour
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cutoff Hour (24h)", color = SentinelTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimeSelector(
                        value = settings.cutoffHour,
                        maxValue = 23,
                        onValueChange = {
                            settings = settings.copy(cutoffHour = it)
                            onSaveSettings(settings)
                        }
                    )
                }

                // Cutoff Minute
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cutoff Minute", color = SentinelTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimeSelector(
                        value = settings.cutoffMinute,
                        maxValue = 59,
                        onValueChange = {
                            settings = settings.copy(cutoffMinute = it)
                            onSaveSettings(settings)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timezone Selector
            var tzExpanded by remember { mutableStateOf(false) }
            Text("Scheduling Timezone", color = SentinelTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = tzExpanded,
                onExpandedChange = { tzExpanded = !tzExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = settings.timezoneId,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SentinelSurfaceVariant,
                        unfocusedContainerColor = SentinelSurfaceVariant,
                        focusedTextColor = SentinelTextPrimary,
                        unfocusedTextColor = SentinelTextPrimary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = tzExpanded,
                    onDismissRequest = { tzExpanded = false }
                ) {
                    timezoneOptions.forEach { tz ->
                        DropdownMenuItem(
                            text = { Text(tz) },
                            onClick = {
                                settings = settings.copy(timezoneId = tz)
                                onSaveSettings(settings)
                                tzExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check Interval
            Text("Monitoring Polling Interval", color = SentinelTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    val isSelected = settings.checkIntervalMinutes == mins
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else SentinelSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SentinelGreen else SentinelBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                settings = settings.copy(checkIntervalMinutes = mins)
                                onSaveSettings(settings)
                            }
                    ) {
                        Text(
                            text = "${mins}m",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSelected) SentinelGreen else SentinelTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cutoff Escalation & Sound Card
        SentinelCard(
            title = "Escalation & Audio Behavior",
            icon = Icons.Default.GraphicEq
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cutoff Audible Alarm", fontWeight = FontWeight.Bold, color = SentinelTextPrimary, fontSize = 14.sp)
                    Text("Plays loud alarm sound and rhythmic vibration if not verified at cutoff", color = SentinelTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.isEscalationEnabled,
                    onCheckedChange = {
                        settings = settings.copy(isEscalationEnabled = it)
                        onSaveSettings(settings)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = SentinelGreen),
                    modifier = Modifier.testTag("escalation_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibration", fontWeight = FontWeight.Bold, color = SentinelTextPrimary, fontSize = 14.sp)
                    Text("Urgent pulsing vibration during alarms", color = SentinelTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.isVibrationEnabled,
                    onCheckedChange = {
                        settings = settings.copy(isVibrationEnabled = it)
                        onSaveSettings(settings)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = SentinelGreen),
                    modifier = Modifier.testTag("vibration_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snooze Settings
            Text("Snooze Duration", color = SentinelTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15).forEach { mins ->
                    val isSelected = settings.snoozeMinutes == mins
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else SentinelSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondary else SentinelBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                settings = settings.copy(snoozeMinutes = mins)
                                onSaveSettings(settings)
                            }
                    ) {
                        Text(
                            text = "${mins} min",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else SentinelTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fail-Safe Policy Selection
            Text("Fail-Safe Verification Policy", color = SentinelTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FailSafeOption(
                    title = "Protective (Recommended)",
                    description = "If LeetCode is unreachable at cutoff, triggers an unverified alert to prevent streak loss.",
                    selected = settings.failSafePolicy == FailSafePolicy.PROTECTIVE.name,
                    onClick = {
                        settings = settings.copy(failSafePolicy = FailSafePolicy.PROTECTIVE.name)
                        onSaveSettings(settings)
                    }
                )
                FailSafeOption(
                    title = "Conservative",
                    description = "If LeetCode is unreachable at cutoff, displays an error warning but does not play loud alarm.",
                    selected = settings.failSafePolicy == FailSafePolicy.CONSERVATIVE.name,
                    onClick = {
                        settings = settings.copy(failSafePolicy = FailSafePolicy.CONSERVATIVE.name)
                        onSaveSettings(settings)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Test Actions Card
        SentinelCard(
            title = "Test Notifications & Alarm",
            subtitle = "Verify audio, notifications, and permissions on your device",
            icon = Icons.Default.Security
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestRoutineNotif,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                    modifier = Modifier.weight(1f).testTag("test_routine_notif_button")
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = SentinelAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", color = SentinelTextPrimary, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onTestCutoffWarning,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                    modifier = Modifier.weight(1f).testTag("test_cutoff_warning_button")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SentinelAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Warning", color = SentinelTextPrimary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (state.isTestAlarmActive) {
                Button(
                    onClick = onStopAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelRed),
                    modifier = Modifier.fillMaxWidth().testTag("stop_test_alarm_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Test Alarm Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onTriggerTestAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.fillMaxWidth().testTag("trigger_test_alarm_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Cutoff Alarm (10s Audio & Vibrate)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TimeSelector(
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit
) {
    Surface(
        color = SentinelSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val next = if (value > 0) value - 1 else maxValue
                    onValueChange(next)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", fontSize = 20.sp, color = SentinelTextSecondary, fontWeight = FontWeight.Bold)
            }

            Text(
                text = String.format("%02d", value),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SentinelTextPrimary
            )

            IconButton(
                onClick = {
                    val next = if (value < maxValue) value + 1 else 0
                    onValueChange(next)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", fontSize = 20.sp, color = SentinelTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FailSafeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF064E3B).copy(alpha = 0.5f) else SentinelSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) SentinelGreen else SentinelBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (selected) SentinelGreen else SentinelTextPrimary,
                fontSize = 13.sp
            )
            Text(
                text = description,
                color = SentinelTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
