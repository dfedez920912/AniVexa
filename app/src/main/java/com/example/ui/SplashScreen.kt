package com.example.ui

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.glassCardBorder
import cu.daniel.anical.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    uiState: AnimeUiState = AnimeUiState(),
    onLocalTreeGranted: (Uri?) -> Unit = {},
    onLoadLocalBackups: () -> Unit = {},
    onRestoreLocalBackup: (Uri, String) -> Unit = { _, _ -> },
    onRequestDriveAccess: (() -> Unit, (IntentSender) -> Unit) -> Unit = { _, _ -> },
    onDriveResolutionResult: (Int, Intent?, () -> Unit) -> Unit = { _, _, _ -> },
    onLoadDriveBackups: () -> Unit = {},
    onRestoreDriveBackup: (String, String) -> Unit = { _, _ -> },
    onClearBackupMessages: () -> Unit = {},
    onSplashFinished: () -> Unit
) {
    val isGlass = LocalIsLiquidGlass.current
    val s = LocalAppStrings.current
    var startAnimation by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pauseAutoAdvance by remember { mutableStateOf(false) }
    var countdownProgress by remember { mutableFloatStateOf(0f) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(showRestoreDialog, pauseAutoAdvance) {
        if (!showRestoreDialog && !pauseAutoAdvance) {
            val totalSteps = 32
            for (step in 1..totalSteps) {
                if (showRestoreDialog || pauseAutoAdvance) return@LaunchedEffect
                countdownProgress = step / totalSteps.toFloat()
                delay(100)
            }
            if (!showRestoreDialog && !pauseAutoAdvance) {
                onSplashFinished()
            }
        }
    }

    val splashModifier = if (isGlass) {
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
        modifier = splashModifier.safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .alpha(alphaAnim),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                border = if (isGlass) glassCardBorder(highContrast = true) else null,
                shadowElevation = 12.dp,
                modifier = Modifier.size(104.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "AniVexa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AniVexa",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = s.splashSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Announcement & quick restore card for encrypted .avb backups
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = adaptiveCardColor(),
                border = glassCardBorder(highContrast = true),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("splash_backup_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = s.splashNewBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = s.splashBackupTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = s.splashBackupDesc,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pauseAutoAdvance = true
                                onClearBackupMessages()
                                showRestoreDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("splash_restore_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(s.splashRestoreBtn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                pauseAutoAdvance = true
                                onSplashFinished()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("splash_continue_button")
                        ) {
                            Text(s.splashContinueBtn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!pauseAutoAdvance && !showRestoreDialog) {
                        LinearProgressIndicator(
                            progress = { countdownProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
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
                onSplashFinished()
            }
        )
    }
}
