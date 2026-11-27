package com.github.dfedez920912.anivexa.backup

import com.github.dfedez920912.anivexa.data.local.FavoriteAnimeEntity
import com.github.dfedez920912.anivexa.data.local.UserRatingEntity
import com.github.dfedez920912.anivexa.data.local.WatchedEpisodeEntity
import com.github.dfedez920912.anivexa.data.local.WatchlistEntity

enum class BackupDestination(val title: String, val folderName: String) {
    LOCAL("Almacenamiento Local", "Anivexa-Backuo"),
    GOOGLE_DRIVE("Google Drive", "AniVexa")
}

enum class BackupOperationMode {
    BACKUP,
    RESTORE
}

data class BackupEntry(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val source: BackupDestination
)

data class BackupPreferencesSnapshot(
    val themeMode: String,
    val isJapanTimeZone: Boolean,
    val notificationsEnabled: Boolean,
    val defaultReminderMinutes: Int,
    val quietHoursEnabled: Boolean,
    val synopsisLanguageCode: String
)

data class AniVexaBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appName: String = EXPECTED_APP_NAME,
    val appVersion: String,
    val exportedAtMillis: Long,
    val favorites: List<FavoriteAnimeEntity>,
    val watchedEpisodes: List<WatchedEpisodeEntity>,
    val watchlist: List<WatchlistEntity>,
    val userRatings: List<UserRatingEntity>,
    val preferences: BackupPreferencesSnapshot
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val EXPECTED_APP_NAME = "AniVexa"
    }
}

data class BackupSummaryStats(
    val favoritesCount: Int,
    val watchlistCount: Int,
    val watchedEpisodesCount: Int,
    val ratingsCount: Int
)

sealed interface DriveAuthResult {
    data class Authorized(val accessToken: String) : DriveAuthResult
    data class NeedsResolution(val intentSender: android.content.IntentSender) : DriveAuthResult
    data class NeedsActivityResolution(val intent: android.content.Intent) : DriveAuthResult
    data class Error(val message: String, val statusCode: Int? = null) : DriveAuthResult
}

sealed interface DriveResolutionOutcome {
    data class Authorized(val accessToken: String) : DriveResolutionOutcome
    data class NeedsResolution(val intentSender: android.content.IntentSender) : DriveResolutionOutcome
    data class NeedsActivityResolution(val intent: android.content.Intent) : DriveResolutionOutcome
    data object Cancelled : DriveResolutionOutcome
    data class Error(val statusCode: Int? = null, val detail: String? = null) : DriveResolutionOutcome
}
