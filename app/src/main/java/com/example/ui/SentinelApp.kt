package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.account.AccountScreen
import com.example.ui.alarm.AlarmSettingsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryDiagnosticsScreen
import com.example.ui.monitoring.MonitoringScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.stats.StatisticsScreen
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBlue
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelDarkBg
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurface
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary

enum class SentinelTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MONITORING("Monitoring", Icons.Default.Sync),
    ALARMS("Alarms", Icons.Default.Alarm),
    ACCOUNT("Account", Icons.Default.Person),
    STATS("Stats & Logs", Icons.Default.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelApp(
    viewModel: SentinelViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(SentinelTab.DASHBOARD) }

    // Permission launcher for POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissions(
            notif = isGranted,
            exact = viewModel.scheduler.canScheduleExactAlarms()
        )
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.updatePermissions(
                notif = granted,
                exact = viewModel.scheduler.canScheduleExactAlarms()
            )
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!state.settings.isOnboardingCompleted) {
        OnboardingScreen(
            state = state,
            onSaveSettings = { viewModel.saveSettings(it) },
            onValidateProfile = { viewModel.validateProfile(it) },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onTestAlarm = { viewModel.triggerTestAlarm() },
            onCompleteOnboarding = { viewModel.completeOnboarding() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.settings.isMonitoringEnabled) SentinelGreen else SentinelAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LEET GUARD",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = SentinelTextPrimary
                        )
                    }
                },
                actions = {
                    if (state.settings.leetcodeUsername.isNotBlank()) {
                        Surface(
                            color = SentinelSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = state.settings.leetcodeUsername,
                                color = SentinelGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SentinelDarkBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SentinelSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                SentinelTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SentinelGreen,
                            selectedTextColor = SentinelGreen,
                            unselectedIconColor = SentinelTextSecondary,
                            unselectedTextColor = SentinelTextSecondary,
                            indicatorColor = SentinelSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = SentinelDarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                SentinelTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onSyncNow = { viewModel.syncNow() },
                    onOpenLeetCode = { viewModel.openLeetCode(state.settings.leetcodeUsername) },
                    onMarkManualComplete = { viewModel.markTodayManualComplete(it) },
                    onNavigateToSettings = { currentTab = SentinelTab.ALARMS },
                    onNavigateToAccount = { currentTab = SentinelTab.ACCOUNT }
                )

                SentinelTab.MONITORING -> MonitoringScreen(
                    state = state,
                    onToggleMonitoring = { enabled ->
                        viewModel.saveSettings(state.settings.copy(isMonitoringEnabled = enabled))
                    },
                    onSyncNow = { viewModel.syncNow() },
                    onSimulate = { status -> viewModel.simulateOutcome(status) },
                    onResetToday = { viewModel.resetToday() }
                )

                SentinelTab.ALARMS -> AlarmSettingsScreen(
                    state = state,
                    onSaveSettings = { viewModel.saveSettings(it) },
                    onTestRoutineNotif = { viewModel.testRoutineNotification() },
                    onTestCutoffWarning = { viewModel.testCutoffWarning() },
                    onTriggerTestAlarm = { viewModel.triggerTestAlarm() },
                    onStopAlarm = { viewModel.stopAlarm() }
                )

                SentinelTab.ACCOUNT -> AccountScreen(
                    state = state,
                    onSaveUsername = { newUsername ->
                        viewModel.saveSettings(state.settings.copy(leetcodeUsername = newUsername))
                    },
                    onValidateProfile = { username -> viewModel.validateProfile(username) },
                    onOpenProfile = { viewModel.openLeetCode(state.settings.leetcodeUsername) }
                )

                SentinelTab.STATS -> {
                    var showDiagnostics by remember { mutableStateOf(false) }
                    if (showDiagnostics) {
                        BackHandler { showDiagnostics = false }
                        HistoryDiagnosticsScreen(
                            state = state,
                            onResetAllData = { viewModel.resetAllData() }
                        )
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { showDiagnostics = true },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder)
                                ) {
                                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = SentinelBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Event Logs & Diagnostics", color = SentinelTextPrimary, fontSize = 11.sp)
                                }
                            }
                            StatisticsScreen(
                                state = state,
                                onExportCsv = { viewModel.repository.exportHistoryAsCsv() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Full Screen Alarm Overlay when alarm rings
            if (state.alarmSession?.status == "ACTIVE" || state.isTestAlarmActive) {
                ActiveAlarmOverlay(
                    isTest = state.isTestAlarmActive,
                    onSnooze = { viewModel.snoozeAlarm() },
                    onStop = { viewModel.stopAlarm() },
                    onOpenLeetCode = { viewModel.openLeetCode(state.settings.leetcodeUsername) },
                    onMarkComplete = { viewModel.markTodayManualComplete("Confirmed during active alarm") }
                )
            }
        }
    }
}

@Composable
fun ActiveAlarmOverlay(
    isTest: Boolean,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onOpenLeetCode: () -> Unit,
    onMarkComplete: () -> Unit
) {
    Surface(
        color = Color(0xFB0A0D14),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                    .border(2.dp, SentinelRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SentinelRed,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isTest) "TEST ALARM ACTIVE" else "CUTOFF TIME REACHED!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = SentinelRed,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isTest)
                    "This is an audio & vibration verification test. Your streak status has not changed."
                else "Today's LeetCode problem has not been verified! Solve your daily problem now to protect your streak.",
                fontSize = 14.sp,
                color = SentinelTextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).testTag("alarm_overlay_snooze_button")
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Snooze 5m", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelRed),
                    modifier = Modifier.weight(1f).testTag("alarm_overlay_stop_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenLeetCode,
                border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                modifier = Modifier.fillMaxWidth().testTag("alarm_overlay_leetcode_button")
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = SentinelBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open LeetCode & Solve Now", color = SentinelTextPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onMarkComplete,
                border = androidx.compose.foundation.BorderStroke(1.dp, SentinelAmber),
                modifier = Modifier.fillMaxWidth().testTag("alarm_overlay_mark_complete_button")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SentinelAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Complete Manually", color = SentinelAmber)
            }
        }
    }
}
