package com.github.dfedez920912.anivexa.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.model.SynopsisLanguage

@Entity(tableName = "cached_anime")
data class CachedAnimeEntity(
    @PrimaryKey
    val id: Int,
    val airingId: Int,
    val titleRomaji: String,
    val titleEnglish: String?,
    val titleNative: String? = null,
    val coverImageUrl: String,
    val bannerImageUrl: String?,
    val synopsis: String,
    val synopsisEs: String = "",
    val synopsisEn: String = "",
    val episode: Int,
    val totalEpisodes: Int?,
    val airingAtSeconds: Long,
    val genres: String,
    val score: Double?,
    val popularity: Int = 0,
    val studio: String?,
    val format: String = "TV",
    val status: String = "RELEASING",
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDateText: String? = null,
    val endDateText: String? = null,
    val nextEpisode: Int? = null,
    val nextAiringAt: Long? = null,
    val streamingPlatform: String = "Crunchyroll",
    val categoryTag: String = "SCHEDULE", // SCHEDULE, SEASON, POPULAR
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toAnimeRelease(): AnimeRelease {
        return AnimeRelease(
            id = id,
            airingId = airingId,
            titleRomaji = titleRomaji,
            titleEnglish = titleEnglish,
            titleNative = titleNative,
            coverImageUrl = coverImageUrl,
            bannerImageUrl = bannerImageUrl,
            synopsis = synopsis,
            synopsisEs = synopsisEs,
            synopsisEn = synopsisEn,
            activeLanguage = if (synopsisEs.isNotBlank()) SynopsisLanguage.SPANISH else SynopsisLanguage.ENGLISH,
            episode = episode,
            totalEpisodes = totalEpisodes,
            airingAtSeconds = airingAtSeconds,
            genres = if (genres.isBlank()) emptyList() else genres.split(", ").filter { it.isNotBlank() },
            score = score,
            popularity = popularity,
            studio = studio,
            studios = listOfNotNull(studio),
            format = format,
            status = status,
            season = season,
            seasonYear = seasonYear,
            startDateText = startDateText,
            endDateText = endDateText,
            nextEpisode = nextEpisode,
            nextAiringAt = nextAiringAt,
            streamingPlatform = streamingPlatform
        )
    }

    companion object {
        fun fromAnimeRelease(anime: AnimeRelease, categoryTag: String = "SCHEDULE"): CachedAnimeEntity {
            return CachedAnimeEntity(
                id = anime.id,
                airingId = anime.airingId,
                titleRomaji = anime.titleRomaji,
                titleEnglish = anime.titleEnglish,
                titleNative = anime.titleNative,
                coverImageUrl = anime.coverImageUrl,
                bannerImageUrl = anime.bannerImageUrl,
                synopsis = anime.synopsis,
                synopsisEs = anime.synopsisEs,
                synopsisEn = anime.synopsisEn,
                episode = anime.episode,
                totalEpisodes = anime.totalEpisodes,
                airingAtSeconds = anime.airingAtSeconds,
                genres = anime.genres.joinToString(", "),
                score = anime.score,
                popularity = anime.popularity,
                studio = anime.studio,
                format = anime.format,
                status = anime.status,
                season = anime.season,
                seasonYear = anime.seasonYear,
                startDateText = anime.startDateText,
                endDateText = anime.endDateText,
                nextEpisode = anime.nextEpisode,
                nextAiringAt = anime.nextAiringAt,
                streamingPlatform = anime.streamingPlatform,
                categoryTag = categoryTag,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
