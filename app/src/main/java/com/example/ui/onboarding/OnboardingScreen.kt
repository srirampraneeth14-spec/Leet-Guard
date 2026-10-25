package com.example.ui.onboarding

import android.os.Build
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserSettingsEntity
import com.example.ui.SentinelUiState
import com.example.ui.alarm.TimeSelector
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBlue
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurface
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary

@Composable
fun OnboardingScreen(
    state: SentinelUiState,
    onSaveSettings: (UserSettingsEntity) -> Unit,
    onValidateProfile: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onTestAlarm: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    var usernameInput by remember(state.settings.leetcodeUsername) {
        mutableStateOf(state.settings.leetcodeUsername)
    }
    var tempSettings by remember(state.settings) { mutableStateOf(state.settings) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicator Dots
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            for (i in 1..4) {
                Box(
                    modifier = Modifier
                        .size(if (step == i) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (step == i) SentinelGreen else if (step > i) SentinelBlue else Color(0xFF1E293B))
                )
                if (i < 4) Spacer(modifier = Modifier.width(6.dp))
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (step) {
                1 -> {
                    // Step 1: Mission & Welcome
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(SentinelGreen.copy(alpha = 0.3f), Color(0xFF1E293B))
                                    )
                                )
                                .border(2.dp, SentinelGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = SentinelGreen,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "LEET GUARD",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = SentinelTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Never Lose Your Problem-Solving Habit Again",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SentinelGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Built specifically to safeguard your daily coding streak. Leet Guard monitors your public LeetCode activity, reminds you gently throughout the day, and escalates to an audible alarm at cutoff time if today's problem remains unsolved.",
                            fontSize = 13.sp,
                            color = SentinelTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                2 -> {
                    // Step 2: LeetCode Profile Setup
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SentinelBlue, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Connect LeetCode Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SentinelTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your public username. No passwords, tokens, or cookies required.",
                            fontSize = 12.sp,
                            color = SentinelTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("LeetCode Username") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SentinelSurfaceVariant,
                                unfocusedContainerColor = SentinelSurfaceVariant,
                                focusedTextColor = SentinelTextPrimary,
                                unfocusedTextColor = SentinelTextPrimary,
                                focusedIndicatorColor = SentinelGreen,
                                unfocusedIndicatorColor = SentinelBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val clean = usernameInput.trim()
                                if (clean.isNotBlank()) {
                                    tempSettings = tempSettings.copy(leetcodeUsername = clean)
                                    onSaveSettings(tempSettings)
                                    onValidateProfile(clean)
                                }
                            },
                            enabled = usernameInput.isNotBlank() && !state.isValidatingProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_validate_button")
                        ) {
                            if (state.isValidatingProfile) {
                                CircularProgressIndicator(color = SentinelGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifying profile...", color = SentinelGreen)
                            } else {
                                Text("Verify Username with LeetCode", color = SentinelGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state.validatedProfile != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SentinelGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verified ${state.validatedProfile.username} (${state.validatedProfile.totalSolved} solved)",
                                        color = SentinelGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3: Cutoff and Schedule
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = SentinelAmber, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Configure Daily Cutoff", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SentinelTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set the cutoff time when the alarm should ring if no accepted solution is verified.",
                            fontSize = 12.sp,
                            color = SentinelTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cutoff Hour (24h)", color = SentinelTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TimeSelector(
                                    value = tempSettings.cutoffHour,
                                    maxValue = 23,
                                    onValueChange = {
                                        tempSettings = tempSettings.copy(cutoffHour = it)
                                        onSaveSettings(tempSettings)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cutoff Minute", color = SentinelTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TimeSelector(
                                    value = tempSettings.cutoffMinute,
                                    maxValue = 59,
                                    onValueChange = {
                                        tempSettings = tempSettings.copy(cutoffMinute = it)
                                        onSaveSettings(tempSettings)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = SentinelSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cutoff set to ${String.format("%02d:%02d", tempSettings.cutoffHour, tempSettings.cutoffMinute)} (${tempSettings.timezoneId})",
                                modifier = Modifier.padding(12.dp),
                                color = SentinelAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                4 -> {
                    // Step 4: Permissions & Alarm Test
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = SentinelGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Notifications & Alarm Audio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SentinelTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ensure notifications and alarm audio are enabled to never miss a cutoff reminder.",
                            fontSize = 12.sp,
                            color = SentinelTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onRequestNotifications,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().testTag("grant_notif_button")
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Notification Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onTestAlarm,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SentinelAmber),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_test_alarm_button")
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = SentinelAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample 3s Alarm Audio & Vibration", color = SentinelAmber)
                        }
                    }
                }
            }
        }

        // Navigation Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = SentinelTextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", color = SentinelTextSecondary)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (step < 4) {
                Button(
                    onClick = { step++ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            } else {
                Button(
                    onClick = {
                        val clean = usernameInput.trim()
                        if (clean.isNotBlank()) {
                            tempSettings = tempSettings.copy(leetcodeUsername = clean)
                            onSaveSettings(tempSettings)
                        }
                        onCompleteOnboarding()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGreen),
                    modifier = Modifier.testTag("activate_sentinel_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Activate Leet Guard", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
