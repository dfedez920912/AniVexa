package com.github.dfedez920912.anivexa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {

    @Query("SELECT episodeKey FROM watched_episodes")
    fun getAllWatchedKeysFlow(): Flow<List<String>>

    @Query("SELECT episodeKey FROM watched_episodes")
    suspend fun getAllWatchedKeysList(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE episodeKey = :key)")
    suspend fun isWatched(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markWatched(entity: WatchedEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAllWatched(entities: List<WatchedEpisodeEntity>)

    @Query("DELETE FROM watched_episodes WHERE episodeKey = :key")
    suspend fun unmarkWatched(key: String)

    @Query("SELECT COUNT(*) FROM watched_episodes WHERE animeId = :animeId")
    suspend fun getWatchedCountForAnime(animeId: Int): Int

    @Query("SELECT episodeNumber FROM watched_episodes WHERE animeId = :animeId")
    suspend fun getWatchedEpisodeNumbersForAnime(animeId: Int): List<Int>

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAllWatchedEntitiesList(): List<WatchedEpisodeEntity>

    @Query("DELETE FROM watched_episodes WHERE animeId = :animeId")
    suspend fun clearForAnime(animeId: Int)

    @Query("DELETE FROM watched_episodes")
    suspend fun clearAll()
}
