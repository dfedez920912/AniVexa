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
    onDriveResolutionResult: (Int, android.content.Intent?, onAuthorized: () -> Unit) -> Unit,
    onCreateDriveBackup: (String) -> Unit,
    onLoadDriveBackups: () -> Unit,
    onRestoreDriveBackup: (String, String) -> Unit,
    onClearMessages: () -> Unit,
    onCompletedContinue: (() -> Unit)? = null
) {
    var step by remember(mode) { mutableStateOf(BackupDialogStep.CHOOSE_DESTINATION) }
    var selectedDestination by remember(mode) { mutableStateOf<BackupDestination?>(null) }
    var selectedEntry by remember(mode) { mutableStateOf<BackupEntry?>(null) }
    var passphrase by remember(mode) { mutableStateOf("") }
    var confirmPassphrase by remember(mode) { mutableStateOf("") }
    var showPassword by remember(mode) { mutableStateOf(false) }
    var localValidationError by remember(mode) { mutableStateOf<String?>(null) }

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

    val driveResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        onDriveResolutionResult(activityResult.resultCode, activityResult.data) {
            selectedDestination = BackupDestination.GOOGLE_DRIVE
            if (mode == BackupOperationMode.BACKUP) {
                step = BackupDialogStep.ENTER_BACKUP_PASSPHRASE
            } else {
                onLoadDriveBackups()
                step = BackupDialogStep.PICK_RESTORE_FILE
            }
        }
    }

    val dialogTitle = when {
        uiState.backupStatusMessage != null ->
            if (mode == BackupOperationMode.BACKUP) "Backup completado" else "Respaldo restaurado"
        step == BackupDialogStep.CHOOSE_DESTINATION ->
            if (mode == BackupOperationMode.BACKUP) "Crear copia de respaldo" else "Restaurar copia (.avb)"
        step == BackupDialogStep.REQUEST_LOCAL_PERMISSION ->
            "Permiso de almacenamiento"
        step == BackupDialogStep.ENTER_BACKUP_PASSPHRASE ->
            "Cifrar archivo .avb"
        step == BackupDialogStep.PICK_RESTORE_FILE ->
            "Seleccionar copia .avb"
        else ->
            "Descifrar y restaurar"
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
                                    "Cifrando y guardando respaldo (.avb)..."
                                } else {
                                    "Leyendo y descifrando respaldo (.avb)..."
                                },
                                fontSize = 13.sp
                            )
                        }
                    }

                    when (step) {
                        BackupDialogStep.CHOOSE_DESTINATION -> {
                            Text(
                                text = if (mode == BackupOperationMode.BACKUP) {
                                    "Elige dónde guardar tus datos cifrados en formato .avb:"
                                } else {
                                    "Elige desde dónde deseas recuperar tu archivo cifrado .avb:"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DestinationOptionCard(
                                title = "Almacenamiento Local",
                                subtitle = "Carpeta ${LocalBackupStorage.BACKUP_FOLDER_NAME} en tu dispositivo (.avb)",
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
                                title = "Google Drive",
                                subtitle = "Tu propia nube, carpeta AniVexa (.avb)",
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
                                text = "Concede acceso a una ubicación de tu dispositivo (por ejemplo la raíz del almacenamiento o Documentos). AniVexa creará ahí la carpeta \"${LocalBackupStorage.BACKUP_FOLDER_NAME}\" para guardar y restaurar tus copias cifradas .avb.",
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
                                Text("Conceder permiso de carpeta")
                            }

                            if (mode == BackupOperationMode.RESTORE) {
                                OutlinedButton(
                                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("backup_pick_manual_file_button")
                                ) {
                                    Text("Seleccionar archivo .avb manualmente")
                                }
                            }
                        }

                        BackupDialogStep.ENTER_BACKUP_PASSPHRASE -> {
                            val targetLabel = if (selectedDestination == BackupDestination.GOOGLE_DRIVE) {
                                "Google Drive / AniVexa"
                            } else {
                                "Dispositivo / ${LocalBackupStorage.BACKUP_FOLDER_NAME}"
                            }
                            Text(
                                text = "Destino: $targetLabel\nCrea una contraseña para cifrar el archivo .avb (AES-256). La necesitarás al restaurar.",
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
                                label = "Contraseña de cifrado (mín. 4 caracteres)",
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
                                testTag = "backup_password_input"
                            )

                            PassphraseTextField(
                                value = confirmPassphrase,
                                onValueChange = {
                                    confirmPassphrase = it
                                    localValidationError = null
                                },
                                label = "Confirmar contraseña",
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
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
                                    "Carpeta AniVexa en Drive"
                                } else {
                                    "Carpeta ${LocalBackupStorage.BACKUP_FOLDER_NAME}"
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
                                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista")
                                }
                            }

                            if (uiState.availableBackups.isEmpty() && !uiState.isBackupBusy) {
                                Text(
                                    text = "No se encontraron archivos .avb en esa carpeta todavía.",
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
                                                    text = formatEntryMeta(entry),
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
                                    Text("Buscar archivo .avb manualmente")
                                }
                            }
                        }

                        BackupDialogStep.ENTER_RESTORE_PASSPHRASE -> {
                            val entry = selectedEntry
                            Text(
                                text = "Archivo: ${entry?.fileName ?: "respaldo.avb"}\nAl restaurar se reemplazarán tus listas, favoritos, episodios vistos y ajustes actuales con los de este respaldo.",
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
                                label = "Contraseña del archivo .avb",
                                showPassword = showPassword,
                                onToggleVisibility = { showPassword = !showPassword },
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
                                "Entrar a AniVexa"
                            } else {
                                "Listo"
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
                                    localValidationError = "La contraseña debe tener al menos 4 caracteres."
                                }
                                passphrase.trim() != confirmPassphrase.trim() -> {
                                    localValidationError = "Las contraseñas no coinciden."
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
                        Text("Crear copia .avb")
                    }
                }

                step == BackupDialogStep.ENTER_RESTORE_PASSPHRASE -> {
                    Button(
                        enabled = !uiState.isBackupBusy && selectedEntry != null,
                        onClick = {
                            val entry = selectedEntry ?: return@Button
                            if (!BackupCrypto.isValidPassphrase(passphrase)) {
                                localValidationError = "Introduce la contraseña de tu archivo .avb."
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
                        Text("Restaurar ahora")
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
                            Text("Atrás")
                        }
                    }
                    TextButton(
                        enabled = !uiState.isBackupBusy,
                        onClick = onDismiss
                    ) {
                        Text("Cerrar")
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
                    contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

private fun formatEntryMeta(entry: BackupEntry): String {
    val dateStr = if (entry.modifiedMillis > 0L) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.modifiedMillis))
    } else {
        "Archivo .avb"
    }
    val sizeStr = if (entry.sizeBytes > 0L) {
        val kb = (entry.sizeBytes / 1024.0).coerceAtLeast(0.1)
        String.format(Locale.US, "%.1f KB", kb)
    } else {
        "Cifrado AES-256"
    }
    return "$dateStr • $sizeStr"
}
