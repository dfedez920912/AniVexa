package com.github.dfedez920912.anivexa.data

import com.github.dfedez920912.anivexa.util.AppLogger
import com.github.dfedez920912.anivexa.model.AnimeCharacter
import com.github.dfedez920912.anivexa.model.AnimeDetailBundle
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.model.AnimeReview
import com.github.dfedez920912.anivexa.model.MediaSeason
import com.github.dfedez920912.anivexa.model.RelatedAnimeItem
import com.github.dfedez920912.anivexa.model.SynopsisLanguage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AniListClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val ENDPOINT = "https://graphql.anilist.co"
        private const val TAG = "AniListClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private data class CacheEntry(val data: Any, val expiresAtMs: Long)
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCached(key: String): T? {
        val entry = memoryCache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAtMs) {
            memoryCache.remove(key)
            return null
        }
        return entry.data as? T
    }

    private fun putCached(key: String, data: Any, ttlMinutes: Long) {
        memoryCache[key] = CacheEntry(
            data = data,
            expiresAtMs = System.currentTimeMillis() + ttlMinutes * 60_000L
        )
    }

    fun clearMemoryCache() {
        memoryCache.clear()
    }

    fun fetchAiringSchedule(
        startEpoch: Long,
        endEpoch: Long,
        maxPages: Int = 3
    ): List<AnimeRelease> {
        val cacheKey = "schedule:$startEpoch:$endEpoch"
        getCached<List<AnimeRelease>>(cacheKey)?.let { return it }

        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}start: Int, ${'$'}end: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  currentPage
                  hasNextPage
                }
                airingSchedules(
                  airingAt_greater: ${'$'}start,
                  airingAt_lesser: ${'$'}end,
                  sort: TIME
                ) {
                  id
                  episode
                  airingAt
                  timeUntilAiring
                  media {
                    id
                    isAdult
                    title {
                      romaji
                      english
                      native
                    }
                    coverImage {
                      extraLarge
                      large
                      medium
                    }
                    bannerImage
                    trailer {
                      thumbnail
                    }
                    description(asHtml: false)
                    genres
                    averageScore
                    popularity
                    episodes
                    format
                    status
                    season
                    seasonYear
                    startDate { year month day }
                    endDate { year month day }
                    nextAiringEpisode {
                      episode
                      airingAt
                    }
                    studios(isMain: true) {
                      nodes {
                        name
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val allItems = mutableListOf<AnimeRelease>()
        var page = 1
        var hasNext = true

        while (hasNext && page <= maxPages) {
            val variables = JSONObject().apply {
                put("page", page)
                put("perPage", 50)
                put("start", startEpoch)
                put("end", endEpoch)
            }
            val data = executeGraphQL(query, variables)
            val pageObj = data.optJSONObject("Page") ?: break
            val pageInfo = pageObj.optJSONObject("pageInfo")
            hasNext = pageInfo?.optBoolean("hasNextPage", false) ?: false

            val schedules = pageObj.optJSONArray("airingSchedules") ?: break
            for (i in 0 until schedules.length()) {
                val item = schedules.optJSONObject(i) ?: continue
                val media = item.optJSONObject("media") ?: continue
                if (media.optBoolean("isAdult", false)) continue
                parseMediaObject(
                    media = media,
                    airingId = item.optInt("id", media.optInt("id")),
                    episode = item.optInt("episode", 1),
                    airingAt = item.optLong("airingAt", 0L)
                )?.let { allItems.add(it) }
            }
            page++
        }

        if (allItems.isNotEmpty()) {
            putCached(cacheKey, allItems, ttlMinutes = 30)
        }
        return allItems
    }

    fun fetchSeasonalAnime(
        season: MediaSeason,
        year: Int,
        page: Int = 1,
        perPage: Int = 30
    ): List<AnimeRelease> {
        val cacheKey = "season:${season.apiValue}:$year:$page:$perPage"
        getCached<List<AnimeRelease>>(cacheKey)?.let { return it }

        val query = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(
                  type: ANIME,
                  season: ${'$'}season,
                  seasonYear: ${'$'}seasonYear,
                  sort: POPULARITY_DESC,
                  isAdult: false
                ) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  trailer {
                    thumbnail
                  }
                  description(asHtml: false)
                  genres
                  averageScore
                  popularity
                  episodes
                  format
                  status
                  season
                  seasonYear
                  startDate { year month day }
                  endDate { year month day }
                  nextAiringEpisode {
                    episode
                    airingAt
                  }
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("season", season.apiValue)
            put("seasonYear", year)
            put("page", page)
            put("perPage", perPage)
        }

        val data = executeGraphQL(query, variables)
        val mediaArray = data.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
        val list = parseMediaList(mediaArray)
        if (list.isNotEmpty()) {
            putCached(cacheKey, list, ttlMinutes = 180)
        }
        return list
    }

    fun fetchPopularRanking(page: Int = 1, perPage: Int = 30): List<AnimeRelease> {
        val cacheKey = "popular:$page:$perPage"
        getCached<List<AnimeRelease>>(cacheKey)?.let { return it }

        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(
                  type: ANIME,
                  sort: TRENDING_DESC,
                  isAdult: false
                ) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  trailer {
                    thumbnail
                  }
                  description(asHtml: false)
                  genres
                  averageScore
                  popularity
                  episodes
                  format
                  status
                  season
                  seasonYear
                  startDate { year month day }
                  endDate { year month day }
                  nextAiringEpisode {
                    episode
                    airingAt
                  }
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }

        val data = executeGraphQL(query, variables)
        val mediaArray = data.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
        val list = parseMediaList(mediaArray)
        if (list.isNotEmpty()) {
            putCached(cacheKey, list, ttlMinutes = 180)
        }
        return list
    }

    fun searchAnime(
        search: String?,
        genresApi: List<String> = emptyList(),
        season: MediaSeason? = null,
        year: Int? = null,
        statusApi: String? = null,
        formatApi: String? = null,
        page: Int = 1,
        perPage: Int = 30
    ): List<AnimeRelease> {
        val trimmedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val cacheKey = "search:$trimmedSearch:${genresApi.joinToString()}:${season?.apiValue}:$year:$statusApi:$formatApi:$page"
        getCached<List<AnimeRelease>>(cacheKey)?.let { return it }

        val sortField = if (trimmedSearch != null) "[SEARCH_MATCH, POPULARITY_DESC]" else "[POPULARITY_DESC]"

        val query = """
            query (
              ${'$'}page: Int,
              ${'$'}perPage: Int,
              ${'$'}search: String,
              ${'$'}genre_in: [String],
              ${'$'}season: MediaSeason,
              ${'$'}seasonYear: Int,
              ${'$'}status: MediaStatus,
              ${'$'}format: MediaFormat
            ) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(
                  type: ANIME,
                  isAdult: false,
                  search: ${'$'}search,
                  genre_in: ${'$'}genre_in,
                  season: ${'$'}season,
                  seasonYear: ${'$'}seasonYear,
                  status: ${'$'}status,
                  format: ${'$'}format,
                  sort: $sortField
                ) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  trailer {
                    thumbnail
                  }
                  description(asHtml: false)
                  genres
                  averageScore
                  popularity
                  episodes
                  format
                  status
                  season
                  seasonYear
                  startDate { year month day }
                  endDate { year month day }
                  nextAiringEpisode {
                    episode
                    airingAt
                  }
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
            if (trimmedSearch != null) put("search", trimmedSearch)
            if (genresApi.isNotEmpty()) put("genre_in", JSONArray(genresApi))
            if (season != null) put("season", season.apiValue)
            if (year != null) put("seasonYear", year)
            if (statusApi != null) put("status", statusApi)
            if (formatApi != null) put("format", formatApi)
        }

        val data = executeGraphQL(query, variables)
        val mediaArray = data.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
        val list = parseMediaList(mediaArray)
        if (list.isNotEmpty()) {
            putCached(cacheKey, list, ttlMinutes = 30)
        }
        return list
    }

    fun fetchAnimeDetails(animeId: Int): AnimeDetailBundle? {
        val cacheKey = "detail:$animeId"
        getCached<AnimeDetailBundle>(cacheKey)?.let { return it }

        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                title {
                  romaji
                  english
                  native
                }
                coverImage {
                  extraLarge
                  large
                  medium
                }
                bannerImage
                trailer {
                  thumbnail
                }
                streamingEpisodes {
                  thumbnail
                }
                description(asHtml: false)
                genres
                averageScore
                popularity
                episodes
                format
                status
                season
                seasonYear
                startDate { year month day }
                endDate { year month day }
                nextAiringEpisode {
                  episode
                  airingAt
                }
                studios {
                  nodes {
                    name
                    isAnimationStudio
                  }
                }
                reviews(page: 1, perPage: 8, sort: RATING_DESC) {
                  nodes {
                    id
                    summary
                    body
                    score
                    rating
                    ratingAmount
                    createdAt
                    user {
                      name
                      avatar {
                        medium
                      }
                    }
                  }
                }
                characters(page: 1, perPage: 16, sort: [ROLE, FAVOURITES_DESC]) {
                  edges {
                    role
                    node {
                      id
                      name {
                        full
                        native
                      }
                      image {
                        large
                        medium
                      }
                    }
                    voiceActors(language: JAPANESE, sort: FAVOURITES_DESC) {
                      name {
                        full
                      }
                      image {
                        medium
                      }
                    }
                  }
                }
                relations {
                  edges {
                    relationType(version: 2)
                    node {
                      id
                      type
                      isAdult
                      format
                      status
                      episodes
                      seasonYear
                      averageScore
                      title {
                        romaji
                        english
                      }
                      coverImage {
                        extraLarge
                        large
                        medium
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("id", animeId) }
        val data = executeGraphQL(query, variables)
        val media = data.optJSONObject("Media") ?: return null
        val anime = parseMediaObject(media) ?: return null

        val reviews = mutableListOf<AnimeReview>()
        val reviewNodes = media.optJSONObject("reviews")?.optJSONArray("nodes")
        if (reviewNodes != null) {
            for (i in 0 until reviewNodes.length()) {
                val r = reviewNodes.optJSONObject(i) ?: continue
                val userObj = r.optJSONObject("user")
                reviews.add(
                    AnimeReview(
                        id = r.optInt("id", i),
                        summary = cleanDescription(r.optString("summary", "")),
                        body = cleanDescription(r.optString("body", "")),
                        score = r.optInt("score", 0),
                        rating = r.optInt("rating", 0),
                        ratingAmount = r.optInt("ratingAmount", 0),
                        userName = userObj?.optString("name")?.takeIf { it.isNotBlank() } ?: "Fan de AniList",
                        userAvatarUrl = userObj?.optJSONObject("avatar")?.optString("medium")?.takeIf { it.isNotBlank() && it != "null" },
                        createdAtSeconds = r.optLong("createdAt", 0L)
                    )
                )
            }
        }

        val characters = mutableListOf<AnimeCharacter>()
        val charEdges = media.optJSONObject("characters")?.optJSONArray("edges")
        if (charEdges != null) {
            for (i in 0 until charEdges.length()) {
                val edge = charEdges.optJSONObject(i) ?: continue
                val node = edge.optJSONObject("node") ?: continue
                val nameObj = node.optJSONObject("name")
                val imgObj = node.optJSONObject("image")
                val vaArray = edge.optJSONArray("voiceActors")
                val vaObj = if (vaArray != null && vaArray.length() > 0) vaArray.optJSONObject(0) else null
                characters.add(
                    AnimeCharacter(
                        id = node.optInt("id", i),
                        fullName = nameObj?.optString("full")?.takeIf { it.isNotBlank() } ?: "Personaje",
                        nativeName = nameObj?.optString("native")?.takeIf { it.isNotBlank() && it != "null" },
                        imageUrl = imgObj?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
                            ?: imgObj?.optString("medium")?.takeIf { it.isNotBlank() && it != "null" },
                        role = edge.optString("role", "SUPPORTING"),
                        voiceActorName = vaObj?.optJSONObject("name")?.optString("full")?.takeIf { it.isNotBlank() && it != "null" },
                        voiceActorImageUrl = vaObj?.optJSONObject("image")?.optString("medium")?.takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }

        val relations = parseRelations(media.optJSONObject("relations")?.optJSONArray("edges"))

        val bundle = AnimeDetailBundle(
            anime = anime,
            reviews = reviews,
            characters = characters,
            relations = relations
        )
        putCached(cacheKey, bundle, ttlMinutes = 360)
        return bundle
    }

    private fun parseRelations(edges: JSONArray?): List<RelatedAnimeItem> {
        if (edges == null) return emptyList()
        val parsed = mutableListOf<RelatedAnimeItem>()
        for (i in 0 until edges.length()) {
            val edge = edges.optJSONObject(i) ?: continue
            val node = edge.optJSONObject("node") ?: continue
            if (!node.optString("type", "").equals("ANIME", ignoreCase = true)) continue
            if (node.optBoolean("isAdult", false)) continue

            val id = node.optInt("id", 0)
            if (id <= 0) continue

            val relationType = edge.optString("relationType", "OTHER").uppercase()
            val format = node.optString("format", "TV").takeIf { it.isNotBlank() && it != "null" } ?: "TV"
            val status = node.optString("status", "FINISHED").takeIf { it.isNotBlank() && it != "null" } ?: "FINISHED"
            val episodesInt = node.optInt("episodes", -1)
            val totalEpisodes = if (episodesInt > 0) episodesInt else null
            val seasonYear = node.optInt("seasonYear", -1).takeIf { it > 0 }
            val avgScoreInt = node.optInt("averageScore", -1)
            val score = if (avgScoreInt > 0) avgScoreInt / 10.0 else null

            val titleObj = node.optJSONObject("title")
            val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotBlank() && it != "null" } ?: "Anime #$id"
            val english = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }

            val coverObj = node.optJSONObject("coverImage")
            val coverUrl = coverObj?.optString("extraLarge")?.takeIf { it.isNotBlank() && it != "null" }
                ?: coverObj?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
                ?: coverObj?.optString("medium")?.takeIf { it.isNotBlank() && it != "null" }
                ?: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/default.jpg"

            parsed.add(
                RelatedAnimeItem(
                    id = id,
                    relationType = relationType,
                    relationLabelEs = mapRelationLabelEs(relationType, format),
                    titleRomaji = romaji,
                    titleEnglish = english,
                    coverImageUrl = coverUrl,
                    format = format,
                    status = status,
                    totalEpisodes = totalEpisodes,
                    seasonYear = seasonYear,
                    score = score
                )
            )
        }
        return parsed
            .distinctBy { it.id }
            .sortedBy { relationSortPriority(it.relationType, it.format) }
    }

    private fun mapRelationLabelEs(relationType: String, format: String): String {
        val upperType = relationType.uppercase()
        val upperFormat = format.uppercase()
        return when (upperType) {
            "PREQUEL" -> "Precuela"
            "SEQUEL" -> "Secuela"
            "PARENT" -> "Serie principal"
            "SIDE_STORY" -> when (upperFormat) {
                "OVA" -> "OVA"
                "MOVIE" -> "Película"
                "SPECIAL" -> "Especial"
                else -> "Historia paralela"
            }
            "SPIN_OFF" -> "Spin-off"
            "ALTERNATIVE" -> "Alternativa"
            "SUMMARY" -> "Resumen"
            "CHARACTER" -> "Crossover"
            else -> when (upperFormat) {
                "OVA" -> "OVA"
                "ONA" -> "ONA"
                "MOVIE" -> "Película"
                "SPECIAL" -> "Especial"
                else -> "Relacionado"
            }
        }
    }

    private fun relationSortPriority(relationType: String, format: String): Int {
        return when (relationType.uppercase()) {
            "PREQUEL" -> 0
            "SEQUEL" -> 1
            "PARENT" -> 2
            "SIDE_STORY" -> if (format.equals("OVA", ignoreCase = true)) 3 else 4
            "SPIN_OFF" -> 5
            "ALTERNATIVE" -> 6
            "SUMMARY" -> 7
            else -> if (format.equals("OVA", ignoreCase = true)) 3 else 8
        }
    }

    private fun executeGraphQL(query: String, variables: JSONObject): JSONObject {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        var response = okHttpClient.newCall(request).execute()
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 2L
            response.close()
            AppLogger.w(TAG, "Rate limited by AniList (429). Retrying after ${retryAfter}s")
            Thread.sleep((retryAfter.coerceIn(1L, 5L)) * 1000L)
            response = okHttpClient.newCall(request).execute()
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw Exception("AniList HTTP ${res.code}: ${res.message}")
            }
            val bodyString = res.body?.string() ?: throw Exception("Empty AniList response")
            val json = JSONObject(bodyString)
            if (json.has("errors")) {
                val errArr = json.optJSONArray("errors")
                val msg = errArr?.optJSONObject(0)?.optString("message") ?: "GraphQL error"
                AppLogger.w(TAG, "AniList GraphQL error: $msg")
            }
            return json.optJSONObject("data") ?: JSONObject()
        }
    }

    private fun parseMediaList(mediaArray: JSONArray): List<AnimeRelease> {
        val list = mutableListOf<AnimeRelease>()
        for (i in 0 until mediaArray.length()) {
            val media = mediaArray.optJSONObject(i) ?: continue
            parseMediaObject(media)?.let { list.add(it) }
        }
        return list
    }

    private fun parseMediaObject(
        media: JSONObject,
        airingId: Int? = null,
        episode: Int? = null,
        airingAt: Long? = null
    ): AnimeRelease? {
        val mediaId = media.optInt("id", 0)
        if (mediaId == 0) return null

        val titleObj = media.optJSONObject("title")
        val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotBlank() && it != "null" } ?: "Anime #$mediaId"
        val english = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
        val nativeTitle = titleObj?.optString("native")?.takeIf { it.isNotBlank() && it != "null" }

        val coverObj = media.optJSONObject("coverImage")
        val coverUrl = sanitizeImageUrl(coverObj?.optString("extraLarge"))
            ?: sanitizeImageUrl(coverObj?.optString("large"))
            ?: sanitizeImageUrl(coverObj?.optString("medium"))
            ?: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/default.jpg"

        val bannerUrl = resolveBannerImageUrl(media)
        val rawDescription = cleanDescription(media.optString("description", ""))

        val genresArray = media.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genresArray != null) {
            for (g in 0 until genresArray.length()) {
                val gStr = genresArray.optString(g)
                if (gStr.isNotBlank() && gStr != "null") genres.add(gStr)
            }
        }

        val avgScoreInt = media.optInt("averageScore", -1)
        val score = if (avgScoreInt > 0) avgScoreInt / 10.0 else null
        val popularity = media.optInt("popularity", 0)

        val episodesInt = media.optInt("episodes", -1)
        val totalEpisodes = if (episodesInt > 0) episodesInt else null

        val format = media.optString("format", "TV").takeIf { it.isNotBlank() && it != "null" } ?: "TV"
        val status = media.optString("status", "RELEASING").takeIf { it.isNotBlank() && it != "null" } ?: "RELEASING"
        val season = media.optString("season", "").takeIf { it.isNotBlank() && it != "null" }
        val seasonYear = media.optInt("seasonYear", -1).takeIf { it > 0 }

        val startDateText = formatFuzzyDate(media.optJSONObject("startDate"))
        val endDateText = formatFuzzyDate(media.optJSONObject("endDate"))

        val nextEpObj = media.optJSONObject("nextAiringEpisode")
        val nextEpisode = nextEpObj?.optInt("episode", -1)?.takeIf { it > 0 }
        val nextAiringAt = nextEpObj?.optLong("airingAt", -1L)?.takeIf { it > 0L }

        val studiosList = mutableListOf<String>()
        val studioNodes = media.optJSONObject("studios")?.optJSONArray("nodes")
        if (studioNodes != null) {
            for (s in 0 until studioNodes.length()) {
                val node = studioNodes.optJSONObject(s) ?: continue
                val name = node.optString("name").takeIf { it.isNotBlank() && it != "null" }
                if (name != null) studiosList.add(name)
            }
        }

        val resolvedEpisode = episode ?: nextEpisode ?: 1
        val resolvedAiringAt = airingAt ?: nextAiringAt ?: 0L

        return AnimeRelease(
            id = mediaId,
            airingId = airingId ?: mediaId,
            titleRomaji = romaji,
            titleEnglish = english,
            titleNative = nativeTitle,
            coverImageUrl = coverUrl,
            bannerImageUrl = bannerUrl,
            synopsis = rawDescription.ifBlank { "Sinopsis no disponible." },
            synopsisEs = "",
            synopsisEn = rawDescription,
            activeLanguage = SynopsisLanguage.ENGLISH,
            episode = resolvedEpisode,
            totalEpisodes = totalEpisodes,
            airingAtSeconds = resolvedAiringAt,
            genres = genres,
            score = score,
            popularity = popularity,
            studio = studiosList.firstOrNull(),
            studios = studiosList,
            format = format,
            status = status,
            season = season,
            seasonYear = seasonYear,
            startDateText = startDateText,
            endDateText = endDateText,
            nextEpisode = nextEpisode,
            nextAiringAt = nextAiringAt
        )
    }

    private fun formatFuzzyDate(obj: JSONObject?): String? {
        if (obj == null) return null
        val year = obj.optInt("year", 0)
        if (year <= 0) return null
        val month = obj.optInt("month", 0)
        val day = obj.optInt("day", 0)
        if (month in 1..12) {
            val monthShort = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            return if (day > 0) "$monthShort $day, $year" else "$monthShort $year"
        }
        return "$year"
    }

    private fun cleanDescription(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        return raw.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)<p.*?>"), "")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("<.*?>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    internal fun resolveBannerImageUrl(media: JSONObject): String? {
        sanitizeImageUrl(media.optString("bannerImage"))?.let { return it }

        val streamingEpisodes = media.optJSONArray("streamingEpisodes")
        if (streamingEpisodes != null) {
            for (i in 0 until streamingEpisodes.length()) {
                val thumb = sanitizeImageUrl(streamingEpisodes.optJSONObject(i)?.optString("thumbnail"))
                if (thumb != null) return thumb
            }
        }

        return sanitizeImageUrl(media.optJSONObject("trailer")?.optString("thumbnail"))
    }

    private fun sanitizeImageUrl(raw: String?): String? {
        val cleaned = raw?.replace(Regex("\\s+"), "")?.trim() ?: return null
        if (cleaned.isBlank() || cleaned.equals("null", ignoreCase = true)) return null
        val httpsUrl = if (cleaned.startsWith("http://", ignoreCase = true)) {
            "https://" + cleaned.substring(7)
        } else {
            cleaned
        }
        return httpsUrl.takeIf { it.startsWith("https://", ignoreCase = true) }
    }
}
