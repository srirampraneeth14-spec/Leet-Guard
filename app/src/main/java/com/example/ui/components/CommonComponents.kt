package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EvidenceSource
import com.example.domain.model.VerificationStatus
import com.example.ui.theme.SentinelAmber
import com.example.ui.theme.SentinelBorder
import com.example.ui.theme.SentinelGreen
import com.example.ui.theme.SentinelRed
import com.example.ui.theme.SentinelSurface
import com.example.ui.theme.SentinelSurfaceVariant
import com.example.ui.theme.SentinelTextMuted
import com.example.ui.theme.SentinelTextPrimary
import com.example.ui.theme.SentinelTextSecondary

@Composable
fun SentinelCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SentinelBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SentinelSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SentinelTextPrimary
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SentinelTextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    evidenceSource: String? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when {
        status == VerificationStatus.VERIFIED_ACCEPTED.name -> {
            Quad(
                Color(0xFF064E3B),
                SentinelGreen,
                "Verified Accepted",
                Icons.Default.CheckCircle
            )
        }
        evidenceSource == EvidenceSource.MANUAL_CONFIRMATION.name ||
        status == VerificationStatus.ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN.name -> {
            Quad(
                Color(0xFF78350F),
                SentinelAmber,
                "Manual Confirmation",
                Icons.Default.Info
            )
        }
        status == VerificationStatus.NO_QUALIFYING_SUBMISSION_FOUND.name -> {
            Quad(
                Color(0xFF451A03),
                Color(0xFFFB923C),
                "Pending Submission",
                Icons.Default.Schedule
            )
        }
        status == VerificationStatus.VERIFICATION_UNAVAILABLE.name -> {
            Quad(
                Color(0xFF374151),
                Color(0xFFE5E7EB),
                "Verification Unavailable",
                Icons.Default.Warning
            )
        }
        status == VerificationStatus.PROFILE_INVALID.name -> {
            Quad(
                Color(0xFF7F1D1D),
                SentinelRed,
                "Profile Not Configured",
                Icons.Default.Error
            )
        }
        else -> {
            Quad(
                Color(0xFF1E293B),
                SentinelTextSecondary,
                "Status: $status",
                Icons.Default.Info
            )
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = SentinelTextPrimary) },
        text = { Text(message, color = SentinelTextSecondary) },
        containerColor = SentinelSurfaceVariant,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text(dismissText, color = SentinelTextSecondary)
            }
        }
    )
}
