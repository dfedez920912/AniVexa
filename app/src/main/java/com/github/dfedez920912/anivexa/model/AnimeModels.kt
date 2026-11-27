package com.github.dfedez920912.anivexa.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SynopsisLanguage(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val nativeName: String
) {
    SPANISH("es", "Español", "ES", "Español"),
    ENGLISH("en", "English", "EN", "English"),
    FRENCH("fr", "Français", "FR", "Français"),
    JAPANESE("ja", "日本語", "JA", "日本語");

    fun toLocale(): Locale = Locale.forLanguageTag(code)

    companion object {
        fun fromCode(code: String?): SynopsisLanguage {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
                ?: SPANISH
        }
    }
}

enum class EpisodeFilter(val label: String, val shortLabel: String) {
    ALL("Todos los episodios", "Todos"),
    ONGOING_EPISODES("Episodios del día", "En emisión"),
    PREMIERES("Nuevos estrenos", "Estrenos")
}

enum class MediaSeason(val apiValue: String, val labelEs: String, val labelEn: String) {
    WINTER("WINTER", "Invierno", "Winter"),
    SPRING("SPRING", "Primavera", "Spring"),
    SUMMER("SUMMER", "Verano", "Summer"),
    FALL("FALL", "Otoño", "Fall");

    companion object {
        fun fromDate(date: LocalDate = LocalDate.now()): MediaSeason {
            return when (date.monthValue) {
                1, 2, 3 -> WINTER
                4, 5, 6 -> SPRING
                7, 8, 9 -> SUMMER
                else -> FALL
            }
        }

        fun fromApi(raw: String?): MediaSeason? {
            return entries.firstOrNull { it.apiValue.equals(raw, ignoreCase = true) }
        }
    }
}

enum class MediaStatusFilter(val apiValue: String?, val labelEs: String) {
    ALL(null, "Todos"),
    RELEASING("RELEASING", "En emisión"),
    FINISHED("FINISHED", "Finalizado"),
    NOT_YET_RELEASED("NOT_YET_RELEASED", "Próximamente")
}

enum class MediaFormatFilter(val apiValue: String?, val labelEs: String) {
    ALL(null, "Todos"),
    TV("TV", "TV"),
    ONA("ONA", "Web / ONA"),
    OVA("OVA", "OVA"),
    MOVIE("MOVIE", "Película"),
    SPECIAL("SPECIAL", "Especial")
}

data class AnimeRelease(
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
    val synopsisFr: String = "",
    val synopsisJa: String = "",
    val activeLanguage: SynopsisLanguage = SynopsisLanguage.SPANISH,
    val isTranslating: Boolean = false,
    val episode: Int,
    val totalEpisodes: Int?,
    val airingAtSeconds: Long,
    val genres: List<String>,
    val score: Double?,
    val popularity: Int = 0,
    val studio: String?,
    val studios: List<String> = emptyList(),
    val format: String = "TV",
    val status: String = "RELEASING",
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDateText: String? = null,
    val endDateText: String? = null,
    val nextEpisode: Int? = null,
    val nextAiringAt: Long? = null,
    val streamingPlatform: String = "Crunchyroll",
    val isFavorite: Boolean = false,
    val isWatched: Boolean = false,
    val isInWatchlist: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val userRating: Int = 0
) {
    val displayTitle: String
        get() = if (!titleEnglish.isNullOrBlank()) titleEnglish else titleRomaji

    val isSeriesPremiere: Boolean
        get() = episode == 1 || format.equals("Película", ignoreCase = true) || format.equals("Movie", ignoreCase = true)

    val isOngoingEpisode: Boolean
        get() = !isSeriesPremiere

    val releaseTypeBadge: String
        get() = if (isSeriesPremiere) "ESTRENO" else "EMISIÓN"

    val statusLabelEs: String
        get() = when (status.uppercase()) {
            "RELEASING" -> "En Emisión"
            "FINISHED" -> "Finalizado"
            "NOT_YET_RELEASED" -> "Próximamente"
            "CANCELLED" -> "Cancelado"
            "HIATUS" -> "En Pausa"
            else -> "En Emisión"
        }

    val isCurrentlyAiringStatus: Boolean
        get() = status.equals("RELEASING", ignoreCase = true)

    val broadcastPeriodDisplay: String
        get() {
            val start = startDateText?.takeIf { it.isNotBlank() } ?: return "Por confirmar"
            val end = endDateText?.takeIf { it.isNotBlank() } ?: "?"
            return "$start - $end"
        }

    fun getActiveSynopsis(): String {
        val fallback = synopsisEn.ifBlank { synopsis.ifBlank { synopsisEs } }
        return when (activeLanguage) {
            SynopsisLanguage.SPANISH -> synopsisEs.ifBlank { fallback }
            SynopsisLanguage.ENGLISH -> synopsisEn.ifBlank { synopsis.ifBlank { synopsisEs } }
            SynopsisLanguage.FRENCH -> synopsisFr.ifBlank { fallback }
            SynopsisLanguage.JAPANESE -> synopsisJa.ifBlank { fallback }
        }
    }

    fun effectiveAiringSeconds(): Long {
        return if (airingAtSeconds > 0L) airingAtSeconds else (nextAiringAt ?: 0L)
    }

    fun effectiveEpisodeNumber(): Int {
        return if (airingAtSeconds > 0L) episode else (nextEpisode ?: episode)
    }

    fun getAiringLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        val ts = effectiveAiringSeconds()
        if (ts <= 0L) return LocalDate.now(zoneId)
        return Instant.ofEpochSecond(ts)
            .atZone(zoneId)
            .toLocalDate()
    }

    fun getFormattedTime(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val ts = effectiveAiringSeconds()
        if (ts <= 0L) return "--:--"
        val zonedDateTime = Instant.ofEpochSecond(ts).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun getFormattedDateTimeShort(
        zoneId: ZoneId = ZoneId.systemDefault(),
        fallback: String = "Sin fecha"
    ): String {
        val ts = effectiveAiringSeconds()
        if (ts <= 0L) return fallback
        val zonedDateTime = Instant.ofEpochSecond(ts).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("d/M HH:mm", Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun getCountdownStatus(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): CountdownStatus {
        val ts = effectiveAiringSeconds()
        if (ts <= 0L) return CountdownStatus.Aired("Sin horario")
        val diff = ts - currentTimeSeconds
        return when {
            diff < -7200 -> CountdownStatus.Aired("Emitido")
            diff <= 0 -> CountdownStatus.AiringNow("¡Estrenando ahora!")
            diff < 3600 -> {
                val mins = (diff / 60).coerceAtLeast(1)
                CountdownStatus.Upcoming("En $mins min")
            }
            diff < 86400 -> {
                val hours = diff / 3600
                val mins = (diff % 3600) / 60
                CountdownStatus.Upcoming("En ${hours}h ${mins}m")
            }
            else -> {
                val days = diff / 86400
                val hours = (diff % 86400) / 3600
                if (days == 1L) CountdownStatus.Upcoming("En 1d ${hours}h")
                else CountdownStatus.Upcoming("En ${days}d ${hours}h")
            }
        }
    }
}

sealed class CountdownStatus(val text: String) {
    class Aired(text: String) : CountdownStatus(text)
    class AiringNow(text: String) : CountdownStatus(text)
    class Upcoming(text: String) : CountdownStatus(text)
}

data class DayScheduleTab(
    val date: LocalDate,
    val label: String,
    val dayOfMonth: Int,
    val monthName: String,
    val fullDayName: String,
    val isToday: Boolean,
    val isTomorrow: Boolean,
    val episodeCount: Int = 0,
    val premieresCount: Int = 0,
    val ongoingCount: Int = 0
)

data class AnimeReview(
    val id: Int,
    val summary: String,
    val body: String,
    val score: Int,
    val rating: Int,
    val ratingAmount: Int,
    val userName: String,
    val userAvatarUrl: String?,
    val createdAtSeconds: Long
)

data class AnimeCharacter(
    val id: Int,
    val fullName: String,
    val nativeName: String?,
    val imageUrl: String?,
    val role: String,
    val voiceActorName: String?,
    val voiceActorImageUrl: String?
)

data class RelatedAnimeItem(
    val id: Int,
    val relationType: String,
    val relationLabelEs: String,
    val titleRomaji: String,
    val titleEnglish: String?,
    val coverImageUrl: String,
    val format: String,
    val status: String,
    val totalEpisodes: Int?,
    val seasonYear: Int?,
    val score: Double?
) {
    val displayTitle: String
        get() = if (!titleEnglish.isNullOrBlank()) titleEnglish else titleRomaji

    fun toAnimeRelease(): AnimeRelease {
        return AnimeRelease(
            id = id,
            airingId = id,
            titleRomaji = titleRomaji,
            titleEnglish = titleEnglish,
            coverImageUrl = coverImageUrl,
            bannerImageUrl = null,
            synopsis = "",
            episode = 1,
            totalEpisodes = totalEpisodes,
            airingAtSeconds = 0L,
            genres = emptyList(),
            score = score,
            studio = null,
            format = format,
            status = status,
            seasonYear = seasonYear
        )
    }
}

data class AnimeDetailBundle(
    val anime: AnimeRelease,
    val reviews: List<AnimeReview>,
    val characters: List<AnimeCharacter>,
    val relations: List<RelatedAnimeItem> = emptyList()
)
