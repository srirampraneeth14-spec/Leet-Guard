package com.example.ui.account

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SentinelUiState
import com.example.ui.components.SentinelCard
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBlue
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextMuted
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary

@Composable
fun AccountScreen(
    state: SentinelUiState,
    onSaveUsername: (String) -> Unit,
    onValidateProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember(state.settings.leetcodeUsername) {
        mutableStateOf(state.settings.leetcodeUsername)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Username Input Card
        SentinelCard(
            title = "LeetCode Profile Connection",
            subtitle = "Public GraphQL profile integration",
            icon = Icons.Default.AccountCircle
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("LeetCode Username") },
                placeholder = { Text("e.g. neetcode, tourist, sriram") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SentinelSurfaceVariant,
                    unfocusedContainerColor = SentinelSurfaceVariant,
                    focusedTextColor = SentinelTextPrimary,
                    unfocusedTextColor = SentinelTextPrimary,
                    focusedIndicatorColor = SentinelGreen,
                    unfocusedIndicatorColor = SentinelBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leetcode_username_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val trimmed = usernameInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSaveUsername(trimmed)
                            onValidateProfile(trimmed)
                        }
                    },
                    enabled = usernameInput.isNotBlank() && !state.isValidatingProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("validate_save_profile_button")
                ) {
                    if (state.isValidatingProfile) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validating...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate & Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onOpenProfile,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_profile_button")
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = SentinelBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Profile", color = SentinelTextPrimary, fontSize = 13.sp)
                }
            }

            // Validation Error if any
            if (state.validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = SentinelRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = state.validationError, color = Color(0xFFFECACA), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Validated Profile Telemetry Card (if fetched)
        AnimatedVisibility(visible = state.validatedProfile != null) {
            val prof = state.validatedProfile
            if (prof != null) {
                SentinelCard(
                    title = "Profile Telemetry: ${prof.username}",
                    subtitle = prof.realName ?: "Verified Public Profile",
                    icon = Icons.Default.Person
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SentinelSurfaceVariant)
                                    .border(1.dp, SentinelGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prof.username.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = SentinelGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = prof.username,
                                    fontWeight = FontWeight.Bold,
                                    color = SentinelTextPrimary,
                                    fontSize = 16.sp
                                )
                                if (prof.ranking != null) {
                                    Text(
                                        text = "Global Rank: #${prof.ranking}",
                                        color = SentinelBlue,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFF064E3B),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SentinelGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SentinelGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online", color = SentinelGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Solved Count Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Total", prof.totalSolved.toString(), SentinelTextPrimary, Modifier.weight(1f))
                        StatPill("Easy", prof.easySolved.toString(), Color(0xFF00B8A3), Modifier.weight(1f))
                        StatPill("Med", prof.mediumSolved.toString(), Color(0xFFFFC01E), Modifier.weight(1f))
                        StatPill("Hard", prof.hardSolved.toString(), Color(0xFFFF375F), Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Zero-Credential Security Card
        SentinelCard(
            title = "Zero-Credential Security Architecture",
            icon = Icons.Default.Lock
        ) {
            Text(
                text = "• No Passwords or Cookies: Leet Guard NEVER asks for or stores your LeetCode password, session tokens, or cookies.\n\n" +
                        "• Public Telemetry Only: Activity is retrieved exclusively through LeetCode's public GraphQL endpoints and user profile feeds.\n\n" +
                        "• On-Device Processing: All streak tracking, alarms, and history calculations run strictly local to your Android device with zero cloud telemetry uploads.",
                color = SentinelTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = SentinelSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SentinelBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = SentinelTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
