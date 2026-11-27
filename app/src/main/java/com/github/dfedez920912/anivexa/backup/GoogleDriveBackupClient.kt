package com.github.dfedez920912.anivexa.backup

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.github.dfedez920912.anivexa.util.AppLogger
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
class GoogleDriveBackupClient(
    private val okHttpClient: OkHttpClient = defaultHttpClient()
) {

    suspend fun authorizeDriveAccess(context: Context): DriveAuthResult {
        val primaryResult = requestIdentityAuthorization(context)
        return if (primaryResult is DriveAuthResult.Error &&
            (primaryResult.statusCode == CommonStatusCodes.INTERNAL_ERROR || primaryResult.statusCode == null)
        ) {
            AppLogger.w(
                "GoogleDriveBackup",
                "Primary GIS authorize returned code=${primaryResult.statusCode}; trying GoogleSignIn fallback"
            )
            val fallbackResult = authorizeViaLegacySignIn(context)
            if (fallbackResult is DriveAuthResult.Error && primaryResult.statusCode == CommonStatusCodes.INTERNAL_ERROR) {
                DriveAuthResult.Error(
                    message = fallbackResult.message,
                    statusCode = fallbackResult.statusCode ?: CommonStatusCodes.INTERNAL_ERROR
                )
            } else {
                fallbackResult
            }
        } else {
            primaryResult
        }
    }

    private suspend fun requestIdentityAuthorization(context: Context): DriveAuthResult =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (!cont.isActive) return@addOnSuccessListener
                    val token = result.accessToken
                    if (result.hasResolution()) {
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent != null) {
                            cont.resume(DriveAuthResult.NeedsResolution(pendingIntent.intentSender))
                        } else {
                            cont.resume(DriveAuthResult.Error("No se pudo iniciar la ventana de permisos de Google Drive."))
                        }
                    } else if (!token.isNullOrBlank()) {
                        cont.resume(DriveAuthResult.Authorized(token))
                    } else {
                        cont.resume(DriveAuthResult.Error("Google Drive no devolvió un token de autorización válido."))
                    }
                }
                .addOnFailureListener { exc ->
                    if (!cont.isActive) return@addOnFailureListener
                    val statusCode = (exc as? ApiException)?.statusCode
                    AppLogger.w("GoogleDriveBackup", "Authorization failed (code=$statusCode): ${exc.message}")
                    val friendly = if (exc is ApiException) {
                        "Error al autorizar Google Drive (código ${exc.statusCode})."
                    } else {
                        exc.message ?: "No se pudo autorizar el acceso a Google Drive."
                    }
                    cont.resume(DriveAuthResult.Error(friendly, statusCode))
                }
        }

    suspend fun authorizeViaLegacySignIn(context: Context): DriveAuthResult {
        val driveScope = Scope(DRIVE_FILE_SCOPE)
        val existing = GoogleSignIn.getLastSignedInAccount(context)
        val existingAccount = existing?.account
        if (existing != null && existingAccount != null && GoogleSignIn.hasPermissions(existing, driveScope)) {
            when (val tokenOutcome = fetchOAuthTokenForAccount(context, existingAccount)) {
                is DriveResolutionOutcome.Authorized -> return DriveAuthResult.Authorized(tokenOutcome.accessToken)
                is DriveResolutionOutcome.NeedsActivityResolution ->
                    return DriveAuthResult.NeedsActivityResolution(tokenOutcome.intent)
                else -> Unit
            }
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        val signInClient = GoogleSignIn.getClient(context, gso)
        return DriveAuthResult.NeedsActivityResolution(signInClient.signInIntent)
    }

    suspend fun extractLegacySignInOutcome(
        context: Context,
        resultCode: Int,
        intent: Intent?
    ): DriveResolutionOutcome {
        if (resultCode == Activity.RESULT_CANCELED && intent == null) {
            return DriveResolutionOutcome.Cancelled
        }
        val signInAccount = if (intent != null) {
            try {
                GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException::class.java)
            } catch (e: ApiException) {
                if (e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == SIGN_IN_CANCELLED_CODE) {
                    return DriveResolutionOutcome.Cancelled
                }
                // If returning from a UserRecoverableAuthException consent activity, data won't carry
                // a GoogleSignInAccount payload; fall back to the cached signed-in account if present.
                val cached = GoogleSignIn.getLastSignedInAccount(context)
                if (resultCode == Activity.RESULT_OK && cached?.account != null) {
                    cached
                } else {
                    AppLogger.w(
                        "GoogleDriveBackup",
                        "Legacy GoogleSignIn failed (code=${e.statusCode}): ${e.message}"
                    )
                    return DriveResolutionOutcome.Error(statusCode = e.statusCode, detail = e.message)
                }
            }
        } else {
            GoogleSignIn.getLastSignedInAccount(context)
        }

        val account = signInAccount?.account
            ?: GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: return if (resultCode == Activity.RESULT_CANCELED) {
                DriveResolutionOutcome.Cancelled
            } else {
                DriveResolutionOutcome.Error(
                    statusCode = CommonStatusCodes.INTERNAL_ERROR,
                    detail = "No Google account available after sign-in."
                )
            }

        return fetchOAuthTokenForAccount(context, account)
    }

    private suspend fun fetchOAuthTokenForAccount(
        context: Context,
        account: Account
    ): DriveResolutionOutcome = withContext(Dispatchers.IO) {
        try {
            val token = GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_FILE_SCOPE")
            if (token.isNotBlank()) {
                DriveResolutionOutcome.Authorized(token)
            } else {
                DriveResolutionOutcome.Error(detail = "Empty OAuth token from GoogleAuthUtil")
            }
        } catch (recoverable: UserRecoverableAuthException) {
            val recoveryIntent = recoverable.intent
            if (recoveryIntent != null) {
                DriveResolutionOutcome.NeedsActivityResolution(recoveryIntent)
            } else {
                DriveResolutionOutcome.Error(detail = recoverable.message)
            }
        } catch (authEx: GoogleAuthException) {
            AppLogger.w("GoogleDriveBackup", "GoogleAuthException: ${authEx.message}")
            DriveResolutionOutcome.Error(
                statusCode = CommonStatusCodes.INTERNAL_ERROR,
                detail = authEx.message
            )
        } catch (ioEx: IOException) {
            AppLogger.w("GoogleDriveBackup", "IOException fetching token: ${ioEx.message}")
            DriveResolutionOutcome.Error(detail = ioEx.message)
        }
    }

    fun extractAuthorizationOutcome(
        context: Context,
        resultCode: Int,
        intent: Intent?
    ): DriveResolutionOutcome {
        if (intent == null) {
            return if (resultCode == Activity.RESULT_CANCELED) {
                DriveResolutionOutcome.Cancelled
            } else {
                DriveResolutionOutcome.Error(detail = "No authorization intent returned (resultCode=$resultCode)")
            }
        }
        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(intent)
            val token = authResult.accessToken
            val pendingIntent = authResult.pendingIntent
            when {
                !token.isNullOrBlank() -> DriveResolutionOutcome.Authorized(token)
                authResult.hasResolution() && pendingIntent != null ->
                    DriveResolutionOutcome.NeedsResolution(pendingIntent.intentSender)
                resultCode == Activity.RESULT_CANCELED -> DriveResolutionOutcome.Cancelled
                else -> DriveResolutionOutcome.Error(detail = "Empty access token returned by Google Play Services")
            }
        } catch (e: ApiException) {
            AppLogger.w("GoogleDriveBackup", "Failed extracting Drive token from result: statusCode=${e.statusCode}, msg=${e.message}")
            if (e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == SIGN_IN_CANCELLED_CODE) {
                DriveResolutionOutcome.Cancelled
            } else {
                DriveResolutionOutcome.Error(statusCode = e.statusCode, detail = e.message)
            }
        }
    }

    fun extractAccessTokenFromIntent(context: Context, intent: Intent?): String? {
        return when (val outcome = extractAuthorizationOutcome(context, Activity.RESULT_OK, intent)) {
            is DriveResolutionOutcome.Authorized -> outcome.accessToken
            else -> null
        }
    }

    suspend fun findOrCreateAniVexaFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        val query = "name = '$DRIVE_FOLDER_NAME' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("fields", "files(id,name)")
            .addQueryParameter("spaces", "drive")
            .build()

        val searchReq = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(searchReq).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(buildDriveHttpError("buscar carpeta AniVexa", response.code, bodyStr))
            }
            val files = JSONObject(bodyStr).optJSONArray("files")
            if (files != null && files.length() > 0) {
                val existingId = files.getJSONObject(0).optString("id")
                if (existingId.isNotBlank()) return@withContext existingId
            }
        }

        val metadataJson = JSONObject().apply {
            put("name", DRIVE_FOLDER_NAME)
            put("mimeType", FOLDER_MIME_TYPE)
        }.toString()

        val createReq = Request.Builder()
            .url(DRIVE_FILES_URL)
            .header("Authorization", "Bearer $accessToken")
            .post(metadataJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(createReq).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(buildDriveHttpError("crear carpeta AniVexa", response.code, bodyStr))
            }
            val createdId = JSONObject(bodyStr).optString("id")
            if (createdId.isBlank()) {
                throw IOException("Google Drive no devolvió el ID de la carpeta $DRIVE_FOLDER_NAME.")
            }
            createdId
        }
    }

    suspend fun uploadEncryptedBackup(
        accessToken: String,
        fileName: String,
        encryptedBytes: ByteArray
    ): BackupEntry = withContext(Dispatchers.IO) {
        val folderId = findOrCreateAniVexaFolder(accessToken)
        val metadata = JSONObject().apply {
            put("name", fileName)
            put("parents", org.json.JSONArray().put(folderId))
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody(JSON_MEDIA_TYPE))
            .addPart(encryptedBytes.toRequestBody(OCTET_STREAM_MEDIA_TYPE))
            .build()

        val uploadUrl = DRIVE_UPLOAD_URL.toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name,size,modifiedTime")
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", "Bearer $accessToken")
            .post(multipartBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(buildDriveHttpError("subir respaldo a Google Drive", response.code, bodyStr))
            }
            val obj = JSONObject(bodyStr)
            BackupEntry(
                id = obj.getString("id"),
                fileName = obj.optString("name", fileName),
                sizeBytes = obj.optString("size", encryptedBytes.size.toString()).toLongOrNull()
                    ?: encryptedBytes.size.toLong(),
                modifiedMillis = parseIsoMillis(obj.optString("modifiedTime")) ?: System.currentTimeMillis(),
                source = BackupDestination.GOOGLE_DRIVE
            )
        }
    }

    suspend fun listBackupFiles(accessToken: String): List<BackupEntry> = withContext(Dispatchers.IO) {
        val folderId = findExistingAniVexaFolderId(accessToken) ?: return@withContext emptyList()
        val query = "'$folderId' in parents and trashed = false"
        val listUrl = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("fields", "files(id,name,size,modifiedTime)")
            .addQueryParameter("spaces", "drive")
            .build()

        val request = Request.Builder()
            .url(listUrl)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(buildDriveHttpError("listar respaldos en Google Drive", response.code, bodyStr))
            }
            val files = JSONObject(bodyStr).optJSONArray("files") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until files.length()) {
                    val obj = files.getJSONObject(i)
                    val name = obj.optString("name", "")
                    if (!name.endsWith(".${BackupCrypto.FILE_EXTENSION}", ignoreCase = true)) continue
                    add(
                        BackupEntry(
                            id = obj.getString("id"),
                            fileName = name,
                            sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L,
                            modifiedMillis = parseIsoMillis(obj.optString("modifiedTime")) ?: 0L,
                            source = BackupDestination.GOOGLE_DRIVE
                        )
                    )
                }
            }
        }
    }

    suspend fun downloadBackupBytes(accessToken: String, fileId: String): ByteArray =
        withContext(Dispatchers.IO) {
            if (!DRIVE_FILE_ID_REGEX.matches(fileId)) {
                throw IOException("Identificador de archivo de Google Drive inválido.")
            }
            val downloadUrl = DRIVE_FILES_URL.toHttpUrl().newBuilder()
                .addPathSegment(fileId)
                .addQueryParameter("alt", "media")
                .build()

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    throw IOException(buildDriveHttpError("descargar respaldo de Google Drive", response.code, bodyStr))
                }
                val stream = response.body?.byteStream()
                    ?: throw IOException("El archivo descargado de Google Drive está vacío.")
                stream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > MAX_BACKUP_BYTES) {
                            throw IOException("El respaldo descargado supera el límite máximo permitido de 25 MB.")
                        }
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                }
            }
        }

    private fun findExistingAniVexaFolderId(accessToken: String): String? {
        val query = "name = '$DRIVE_FOLDER_NAME' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("fields", "files(id,name)")
            .addQueryParameter("spaces", "drive")
            .build()

        val searchReq = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(searchReq).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(buildDriveHttpError("buscar carpeta AniVexa", response.code, bodyStr))
            }
            val files = JSONObject(bodyStr).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        }
    }

    private fun parseIsoMillis(raw: String): Long? {
        if (raw.isBlank()) return null
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun buildDriveHttpError(action: String, code: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return if (detail.isNotBlank()) {
            "Error al $action ($code): $detail"
        } else {
            "Error al $action (HTTP $code)."
        }
    }

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val DRIVE_FOLDER_NAME = "AniVexa"
        private const val SIGN_IN_CANCELLED_CODE = 12501
        private const val MAX_BACKUP_BYTES = 25 * 1024 * 1024 // 25 MB
        private val DRIVE_FILE_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,256}$")
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
