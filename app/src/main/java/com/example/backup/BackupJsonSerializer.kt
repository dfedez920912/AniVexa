package com.example.backup

import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.UserRatingEntity
import com.example.data.local.WatchedEpisodeEntity
import com.example.data.local.WatchlistEntity
import com.example.data.local.WatchlistStatus
import com.example.model.AppThemeMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupJsonSerializer {

    fun serialize(payload: AniVexaBackupPayload): String {
        val root = JSONObject().apply {
            put("schemaVersion", payload.schemaVersion)
            put("appName", payload.appName)
            put("appVersion", payload.appVersion)
            put("exportedAtMillis", payload.exportedAtMillis)

            put("favorites", JSONArray().apply {
                payload.favorites.forEach { fav ->
                    put(
                        JSONObject().apply {
                            put("animeId", fav.animeId)
                            put("titleRomaji", fav.titleRomaji)
                            if (fav.titleEnglish != null) put("titleEnglish", fav.titleEnglish) else put("titleEnglish", JSONObject.NULL)
                            put("coverImageUrl", fav.coverImageUrl)
                            if (fav.bannerImageUrl != null) put("bannerImageUrl", fav.bannerImageUrl) else put("bannerImageUrl", JSONObject.NULL)
                            put("synopsis", fav.synopsis)
                            put("genres", fav.genres)
                            if (fav.studio != null) put("studio", fav.studio) else put("studio", JSONObject.NULL)
                            if (fav.score != null) put("score", fav.score) else put("score", JSONObject.NULL)
                            put("format", fav.format)
                            put("hasReminder", fav.hasReminder)
                            put("reminderMinutesBefore", fav.reminderMinutesBefore)
                            put("addedAt", fav.addedAt)
                        }
                    )
                }
            })

            put("watchedEpisodes", JSONArray().apply {
                payload.watchedEpisodes.forEach { ep ->
                    put(
                        JSONObject().apply {
                            put("episodeKey", ep.episodeKey)
                            put("animeId", ep.animeId)
                            put("episodeNumber", ep.episodeNumber)
                            put("watchedAt", ep.watchedAt)
                        }
                    )
                }
            })

            put("watchlist", JSONArray().apply {
                payload.watchlist.forEach { item ->
                    put(
                        JSONObject().apply {
                            put("animeId", item.animeId)
                            put("titleRomaji", item.titleRomaji)
                            if (item.titleEnglish != null) put("titleEnglish", item.titleEnglish) else put("titleEnglish", JSONObject.NULL)
                            put("coverImageUrl", item.coverImageUrl)
                            if (item.bannerImageUrl != null) put("bannerImageUrl", item.bannerImageUrl) else put("bannerImageUrl", JSONObject.NULL)
                            put("synopsis", item.synopsis)
                            put("genres", item.genres)
                            if (item.studio != null) put("studio", item.studio) else put("studio", JSONObject.NULL)
                            if (item.score != null) put("score", item.score) else put("score", JSONObject.NULL)
                            put("format", item.format)
                            if (item.totalEpisodes != null) put("totalEpisodes", item.totalEpisodes) else put("totalEpisodes", JSONObject.NULL)
                            put("watchedEpisodes", item.watchedEpisodes)
                            put("status", item.status)
                            put("streamingPlatform", item.streamingPlatform)
                            put("notes", item.notes)
                            put("addedAt", item.addedAt)
                        }
                    )
                }
            })

            put("userRatings", JSONArray().apply {
                payload.userRatings.forEach { rating ->
                    put(
                        JSONObject().apply {
                            put("animeId", rating.animeId)
                            put("rating", rating.rating)
                            put("ratedAt", rating.ratedAt)
                        }
                    )
                }
            })

            put(
                "preferences",
                JSONObject().apply {
                    put("themeMode", payload.preferences.themeMode)
                    put("isJapanTimeZone", payload.preferences.isJapanTimeZone)
                    put("notificationsEnabled", payload.preferences.notificationsEnabled)
                    put("defaultReminderMinutes", payload.preferences.defaultReminderMinutes)
                    put("quietHoursEnabled", payload.preferences.quietHoursEnabled)
                    put("synopsisLanguageCode", payload.preferences.synopsisLanguageCode)
                }
            )
        }
        return root.toString()
    }

    fun deserialize(rawJson: String): AniVexaBackupPayload {
        val root = try {
            JSONObject(rawJson)
        } catch (e: JSONException) {
            throw BackupFormatException("El contenido descifrado no tiene un formato JSON válido.", e)
        }

        val appName = root.optString("appName", "")
        if (appName != AniVexaBackupPayload.EXPECTED_APP_NAME) {
            throw BackupFormatException("El archivo no pertenece a AniVexa.")
        }

        if (!root.has("schemaVersion")) {
            throw BackupFormatException("Falta la versión de esquema en el respaldo.")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion <= 0 || schemaVersion > AniVexaBackupPayload.CURRENT_SCHEMA_VERSION) {
            throw BackupFormatException("Versión de respaldo no compatible ($schemaVersion).")
        }

        val favArray = root.optJSONArray("favorites")
            ?: throw BackupFormatException("Faltan datos de favoritos en el respaldo.")
        val watchedArray = root.optJSONArray("watchedEpisodes")
            ?: throw BackupFormatException("Faltan datos de episodios vistos en el respaldo.")
        val watchlistArray = root.optJSONArray("watchlist")
            ?: throw BackupFormatException("Faltan datos de Mi Lista en el respaldo.")
        val ratingsArray = root.optJSONArray("userRatings")
            ?: throw BackupFormatException("Faltan datos de calificaciones en el respaldo.")

        val favorites = buildList {
            for (i in 0 until favArray.length()) {
                val obj = favArray.getJSONObject(i)
                val animeId = obj.getInt("animeId")
                val titleRomaji = obj.getString("titleRomaji")
                add(
                    FavoriteAnimeEntity(
                        animeId = animeId,
                        titleRomaji = titleRomaji,
                        titleEnglish = obj.optNullableString("titleEnglish"),
                        coverImageUrl = sanitizeHttpsUrl(obj.optString("coverImageUrl", "")) ?: "",
                        bannerImageUrl = sanitizeHttpsUrl(obj.optNullableString("bannerImageUrl")),
                        synopsis = obj.optString("synopsis", ""),
                        genres = obj.optString("genres", ""),
                        studio = obj.optNullableString("studio"),
                        score = obj.optNullableDouble("score"),
                        format = obj.optString("format", "TV"),
                        hasReminder = obj.optBoolean("hasReminder", true),
                        reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 15).coerceIn(0, 180),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val watchedEpisodes = buildList {
            for (i in 0 until watchedArray.length()) {
                val obj = watchedArray.getJSONObject(i)
                val animeId = obj.getInt("animeId")
                val episodeNumber = obj.getInt("episodeNumber")
                val episodeKey = obj.optString("episodeKey", "${animeId}_$episodeNumber")
                add(
                    WatchedEpisodeEntity(
                        episodeKey = episodeKey,
                        animeId = animeId,
                        episodeNumber = episodeNumber,
                        watchedAt = obj.optLong("watchedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val watchlist = buildList {
            for (i in 0 until watchlistArray.length()) {
                val obj = watchlistArray.getJSONObject(i)
                val animeId = obj.getInt("animeId")
                val titleRomaji = obj.getString("titleRomaji")
                val rawStatus = obj.optString("status", WatchlistStatus.PLAN_TO_WATCH.name)
                val validStatus = try {
                    WatchlistStatus.valueOf(rawStatus).name
                } catch (_: Exception) {
                    WatchlistStatus.PLAN_TO_WATCH.name
                }
                add(
                    WatchlistEntity(
                        animeId = animeId,
                        titleRomaji = titleRomaji,
                        titleEnglish = obj.optNullableString("titleEnglish"),
                        coverImageUrl = sanitizeHttpsUrl(obj.optString("coverImageUrl", "")) ?: "",
                        bannerImageUrl = sanitizeHttpsUrl(obj.optNullableString("bannerImageUrl")),
                        synopsis = obj.optString("synopsis", ""),
                        genres = obj.optString("genres", ""),
                        studio = obj.optNullableString("studio"),
                        score = obj.optNullableDouble("score"),
                        format = obj.optString("format", "TV"),
                        totalEpisodes = obj.optNullableInt("totalEpisodes"),
                        watchedEpisodes = obj.optInt("watchedEpisodes", 0).coerceAtLeast(0),
                        status = validStatus,
                        streamingPlatform = obj.optString("streamingPlatform", "Crunchyroll"),
                        notes = obj.optString("notes", ""),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val userRatings = buildList {
            for (i in 0 until ratingsArray.length()) {
                val obj = ratingsArray.getJSONObject(i)
                add(
                    UserRatingEntity(
                        animeId = obj.getInt("animeId"),
                        rating = obj.getInt("rating").coerceIn(1, 5),
                        ratedAt = obj.optLong("ratedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val prefsObj = root.optJSONObject("preferences")
        val prefs = BackupPreferencesSnapshot(
            themeMode = prefsObj?.optString("themeMode", AppThemeMode.ANIVEXA_DARK.name)
                ?: AppThemeMode.ANIVEXA_DARK.name,
            isJapanTimeZone = prefsObj?.optBoolean("isJapanTimeZone", false) ?: false,
            notificationsEnabled = prefsObj?.optBoolean("notificationsEnabled", true) ?: true,
            defaultReminderMinutes = prefsObj?.optInt("defaultReminderMinutes", 15) ?: 15,
            quietHoursEnabled = prefsObj?.optBoolean("quietHoursEnabled", false) ?: false,
            synopsisLanguageCode = prefsObj?.optString("synopsisLanguageCode", "es") ?: "es"
        )

        return AniVexaBackupPayload(
            schemaVersion = schemaVersion,
            appName = appName,
            appVersion = root.optString("appVersion", "0.1.7"),
            exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis()),
            favorites = favorites,
            watchedEpisodes = watchedEpisodes,
            watchlist = watchlist,
            userRatings = userRatings,
            preferences = prefs
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun sanitizeHttpsUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("https://", ignoreCase = true)) trimmed else null
    }
}
