package com.example.ui

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupOperationMode
import com.example.i18n.LocalAppStrings
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.adaptiveCardColor
import com.example.ui.theme.adaptiveCardCorner
import com.example.ui.theme.glassCardBorder
import cu.daniel.anical.R

@Composable
fun OnboardingScreen(
    uiState: AnimeUiState = AnimeUiState(),
    onLocalTreeGranted: (Uri?) -> Unit = {},
    onLoadLocalBackups: () -> Unit = {},
    onRestoreLocalBackup: (Uri, String) -> Unit = { _, _ -> },
    onRequestDriveAccess: (() -> Unit, (IntentSender) -> Unit) -> Unit = { _, _ -> },
    onDriveResolutionResult: (Int, Intent?, () -> Unit, (IntentSender) -> Unit) -> Unit = { _, _, _, _ -> },
    onLoadDriveBackups: () -> Unit = {},
    onRestoreDriveBackup: (String, String) -> Unit = { _, _ -> },
    onClearBackupMessages: () -> Unit = {},
    onOnboardingFinished: () -> Unit
) {
    val isGlass = LocalIsLiquidGlass.current
    val s = LocalAppStrings.current
    var showRestoreDialog by remember { mutableStateOf(false) }

    val backdropModifier = if (isGlass) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0E1525),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    }

    Box(
        modifier = backdropModifier
            .safeDrawingPadding()
            .testTag("onboarding_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = if (isGlass) glassCardBorder(highContrast = true) else null,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(68.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = "AniVexa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = s.onboardingTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = s.onboardingSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Highlighted card announcing encrypted .avb backup & instant personal database restore
                Surface(
                    shape = RoundedCornerShape(adaptiveCardCorner(18.dp, 20.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isGlass) 0.28f else 0.22f),
                    border = glassCardBorder(highContrast = true),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_backup_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = s.splashNewBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = s.splashBackupTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = s.splashBackupDesc,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                onClearBackupMessages()
                                showRestoreDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_restore_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = s.splashRestoreBtn,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OnboardingFeatureCard(
                    icon = Icons.Default.NotificationsActive,
                    title = s.onboardingFeature1Title,
                    description = s.onboardingFeature1Desc
                )

                Spacer(modifier = Modifier.height(10.dp))

                OnboardingFeatureCard(
                    icon = Icons.Default.BookmarkAdded,
                    title = s.onboardingFeature2Title,
                    description = s.onboardingFeature2Desc
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onOnboardingFinished,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_continue_button")
            ) {
                Text(
                    text = s.onboardingStartBtn,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showRestoreDialog) {
        BackupRestoreDialog(
            mode = BackupOperationMode.RESTORE,
            uiState = uiState,
            onDismiss = {
                showRestoreDialog = false
                onClearBackupMessages()
            },
            onLocalTreeGranted = onLocalTreeGranted,
            onCreateLocalBackup = {},
            onLoadLocalBackups = onLoadLocalBackups,
            onRestoreLocalBackup = onRestoreLocalBackup,
            onRequestDriveAccess = onRequestDriveAccess,
            onDriveResolutionResult = onDriveResolutionResult,
            onCreateDriveBackup = {},
            onLoadDriveBackups = onLoadDriveBackups,
            onRestoreDriveBackup = onRestoreDriveBackup,
            onClearMessages = onClearBackupMessages,
            onCompletedContinue = {
                showRestoreDialog = false
                onClearBackupMessages()
                onOnboardingFinished()
            }
        )
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(adaptiveCardCorner(14.dp, 18.dp)),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
