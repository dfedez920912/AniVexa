package com.github.dfedez920912.anivexa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteAnimeEntity::class,
        WatchedEpisodeEntity::class,
        WatchlistEntity::class,
        CachedAnimeEntity::class,
        UserRatingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteAnimeDao(): FavoriteAnimeDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun cachedAnimeDao(): CachedAnimeDao
    abstract fun userRatingDao(): UserRatingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getExistingColumns(db: SupportSQLiteDatabase, pragmaQuery: String): Set<String> {
            val columns = mutableSetOf<String>()
            db.query(pragmaQuery).use { cursor -> // mobsf-ignore: android_kotlin_sql_raw_query
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0) {
                        columns.add(cursor.getString(nameIdx))
                    }
                }
            }
            return columns
        }

        private fun addMissingColumns(
            db: SupportSQLiteDatabase,
            pragmaQuery: String,
            columnStatements: List<Pair<String, String>>
        ) {
            val existing = getExistingColumns(db, pragmaQuery)
            columnStatements.forEach { (colName, alterSql) ->
                if (!existing.contains(colName)) {
                    db.execSQL(alterSql) // mobsf-ignore: android_kotlin_sql_raw_query
                }
            }
        }

        private fun migrateNonDestructivelyToV6(db: SupportSQLiteDatabase) {
            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
                """
                CREATE TABLE IF NOT EXISTS `favorite_anime` (
                    `animeId` INTEGER NOT NULL,
                    `titleRomaji` TEXT NOT NULL,
                    `titleEnglish` TEXT,
                    `coverImageUrl` TEXT NOT NULL,
                    `bannerImageUrl` TEXT,
                    `synopsis` TEXT NOT NULL,
                    `genres` TEXT NOT NULL,
                    `studio` TEXT,
                    `score` REAL,
                    `format` TEXT NOT NULL,
                    `hasReminder` INTEGER NOT NULL DEFAULT 1,
                    `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 15,
                    `addedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`animeId`)
                )
                """.trimIndent()
            )
            addMissingColumns(
                db,
                "PRAGMA table_info(`favorite_anime`)",
                listOf(
                    "titleEnglish" to "ALTER TABLE `favorite_anime` ADD COLUMN `titleEnglish` TEXT",
                    "bannerImageUrl" to "ALTER TABLE `favorite_anime` ADD COLUMN `bannerImageUrl` TEXT",
                    "synopsis" to "ALTER TABLE `favorite_anime` ADD COLUMN `synopsis` TEXT NOT NULL DEFAULT ''",
                    "genres" to "ALTER TABLE `favorite_anime` ADD COLUMN `genres` TEXT NOT NULL DEFAULT ''",
                    "studio" to "ALTER TABLE `favorite_anime` ADD COLUMN `studio` TEXT",
                    "score" to "ALTER TABLE `favorite_anime` ADD COLUMN `score` REAL",
                    "format" to "ALTER TABLE `favorite_anime` ADD COLUMN `format` TEXT NOT NULL DEFAULT 'TV'",
                    "hasReminder" to "ALTER TABLE `favorite_anime` ADD COLUMN `hasReminder` INTEGER NOT NULL DEFAULT 1",
                    "reminderMinutesBefore" to "ALTER TABLE `favorite_anime` ADD COLUMN `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 15",
                    "addedAt" to "ALTER TABLE `favorite_anime` ADD COLUMN `addedAt` INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
                """
                CREATE TABLE IF NOT EXISTS `watched_episodes` (
                    `episodeKey` TEXT NOT NULL,
                    `animeId` INTEGER NOT NULL,
                    `episodeNumber` INTEGER NOT NULL,
                    `watchedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`episodeKey`)
                )
                """.trimIndent()
            )
            addMissingColumns(
                db,
                "PRAGMA table_info(`watched_episodes`)",
                listOf(
                    "watchedAt" to "ALTER TABLE `watched_episodes` ADD COLUMN `watchedAt` INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
                """
                CREATE TABLE IF NOT EXISTS `watchlist_anime` (
                    `animeId` INTEGER NOT NULL,
                    `titleRomaji` TEXT NOT NULL,
                    `titleEnglish` TEXT,
                    `coverImageUrl` TEXT NOT NULL,
                    `bannerImageUrl` TEXT,
                    `synopsis` TEXT NOT NULL,
                    `genres` TEXT NOT NULL,
                    `studio` TEXT,
                    `score` REAL,
                    `format` TEXT NOT NULL,
                    `totalEpisodes` INTEGER,
                    `watchedEpisodes` INTEGER NOT NULL DEFAULT 0,
                    `status` TEXT NOT NULL DEFAULT 'PLAN_TO_WATCH',
                    `streamingPlatform` TEXT NOT NULL DEFAULT 'Crunchyroll',
                    `notes` TEXT NOT NULL DEFAULT '',
                    `addedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`animeId`)
                )
                """.trimIndent()
            )
            addMissingColumns(
                db,
                "PRAGMA table_info(`watchlist_anime`)",
                listOf(
                    "titleEnglish" to "ALTER TABLE `watchlist_anime` ADD COLUMN `titleEnglish` TEXT",
                    "bannerImageUrl" to "ALTER TABLE `watchlist_anime` ADD COLUMN `bannerImageUrl` TEXT",
                    "synopsis" to "ALTER TABLE `watchlist_anime` ADD COLUMN `synopsis` TEXT NOT NULL DEFAULT ''",
                    "genres" to "ALTER TABLE `watchlist_anime` ADD COLUMN `genres` TEXT NOT NULL DEFAULT ''",
                    "studio" to "ALTER TABLE `watchlist_anime` ADD COLUMN `studio` TEXT",
                    "score" to "ALTER TABLE `watchlist_anime` ADD COLUMN `score` REAL",
                    "format" to "ALTER TABLE `watchlist_anime` ADD COLUMN `format` TEXT NOT NULL DEFAULT 'TV'",
                    "totalEpisodes" to "ALTER TABLE `watchlist_anime` ADD COLUMN `totalEpisodes` INTEGER",
                    "watchedEpisodes" to "ALTER TABLE `watchlist_anime` ADD COLUMN `watchedEpisodes` INTEGER NOT NULL DEFAULT 0",
                    "status" to "ALTER TABLE `watchlist_anime` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'PLAN_TO_WATCH'",
                    "streamingPlatform" to "ALTER TABLE `watchlist_anime` ADD COLUMN `streamingPlatform` TEXT NOT NULL DEFAULT 'Crunchyroll'",
                    "notes" to "ALTER TABLE `watchlist_anime` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''",
                    "addedAt" to "ALTER TABLE `watchlist_anime` ADD COLUMN `addedAt` INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
                """
                CREATE TABLE IF NOT EXISTS `user_ratings` (
                    `animeId` INTEGER NOT NULL,
                    `rating` INTEGER NOT NULL,
                    `ratedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`animeId`)
                )
                """.trimIndent()
            )
            addMissingColumns(
                db,
                "PRAGMA table_info(`user_ratings`)",
                listOf(
                    "ratedAt" to "ALTER TABLE `user_ratings` ADD COLUMN `ratedAt` INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL( // mobsf-ignore: android_kotlin_sql_raw_query
                """
                CREATE TABLE IF NOT EXISTS `cached_anime` (
                    `id` INTEGER NOT NULL,
                    `airingId` INTEGER NOT NULL,
                    `titleRomaji` TEXT NOT NULL,
                    `titleEnglish` TEXT,
                    `titleNative` TEXT,
                    `coverImageUrl` TEXT NOT NULL,
                    `bannerImageUrl` TEXT,
                    `synopsis` TEXT NOT NULL,
                    `synopsisEs` TEXT NOT NULL DEFAULT '',
                    `synopsisEn` TEXT NOT NULL DEFAULT '',
                    `episode` INTEGER NOT NULL,
                    `totalEpisodes` INTEGER,
                    `airingAtSeconds` INTEGER NOT NULL,
                    `genres` TEXT NOT NULL,
                    `score` REAL,
                    `popularity` INTEGER NOT NULL DEFAULT 0,
                    `studio` TEXT,
                    `format` TEXT NOT NULL DEFAULT 'TV',
                    `status` TEXT NOT NULL DEFAULT 'RELEASING',
                    `season` TEXT,
                    `seasonYear` INTEGER,
                    `startDateText` TEXT,
                    `endDateText` TEXT,
                    `nextEpisode` INTEGER,
                    `nextAiringAt` INTEGER,
                    `streamingPlatform` TEXT NOT NULL DEFAULT 'Crunchyroll',
                    `categoryTag` TEXT NOT NULL DEFAULT 'SCHEDULE',
                    `cachedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            addMissingColumns(
                db,
                "PRAGMA table_info(`cached_anime`)",
                listOf(
                    "titleEnglish" to "ALTER TABLE `cached_anime` ADD COLUMN `titleEnglish` TEXT",
                    "titleNative" to "ALTER TABLE `cached_anime` ADD COLUMN `titleNative` TEXT",
                    "bannerImageUrl" to "ALTER TABLE `cached_anime` ADD COLUMN `bannerImageUrl` TEXT",
                    "synopsisEs" to "ALTER TABLE `cached_anime` ADD COLUMN `synopsisEs` TEXT NOT NULL DEFAULT ''",
                    "synopsisEn" to "ALTER TABLE `cached_anime` ADD COLUMN `synopsisEn` TEXT NOT NULL DEFAULT ''",
                    "totalEpisodes" to "ALTER TABLE `cached_anime` ADD COLUMN `totalEpisodes` INTEGER",
                    "score" to "ALTER TABLE `cached_anime` ADD COLUMN `score` REAL",
                    "popularity" to "ALTER TABLE `cached_anime` ADD COLUMN `popularity` INTEGER NOT NULL DEFAULT 0",
                    "studio" to "ALTER TABLE `cached_anime` ADD COLUMN `studio` TEXT",
                    "format" to "ALTER TABLE `cached_anime` ADD COLUMN `format` TEXT NOT NULL DEFAULT 'TV'",
                    "status" to "ALTER TABLE `cached_anime` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'RELEASING'",
                    "season" to "ALTER TABLE `cached_anime` ADD COLUMN `season` TEXT",
                    "seasonYear" to "ALTER TABLE `cached_anime` ADD COLUMN `seasonYear` INTEGER",
                    "startDateText" to "ALTER TABLE `cached_anime` ADD COLUMN `startDateText` TEXT",
                    "endDateText" to "ALTER TABLE `cached_anime` ADD COLUMN `endDateText` TEXT",
                    "nextEpisode" to "ALTER TABLE `cached_anime` ADD COLUMN `nextEpisode` INTEGER",
                    "nextAiringAt" to "ALTER TABLE `cached_anime` ADD COLUMN `nextAiringAt` INTEGER",
                    "streamingPlatform" to "ALTER TABLE `cached_anime` ADD COLUMN `streamingPlatform` TEXT NOT NULL DEFAULT 'Crunchyroll'",
                    "categoryTag" to "ALTER TABLE `cached_anime` ADD COLUMN `categoryTag` TEXT NOT NULL DEFAULT 'SCHEDULE'",
                    "cachedAt" to "ALTER TABLE `cached_anime` ADD COLUMN `cachedAt` INTEGER NOT NULL DEFAULT 0"
                )
            )
        }

        internal val MIGRATIONS: Array<Migration> = (1..5).map { startVersion ->
            object : Migration(startVersion, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    migrateNonDestructivelyToV6(db)
                }
            }
        }.toTypedArray()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anical_database"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
