package com.example.model

enum class AnimeSortOrder(val label: String, val shortLabel: String) {
    AIRING_TIME("Hora de emisión", "Hora"),
    SCORE("Mayor puntuación", "Nota"),
    TITLE_AZ("Título (A-Z)", "A-Z"),
    EPISODE("Número de episodio", "Episodio")
}

enum class WatchlistSortOrder(val label: String) {
    RECENT("Añadidos recientemente"),
    TITLE_AZ("Título (A-Z)"),
    SCORE("Mayor puntuación"),
    PROGRESS("Mayor progreso")
}

object GenreHelper {
    val GENRE_SYNONYMS = mapOf(
        "Acción" to listOf("Action", "Acción"),
        "Action" to listOf("Action", "Acción"),
        "Drama" to listOf("Drama"),
        "Comedia" to listOf("Comedy", "Comedia"),
        "Comedy" to listOf("Comedy", "Comedia"),
        "Fantasía" to listOf("Fantasy", "Fantasía"),
        "Fantasy" to listOf("Fantasy", "Fantasía"),
        "Aventura" to listOf("Adventure", "Aventura"),
        "Adventure" to listOf("Adventure", "Aventura"),
        "Sobrenatural" to listOf("Supernatural", "Sobrenatural"),
        "Supernatural" to listOf("Supernatural", "Sobrenatural"),
        "Sci-Fi" to listOf("Sci-Fi", "Ciencia Ficción"),
        "Misterio" to listOf("Mystery", "Misterio"),
        "Mystery" to listOf("Mystery", "Misterio"),
        "Deportes" to listOf("Sports", "Deportes"),
        "Sports" to listOf("Sports", "Deportes"),
        "Romance" to listOf("Romance"),
        "Psicológico" to listOf("Psychological", "Psicológico"),
        "Psychological" to listOf("Psychological", "Psicológico"),
        "Slice of Life" to listOf("Slice of Life", "Vida Cotidiana"),
        "Suspense" to listOf("Thriller", "Suspense", "Suspenso"),
        "Thriller" to listOf("Thriller", "Suspense", "Suspenso"),
        "Terror" to listOf("Horror", "Terror"),
        "Horror" to listOf("Horror", "Terror"),
        "Mecha" to listOf("Mecha"),
        "Música" to listOf("Music", "Música"),
        "Music" to listOf("Music", "Música"),
        "Shounen" to listOf("Shounen", "Shonen")
    )

    val FEATURED_GENRES = listOf(
        "Acción",
        "Aventura",
        "Comedia",
        "Drama",
        "Fantasía",
        "Misterio",
        "Psicológico",
        "Romance",
        "Sci-Fi",
        "Slice of Life",
        "Sobrenatural",
        "Deportes",
        "Thriller",
        "Terror",
        "Mecha",
        "Música"
    )

    val POPULAR_STUDIOS = listOf(
        "MAPPA",
        "ufotable",
        "A-1 Pictures",
        "CloverWorks",
        "Science SARU",
        "BONES",
        "WIT Studio",
        "Kyoto Animation",
        "Toei Animation",
        "Madhouse",
        "White Fox",
        "Production I.G",
        "Trigger",
        "Doga Kobo",
        "OLM",
        "8bit",
        "Pierrot"
    )

    fun toApiGenre(displayGenre: String): String {
        return when (displayGenre.trim().lowercase()) {
            "acción", "action" -> "Action"
            "aventura", "adventure" -> "Adventure"
            "comedia", "comedy" -> "Comedy"
            "drama" -> "Drama"
            "fantasía", "fantasy" -> "Fantasy"
            "misterio", "mystery" -> "Mystery"
            "psicológico", "psychological" -> "Psychological"
            "romance" -> "Romance"
            "sci-fi", "ciencia ficción" -> "Sci-Fi"
            "slice of life", "vida cotidiana" -> "Slice of Life"
            "sobrenatural", "supernatural" -> "Supernatural"
            "deportes", "sports" -> "Sports"
            "thriller", "suspense", "suspenso" -> "Thriller"
            "terror", "horror" -> "Horror"
            "mecha" -> "Mecha"
            "música", "music" -> "Music"
            else -> displayGenre.trim()
        }
    }

    fun matchesAnyGenre(animeGenres: List<String>, filterGenres: Set<String>): Boolean {
        if (filterGenres.isEmpty()) return true
        return filterGenres.any { targetGenre ->
            val synonyms = GENRE_SYNONYMS[targetGenre] ?: listOf(targetGenre)
            animeGenres.any { g ->
                synonyms.any { syn -> syn.equals(g.trim(), ignoreCase = true) }
            }
        }
    }

    fun normalizeGenre(genre: String): String {
        return when (genre.trim().lowercase()) {
            "action" -> "Acción"
            "comedy" -> "Comedia"
            "fantasy" -> "Fantasía"
            "adventure" -> "Aventura"
            "supernatural" -> "Sobrenatural"
            "mystery" -> "Misterio"
            "sports" -> "Deportes"
            "psychological" -> "Psicológico"
            "horror" -> "Terror"
            "music" -> "Música"
            else -> genre.trim().replaceFirstChar { it.uppercase() }
        }
    }
}
