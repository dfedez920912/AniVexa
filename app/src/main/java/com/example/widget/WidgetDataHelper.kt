package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.data.AniListClient
import com.example.data.local.AppDatabase
import com.example.data.local.CachedAnimeEntity
import com.example.model.AnimeRelease
import cu.daniel.anical.R
import java.time.LocalDate
import java.time.ZoneId

object WidgetDataHelper {

    private const val TAG = "WidgetDataHelper"
    private const val PREFS_NAME = "anivexa_prefs"
    private const val KEY_JST_ZONE = "jst_zone"
    private const val KEY_LAST_SYNC_MS = "last_schedule_sync_ms"
    private const val KEY_LAST_SYNC_DATE = "last_schedule_sync_date"
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

        val dao = AppDatabase.getDatabase(context).cachedAnimeDao()
        val cachedToday = try {
            dao.getAllCachedAnimeList()
                .map { it.toAnimeRelease() }
                .filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
                .sortedBy { it.effectiveAiringSeconds() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading cached anime for widget: ${e.message}")
            emptyList()
        }

        val syncedTodayRecently = lastSyncDate == today.toString() && (nowMs - lastSyncMs) < REFRESH_INTERVAL_MS
        val shouldFetch = forceNetwork || !syncedTodayRecently || (cachedToday.isEmpty() && lastSyncDate != today.toString())

        if (shouldFetch) {
            try {
                val startEpoch = today.atStartOfDay(zoneId).toEpochSecond()
                val endEpoch = today.atTime(23, 59, 59).atZone(zoneId).toEpochSecond()
                val remote = AniListClient().fetchAiringSchedule(startEpoch, endEpoch, maxPages = 3)
                markScheduleSynced(context, today)
                if (remote.isNotEmpty()) {
                    dao.insertAll(remote.map { CachedAnimeEntity.fromAnimeRelease(it, categoryTag = "SCHEDULE") })
                }
                val remoteToday = remote
                    .filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == today }
                    .sortedBy { it.effectiveAiringSeconds() }
                return Pair(remoteToday, zoneId)
            } catch (e: Exception) {
                Log.w(TAG, "Widget AniList fetch failed, falling back to cached DB: ${e.message}")
            }
        }

        return Pair(cachedToday, zoneId)
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
            Log.w(TAG, "notifyAllWidgets error: ${e.message}")
        }
    }
}
