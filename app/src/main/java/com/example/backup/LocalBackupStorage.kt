package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.util.AppLogger
import java.io.ByteArrayOutputStream
import java.io.IOException

object LocalBackupStorage {

    const val BACKUP_FOLDER_NAME = "Anivexa-Backuo"
    private const val MAX_BACKUP_BYTES = 25 * 1024 * 1024 // 25 MB
    private const val PREFS_NAME = "anivexa_backup_prefs"
    private const val KEY_TREE_URI = "local_backup_tree_uri"

    fun getSavedTreeUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TREE_URI, null) ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val hasAccess = context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission && perm.isWritePermission
        }
        return if (hasAccess) uri else null
    }

    fun persistTreeUri(context: Context, treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (e: SecurityException) {
            AppLogger.w("LocalBackupStorage", "Could not take persistable URI permission: ${e.message}")
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .apply()
    }

    fun ensureBackupFolderUri(context: Context, treeUri: Uri): Uri {
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)

        val rootName = queryDisplayName(context, rootDocUri)
        if (rootName.equals(BACKUP_FOLDER_NAME, ignoreCase = true)) {
            return rootDocUri
        }

        val existingFolderId = findChildDocumentId(
            context = context,
            treeUri = treeUri,
            parentDocId = treeDocId,
            targetName = BACKUP_FOLDER_NAME,
            directoriesOnly = true
        )
        if (existingFolderId != null) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, existingFolderId)
        }

        return DocumentsContract.createDocument(
            resolver,
            rootDocUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            BACKUP_FOLDER_NAME
        ) ?: throw IOException("No se pudo crear la carpeta $BACKUP_FOLDER_NAME en el almacenamiento seleccionado.")
    }

    fun writeBackup(
        context: Context,
        treeUri: Uri,
        fileName: String,
        encryptedBytes: ByteArray
    ): BackupEntry {
        val folderDocUri = ensureBackupFolderUri(context, treeUri)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver,
            folderDocUri,
            "application/octet-stream",
            fileName
        ) ?: throw IOException("No se pudo crear el archivo $fileName en $BACKUP_FOLDER_NAME.")

        context.contentResolver.openOutputStream(fileUri, "wt")?.use { output ->
            output.write(encryptedBytes)
            output.flush()
        } ?: throw IOException("No se pudo abrir el flujo de escritura para $fileName.")

        return BackupEntry(
            id = fileUri.toString(),
            fileName = fileName,
            sizeBytes = encryptedBytes.size.toLong(),
            modifiedMillis = System.currentTimeMillis(),
            source = BackupDestination.LOCAL
        )
    }

    fun listBackups(context: Context, treeUri: Uri): List<BackupEntry> {
        val folderDocUri = ensureBackupFolderUri(context, treeUri)
        val folderDocId = DocumentsContract.getDocumentId(folderDocUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)

        val entries = mutableListOf<BackupEntry>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                if (!name.endsWith(".${BackupCrypto.FILE_EXTENSION}", ignoreCase = true)) continue

                val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                val modified = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                entries.add(
                    BackupEntry(
                        id = docUri.toString(),
                        fileName = name,
                        sizeBytes = size,
                        modifiedMillis = modified,
                        source = BackupDestination.LOCAL
                    )
                )
            }
        }
        return entries.sortedByDescending { it.modifiedMillis }
    }

    fun readBytes(context: Context, documentUri: Uri): ByteArray {
        val bytes = context.contentResolver.openInputStream(documentUri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_BACKUP_BYTES) {
                    throw IOException("El archivo supera el límite máximo permitido de 25 MB para copias .avb.")
                }
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        } ?: throw IOException("No se pudo leer el archivo seleccionado.")

        if (!BackupCrypto.hasAvbMagicHeader(bytes)) {
            throw BackupCryptoException("El archivo seleccionado no tiene un formato .avb válido de AniVexa.")
        }
        return bytes
    }

    private fun queryDisplayName(context: Context, docUri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return try {
            context.contentResolver.query(docUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findChildDocumentId(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        targetName: String,
        directoriesOnly: Boolean
    ): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        return try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    if (directoriesOnly && mime != DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (name.equals(targetName, ignoreCase = true)) {
                        return@use docId
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
