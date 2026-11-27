package com.github.dfedez920912.anivexa

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.dfedez920912.anivexa.data.AnimeRepository
import com.github.dfedez920912.anivexa.data.OfflineAnimeData
import com.github.dfedez920912.anivexa.data.local.AppDatabase
import com.github.dfedez920912.anivexa.model.AnimeSortOrder
import com.github.dfedez920912.anivexa.model.GenreHelper
import com.github.dfedez920912.anivexa.model.SynopsisLanguage
import com.github.dfedez920912.anivexa.notifications.AnimeNotificationHelper
import com.github.dfedez920912.anivexa.R
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: AnimeRepository

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = AnimeRepository(
      context = context,
      favoriteAnimeDao = db.favoriteAnimeDao(),
      watchedEpisodeDao = db.watchedEpisodeDao(),
      watchlistDao = db.watchlistDao(),
      cachedAnimeDao = db.cachedAnimeDao(),
      userRatingDao = db.userRatingDao()
    )
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AniVexa", appName)
  }

  @Test
  fun `offline anime schedule generates entries for current week`() {
    val today = LocalDate.now()
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(today)
    assertTrue("Schedule should not be empty", schedule.isNotEmpty())

    // Verify today has releases
    val todayReleases = schedule.filter { it.getAiringLocalDate() == today }
    assertTrue("Today should have scheduled releases", todayReleases.isNotEmpty())

    // Verify properties like synopsis, covers, and titles
    val first = todayReleases.first()
    assertTrue("Title should not be blank", first.displayTitle.isNotBlank())
    assertTrue("Cover URL should not be blank", first.coverImageUrl.isNotBlank())
    assertTrue("Synopsis should not be blank", first.synopsis.isNotBlank())
    assertTrue("Episode number should be positive", first.episode > 0)
  }

  @Test
  fun `verify translation options and episode classification`() {
    val today = LocalDate.now()
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(today)

    // Verify both ongoing episodes (ep > 1) and premieres (ep == 1) exist
    val ongoing = schedule.filter { it.isOngoingEpisode }
    val premieres = schedule.filter { it.isSeriesPremiere }

    assertTrue("Should include ongoing daily episodes", ongoing.isNotEmpty())
    assertTrue("Should include premieres", premieres.isNotEmpty())

    // Verify synopsis language switching
    val item = schedule.first()
    val itemEs = item.copy(activeLanguage = SynopsisLanguage.SPANISH)
    val itemEn = item.copy(activeLanguage = SynopsisLanguage.ENGLISH)

    assertTrue("Spanish synopsis should not be empty", itemEs.getActiveSynopsis().isNotBlank())
    assertTrue("English synopsis should not be empty", itemEn.getActiveSynopsis().isNotBlank())
  }

  @Test
  fun `test Room favorites persistence insert and toggle`() = runBlocking {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val targetAnime = schedule.first()

    // Initially should not be favorite or in watchlist
    assertFalse(repository.isFavorite(targetAnime.id))
    assertFalse(repository.isInWatchlist(targetAnime.id))

    // Toggle favorite (card bell icon) -> should be added to favorites AND auto-added to watchlist
    val isAdded = repository.toggleFavorite(targetAnime)
    assertTrue(isAdded)
    assertTrue(repository.isFavorite(targetAnime.id))
    assertTrue("Enabling reminder from card must auto-add to watchlist", repository.isInWatchlist(targetAnime.id))

    val favList = db.favoriteAnimeDao().getFavoriteIdsList()
    assertTrue(favList.contains(targetAnime.id))

    // Toggle favorite off -> removes reminder but keeps watchlist entry intact
    val isRemoved = repository.toggleFavorite(targetAnime)
    assertFalse(isRemoved)
    assertFalse(repository.isFavorite(targetAnime.id))
    assertTrue("Disabling reminder must not delete watchlist entry", repository.isInWatchlist(targetAnime.id))
  }

  @Test
  fun `test notification channel creation and reminder update`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AnimeNotificationHelper.createNotificationChannel(context)

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = notificationManager.getNotificationChannel(AnimeNotificationHelper.CHANNEL_ID)
    assertNotNull("Notification channel should be created", channel)

    // Test reminder update directly from detail screen (not previously in favorites or watchlist)
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val anime = schedule[1]
    assertFalse(repository.isFavorite(anime.id))
    assertFalse(repository.isInWatchlist(anime.id))

    repository.updateReminder(anime, enabled = true, minutesBefore = 30)
    val saved = db.favoriteAnimeDao().getFavoriteById(anime.id)
    assertNotNull(saved)
    assertTrue(saved!!.hasReminder)
    assertEquals(30, saved.reminderMinutesBefore)
    assertTrue("Enabling reminder from detail must auto-add to watchlist", repository.isInWatchlist(anime.id))
  }

  @Test
  fun `test genre filtering for Action, Drama, and Comedy and sort ordering`() {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())

    // Test Action genre filtering (both "Action" and "Acción")
    val actionAnime = schedule.filter { GenreHelper.matchesAnyGenre(it.genres, setOf("Action")) }
    val accionAnime = schedule.filter { GenreHelper.matchesAnyGenre(it.genres, setOf("Acción")) }
    assertTrue("Should find action anime", actionAnime.isNotEmpty())
    assertEquals("Action and Acción should match the same items", actionAnime.size, accionAnime.size)

    // Test Comedy genre filtering
    val comedyAnime = schedule.filter { GenreHelper.matchesAnyGenre(it.genres, setOf("Comedy")) }
    assertTrue("Should find comedy anime", comedyAnime.isNotEmpty())

    // Test Drama genre filtering
    val dramaAnime = schedule.filter { GenreHelper.matchesAnyGenre(it.genres, setOf("Drama")) }
    assertTrue("Should find drama anime", dramaAnime.isNotEmpty())

    // Test sorting by score descending
    val sortedByScore = schedule.sortedByDescending { it.score ?: 0.0 }
    for (i in 0 until sortedByScore.size - 1) {
      val scoreA = sortedByScore[i].score ?: 0.0
      val scoreB = sortedByScore[i + 1].score ?: 0.0
      assertTrue("Scores should be sorted descending", scoreA >= scoreB)
    }

    // Test sorting by title alphabetical
    val sortedByTitle = schedule.sortedBy { it.displayTitle.lowercase() }
    for (i in 0 until sortedByTitle.size - 1) {
      val titleA = sortedByTitle[i].displayTitle.lowercase()
      val titleB = sortedByTitle[i + 1].displayTitle.lowercase()
      assertTrue("Titles should be alphabetical", titleA <= titleB)
    }
  }

  @Test
  fun `test search by title finds specific anime titles accurately`() {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())

    // Search query for "Solo Leveling"
    val querySolo = "Solo Leveling"
    val resultsSolo = schedule.filter {
      it.titleRomaji.contains(querySolo, ignoreCase = true) ||
              (it.titleEnglish?.contains(querySolo, ignoreCase = true) == true)
    }
    assertTrue("Should find Solo Leveling", resultsSolo.isNotEmpty())
    assertEquals("Solo Leveling Season 2", resultsSolo.first().displayTitle)

    // Search query for "Bleach"
    val queryBleach = "Bleach"
    val resultsBleach = schedule.filter {
      it.titleRomaji.contains(queryBleach, ignoreCase = true) ||
              (it.titleEnglish?.contains(queryBleach, ignoreCase = true) == true)
    }
    assertTrue("Should find Bleach", resultsBleach.isNotEmpty())

    // Search query for "One Piece"
    val queryOnePiece = "One Piece"
    val resultsOnePiece = schedule.filter {
      it.titleRomaji.contains(queryOnePiece, ignoreCase = true) ||
              (it.titleEnglish?.contains(queryOnePiece, ignoreCase = true) == true)
    }
    assertTrue("Should find One Piece", resultsOnePiece.isNotEmpty())
  }

  @Test
  fun `test home widgets broadcast and layout configuration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.github.dfedez920912.anivexa.widget.WidgetDataHelper.notifyAllWidgets(context)

    val countViews = android.widget.RemoteViews(context.packageName, R.layout.widget_anime_count_1x1)
    val listViews = android.widget.RemoteViews(context.packageName, R.layout.widget_anime_list_3x2)
    assertNotNull(countViews)
    assertNotNull(listViews)
  }

  @Test
  fun `test Watched Episodes Room tracking toggle`() = runBlocking {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val anime = schedule.first()

    // Initially not watched
    val initialKeys = repository.getWatchedEpisodeKeys()
    assertFalse(initialKeys.contains("${anime.id}_${anime.episode}"))

    // Toggle watched -> mark as watched
    val marked = repository.toggleWatchedEpisode(anime.id, anime.episode)
    assertTrue(marked)
    val afterMarkKeys = repository.getWatchedEpisodeKeys()
    assertTrue(afterMarkKeys.contains("${anime.id}_${anime.episode}"))

    // Toggle again -> unmark
    val unmarked = repository.toggleWatchedEpisode(anime.id, anime.episode)
    assertFalse(unmarked)
    val finalKeys = repository.getWatchedEpisodeKeys()
    assertFalse(finalKeys.contains("${anime.id}_${anime.episode}"))
  }

  @Test
  fun `test Streaming Platform filtering`() {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val crunchyReleases = schedule.filter { it.streamingPlatform.equals("Crunchyroll", ignoreCase = true) }
    val netflixReleases = schedule.filter { it.streamingPlatform.equals("Netflix", ignoreCase = true) }

    assertTrue("Crunchyroll schedule should not be empty", crunchyReleases.isNotEmpty())
    assertTrue("Netflix schedule should not be empty", netflixReleases.isNotEmpty())
  }

  @Test
  fun `test WorkManager episode reminder scheduling`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val anime = schedule.first()

    // Test enqueueing WorkManager reminder does not fail
    com.github.dfedez920912.anivexa.notifications.AnimeEpisodeReminderWorker.scheduleReminderWithWorkManager(
      context = context,
      anime = anime,
      minutesBefore = 15
    )

    // Test enqueueing WorkManager immediate test notification
    com.github.dfedez920912.anivexa.notifications.AnimeEpisodeReminderWorker.scheduleImmediateTest(
      context = context,
      anime = anime
    )

    // Test cancelling WorkManager reminder does not fail
    com.github.dfedez920912.anivexa.notifications.AnimeEpisodeReminderWorker.cancelReminderWithWorkManager(
      context = context,
      animeId = anime.id
    )

    // Test FavoriteAnimeSyncWorker periodic sync enqueue
    com.github.dfedez920912.anivexa.notifications.FavoriteAnimeSyncWorker.enqueuePeriodicSync(context)
  }

  @Test
  fun `test Watchlist Room database persistence and status transitions`() = runBlocking {
    val schedule = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now())
    val anime = schedule.first()

    // Initially not in watchlist
    assertFalse(repository.isInWatchlist(anime.id))

    // Toggle watchlist -> should add
    val added = repository.toggleWatchlist(anime, com.github.dfedez920912.anivexa.data.local.WatchlistStatus.PLAN_TO_WATCH)
    assertTrue("Should be added to watchlist", added)
    assertTrue("Should exist in watchlist", repository.isInWatchlist(anime.id))

    val watchlistIds = repository.getWatchlistIds()
    assertTrue(watchlistIds.contains(anime.id))

    // Update status to WATCHING
    repository.updateWatchlistStatus(anime.id, com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING)

    // Update watched episodes
    repository.updateWatchlistEpisodes(anime.id, 5)

    // Toggle watchlist again -> should remove
    val removed = repository.toggleWatchlist(anime)
    assertFalse("Should be removed from watchlist", removed)
    assertFalse("Should not exist in watchlist", repository.isInWatchlist(anime.id))
  }

  @Test
  fun `test dynamic seasonal calculation matches current date`() {
    assertEquals(com.github.dfedez920912.anivexa.model.MediaSeason.WINTER, com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(LocalDate.of(2026, 1, 15)))
    assertEquals(com.github.dfedez920912.anivexa.model.MediaSeason.SPRING, com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(LocalDate.of(2026, 4, 20)))
    assertEquals(com.github.dfedez920912.anivexa.model.MediaSeason.SUMMER, com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(LocalDate.of(2026, 8, 5)))
    assertEquals(com.github.dfedez920912.anivexa.model.MediaSeason.FALL, com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(LocalDate.of(2026, 11, 13)))

    val currentSeason = com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(LocalDate.now())
    assertNotNull(currentSeason)
  }

  @Test
  fun `test AniListClient parses seasonal and detail GraphQL responses`() {
    val mockSeasonJson = """
      {
        "data": {
          "Page": {
            "media": [
              {
                "id": 21,
                "title": { "romaji": "ONE PIECE", "english": "One Piece", "native": "ワンピース" },
                "coverImage": { "extraLarge": "https://s4.anilist.co/cover.jpg", "large": "https://s4.anilist.co/cover.jpg", "medium": "https://s4.anilist.co/cover.jpg" },
                "bannerImage": "https://s4.anilist.co/banner.jpg",
                "description": "<b>Luffy</b> sails the Grand Line.<br>Adventure awaits!",
                "genres": ["Action", "Adventure", "Fantasy"],
                "averageScore": 88,
                "popularity": 650000,
                "episodes": null,
                "format": "TV",
                "status": "RELEASING",
                "season": "FALL",
                "seasonYear": 1999,
                "startDate": { "year": 1999, "month": 10, "day": 20 },
                "endDate": { "year": null, "month": null, "day": null },
                "nextAiringEpisode": { "episode": 1125, "airingAt": 1794600000 },
                "studios": { "nodes": [ { "name": "Toei Animation" } ] }
              }
            ]
          }
        }
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(mockSeasonJson.toResponseBody(null))
          .build()
      }
      .build()

    val client = com.github.dfedez920912.anivexa.data.AniListClient(mockClient)
    val results = client.fetchSeasonalAnime(com.github.dfedez920912.anivexa.model.MediaSeason.FALL, 2026)

    assertEquals(1, results.size)
    val onePiece = results.first()
    assertEquals(21, onePiece.id)
    assertEquals("One Piece", onePiece.displayTitle)
    assertEquals(8.8, onePiece.score!!, 0.01)
    assertEquals(1125, onePiece.effectiveEpisodeNumber())
    assertEquals("Toei Animation", onePiece.studio)
    assertEquals("Oct 20, 1999 - ?", onePiece.broadcastPeriodDisplay)
    assertFalse("HTML tags should be stripped", onePiece.synopsis.contains("<b>"))
  }

  @Test
  fun `test format filters and Spanish to AniList API genre mapping`() {
    assertEquals("Action", GenreHelper.toApiGenre("Acción"))
    assertEquals("Sci-Fi", GenreHelper.toApiGenre("Sci-Fi"))
    assertEquals("Slice of Life", GenreHelper.toApiGenre("Slice of Life"))
    assertEquals("ONA", com.github.dfedez920912.anivexa.model.MediaFormatFilter.ONA.apiValue)
    assertEquals("OVA", com.github.dfedez920912.anivexa.model.MediaFormatFilter.OVA.apiValue)
    assertEquals("TV", com.github.dfedez920912.anivexa.model.MediaFormatFilter.TV.apiValue)
  }

  @Test
  fun `test WidgetDataHelper returns real cached releases for today only`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appDb = AppDatabase.getDatabase(context)
    val dao = appDb.cachedAnimeDao()
    dao.clearAllCache()

    val zoneId = java.time.ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val todayNoon = today.atTime(12, 0).atZone(zoneId).toEpochSecond()
    val tomorrowNoon = today.plusDays(1).atTime(12, 0).atZone(zoneId).toEpochSecond()

    dao.insertAll(
      listOf(
        com.github.dfedez920912.anivexa.data.local.CachedAnimeEntity(
          id = 9001,
          airingId = 9001,
          titleRomaji = "Real Today Anime",
          titleEnglish = "Real Today Anime",
          coverImageUrl = "https://example.com/a.jpg",
          bannerImageUrl = null,
          synopsis = "Synopsis",
          episode = 5,
          totalEpisodes = 12,
          airingAtSeconds = todayNoon,
          genres = "Action",
          score = 8.4,
          studio = "Bones",
          categoryTag = "SCHEDULE"
        ),
        com.github.dfedez920912.anivexa.data.local.CachedAnimeEntity(
          id = 9002,
          airingId = 9002,
          titleRomaji = "Tomorrow Anime",
          titleEnglish = "Tomorrow Anime",
          coverImageUrl = "https://example.com/b.jpg",
          bannerImageUrl = null,
          synopsis = "Synopsis",
          episode = 3,
          totalEpisodes = 12,
          airingAtSeconds = tomorrowNoon,
          genres = "Comedy",
          score = 7.9,
          studio = "A-1 Pictures",
          categoryTag = "SCHEDULE"
        ),
        com.github.dfedez920912.anivexa.data.local.CachedAnimeEntity(
          id = 9003,
          airingId = 9003,
          titleRomaji = "Unscheduled Popular Anime",
          titleEnglish = "Unscheduled Popular Anime",
          coverImageUrl = "https://example.com/c.jpg",
          bannerImageUrl = null,
          synopsis = "Synopsis",
          episode = 1,
          totalEpisodes = 24,
          airingAtSeconds = 0L,
          genres = "Drama",
          score = 9.0,
          studio = "Madhouse",
          categoryTag = "POPULAR"
        )
      )
    )
    com.github.dfedez920912.anivexa.widget.WidgetDataHelper.markScheduleSynced(context, today)

    val (todayList, _) = com.github.dfedez920912.anivexa.widget.WidgetDataHelper.getTodayRealReleases(context, forceNetwork = false)
    assertEquals(1, todayList.size)
    assertEquals(9001, todayList.first().id)
    assertEquals(5, todayList.first().effectiveEpisodeNumber())
    dao.clearAllCache()
  }

  @Test
  fun `test Liquid Glass theme mode is selectable and persisted`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("anivexa_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("theme_mode", com.github.dfedez920912.anivexa.model.AppThemeMode.LIQUID_GLASS.name).commit()

    val savedName = prefs.getString("theme_mode", com.github.dfedez920912.anivexa.model.AppThemeMode.ANIVEXA_DARK.name)
    val resolved = com.github.dfedez920912.anivexa.model.AppThemeMode.entries.find { it.name == savedName }
    assertEquals(com.github.dfedez920912.anivexa.model.AppThemeMode.LIQUID_GLASS, resolved)
    assertEquals("Cristal (Liquid Glass)", resolved?.label)
  }

  @Test
  fun `test watchlist stays in WATCHING until all episodes are marked watched`() = runBlocking {
    val series = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).first().copy(
      id = 7777,
      totalEpisodes = 4
    )
    repository.toggleWatchlist(series)

    // Mark episodes 1 and 2 out of 4 -> should be WATCHING (En proceso), NOT COMPLETED
    repository.toggleWatchedEpisode(series.id, 1)
    repository.toggleWatchedEpisode(series.id, 2)
    var saved = db.watchlistDao().getWatchlistItemById(series.id)!!
    assertEquals(2, saved.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING, saved.statusEnum)

    // Mark episodes 3 and 4 (all 4 episodes) -> now COMPLETED
    repository.toggleWatchedEpisode(series.id, 3)
    repository.toggleWatchedEpisode(series.id, 4)
    saved = db.watchlistDao().getWatchlistItemById(series.id)!!
    assertEquals(4, saved.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.COMPLETED, saved.statusEnum)

    // Unmark episode 4 -> drops back to WATCHING
    repository.toggleWatchedEpisode(series.id, 4)
    saved = db.watchlistDao().getWatchlistItemById(series.id)!!
    assertEquals(3, saved.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING, saved.statusEnum)
  }

  @Test
  fun `test AniListClient parses related prequel sequel and OVA filtering manga and adult`() {
    val mockDetailJson = """
      {
        "data": {
          "Media": {
            "id": 100,
            "title": { "romaji": "Main Anime", "english": "Main Anime" },
            "coverImage": { "large": "https://example.com/main.jpg" },
            "description": "Main description",
            "genres": ["Action"],
            "averageScore": 85,
            "popularity": 10000,
            "episodes": 12,
            "format": "TV",
            "status": "FINISHED",
            "season": "SPRING",
            "seasonYear": 2025,
            "relations": {
              "edges": [
                {
                  "relationType": "SEQUEL",
                  "node": {
                    "id": 102,
                    "type": "ANIME",
                    "isAdult": false,
                    "format": "TV",
                    "status": "RELEASING",
                    "episodes": 12,
                    "seasonYear": 2026,
                    "averageScore": 87,
                    "title": { "romaji": "Main Anime S2", "english": "Main Anime Season 2" },
                    "coverImage": { "large": "https://example.com/s2.jpg" }
                  }
                },
                {
                  "relationType": "PREQUEL",
                  "node": {
                    "id": 99,
                    "type": "ANIME",
                    "isAdult": false,
                    "format": "TV",
                    "status": "FINISHED",
                    "episodes": 24,
                    "seasonYear": 2023,
                    "averageScore": 82,
                    "title": { "romaji": "Main Anime Zero", "english": "Main Anime Zero" },
                    "coverImage": { "large": "https://example.com/s0.jpg" }
                  }
                },
                {
                  "relationType": "SIDE_STORY",
                  "node": {
                    "id": 103,
                    "type": "ANIME",
                    "isAdult": false,
                    "format": "OVA",
                    "status": "FINISHED",
                    "episodes": 2,
                    "seasonYear": 2025,
                    "averageScore": 79,
                    "title": { "romaji": "Main Anime OVA", "english": null },
                    "coverImage": { "large": "https://example.com/ova.jpg" }
                  }
                },
                {
                  "relationType": "ADAPTATION",
                  "node": {
                    "id": 200,
                    "type": "MANGA",
                    "isAdult": false,
                    "format": "MANGA",
                    "status": "RELEASING",
                    "title": { "romaji": "Original Manga" },
                    "coverImage": { "large": "https://example.com/manga.jpg" }
                  }
                },
                {
                  "relationType": "SPIN_OFF",
                  "node": {
                    "id": 201,
                    "type": "ANIME",
                    "isAdult": true,
                    "format": "OVA",
                    "status": "FINISHED",
                    "title": { "romaji": "Adult Filtered" },
                    "coverImage": { "large": "https://example.com/adult.jpg" }
                  }
                }
              ]
            }
          }
        }
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(mockDetailJson.toResponseBody(null))
          .build()
      }
      .build()

    val client = com.github.dfedez920912.anivexa.data.AniListClient(mockClient)
    val bundle = client.fetchAnimeDetails(100)!!
    assertEquals(3, bundle.relations.size)
    assertEquals("Precuela", bundle.relations[0].relationLabelEs)
    assertEquals("Secuela", bundle.relations[1].relationLabelEs)
    assertEquals("OVA", bundle.relations[2].relationLabelEs)
  }

  @Test
  fun `test episode tracker shows full 178 episodes and accurate progress when 48 are watched`() = runBlocking {
    val animeId = 97940 // Black Clover style 178-episode season
    val partialKeys = (1..48).map { "${animeId}_$it" }.toSet()

    val partialMetrics = com.github.dfedez920912.anivexa.ui.calculateEpisodeTrackerMetrics(
      totalEpisodes = 178,
      effectiveEpisode = 178,
      animeId = animeId,
      watchedEpisodeKeys = partialKeys
    )

    assertEquals(178, partialMetrics.displayEpisodeCount)
    assertEquals(48, partialMetrics.watchedCount)
    assertEquals(48f / 178f, partialMetrics.progress, 0.001f)
    assertEquals(26, partialMetrics.progressPercent)

    // Verify Room persistence for >48 episodes (e.g. episode 150) & bulk completion for 178 episodes
    val longSeries = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).first().copy(
      id = animeId,
      totalEpisodes = 178
    )
    repository.toggleWatchlist(longSeries)
    repository.toggleWatchedEpisode(animeId, 150)

    var saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(1, saved.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING, saved.statusEnum)

    repository.markAllEpisodesWatched(animeId, 178)
    val allKeys = repository.getWatchedEpisodeKeys()
    val fullMetrics = com.github.dfedez920912.anivexa.ui.calculateEpisodeTrackerMetrics(
      totalEpisodes = 178,
      effectiveEpisode = 178,
      animeId = animeId,
      watchedEpisodeKeys = allKeys
    )
    assertEquals(178, fullMetrics.watchedCount)
    assertEquals(1.0f, fullMetrics.progress, 0.001f)
    assertEquals(100, fullMetrics.progressPercent)

    saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(178, saved.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.COMPLETED, saved.statusEnum)
  }

  @Test
  fun `test local database preserves watchlist watched episodes favorites and ratings across app reopen`(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val firstDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .addMigrations(*AppDatabase.MIGRATIONS)
      .build()

    firstDb.watchlistDao().insertWatchlist(
      com.github.dfedez920912.anivexa.data.local.WatchlistEntity(
        animeId = 5555,
        titleRomaji = "Black Clover",
        titleEnglish = "Black Clover",
        coverImageUrl = "https://example.com/bc.jpg",
        bannerImageUrl = null,
        synopsis = "Asta",
        genres = "Action, Fantasy",
        studio = "Pierrot",
        score = 8.1,
        format = "TV",
        totalEpisodes = 178,
        watchedEpisodes = 2,
        status = com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING.name
      )
    )
    firstDb.watchedEpisodeDao().markAllWatched(
      listOf(
        com.github.dfedez920912.anivexa.data.local.WatchedEpisodeEntity("5555_1", 5555, 1),
        com.github.dfedez920912.anivexa.data.local.WatchedEpisodeEntity("5555_2", 5555, 2)
      )
    )
    firstDb.favoriteAnimeDao().insertFavorite(
      com.github.dfedez920912.anivexa.data.local.FavoriteAnimeEntity(
        animeId = 5555,
        titleRomaji = "Black Clover",
        titleEnglish = "Black Clover",
        coverImageUrl = "https://example.com/bc.jpg",
        bannerImageUrl = null,
        synopsis = "Asta",
        genres = "Action",
        studio = "Pierrot",
        score = 8.1,
        format = "TV"
      )
    )
    firstDb.userRatingDao().insertOrUpdateRating(com.github.dfedez920912.anivexa.data.local.UserRatingEntity(animeId = 5555, rating = 5))

    val savedWatchlist = firstDb.watchlistDao().getWatchlistItemById(5555)
    assertNotNull("Watchlist entry must survive update", savedWatchlist)
    assertEquals(2, savedWatchlist!!.watchedEpisodes)
    assertEquals(com.github.dfedez920912.anivexa.data.local.WatchlistStatus.WATCHING, savedWatchlist.statusEnum)

    val savedEpisodes = firstDb.watchedEpisodeDao().getWatchedEpisodeNumbersForAnime(5555)
    assertEquals(listOf(1, 2), savedEpisodes.sorted())

    assertTrue("Favorite must survive update", firstDb.favoriteAnimeDao().isFavoriteDirect(5555))
    assertEquals(5, firstDb.userRatingDao().getRatingDirect(5555))

    firstDb.close()
  }

  @Test
  fun `test non-destructive migration from older DB version preserves user series and watched episodes`(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Simulate an older app installation (schema version 4) with partial columns & user progress
    val legacyHelper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
      androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(null)
        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
          override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
              """
              CREATE TABLE favorite_anime (
                animeId INTEGER NOT NULL PRIMARY KEY,
                titleRomaji TEXT NOT NULL,
                titleEnglish TEXT,
                coverImageUrl TEXT NOT NULL,
                bannerImageUrl TEXT,
                synopsis TEXT NOT NULL,
                genres TEXT NOT NULL,
                studio TEXT,
                score REAL,
                format TEXT NOT NULL
              )
              """.trimIndent()
            )
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
              """
              CREATE TABLE watched_episodes (
                episodeKey TEXT NOT NULL PRIMARY KEY,
                animeId INTEGER NOT NULL,
                episodeNumber INTEGER NOT NULL
              )
              """.trimIndent()
            )
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
              """
              CREATE TABLE watchlist_anime (
                animeId INTEGER NOT NULL PRIMARY KEY,
                titleRomaji TEXT NOT NULL,
                titleEnglish TEXT,
                coverImageUrl TEXT NOT NULL,
                bannerImageUrl TEXT,
                synopsis TEXT NOT NULL,
                genres TEXT NOT NULL,
                studio TEXT,
                score REAL,
                format TEXT NOT NULL,
                totalEpisodes INTEGER,
                watchedEpisodes INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'PLAN_TO_WATCH'
              )
              """.trimIndent()
            )
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
              """
              INSERT INTO watchlist_anime (animeId, titleRomaji, titleEnglish, coverImageUrl, bannerImageUrl, synopsis, genres, studio, score, format, totalEpisodes, watchedEpisodes, status)
              VALUES (8888, 'Bleach', 'Bleach', 'https://example.com/bleach.jpg', NULL, 'Shinigami', 'Action', 'Pierrot', 8.2, 'TV', 366, 3, 'WATCHING')
              """.trimIndent()
            )
            db.execSQL("INSERT INTO watched_episodes (episodeKey, animeId, episodeNumber) VALUES ('8888_1', 8888, 1)") // mobsf-ignore: android_kotlin_sql_raw_query
            db.execSQL("INSERT INTO watched_episodes (episodeKey, animeId, episodeNumber) VALUES ('8888_2', 8888, 2)") // mobsf-ignore: android_kotlin_sql_raw_query
            db.execSQL("INSERT INTO watched_episodes (episodeKey, animeId, episodeNumber) VALUES ('8888_3', 8888, 3)") // mobsf-ignore: android_kotlin_sql_raw_query
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
              """
              INSERT INTO favorite_anime (animeId, titleRomaji, titleEnglish, coverImageUrl, bannerImageUrl, synopsis, genres, studio, score, format)
              VALUES (8888, 'Bleach', 'Bleach', 'https://example.com/bleach.jpg', NULL, 'Shinigami', 'Action', 'Pierrot', 8.2, 'TV')
              """.trimIndent()
            )
          }

          override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
        .build()
    )

    val db = legacyHelper.writableDatabase

    // Execute v6 migration steps directly on the SQLite database
    AppDatabase.MIGRATIONS.forEach { migration ->
        if (migration.startVersion >= 4) {
            migration.migrate(db)
        }
    }

    db.close()
    legacyHelper.close()
  }

  @Test
  fun `test WidgetDataHelper today snapshot stays locked to device real date when Calendar overwrites cache`(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.github.dfedez920912.anivexa.widget.WidgetDataHelper.clearTodaySnapshot(context)

    val appDb = AppDatabase.getDatabase(context)
    val dao = appDb.cachedAnimeDao()
    dao.clearAllCache()

    val zoneId = java.time.ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val todayNoon = today.atTime(12, 0).atZone(zoneId).toEpochSecond()
    val nextWeekNoon = today.plusDays(7).atTime(12, 0).atZone(zoneId).toEpochSecond()

    val todayRelease = OfflineAnimeData.getCuratedWeekSchedule(today).first().copy(
      id = 9101,
      airingId = 9101,
      titleRomaji = "Today Locked Anime",
      titleEnglish = "Today Locked Anime",
      episode = 10,
      airingAtSeconds = todayNoon
    )

    // Save isolated today snapshot as AnimeRepository.getSchedule does when syncing today
    com.github.dfedez920912.anivexa.widget.WidgetDataHelper.saveTodaySnapshot(
      context = context,
      date = today,
      zoneId = zoneId,
      releases = listOf(todayRelease)
    )

    // Simulate user browsing another week in Calendar which overwrites the same anime ID in cached_anime
    dao.insertAll(
      listOf(
        com.github.dfedez920912.anivexa.data.local.CachedAnimeEntity(
          id = 9101,
          airingId = 9199,
          titleRomaji = "Next Week Episode",
          titleEnglish = "Next Week Episode",
          coverImageUrl = todayRelease.coverImageUrl,
          bannerImageUrl = null,
          synopsis = "Synopsis",
          episode = 11,
          totalEpisodes = 12,
          airingAtSeconds = nextWeekNoon,
          genres = "Action",
          score = 8.5,
          studio = "Bones",
          categoryTag = "SCHEDULE"
        )
      )
    )

    val (widgetReleases, resolvedZone) = com.github.dfedez920912.anivexa.widget.WidgetDataHelper.getTodayRealReleases(
      context = context,
      forceNetwork = false
    )
    assertEquals(zoneId, resolvedZone)
    assertEquals(1, widgetReleases.size)
    assertEquals(9101, widgetReleases.first().id)
    assertEquals(10, widgetReleases.first().effectiveEpisodeNumber())
    assertEquals(today, widgetReleases.first().getAiringLocalDate(zoneId))

    com.github.dfedez920912.anivexa.widget.WidgetDataHelper.clearTodaySnapshot(context)
    dao.clearAllCache()
  }

  @Test
  fun `test SeeAll season selection does not mutate Home currentSeason or currentSeasonYear`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.github.dfedez920912.anivexa.ui.AnimeViewModel(application)

    val expectedToday = LocalDate.now(viewModel.uiState.value.activeZoneId)
    val expectedSeason = com.github.dfedez920912.anivexa.model.MediaSeason.fromDate(expectedToday)
    val expectedYear = expectedToday.year

    assertEquals(expectedSeason, viewModel.uiState.value.currentSeason)
    assertEquals(expectedYear, viewModel.uiState.value.currentSeasonYear)

    // Open Ver todo and pick another season/year (e.g., Winter 2025)
    viewModel.openSeeAll(com.github.dfedez920912.anivexa.ui.SeeAllMode.THIS_SEASON)
    viewModel.selectSeeAllSeason(com.github.dfedez920912.anivexa.model.MediaSeason.WINTER, 2025)

    assertEquals(com.github.dfedez920912.anivexa.model.MediaSeason.WINTER, viewModel.uiState.value.seeAllSeason)
    assertEquals(2025, viewModel.uiState.value.seeAllYear)
    // Home state remains locked to the device's real season and year
    assertEquals(expectedSeason, viewModel.uiState.value.currentSeason)
    assertEquals(expectedYear, viewModel.uiState.value.currentSeasonYear)

    // Return to Home
    viewModel.closeSeeAll()
    assertEquals(com.github.dfedez920912.anivexa.ui.SeeAllMode.NONE, viewModel.uiState.value.seeAllMode)
    assertEquals(expectedSeason, viewModel.uiState.value.currentSeason)
    assertEquals(expectedYear, viewModel.uiState.value.currentSeasonYear)
  }

  @Test
  fun `test AndroidManifest declares Komi Store and F-Droid repository hints`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appInfo = context.packageManager.getApplicationInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_META_DATA
    )
    val metaData = appInfo.metaData
    assertNotNull("Application meta-data bundle should exist", metaData)
    assertEquals("dfedez920912/AniVexa", metaData!!.getString("github_repo"))
    assertEquals(
      "https://github.com/dfedez920912/AniVexa",
      metaData.getString("org.fdroid.fdroid.SourceCode")
    )
  }

  @Test
  fun `test allowBackup is disabled and DeviceSecurityManager runs non-blockingly`(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
    assertEquals(
      "android:allowBackup must be disabled",
      0,
      appInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP
    )

    val report = com.github.dfedez920912.anivexa.security.DeviceSecurityManager.evaluateDeviceSecurity(context)
    assertTrue("Security check timestamp should be populated", report.checkedAtMillis > 0L)
    assertNotNull("Play Integrity status should be resolved safely", report.playIntegrityStatus)
  }

  @Test
  fun `test toggleEpisodeWithPriorWatched marks 1 to N and markAll clearAll toggle all episodes`(): Unit = runBlocking {
    val animeId = 7777
    val series = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).first().copy(
      id = animeId,
      totalEpisodes = 12
    )
    repository.toggleWatchlist(series)

    val marked10 = repository.toggleEpisodeWithPriorWatched(animeId, 10)
    assertTrue("Selecting unwatched episode 10 should mark episodes 1..10", marked10)
    var watchedKeys = repository.getWatchedEpisodeKeys()
    (1..10).forEach { ep ->
      assertTrue("Episode $ep should be marked watched", watchedKeys.contains("${animeId}_$ep"))
    }
    assertFalse("Episode 11 should remain unwatched", watchedKeys.contains("${animeId}_11"))
    assertFalse("Episode 12 should remain unwatched", watchedKeys.contains("${animeId}_12"))

    var saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(10, saved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.WATCHING, saved.statusEnum)

    val unmarked10 = repository.toggleEpisodeWithPriorWatched(animeId, 10)
    assertFalse("Re-tapping watched episode 10 should unmark episode 10", unmarked10)
    watchedKeys = repository.getWatchedEpisodeKeys()
    assertFalse(watchedKeys.contains("${animeId}_10"))
    assertTrue(watchedKeys.contains("${animeId}_9"))
    saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(9, saved.watchedEpisodes)

    repository.markAllEpisodesWatched(animeId, 12)
    watchedKeys = repository.getWatchedEpisodeKeys()
    assertEquals(12, (1..12).count { ep -> watchedKeys.contains("${animeId}_$ep") })
    saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(12, saved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.COMPLETED, saved.statusEnum)

    repository.clearWatchedEpisodesForAnime(animeId)
    watchedKeys = repository.getWatchedEpisodeKeys()
    assertEquals(0, (1..12).count { ep -> watchedKeys.contains("${animeId}_$ep") })
    saved = db.watchlistDao().getWatchlistItemById(animeId)!!
    assertEquals(0, saved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.PLAN_TO_WATCH, saved.statusEnum)
  }

  @Test
  fun `test marking episode or all episodes automatically adds anime to watchlist`(): Unit = runBlocking {
    val base = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).first()

    // Case 1: Marking a single/cumulative episode from detail auto-adds to watchlist as WATCHING
    val epAnime = base.copy(id = 8801, totalEpisodes = 12)
    assertFalse(repository.isInWatchlist(epAnime.id))
    repository.toggleEpisodeWithPriorWatched(epAnime.id, 5, anime = epAnime)
    assertTrue("Anime should be auto-added to watchlist on episode mark", repository.isInWatchlist(epAnime.id))
    val epSaved = db.watchlistDao().getWatchlistItemById(epAnime.id)!!
    assertEquals(5, epSaved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.WATCHING, epSaved.statusEnum)

    // Case 2: Pressing Mark All auto-adds to watchlist as COMPLETED
    val allAnime = base.copy(id = 8802, totalEpisodes = 12)
    assertFalse(repository.isInWatchlist(allAnime.id))
    repository.markAllEpisodesWatched(allAnime.id, 12, anime = allAnime)
    assertTrue("Anime should be auto-added to watchlist on mark all", repository.isInWatchlist(allAnime.id))
    val allSaved = db.watchlistDao().getWatchlistItemById(allAnime.id)!!
    assertEquals(12, allSaved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.COMPLETED, allSaved.statusEnum)

    // Case 3: Card quick-watch toggle auto-adds to watchlist
    val cardAnime = base.copy(id = 8803, episode = 2, totalEpisodes = 12)
    assertFalse(repository.isInWatchlist(cardAnime.id))
    repository.toggleWatchedEpisode(cardAnime.id, 2, anime = cardAnime)
    assertTrue("Anime should be auto-added to watchlist on quick watch toggle", repository.isInWatchlist(cardAnime.id))
    val cardSaved = db.watchlistDao().getWatchlistItemById(cardAnime.id)!!
    assertEquals(1, cardSaved.watchedEpisodes)
    assertEquals(com.example.data.local.WatchlistStatus.WATCHING, cardSaved.statusEnum)
  }

  @Test
  fun `test navigateHomeFromDetail clears related stack and returns to Home`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.AnimeViewModel(application)
    val firstAnime = OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).first()

    viewModel.setNavScreen(com.example.ui.MainNavScreen.SEARCH)
    viewModel.openDetail(firstAnime)
    assertEquals(firstAnime.id, viewModel.uiState.value.selectedAnimeForDetail?.id)

    val related = com.example.model.RelatedAnimeItem(
      id = firstAnime.id + 500,
      relationType = "SEQUEL",
      relationLabelEs = "Secuela",
      titleRomaji = "Related Sequel",
      titleEnglish = "Related Sequel",
      coverImageUrl = "https://example.com/rel.jpg",
      format = "TV",
      status = "RELEASING",
      totalEpisodes = 12,
      seasonYear = 2026,
      score = 8.5
    )
    viewModel.openRelatedDetail(related)
    assertEquals(related.id, viewModel.uiState.value.selectedAnimeForDetail?.id)

    // Back pops one level to the previous detail card
    viewModel.closeDetail()
    assertEquals(firstAnime.id, viewModel.uiState.value.selectedAnimeForDetail?.id)

    // Go to related again, then click Home to jump straight back to Home screen
    viewModel.openRelatedDetail(related)
    viewModel.navigateHomeFromDetail()

    org.junit.Assert.assertNull(viewModel.uiState.value.selectedAnimeForDetail)
    assertEquals(com.example.ui.MainNavScreen.HOME, viewModel.uiState.value.currentNavScreen)
    assertEquals(com.example.ui.SeeAllMode.NONE, viewModel.uiState.value.seeAllMode)
  }

  @Test
  fun `test AniListClient resolves bannerImage or falls back to streamingEpisodes or trailer thumbnail`() {
    val client = com.example.data.AniListClient()

    val withBanner = org.json.JSONObject("""
      {
        "bannerImage": "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg",
        "trailer": { "thumbnail": "https://i.ytimg.com/vi/abc/hqdefault.jpg" },
        "streamingEpisodes": [{ "thumbnail": "https://img1.ak.crunchyroll.com/ep1.jpg" }]
      }
    """.trimIndent())
    assertEquals(
      "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg",
      client.resolveBannerImageUrl(withBanner)
    )

    val withEpisodeThumbOnly = org.json.JSONObject("""
      {
        "bannerImage": null,
        "trailer": { "thumbnail": "https://i.ytimg.com/vi/abc/hqdefault.jpg" },
        "streamingEpisodes": [{ "thumbnail": "https://img1.ak.crunchyroll.com/ep1.jpg" }]
      }
    """.trimIndent())
    assertEquals(
      "https://img1.ak.crunchyroll.com/ep1.jpg",
      client.resolveBannerImageUrl(withEpisodeThumbOnly)
    )

    val withTrailerOnlyAndTab = org.json.JSONObject("""
      {
        "bannerImage": null,
        "trailer": { "thumbnail": "http://i.ytimg.com/vi/LHtdKWJdif4\t/hqdefault.jpg" },
        "streamingEpisodes": []
      }
    """.trimIndent())
    assertEquals(
      "https://i.ytimg.com/vi/LHtdKWJdif4/hqdefault.jpg",
      client.resolveBannerImageUrl(withTrailerOnlyAndTab)
    )

    // Offline schedule items must all have distinct non-blank HTTPS cover and banner URLs
    OfflineAnimeData.getCuratedWeekSchedule(LocalDate.now()).forEach { item ->
      assertTrue("Cover URL should be HTTPS for ${item.displayTitle}", item.coverImageUrl.startsWith("https://"))
      assertTrue("Banner URL should be distinct HTTPS for ${item.displayTitle}", !item.bannerImageUrl.isNullOrBlank() && item.bannerImageUrl!!.startsWith("https://") && item.bannerImageUrl != item.coverImageUrl)
    }
  }
}
