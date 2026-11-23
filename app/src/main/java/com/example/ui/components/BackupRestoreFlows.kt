package com.example.ui.components

import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupCrypto
import com.example.backup.BackupDestination
import com.example.backup.BackupEntry
import com.example.backup.BackupOperationMode
import com.example.backup.LocalBackupStorage
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.AnimeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BackupDialogStep {
    CHOOSE_DESTINATION,
    REQUEST_LOCAL_PERMISSION,
    ENTER_BACKUP_PASSPHRASE,
    PICK_RESTORE_FILE,
    ENTER_RESTORE_PASSPHRASE
}

@Composable
fun BackupRestoreDialog(
    mode: BackupOperationMode,
    uiState: AnimeUiState,
    onDismiss: () -> Unit,
    onLocalTreeGranted: (Uri?) -> Unit,
    onCreateLocalBackup: (String) -> Unit,
    onLoadLocalBackups: () -> Unit,
    onRestoreLocalBackup: (Uri, String) -> Unit,
    onRequestDriveAccess: (onAuthorized: () -> Unit, onNeedsResolution: (IntentSender) -> Unit) -> Unit,
    onDriveResolutionResult: (Int, android.content.Intent?, onAuthorized: () -> Unit, onNeedsResolution: (IntentSender) -> Unit) -> Unit,
    onCreateDriveBackup: (String) -> Unit,
    onLoadDriveBackups: () -> Unit,
    onRestoreDriveBackup: (String, String) -> Unit,
    onClearMessages: () -> Unit,
    onCompletedContinue: (() -> Unit)? = null
) {
    val s = LocalAppStrings.current
    var step by rememberSaveable(mode) { mutableStateOf(BackupDialogStep.CHOOSE_DESTINATION) }
    var selectedDestination by rememberSaveable(mode) { mutableStateOf<BackupDestination?>(null) }
    var selectedEntry by remember(mode) { mutableStateOf<BackupEntry?>(null) }
    var passphrase by rememberSaveable(mode) { mutableStateOf("") }
    var confirmPassphrase by rememberSaveable(mode) { mutableStateOf("") }
    var showPassword by rememberSaveable(mode) { mutableStateOf(false) }
    var localValidationError by remember(mode) { mutableStateOf<String?>(null) }
    val currentOnLoadDriveBackups by rememberUpdatedState(onLoadDriveBackups)
    val currentOnDriveResolutionResult by rememberUpdatedState(onDriveResolutionResult)

    LaunchedEffect(mode) {
        onClearMessages()
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { fileUri ->
        if (fileUri != null) {
            selectedDestination = BackupDestination.LOCAL
            selectedEntry = BackupEntry(
                id = fileUri.toString(),
                fileName = fileUri.lastPathSegment?.substringAfterLast('/') ?: "respaldo.avb",
                sizeBytes = 0L,
                modifiedMillis = System.currentTimeMillis(),
                source = BackupDestination.LOCAL
            )
            localValidationError = null
            onClearMessages()
            step = BackupDialogStep.ENTER_RESTORE_PASSPHRASE
        }
    }

    val localTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        onLocalTreeGranted(treeUri)
        if (treeUri != null) {
            selectedDestination = BackupDestination.LOCAL
            if (mode == BackupOperationMode.BACKUP) {
                step = BackupDialogStep.ENTER_BACKUP_PASSPHRASE
            } else {
                onLoadLocalBackups()
                step = BackupDialogStep.PICK_RESTORE_FILE
            }
        }
    }

    lateinit var launchDriveResolution: (IntentSender) -> Unit
    val driveResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        currentOnDriveResolutionResult(
            activityResult.resultCode,
            activityResult.data,
            {
                selectedDestination = BackupDestination.GOOGLE_DRIVE
                if (mode == BackupOperationMode.BACKUP) {
                    step = BackupDialogStep.ENTER_BACKUP_PASSPHRASE
                } else {
                    currentOnLoadDriveBackups()
                    step = BackupDialogStep.PICK_RESTORE_FILE
                }
            },
            { nextIntentSender ->
                launchDriveResolution(nextIntentSender)
            }
        )
    }
    launchDriveResolution = { intentSender ->
        driveResolutionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    val dialogTitle = when {
        uiState.backupStatusMessage != null ->
            if (mode == BackupOperationMode.BACKUP) s.backupDoneTitle else s.restoreDoneTitle
        step == BackupDialogStep.CHOOSE_DESTINATION ->
            if (mode == BackupOperationMode.BACKUP) s.backupChooseDestTitle else s.restoreChooseDestTitle
        step == BackupDialogStep.REQUEST_LOCAL_PERMISSION ->
            s.backupLocalPermTitle
        step == BackupDialogStep.ENTER_BACKUP_PASSPHRASE ->
            s.backupEncryptTitle
        step == BackupDialogStep.PICK_RESTORE_FILE ->
            s.restorePickFileTitle
        else ->
            s.restoreDecryptTitle
    }

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isBackupBusy) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = dialogTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.backupStatusMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.backupStatusMessage,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    val activeError = localValidationError ?: uiState.backupErrorMessage
                    if (activeError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeError,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (uiState.isBackupBusy) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = if (mode == BackupOperationMode.BACKUP) {
                                    s.backupBusyCreating
                                } else {
                                    s.backupBusyRestoring
                                },
                                fontSize = 13.sp
                            )
                        }
                    }

                    when (step) {
                        BackupDialogStep.CHOOSE_DESTINATION -> {
                            Text(
                                text = if (mode == BackupOperationMode.BACKUP) {
                                    s.backupChooseDestPrompt
                                } else {
                                    s.restoreChooseDestPrompt
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DestinationOptionCard(
                                title = s.backupLocalTitle,
                                subtitle = s.backupLocalSubtitle(LocalBackupStorage.BACKUP_FOLDER_NAME),
                                icon = Icons.Default.PhoneAndroid,
                                testTag = "backup_dest_local",
                                enabled = !uiState.isBackupBusy,
                                onClick = {
                                    onClearMessages()
                                    localValidationError = null
                                    selectedDestination = BackupDestination.LOCAL
                                    if (uiState.hasLocalBackupFolder) {
                                        if (mode == BackupOperationMode.BACKUP) {
                                            step = BackupDialogStep.ENTER_BACKUP_PASSPHRASE
                                        } else {
                                            onLoadLocalBackups()
                                            step = BackupDialogStep.PICK_RESTORE_FILE
                                        }
                                    } else {
                                        step = BackupDialogStep.REQUEST_LOCAL_PERMISSION
                                    }
                                }
                            )

                            DestinationOptionCard(
                                title = s.backupDriveTitle,
                                subtitle = s.backupDriveSubtitle,
                                icon = Icons.Default.CloudUpload,
                                testTag = "backup_dest_drive",
                                enabled = !uiState.isBackupBusy,
                                onClick = {
                                    onClearMessages()
                                    localValidationError = null
                                    selectedDestination = BackupDestination.GOOGLE_DRIVE
                                    onRequestDriveAccess(
                                        {
                                            if (mode == BackupOperationMode.BACKUP) {
                                                step = BackupDialogStep.ENTER_BACKUP_PASSPHRASE
                                            } else {
                                                onLoadDriveBackups()
                                                step = BackupDialogStep.PICK_RESTORE_FILE
                                            }
                                        },
                                        { intentSender ->
                                            driveResolutionLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }
                                    )
                                }
                            )
                        }

                        BackupDialogStep.REQUEST_LOCAL_PERMISSION -> {
                            Text(
                                text = s.backupLocalPermBody(LocalBackupStorage.BACKUP_FOLDER_NAME),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    onClearMessages()
                                    val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        runCatching {
                                            DocumentsContract.buildDocumentUri(
                                                "com.android.externalstorage.documents",
                                                "primary:"
                                            )
                                        }.getOrNull()
                                    } else {
                                        null
                                    }
                                    localTreeLauncher.launch(initialUri)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_grant_local_perm_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(s.backupGrantFolderBtn)
                            }

                            if (mode == BackupOperationMode.RESTORE) {
                                OutlinedButton(
                                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("backup_pick_manual_file_button")
                                ) {
                                    Text(s.restorePickManualBtn)
                                }
                            }
                        }

                        BackupDialogStep.ENTER_BACKUP_PASSPHRASE -> {
                            val targetLabel = if (selectedDestination == BackupDestination.GOOGLE_DRIVE) {
                                s.backupTargetDrivePath
                            } else {
                                s.backupTargetLocalPath(LocalBackupStorage.BACKUP_FOLDER_NAME)
                            }
                            Text(
                                text = s.backupEnterPassPrompt(targetLabel),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            PassphraseTextField(
                                value = passphrase,
                                onValueChange = {
                                    passphrase = it
                                    localValidationError = null
                                },
                                label = s.backupPassLabel,
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
                                hideDesc = s.passHideDesc,
                                showDesc = s.passShowDesc,
                                testTag = "backup_password_input"
                            )

                            PassphraseTextField(
                                value = confirmPassphrase,
                                onValueChange = {
                                    confirmPassphrase = it
                                    localValidationError = null
                                },
                                label = s.backupConfirmPassLabel,
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
                                hideDesc = s.passHideDesc,
                                showDesc = s.passShowDesc,
                                testTag = "backup_confirm_password_input"
                            )
                        }

                        BackupDialogStep.PICK_RESTORE_FILE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val folderHint = if (selectedDestination == BackupDestination.GOOGLE_DRIVE) {
                                    s.restoreFolderHintDrive
                                } else {
                                    s.restoreFolderHintLocal(LocalBackupStorage.BACKUP_FOLDER_NAME)
                                }
                                Text(
                                    text = folderHint,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        if (selectedDestination == BackupDestination.GOOGLE_DRIVE) {
                                            onLoadDriveBackups()
                                        } else {
                                            onLoadLocalBackups()
                                        }
                                    },
                                    enabled = !uiState.isBackupBusy
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = s.restoreRefreshListDesc)
                                }
                            }

                            if (uiState.availableBackups.isEmpty() && !uiState.isBackupBusy) {
                                Text(
                                    text = s.restoreNoFilesFound,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.availableBackups, key = { it.id }) { entry ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            tonalElevation = 2.dp,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !uiState.isBackupBusy) {
                                                    selectedEntry = entry
                                                    localValidationError = null
                                                    onClearMessages()
                                                    step = BackupDialogStep.ENTER_RESTORE_PASSPHRASE
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = entry.fileName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = formatEntryMeta(entry, s),
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedDestination == BackupDestination.LOCAL) {
                                OutlinedButton(
                                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("restore_pick_other_file_button")
                                ) {
                                    Text(s.restoreBrowseOtherBtn)
                                }
                            }
                        }

                        BackupDialogStep.ENTER_RESTORE_PASSPHRASE -> {
                            val entry = selectedEntry
                            Text(
                                text = s.restoreWarningBody(entry?.fileName ?: "respaldo.avb"),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            PassphraseTextField(
                                value = passphrase,
                                onValueChange = {
                                    passphrase = it
                                    localValidationError = null
                                },
                                label = s.restorePassLabel,
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
                                hideDesc = s.passHideDesc,
                                showDesc = s.passShowDesc,
                                testTag = "restore_password_input"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                uiState.backupStatusMessage != null -> {
                    Button(
                        onClick = {
                            if (onCompletedContinue != null && mode == BackupOperationMode.RESTORE) {
                                onCompletedContinue()
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("backup_done_button")
                    ) {
                        Text(
                            if (onCompletedContinue != null && mode == BackupOperationMode.RESTORE) {
                                s.backupEnterAppBtn
                            } else {
                                s.backupDoneBtn
                            }
                        )
                    }
                }

                step == BackupDialogStep.ENTER_BACKUP_PASSPHRASE -> {
                    Button(
                        enabled = !uiState.isBackupBusy,
                        onClick = {
                            when {
                                !BackupCrypto.isValidPassphrase(passphrase) -> {
                                    localValidationError = s.backupErrorMinLength
                                }
                                passphrase.trim() != confirmPassphrase.trim() -> {
                                    localValidationError = s.backupErrorMismatch
                                }
                                selectedDestination == BackupDestination.GOOGLE_DRIVE -> {
                                    onCreateDriveBackup(passphrase)
                                }
                                else -> {
                                    onCreateLocalBackup(passphrase)
                                }
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("backup_confirm_create_button")
                    ) {
                        Text(s.backupCreateActionBtn)
                    }
                }

                step == BackupDialogStep.ENTER_RESTORE_PASSPHRASE -> {
                    Button(
                        enabled = !uiState.isBackupBusy && selectedEntry != null,
                        onClick = {
                            val entry = selectedEntry ?: return@Button
                            if (!BackupCrypto.isValidPassphrase(passphrase)) {
                                localValidationError = s.backupErrorEnterPassRestore
                                return@Button
                            }
                            if (entry.source == BackupDestination.GOOGLE_DRIVE) {
                                onRestoreDriveBackup(entry.id, passphrase)
                            } else {
                                onRestoreLocalBackup(Uri.parse(entry.id), passphrase)
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("restore_confirm_button")
                    ) {
                        Text(s.restoreActionBtn)
                    }
                }
            }
        },
        dismissButton = {
            if (uiState.backupStatusMessage == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (step != BackupDialogStep.CHOOSE_DESTINATION && !uiState.isBackupBusy) {
                        TextButton(
                            onClick = {
                                onClearMessages()
                                localValidationError = null
                                step = when (step) {
                                    BackupDialogStep.ENTER_RESTORE_PASSPHRASE -> {
                                        if (uiState.availableBackups.isNotEmpty()) {
                                            BackupDialogStep.PICK_RESTORE_FILE
                                        } else {
                                            BackupDialogStep.CHOOSE_DESTINATION
                                        }
                                    }
                                    else -> BackupDialogStep.CHOOSE_DESTINATION
                                }
                            }
                        ) {
                            Text(s.dialogBackBtn)
                        }
                    }
                    TextButton(
                        enabled = !uiState.isBackupBusy,
                        onClick = onDismiss
                    ) {
                        Text(s.dialogCloseBtn)
                    }
                }
            }
        }
    )
}

@Composable
private fun DestinationOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PassphraseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    hideDesc: String,
    showDesc: String,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.5.sp) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) hideDesc else showDesc
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

private fun formatEntryMeta(entry: BackupEntry, s: AppStrings): String {
    val dateStr = if (entry.modifiedMillis > 0L) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.modifiedMillis))
    } else {
        s.backupMetaFallback
    }
    val sizeStr = if (entry.sizeBytes > 0L) {
        val kb = (entry.sizeBytes / 1024.0).coerceAtLeast(0.1)
        String.format(Locale.US, "%.1f KB", kb)
    } else {
        s.backupMetaEncrypted
    }
    return "$dateStr • $sizeStr"
}
