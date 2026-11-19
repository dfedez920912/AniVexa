package com.example.data.local

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

        private fun getExistingColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
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
            tableName: String,
            columnDefinitions: List<Pair<String, String>>
        ) {
            val existing = getExistingColumns(db, tableName)
            columnDefinitions.forEach { (colName, sqlDefinition) ->
                if (!existing.contains(colName)) {
                    db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$colName` $sqlDefinition")
                }
            }
        }

        private fun migrateNonDestructivelyToV6(db: SupportSQLiteDatabase) {
            db.execSQL(
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
                "favorite_anime",
                listOf(
                    "titleEnglish" to "TEXT",
                    "bannerImageUrl" to "TEXT",
                    "synopsis" to "TEXT NOT NULL DEFAULT ''",
                    "genres" to "TEXT NOT NULL DEFAULT ''",
                    "studio" to "TEXT",
                    "score" to "REAL",
                    "format" to "TEXT NOT NULL DEFAULT 'TV'",
                    "hasReminder" to "INTEGER NOT NULL DEFAULT 1",
                    "reminderMinutesBefore" to "INTEGER NOT NULL DEFAULT 15",
                    "addedAt" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL(
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
                "watched_episodes",
                listOf(
                    "watchedAt" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL(
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
                "watchlist_anime",
                listOf(
                    "titleEnglish" to "TEXT",
                    "bannerImageUrl" to "TEXT",
                    "synopsis" to "TEXT NOT NULL DEFAULT ''",
                    "genres" to "TEXT NOT NULL DEFAULT ''",
                    "studio" to "TEXT",
                    "score" to "REAL",
                    "format" to "TEXT NOT NULL DEFAULT 'TV'",
                    "totalEpisodes" to "INTEGER",
                    "watchedEpisodes" to "INTEGER NOT NULL DEFAULT 0",
                    "status" to "TEXT NOT NULL DEFAULT 'PLAN_TO_WATCH'",
                    "streamingPlatform" to "TEXT NOT NULL DEFAULT 'Crunchyroll'",
                    "notes" to "TEXT NOT NULL DEFAULT ''",
                    "addedAt" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL(
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
                "user_ratings",
                listOf(
                    "ratedAt" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            db.execSQL(
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
                "cached_anime",
                listOf(
                    "titleEnglish" to "TEXT",
                    "titleNative" to "TEXT",
                    "bannerImageUrl" to "TEXT",
                    "synopsisEs" to "TEXT NOT NULL DEFAULT ''",
                    "synopsisEn" to "TEXT NOT NULL DEFAULT ''",
                    "totalEpisodes" to "INTEGER",
                    "score" to "REAL",
                    "popularity" to "INTEGER NOT NULL DEFAULT 0",
                    "studio" to "TEXT",
                    "format" to "TEXT NOT NULL DEFAULT 'TV'",
                    "status" to "TEXT NOT NULL DEFAULT 'RELEASING'",
                    "season" to "TEXT",
                    "seasonYear" to "INTEGER",
                    "startDateText" to "TEXT",
                    "endDateText" to "TEXT",
                    "nextEpisode" to "INTEGER",
                    "nextAiringAt" to "INTEGER",
                    "streamingPlatform" to "TEXT NOT NULL DEFAULT 'Crunchyroll'",
                    "categoryTag" to "TEXT NOT NULL DEFAULT 'SCHEDULE'",
                    "cachedAt" to "INTEGER NOT NULL DEFAULT 0"
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
