package com.github.dfedez920912.anivexa.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import com.github.dfedez920912.anivexa.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.github.dfedez920912.anivexa.backup.BackupOperationMode
import com.github.dfedez920912.anivexa.backup.GoogleDriveBackupClient
import com.github.dfedez920912.anivexa.backup.LocalBackupStorage
import com.github.dfedez920912.anivexa.i18n.AppLocalization
import com.github.dfedez920912.anivexa.i18n.LocalAppLanguage
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.model.AppThemeMode
import com.github.dfedez920912.anivexa.model.SynopsisLanguage
import com.github.dfedez920912.anivexa.ui.components.BackupRestoreDialog
import com.github.dfedez920912.anivexa.ui.theme.AniGlassSheetSurface
import com.github.dfedez920912.anivexa.ui.theme.LocalBottomDockPadding
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import com.github.dfedez920912.anivexa.ui.theme.screenBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AnimeUiState,
    onToggleTimeZone: () -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetDefaultReminderMinutes: (Int) -> Unit,
    onToggleQuietHours: () -> Unit,
    onSetGlobalLanguage: (SynopsisLanguage) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onCheckDeviceSecurity: () -> Unit = {},
    onLocalTreeGranted: (Uri?, () -> Unit) -> Unit = { _, _ -> },
    onCreateLocalBackup: () -> Unit = {},
    onLoadLocalBackups: () -> Unit = {},
    onRestoreLocalBackup: (Uri) -> Unit = {},
    onRequestDriveAccess: (Context, () -> Unit, (IntentSender) -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _ -> },
    onDriveResolutionResult: (Context, Int, Intent?, () -> Unit, (IntentSender) -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onDriveActivityResult: (Context, Int, Intent?, () -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onCreateDriveBackup: () -> Unit = {},
    onLoadDriveBackups: () -> Unit = {},
    onRestoreDriveBackup: (String) -> Unit = {},
    onClearBackupMessages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val bottomDockPadding = LocalBottomDockPadding.current
    var activeBackupDialogMode by remember { mutableStateOf<BackupOperationMode?>(null) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotificationsEnabled(granted)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .screenBackdrop()
            .testTag("settings_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp + bottomDockPadding
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = s.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Notifications Section
        item {
            SettingsCard(title = s.settingsNotifSection, icon = R.drawable.ic_anime_notifications_active_filled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.settingsAlertsTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            s.settingsAlertsSubtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetNotificationsEnabled(true)
                                }
                            } else {
                                onSetNotificationsEnabled(enabled)
                            }
                        },
                        modifier = Modifier.testTag("settings_notif_switch")
                    )
                }

                if (uiState.notificationsEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = s.settingsDefaultLeadTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            15 to s.settingsMinUnit(15),
                            30 to s.settingsMinUnit(30),
                            60 to s.settingsHourUnit,
                            0 to s.settingsOnAiring
                        ).forEach { (mins, label) ->
                            FilterChip(
                                selected = uiState.defaultReminderMinutes == mins,
                                onClick = { onSetDefaultReminderMinutes(mins) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.settingsQuietHoursTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            Text(
                                s.settingsQuietHoursSubtitle,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.quietHoursEnabled,
                            onCheckedChange = { onToggleQuietHours() }
                        )
                    }
                }
            }
        }

        // Timezone & App Language Section
        item {
            SettingsCard(title = s.settingsRegionLangSection, icon = R.drawable.ic_anime_public_filled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.settingsJstTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (uiState.isJapanTimeZone) s.settingsJstSubtitleOn else s.settingsJstSubtitleOff,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isJapanTimeZone,
                        onCheckedChange = { onToggleTimeZone() },
                        modifier = Modifier.testTag("settings_jst_switch")
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(s.settingsLanguageTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = s.settingsLanguageSubtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = languageMenuExpanded,
                        onExpandedChange = { languageMenuExpanded = !languageMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = appLanguage.nativeName,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("settings_language_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false },
                            containerColor = if (LocalIsLiquidGlass.current) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
                        ) {
                            SynopsisLanguage.entries.forEach { lang ->
                                val isSelected = uiState.globalLanguage == lang
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lang.nativeName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = lang.shortLabel,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        languageMenuExpanded = false
                                        onSetGlobalLanguage(lang)
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    modifier = Modifier.testTag("settings_lang_option_${lang.code}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Appearance Section
        item {
            SettingsCard(title = s.settingsAppearanceSection, icon = R.drawable.ic_anime_palette_filled) {
                val isGlass = LocalIsLiquidGlass.current
                AppThemeMode.entries.forEach { mode ->
                    val selected = uiState.appThemeMode == mode
                    Surface(
                        shape = RoundedCornerShape(if (isGlass) 14.dp else 10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (selected && isGlass) glassCardBorder(highContrast = true) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetThemeMode(mode) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = AppLocalization.themeModeLabel(mode, s),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppLocalization.themeModeDescription(mode, s),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Offline Storage Section
        item {
            SettingsCard(title = s.settingsOfflineSection, icon = R.drawable.ic_anime_cloud_download_filled) {
                Text(
                    text = s.settingsCachedSeriesCount(uiState.cachedCount),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(s.settingsSyncNow, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(s.settingsClearCache, fontSize = 12.sp)
                    }
                }
            }
        }

        // Encrypted Backup & Restore Section (.avb)
        item {
            SettingsCard(title = s.settingsBackupSection, icon = R.drawable.ic_anime_settings_filled) {
                Text(
                    text = s.settingsBackupDesc(
                        LocalBackupStorage.BACKUP_FOLDER_NAME,
                        GoogleDriveBackupClient.DRIVE_FOLDER_NAME
                    ),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onClearBackupMessages()
                            activeBackupDialogMode = BackupOperationMode.BACKUP
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("settings_create_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(s.settingsBackupBtn, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            onClearBackupMessages()
                            activeBackupDialogMode = BackupOperationMode.RESTORE
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("settings_restore_backup_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(s.settingsRestoreBtn, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Security & Device Integrity Section
        item {
            val sec = uiState.deviceSecurityReport
            SettingsCard(title = s.settingsSecuritySection, icon = R.drawable.ic_anime_security_filled) {
                Text(
                    text = if (sec.isRooted) {
                        s.settingsRootDetected(sec.rootIndicators.joinToString())
                    } else {
                        s.settingsNoRootDetected
                    },
                    fontSize = 12.5.sp,
                    color = if (sec.isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s.settingsPlayIntegrityLine(
                        AppLocalization.playIntegrityLabel(sec.playIntegrityStatus, s),
                        AppLocalization.playIntegrityDetail(sec.playIntegrityStatus, s)
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.settingsHardeningInfo,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCheckDeviceSecurity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_security_check_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(s.settingsVerifySecurityBtn, fontSize = 12.sp)
                }
            }
        }

        // About AniVexa
        item {
            val uriHandler = LocalUriHandler.current
            SettingsCard(title = s.settingsAboutSection, icon = R.drawable.ic_anime_info_filled) {
                Text(
                    text = s.settingsAboutTagline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.settingsAboutDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/dfedez920912/AniVexa/blob/main/PRIVACY_POLICY.md")
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(s.settingsPrivacyPolicyTitle, fontSize = 12.5.sp)
                }
            }
        }
    }

    activeBackupDialogMode?.let { mode ->
        BackupRestoreDialog(
            mode = mode,
            uiState = uiState,
            onDismiss = {
                activeBackupDialogMode = null
                onClearBackupMessages()
            },
            onLocalTreeGranted = onLocalTreeGranted,
            onCreateLocalBackup = onCreateLocalBackup,
            onLoadLocalBackups = onLoadLocalBackups,
            onRestoreLocalBackup = onRestoreLocalBackup,
            onRequestDriveAccess = onRequestDriveAccess,
            onDriveResolutionResult = onDriveResolutionResult,
            onDriveActivityResult = onDriveActivityResult,
            onCreateDriveBackup = onCreateDriveBackup,
            onLoadDriveBackups = onLoadDriveBackups,
            onRestoreDriveBackup = onRestoreDriveBackup,
            onClearMessages = onClearBackupMessages
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: Int,
    content: @Composable () -> Unit
) {
    val corner = adaptiveCardCorner(standard = 16.dp, glass = 22.dp)
    Surface(
        shape = RoundedCornerShape(corner),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}
