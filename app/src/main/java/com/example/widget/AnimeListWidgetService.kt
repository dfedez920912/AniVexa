package com.example.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.model.AnimeRelease
import cu.daniel.anical.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.ZoneId

class AnimeListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AnimeListWidgetFactory(applicationContext)
    }
}

class AnimeListWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var animeList = listOf<AnimeRelease>()
    private var activeZoneId: ZoneId = ZoneId.systemDefault()

    override fun onCreate() {
        loadData()
    }

    @SuppressLint("NewApi")
    override fun onDataSetChanged() {
        loadData()
    }

    @SuppressLint("NewApi")
    private fun loadData() {
        try {
            val (items, zone) = runBlocking(Dispatchers.IO) {
                WidgetDataHelper.getTodayRealReleases(context, forceNetwork = false)
            }
            animeList = items
            activeZoneId = zone
        } catch (e: Exception) {
            animeList = emptyList()
        }
    }

    override fun onDestroy() {
        animeList = emptyList()
    }

    override fun getCount(): Int = animeList.size

    @SuppressLint("NewApi")
    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_anime_list_item)
        if (position in animeList.indices) {
            val anime = animeList[position]
            views.setTextViewText(R.id.item_anime_title, "${anime.displayTitle} (Ep ${anime.effectiveEpisodeNumber()})")
            views.setTextViewText(R.id.item_anime_time, anime.getFormattedTime(activeZoneId))

            val fillInIntent = Intent().apply {
                putExtra("anime_id", anime.id)
            }
            views.setOnClickFillInIntent(R.id.item_anime_title, fillInIntent)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = animeList.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
