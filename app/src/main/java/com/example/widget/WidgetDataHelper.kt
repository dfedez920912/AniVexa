package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.data.AniListClient
import com.example.data.local.AppDatabase
import com.example.data.local.CachedAnimeEntity
import com.example.model.AnimeRelease
import com.example.util.AppLogger
import cu.daniel.anical.R
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

object WidgetDataHelper {

    private const val TAG = "WidgetDataHelper"
    private const val PREFS_NAME = "anivexa_prefs"
    private const val KEY_JST_ZONE = "jst_zone"
    private const val KEY_LAST_SYNC_MS = "last_schedule_sync_ms"
    private const val KEY_LAST_SYNC_DATE = "last_schedule_sync_date"
    private const val KEY_SNAPSHOT_JSON = "widget_today_snapshot_json"
    private const val KEY_SNAPSHOT_DATE = "widget_today_snapshot_date"
    private const val KEY_SNAPSHOT_ZONE = "widget_today_snapshot_zone"
    private const val REFRESH_INTERVAL_MS = 20 * 60_000L

    fun getActiveZoneId(context: Context): ZoneId {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useJst = prefs.getBoolean(KEY_JST_ZONE, false)
        return if (useJst) ZoneId.of("Asia/Tokyo") else ZoneId.systemDefault()
    }

    fun markScheduleSynced(context: Context, date: LocalDate) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis())
            .putString(KEY_LAST_SYNC_DATE, date.toString())
            .apply()
    }

    fun clearTodaySnapshot(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SNAPSHOT_JSON)
            .remove(KEY_SNAPSHOT_DATE)
            .remove(KEY_SNAPSHOT_ZONE)
            .remove(KEY_LAST_SYNC_MS)
            .remove(KEY_LAST_SYNC_DATE)
            .apply()
    }

    /**
     * Stores an isolated snapshot strictly for [date] (the real device date) so that
     * browsing other days/weeks/months in the Calendar screen never overwrites widget data.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTodaySnapshot(
        context: Context,
        date: LocalDate,
        zoneId: ZoneId,
        releases: List<AnimeRelease>
    ) {
        val today = LocalDate.now(zoneId)
        if (date != today) return

        val filtered = releases
            .filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
            .distinctBy { "${it.id}_${it.effectiveEpisodeNumber()}" }
            .sortedBy { it.effectiveAiringSeconds() }

        val array = JSONArray()
        filtered.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("airingId", item.airingId)
                    put("titleRomaji", item.titleRomaji)
                    put("titleEnglish", item.titleEnglish ?: JSONObject.NULL)
                    put("coverImageUrl", item.coverImageUrl)
                    put("episode", item.effectiveEpisodeNumber())
                    if (item.totalEpisodes != null) put("totalEpisodes", item.totalEpisodes)
                    put("airingAtSeconds", item.effectiveAiringSeconds())
                    put("format", item.format)
                    put("streamingPlatform", item.streamingPlatform)
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SNAPSHOT_JSON, array.toString())
            .putString(KEY_SNAPSHOT_DATE, today.toString())
            .putString(KEY_SNAPSHOT_ZONE, zoneId.id)
            .putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis())
            .putString(KEY_LAST_SYNC_DATE, today.toString())
            .apply()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTodaySnapshot(
        context: Context,
        today: LocalDate,
        zoneId: ZoneId
    ): List<AnimeRelease>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snapDate = prefs.getString(KEY_SNAPSHOT_DATE, null) ?: return null
        val snapZone = prefs.getString(KEY_SNAPSHOT_ZONE, null) ?: return null
        if (snapDate != today.toString() || snapZone != zoneId.id) return null

        val raw = prefs.getString(KEY_SNAPSHOT_JSON, null) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AnimeRelease>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val titleEn = if (obj.isNull("titleEnglish")) null else obj.optString("titleEnglish").takeIf { it.isNotBlank() }
                val totalEps = if (obj.has("totalEpisodes") && !obj.isNull("totalEpisodes")) obj.optInt("totalEpisodes") else null
                list.add(
                    AnimeRelease(
                        id = obj.getInt("id"),
                        airingId = obj.optInt("airingId", obj.getInt("id")),
                        titleRomaji = obj.optString("titleRomaji", ""),
                        titleEnglish = titleEn,
                        coverImageUrl = obj.optString("coverImageUrl", ""),
                        bannerImageUrl = null,
                        synopsis = "",
                        episode = obj.optInt("episode", 1),
                        totalEpisodes = totalEps,
                        airingAtSeconds = obj.optLong("airingAtSeconds", 0L),
                        genres = emptyList(),
                        score = null,
                        studio = null,
                        format = obj.optString("format", "TV"),
                        streamingPlatform = obj.optString("streamingPlatform", "Crunchyroll")
                    )
                )
            }
            list.filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
                .sortedBy { it.effectiveAiringSeconds() }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed parsing today widget snapshot: ${e.message}")
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getTodayRealReleases(
        context: Context,
        forceNetwork: Boolean = false
    ): Pair<List<AnimeRelease>, ZoneId> {
        val zoneId = getActiveZoneId(context)
        val today = LocalDate.now(zoneId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSyncMs = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
        val lastSyncDate = prefs.getString(KEY_LAST_SYNC_DATE, null)
        val nowMs = System.currentTimeMillis()

        val syncedTodayRecently = lastSyncDate == today.toString() && (nowMs - lastSyncMs) < REFRESH_INTERVAL_MS
        val snapshotToday = loadTodaySnapshot(context, today, zoneId)
        if (!forceNetwork && syncedTodayRecently && snapshotToday != null) {
            return Pair(snapshotToday, zoneId)
        }

        val dao = AppDatabase.getDatabase(context).cachedAnimeDao()
        val cachedToday = try {
            dao.getAllCachedAnimeList()
                .map { it.toAnimeRelease() }
                .filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
                .sortedBy { it.effectiveAiringSeconds() }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed reading cached anime for widget: ${e.message}")
            emptyList()
        }

        val shouldFetch = forceNetwork || !syncedTodayRecently || (snapshotToday == null && cachedToday.isEmpty() && lastSyncDate != today.toString())

        if (shouldFetch) {
            try {
                val startEpoch = today.atStartOfDay(zoneId).toEpochSecond()
                val endEpoch = today.atTime(23, 59, 59).atZone(zoneId).toEpochSecond()
                val remote = AniListClient().fetchAiringSchedule(startEpoch, endEpoch, maxPages = 3)
                val remoteToday = remote
                    .filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
                    .sortedBy { it.effectiveAiringSeconds() }
                saveTodaySnapshot(context, today, zoneId, remoteToday)
                if (remote.isNotEmpty()) {
                    dao.insertAll(remote.map { CachedAnimeEntity.fromAnimeRelease(it, categoryTag = "SCHEDULE") })
                }
                return Pair(remoteToday, zoneId)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Widget AniList fetch failed, falling back to snapshot/cached DB: ${e.message}")
            }
        }

        return Pair(snapshotToday ?: cachedToday, zoneId)
    }

    fun notifyAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val countComponent = ComponentName(context, AnimeCountWidgetProvider::class.java)
            val countIds = appWidgetManager.getAppWidgetIds(countComponent)
            if (countIds.isNotEmpty()) {
                val countIntent = Intent(context, AnimeCountWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, countIds)
                    `package` = context.packageName
                }
                context.sendBroadcast(countIntent)
            }

            val listComponent = ComponentName(context, AnimeListWidgetProvider::class.java)
            val listIds = appWidgetManager.getAppWidgetIds(listComponent)
            if (listIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(listIds, R.id.widget_list_view)
                val listIntent = Intent(context, AnimeListWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, listIds)
                    `package` = context.packageName
                }
                context.sendBroadcast(listIntent)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "notifyAllWidgets error: ${e.message}")
        }
    }
}
