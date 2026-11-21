package com.example.backup

import android.content.Context
import android.content.SharedPreferences
import com.example.data.AnimeRepository
import com.example.data.UserDatabaseBackupSnapshot
import com.example.model.AppThemeMode
import com.example.model.SynopsisLanguage
import com.example.widget.WidgetDataHelper
import cu.daniel.anical.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(
    private val context: Context,
    private val repository: AnimeRepository,
    private val prefs: SharedPreferences = context.getSharedPreferences("anivexa_prefs", Context.MODE_PRIVATE)
) {

    fun generateBackupFileName(timestampMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "anivexa_backup_${formatter.format(Date(timestampMillis))}.${BackupCrypto.FILE_EXTENSION}"
    }

    suspend fun exportEncryptedBackup(passphrase: String): Pair<ByteArray, BackupSummaryStats> =
        withContext(Dispatchers.IO) {
            val dbSnapshot = repository.exportUserDatabaseSnapshot()
            val payload = AniVexaBackupPayload(
                appVersion = BuildConfig.VERSION_NAME,
                exportedAtMillis = System.currentTimeMillis(),
                favorites = dbSnapshot.favorites,
                watchedEpisodes = dbSnapshot.watchedEpisodes,
                watchlist = dbSnapshot.watchlist,
                userRatings = dbSnapshot.userRatings,
                preferences = readPreferencesSnapshot()
            )
            val jsonBytes = BackupJsonSerializer.serialize(payload).toByteArray(StandardCharsets.UTF_8)
            val encrypted = BackupCrypto.encrypt(jsonBytes, passphrase)
            val stats = BackupSummaryStats(
                favoritesCount = dbSnapshot.favorites.size,
                watchlistCount = dbSnapshot.watchlist.size,
                watchedEpisodesCount = dbSnapshot.watchedEpisodes.size,
                ratingsCount = dbSnapshot.userRatings.size
            )
            encrypted to stats
        }

    suspend fun restoreFromEncryptedBytes(
        encryptedBytes: ByteArray,
        passphrase: String
    ): BackupSummaryStats = withContext(Dispatchers.IO) {
        val plainBytes = BackupCrypto.decrypt(encryptedBytes, passphrase)
        val rawJson = String(plainBytes, StandardCharsets.UTF_8)
        val payload = BackupJsonSerializer.deserialize(rawJson)

        val dbSnapshot = UserDatabaseBackupSnapshot(
            favorites = payload.favorites,
            watchedEpisodes = payload.watchedEpisodes,
            watchlist = payload.watchlist,
            userRatings = payload.userRatings
        )
        repository.replaceAllUserBackupData(dbSnapshot)
        writePreferencesSnapshot(payload.preferences)
        WidgetDataHelper.clearTodaySnapshot(context)
        WidgetDataHelper.notifyAllWidgets(context)

        BackupSummaryStats(
            favoritesCount = payload.favorites.size,
            watchlistCount = payload.watchlist.size,
            watchedEpisodesCount = payload.watchedEpisodes.size,
            ratingsCount = payload.userRatings.size
        )
    }

    private fun readPreferencesSnapshot(): BackupPreferencesSnapshot {
        return BackupPreferencesSnapshot(
            themeMode = prefs.getString("theme_mode", AppThemeMode.ANIVEXA_DARK.name)
                ?: AppThemeMode.ANIVEXA_DARK.name,
            isJapanTimeZone = prefs.getBoolean("jst_zone", false),
            notificationsEnabled = prefs.getBoolean("notif_enabled", true),
            defaultReminderMinutes = prefs.getInt("default_reminder_min", 15),
            quietHoursEnabled = prefs.getBoolean("quiet_hours", false),
            synopsisLanguageCode = prefs.getString("synopsis_lang", "es") ?: "es"
        )
    }

    private fun writePreferencesSnapshot(snapshot: BackupPreferencesSnapshot) {
        val validTheme = try {
            AppThemeMode.valueOf(snapshot.themeMode).name
        } catch (_: Exception) {
            AppThemeMode.ANIVEXA_DARK.name
        }
        val validLang = SynopsisLanguage.fromCode(snapshot.synopsisLanguageCode).code
        val validReminderMinutes = snapshot.defaultReminderMinutes.coerceIn(0, 180)
        prefs.edit()
            .putString("theme_mode", validTheme)
            .putBoolean("jst_zone", snapshot.isJapanTimeZone)
            .putBoolean("notif_enabled", snapshot.notificationsEnabled)
            .putInt("default_reminder_min", validReminderMinutes)
            .putBoolean("quiet_hours", snapshot.quietHoursEnabled)
            .putString("synopsis_lang", validLang)
            .apply()
    }
}
