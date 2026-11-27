package com.github.dfedez920912.anivexa.notifications

import android.content.Context
import android.util.Log // mobsf-ignore: android_kotlin_logging
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.dfedez920912.anivexa.data.AnimeRepository
import com.github.dfedez920912.anivexa.util.AppLogger
import com.github.dfedez920912.anivexa.BuildConfig
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * WorkManager Periodic Worker that checks for newly scheduled or upcoming
 * episodes of the user's favorited anime in the background.
 */
class FavoriteAnimeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.d(TAG, "Starting periodic sync for favorite anime episodes...")
        return try {
            val repository = AnimeRepository(applicationContext)
            val favoriteIds = repository.getFavoriteIds()
            if (favoriteIds.isEmpty()) {
                AppLogger.d(TAG, "No favorite anime tracked. Sync finished.")
                return Result.success()
            }

            val result = repository.getSchedule(LocalDate.now(), 7)
            val schedule = result.getOrNull() ?: emptyList()

            val nowMillis = System.currentTimeMillis()
            var scheduledCount = 0

            schedule.filter { favoriteIds.contains(it.id) }.forEach { anime ->
                val episodeAirMillis = anime.airingAtSeconds * 1000
                // If episode airs in the future or within the last 30 minutes
                if (episodeAirMillis > nowMillis - (30 * 60 * 1000)) {
                    AnimeEpisodeReminderWorker.scheduleReminderWithWorkManager(
                        context = applicationContext,
                        anime = anime,
                        minutesBefore = anime.reminderMinutesBefore
                    )
                    scheduledCount++
                }
            }

            AppLogger.d(TAG, "Periodic sync completed: scheduled/refreshed reminders for $scheduledCount favorite episodes.")
            Result.success()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in FavoriteAnimeSyncWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FavAnimeSyncWorker"
        const val PERIODIC_WORK_NAME = "favorite_anime_periodic_sync"

        private fun getWorkManagerSafe(context: Context): WorkManager? {
            return try {
                WorkManager.getInstance(context)
            } catch (e: IllegalStateException) {
                try {
                    val config = androidx.work.Configuration.Builder()
                        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR) // mobsf-ignore: android_kotlin_logging
                        .build()
                    WorkManager.initialize(context, config)
                    WorkManager.getInstance(context)
                } catch (ex: Exception) {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Enqueues a periodic background sync worker that runs every 6 hours
         * whenever network connectivity is available to refresh favorite anime episode alerts.
         */
        fun enqueuePeriodicSync(context: Context) {
            val workManager = getWorkManagerSafe(context) ?: return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<FavoriteAnimeSyncWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 1,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("favorite_anime_sync")
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            AppLogger.d(TAG, "Enqueued periodic sync worker every 6 hours.")
        }
    }
}
