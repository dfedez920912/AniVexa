package com.example.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.util.AppLogger
import com.example.data.local.AppDatabase
import com.example.data.local.CachedAnimeDao
import com.example.data.local.CachedAnimeEntity
import com.example.data.local.FavoriteAnimeDao
import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.UserRatingDao
import com.example.data.local.UserRatingEntity
import com.example.data.local.WatchedEpisodeDao
import com.example.data.local.WatchedEpisodeEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.data.local.WatchlistStatus
import com.example.model.AnimeDetailBundle
import com.example.model.AnimeRelease
import com.example.model.GenreHelper
import com.example.model.MediaSeason
import com.example.model.SynopsisLanguage
import com.example.notifications.AnimeNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class AnimeRepository(
    private val context: Context,
    private val favoriteAnimeDao: FavoriteAnimeDao = AppDatabase.getDatabase(context).favoriteAnimeDao(),
    private val watchedEpisodeDao: WatchedEpisodeDao = AppDatabase.getDatabase(context).watchedEpisodeDao(),
    private val watchlistDao: WatchlistDao = AppDatabase.getDatabase(context).watchlistDao(),
    private val cachedAnimeDao: CachedAnimeDao = AppDatabase.getDatabase(context).cachedAnimeDao(),
    private val userRatingDao: UserRatingDao = AppDatabase.getDatabase(context).userRatingDao()
) {
    private val translationService = TranslationService()
    private val aniListClient = AniListClient()

    val userRatingsFlow: Flow<Map<Int, Int>> = userRatingDao.getAllRatingsFlow()
        .map { list -> list.associate { it.animeId to it.rating } }

    val favoriteIdsFlow: Flow<Set<Int>> = favoriteAnimeDao.getFavoriteIdsFlow()
        .map { list -> list.toSet() }

    val allFavoritesFlow: Flow<List<FavoriteAnimeEntity>> = favoriteAnimeDao.getAllFavorites()

    val watchedEpisodeKeysFlow: Flow<Set<String>> = watchedEpisodeDao.getAllWatchedKeysFlow()
        .map { list -> list.toSet() }

    val allWatchlistFlow: Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlist()

    val watchlistIdsFlow: Flow<Set<Int>> = watchlistDao.getWatchlistIdsFlow()
        .map { list -> list.toSet() }

    val cachedCountFlow: Flow<Int> = cachedAnimeDao.getCachedCountFlow()

    suspend fun setUserRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        if (rating <= 0) {
            userRatingDao.deleteRating(animeId)
        } else {
            userRatingDao.insertOrUpdateRating(
                UserRatingEntity(animeId = animeId, rating = rating.coerceIn(1, 5))
            )
        }
    }

    suspend fun getWatchlistIds(): Set<Int> = withContext(Dispatchers.IO) {
        watchlistDao.getWatchlistIdsList().toSet()
    }

    suspend fun toggleWatchlist(
        anime: AnimeRelease,
        status: WatchlistStatus = WatchlistStatus.PLAN_TO_WATCH
    ): Boolean = withContext(Dispatchers.IO) {
        val exists = watchlistDao.isInWatchlistDirect(anime.id)
        if (exists) {
            watchlistDao.deleteFromWatchlist(anime.id)
            false
        } else {
            val watchedCount = watchedEpisodeDao.getWatchedCountForAnime(anime.id)
            val total = anime.totalEpisodes
            val resolvedStatus = when {
                total != null && total > 0 && watchedCount >= total -> WatchlistStatus.COMPLETED
                watchedCount > 0 -> WatchlistStatus.WATCHING
                else -> status
            }
            watchlistDao.insertWatchlist(
                WatchlistEntity.fromAnimeRelease(
                    anime = anime,
                    status = resolvedStatus,
                    watchedEpisodes = watchedCount
                )
            )
            true
        }
    }

    suspend fun addOrUpdateWatchlistStatus(anime: AnimeRelease, status: WatchlistStatus) = withContext(Dispatchers.IO) {
        val exists = watchlistDao.isInWatchlistDirect(anime.id)
        if (exists) {
            watchlistDao.updateStatus(anime.id, status.name)
        } else {
            watchlistDao.insertWatchlist(
                WatchlistEntity.fromAnimeRelease(anime = anime, status = status)
            )
        }
    }

    suspend fun updateWatchlistStatus(animeId: Int, status: WatchlistStatus) = withContext(Dispatchers.IO) {
        watchlistDao.updateStatus(animeId, status.name)
    }

    suspend fun updateWatchlistEpisodes(animeId: Int, episodes: Int) = withContext(Dispatchers.IO) {
        watchlistDao.updateWatchedEpisodes(animeId, episodes)
    }

    suspend fun removeFromWatchlist(animeId: Int) = withContext(Dispatchers.IO) {
        watchlistDao.deleteFromWatchlist(animeId)
    }

    suspend fun isInWatchlist(animeId: Int): Boolean = withContext(Dispatchers.IO) {
        watchlistDao.isInWatchlistDirect(animeId)
    }

    suspend fun getWatchedEpisodeKeys(): Set<String> = withContext(Dispatchers.IO) {
        watchedEpisodeDao.getAllWatchedKeysList().toSet()
    }

    suspend fun getWatchedCountForAnime(animeId: Int): Int = withContext(Dispatchers.IO) {
        watchedEpisodeDao.getWatchedCountForAnime(animeId)
    }

    suspend fun markEpisodeWatched(animeId: Int, episodeNumber: Int) = withContext(Dispatchers.IO) {
        val key = "${animeId}_${episodeNumber}"
        if (!watchedEpisodeDao.isWatched(key)) {
            watchedEpisodeDao.markWatched(
                WatchedEpisodeEntity(
                    episodeKey = key,
                    animeId = animeId,
                    episodeNumber = episodeNumber
                )
            )
        }
        syncWatchlistProgressWithWatchedEpisodes(animeId)
    }

    suspend fun markAllEpisodesWatched(animeId: Int, totalEpisodes: Int?) = withContext(Dispatchers.IO) {
        if (totalEpisodes != null && totalEpisodes > 0) {
            val entities = (1..totalEpisodes).map { ep ->
                WatchedEpisodeEntity(
                    episodeKey = "${animeId}_$ep",
                    animeId = animeId,
                    episodeNumber = ep
                )
            }
            watchedEpisodeDao.markAllWatched(entities)
            watchlistDao.updateWatchedEpisodes(animeId, totalEpisodes)
        }
        watchlistDao.updateStatus(animeId, WatchlistStatus.COMPLETED.name)
    }

    suspend fun clearWatchedEpisodesForAnime(animeId: Int) = withContext(Dispatchers.IO) {
        watchedEpisodeDao.clearForAnime(animeId)
        watchlistDao.updateWatchedEpisodes(animeId, 0)
        watchlistDao.updateStatus(animeId, WatchlistStatus.PLAN_TO_WATCH.name)
    }

    suspend fun toggleWatchedEpisode(animeId: Int, episodeNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val key = "${animeId}_${episodeNumber}"
        val isAlreadyWatched = watchedEpisodeDao.isWatched(key)
        val result = if (isAlreadyWatched) {
            watchedEpisodeDao.unmarkWatched(key)
            false
        } else {
            watchedEpisodeDao.markWatched(
                WatchedEpisodeEntity(
                    episodeKey = key,
                    animeId = animeId,
                    episodeNumber = episodeNumber
                )
            )
            true
        }
        syncWatchlistProgressWithWatchedEpisodes(animeId)
        result
    }

    private suspend fun syncWatchlistProgressWithWatchedEpisodes(animeId: Int) {
        watchlistDao.getWatchlistItemById(animeId)?.let { existing ->
            val watchedCount = watchedEpisodeDao.getWatchedCountForAnime(animeId)
            watchlistDao.updateWatchedEpisodes(animeId, watchedCount)
            if (existing.statusEnum != WatchlistStatus.DROPPED) {
                val total = existing.totalEpisodes
                val nextStatus = when {
                    total != null && total > 0 && watchedCount >= total -> WatchlistStatus.COMPLETED
                    watchedCount > 0 -> WatchlistStatus.WATCHING
                    else -> WatchlistStatus.PLAN_TO_WATCH
                }
                if (existing.statusEnum != nextStatus) {
                    watchlistDao.updateStatus(animeId, nextStatus.name)
                }
            }
        }
    }

    suspend fun getFavoriteIds(): Set<Int> = withContext(Dispatchers.IO) {
        favoriteAnimeDao.getFavoriteIdsList().toSet()
    }

    suspend fun toggleFavorite(anime: AnimeRelease): Boolean = withContext(Dispatchers.IO) {
        val exists = favoriteAnimeDao.isFavoriteDirect(anime.id)
        val result = if (exists) {
            favoriteAnimeDao.deleteFavorite(anime.id)
            AnimeNotificationHelper.cancelAnimeReminder(context, anime.id)
            false
        } else {
            favoriteAnimeDao.insertFavorite(
                FavoriteAnimeEntity.fromAnimeRelease(
                    anime = anime,
                    hasReminder = true,
                    minutesBefore = anime.reminderMinutesBefore
                )
            )
            AnimeNotificationHelper.scheduleAnimeReminder(context, anime, anime.reminderMinutesBefore)
            true
        }
        com.example.widget.WidgetDataHelper.notifyAllWidgets(context)
        result
    }

    suspend fun updateReminder(anime: AnimeRelease, enabled: Boolean, minutesBefore: Int) = withContext(Dispatchers.IO) {
        val exists = favoriteAnimeDao.isFavoriteDirect(anime.id)
        if (!exists && enabled) {
            favoriteAnimeDao.insertFavorite(
                FavoriteAnimeEntity.fromAnimeRelease(
                    anime = anime,
                    hasReminder = true,
                    minutesBefore = minutesBefore
                )
            )
        } else if (exists) {
            favoriteAnimeDao.updateReminderStatus(anime.id, enabled)
            favoriteAnimeDao.updateReminderMinutes(anime.id, minutesBefore)
        }
        if (enabled) {
            AnimeNotificationHelper.scheduleAnimeReminder(context, anime, minutesBefore)
        } else {
            AnimeNotificationHelper.cancelAnimeReminder(context, anime.id)
        }
    }

    fun sendTestNotification(anime: AnimeRelease, minutesBefore: Int = 15) {
        AnimeNotificationHelper.showImmediateNotification(
            context = context,
            animeTitle = anime.displayTitle,
            episode = anime.episode,
            airingTime = anime.getFormattedTime(),
            minutesBefore = minutesBefore
        )
    }

    suspend fun isFavorite(animeId: Int): Boolean = withContext(Dispatchers.IO) {
        favoriteAnimeDao.isFavoriteDirect(animeId)
    }

    suspend fun translateSynopsis(text: String, targetLanguage: SynopsisLanguage): String {
        return translationService.translate(text, targetLanguage.code)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getSchedule(
        startDate: LocalDate = LocalDate.now(),
        daysCount: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Result<List<AnimeRelease>> = withContext(Dispatchers.IO) {
        val startEpoch = startDate.atStartOfDay(zoneId).toEpochSecond()
        val endEpoch = startDate.plusDays(daysCount.toLong() - 1).atTime(23, 59, 59).atZone(zoneId).toEpochSecond()

        val favIds = favoriteAnimeDao.getFavoriteIdsList().toSet()
        val watchedKeys = watchedEpisodeDao.getAllWatchedKeysList().toSet()
        val userRatingsMap = userRatingDao.getAllRatingsList().associate { it.animeId to it.rating }

        try {
            val remoteReleases = aniListClient.fetchAiringSchedule(startEpoch, endEpoch)
            if (remoteReleases.isNotEmpty()) {
                cachedAnimeDao.insertAll(
                    remoteReleases.map { CachedAnimeEntity.fromAnimeRelease(it, categoryTag = "SCHEDULE") }
                )
                val todayInZone = LocalDate.now(zoneId)
                val endDate = startDate.plusDays(daysCount.toLong() - 1)
                if (!todayInZone.isBefore(startDate) && !todayInZone.isAfter(endDate)) {
                    val todayOnly = remoteReleases.filter {
                        it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zoneId) == todayInZone
                    }
                    com.example.widget.WidgetDataHelper.saveTodaySnapshot(context, todayInZone, zoneId, todayOnly)
                    com.example.widget.WidgetDataHelper.notifyAllWidgets(context)
                }
                return@withContext Result.success(
                    decorateReleases(remoteReleases, favIds, watchedKeys, userRatingsMap)
                )
            }
        } catch (e: Exception) {
            AppLogger.w("AnimeRepository", "AniList schedule fetch failed, falling back to Room cache: ${e.message}")
        }

        val cached = cachedAnimeDao.getAllCachedAnimeList().map { it.toAnimeRelease() }
        val fallback = if (cached.isNotEmpty()) cached else OfflineAnimeData.getCuratedWeekSchedule(startDate)
        Result.success(decorateReleases(fallback, favIds, watchedKeys, userRatingsMap))
    }

    suspend fun getSeasonalAnime(
        season: MediaSeason = MediaSeason.fromDate(LocalDate.now()),
        year: Int = LocalDate.now().year,
        page: Int = 1,
        perPage: Int = 30,
        cacheForHome: Boolean = true
    ): Pair<List<AnimeRelease>, Boolean> = withContext(Dispatchers.IO) {
        val favIds = favoriteAnimeDao.getFavoriteIdsList().toSet()
        val watchedKeys = watchedEpisodeDao.getAllWatchedKeysList().toSet()
        val userRatingsMap = userRatingDao.getAllRatingsList().associate { it.animeId to it.rating }

        try {
            val remote = aniListClient.fetchSeasonalAnime(season, year, page, perPage)
            if (remote.isNotEmpty()) {
                if (cacheForHome) {
                    cachedAnimeDao.insertAll(
                        remote.map { CachedAnimeEntity.fromAnimeRelease(it, categoryTag = "SEASON") }
                    )
                }
                return@withContext Pair(decorateReleases(remote, favIds, watchedKeys, userRatingsMap), false)
            }
        } catch (e: Exception) {
            AppLogger.w("AnimeRepository", "AniList seasonal fetch failed, using offline cache: ${e.message}")
        }

        val cachedSeason = cachedAnimeDao.getCachedByCategory("SEASON").map { it.toAnimeRelease() }
        val fallback = if (cachedSeason.isNotEmpty()) {
            cachedSeason
        } else {
            val allCached = cachedAnimeDao.getAllCachedAnimeList().map { it.toAnimeRelease() }
            allCached.ifEmpty { OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()) }
        }
        Pair(decorateReleases(fallback, favIds, watchedKeys, userRatingsMap), true)
    }

    suspend fun getPopularRanking(
        page: Int = 1,
        perPage: Int = 30
    ): Pair<List<AnimeRelease>, Boolean> = withContext(Dispatchers.IO) {
        val favIds = favoriteAnimeDao.getFavoriteIdsList().toSet()
        val watchedKeys = watchedEpisodeDao.getAllWatchedKeysList().toSet()
        val userRatingsMap = userRatingDao.getAllRatingsList().associate { it.animeId to it.rating }

        try {
            val remote = aniListClient.fetchPopularRanking(page, perPage)
            if (remote.isNotEmpty()) {
                cachedAnimeDao.insertAll(
                    remote.map { CachedAnimeEntity.fromAnimeRelease(it, categoryTag = "POPULAR") }
                )
                return@withContext Pair(decorateReleases(remote, favIds, watchedKeys, userRatingsMap), false)
            }
        } catch (e: Exception) {
            AppLogger.w("AnimeRepository", "AniList popular fetch failed, using offline cache: ${e.message}")
        }

        val cachedPopular = cachedAnimeDao.getCachedByCategory("POPULAR").map { it.toAnimeRelease() }
        val fallback = if (cachedPopular.isNotEmpty()) {
            cachedPopular
        } else {
            val allCached = cachedAnimeDao.getAllCachedAnimeList().map { it.toAnimeRelease() }
            allCached.ifEmpty { OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()) }
        }
        Pair(decorateReleases(fallback, favIds, watchedKeys, userRatingsMap), true)
    }

    suspend fun searchAnime(
        query: String,
        selectedGenres: Set<String>,
        season: MediaSeason?,
        year: Int?,
        statusApi: String?,
        formatApi: String?,
        studioFilter: String?
    ): Pair<List<AnimeRelease>, Boolean> = withContext(Dispatchers.IO) {
        val favIds = favoriteAnimeDao.getFavoriteIdsList().toSet()
        val watchedKeys = watchedEpisodeDao.getAllWatchedKeysList().toSet()
        val userRatingsMap = userRatingDao.getAllRatingsList().associate { it.animeId to it.rating }

        val apiGenres = selectedGenres.map { GenreHelper.toApiGenre(it) }

        try {
            val remote = aniListClient.searchAnime(
                search = query.takeIf { it.isNotBlank() },
                genresApi = apiGenres,
                season = season,
                year = year,
                statusApi = statusApi,
                formatApi = formatApi
            )
            val studioFiltered = if (!studioFilter.isNullOrBlank()) {
                remote.filter { anime ->
                    anime.studio?.contains(studioFilter, ignoreCase = true) == true ||
                        anime.studios.any { it.contains(studioFilter, ignoreCase = true) }
                }
            } else {
                remote
            }
            return@withContext Pair(decorateReleases(studioFiltered, favIds, watchedKeys, userRatingsMap), false)
        } catch (e: Exception) {
            AppLogger.w("AnimeRepository", "AniList search failed, filtering offline cache: ${e.message}")
        }

        val offlinePool = (cachedAnimeDao.getAllCachedAnimeList().map { it.toAnimeRelease() })
            .ifEmpty { OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()) }
        val filteredOffline = offlinePool.filter { anime ->
            val matchesQuery = query.isBlank() ||
                anime.titleRomaji.contains(query, ignoreCase = true) ||
                (anime.titleEnglish?.contains(query, ignoreCase = true) == true) ||
                (anime.studio?.contains(query, ignoreCase = true) == true)
            val matchesGenre = GenreHelper.matchesAnyGenre(anime.genres, selectedGenres)
            val matchesSeason = season == null || anime.season.equals(season.apiValue, ignoreCase = true)
            val matchesYear = year == null || anime.seasonYear == year
            val matchesStatus = statusApi == null || anime.status.equals(statusApi, ignoreCase = true)
            val matchesFormat = formatApi == null || anime.format.equals(formatApi, ignoreCase = true)
            val matchesStudio = studioFilter.isNullOrBlank() || anime.studio?.contains(studioFilter, ignoreCase = true) == true
            matchesQuery && matchesGenre && matchesSeason && matchesYear && matchesStatus && matchesFormat && matchesStudio
        }
        Pair(decorateReleases(filteredOffline, favIds, watchedKeys, userRatingsMap), true)
    }

    suspend fun getAnimeDetailBundle(animeId: Int): AnimeDetailBundle? = withContext(Dispatchers.IO) {
        val favIds = favoriteAnimeDao.getFavoriteIdsList().toSet()
        val watchedKeys = watchedEpisodeDao.getAllWatchedKeysList().toSet()
        val userRatingsMap = userRatingDao.getAllRatingsList().associate { it.animeId to it.rating }

        try {
            val bundle = aniListClient.fetchAnimeDetails(animeId)
            if (bundle != null) {
                val decorated = decorateReleases(listOf(bundle.anime), favIds, watchedKeys, userRatingsMap).first()
                return@withContext bundle.copy(anime = decorated)
            }
        } catch (e: Exception) {
            AppLogger.w("AnimeRepository", "AniList detail fetch failed for id=$animeId: ${e.message}")
        }
        null
    }

    suspend fun clearOfflineCache() = withContext(Dispatchers.IO) {
        cachedAnimeDao.clearAllCache()
        aniListClient.clearMemoryCache()
    }

    private fun decorateReleases(
        items: List<AnimeRelease>,
        favIds: Set<Int>,
        watchedKeys: Set<String>,
        userRatingsMap: Map<Int, Int>
    ): List<AnimeRelease> {
        return items.map { item ->
            val key = "${item.id}_${item.episode}"
            item.copy(
                isFavorite = favIds.contains(item.id),
                isWatched = watchedKeys.contains(key),
                hasReminder = favIds.contains(item.id),
                userRating = userRatingsMap[item.id] ?: item.userRating
            )
        }
    }
}
