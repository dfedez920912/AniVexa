package com.github.dfedez920912.anivexa.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.dfedez920912.anivexa.MainActivity
import com.github.dfedez920912.anivexa.i18n.AppLocalization
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.util.AppLogger
import com.github.dfedez920912.anivexa.R

object AnimeNotificationHelper {

    const val CHANNEL_ID = "anime_episode_reminders_channel"

    const val EXTRA_ANIME_ID = "extra_anime_id"
    const val EXTRA_ANIME_TITLE = "extra_anime_title"
    const val EXTRA_EPISODE = "extra_episode"
    const val EXTRA_AIRING_TIME = "extra_airing_time"
    const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val s = AppLocalization.stringsFor(context)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, s.notifChannelName, importance).apply {
                description = s.notifChannelDesc
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Initialize WorkManager periodic background sync
        try {
            FavoriteAnimeSyncWorker.enqueuePeriodicSync(context)
        } catch (e: Exception) {
            AppLogger.w("AnimeNotificationHelper", "Could not enqueue periodic sync: ${e.message}")
        }
    }

    fun scheduleAnimeReminder(
        context: Context,
        anime: AnimeRelease,
        minutesBefore: Int = 15
    ) {
        createNotificationChannel(context)

        // Primary: WorkManager guaranteed background scheduling
        try {
            AnimeEpisodeReminderWorker.scheduleReminderWithWorkManager(context, anime, minutesBefore)
            AppLogger.d("AnimeNotificationHelper", "WorkManager reminder enqueued for ${anime.displayTitle}")
        } catch (e: Exception) {
            AppLogger.w("AnimeNotificationHelper", "WorkManager schedule exception: ${e.message}")
        }

        // Secondary: AlarmManager exact alarm fallback
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val effectiveAiringSec = anime.effectiveAiringSeconds()
        val effectiveEp = anime.effectiveEpisodeNumber()
        if (effectiveAiringSec <= 0L) return

        val triggerTimeMillis = (effectiveAiringSec * 1000) - (minutesBefore * 60 * 1000)
        val nowMillis = System.currentTimeMillis()

        // If the episode is in the future, schedule the alarm. If it's very soon (e.g. today within window), schedule for immediate/soon.
        val targetMillis = if (triggerTimeMillis <= nowMillis) {
            if (effectiveAiringSec * 1000 > nowMillis) {
                nowMillis + 5000
            } else if (nowMillis - (effectiveAiringSec * 1000) < 15 * 60 * 1000) {
                nowMillis + 2000
            } else {
                return // Already aired
            }
        } else {
            triggerTimeMillis
        }

        val intent = Intent(context, AnimeReminderReceiver::class.java).apply {
            `package` = context.packageName
            putExtra(EXTRA_ANIME_ID, anime.id)
            putExtra(EXTRA_ANIME_TITLE, anime.displayTitle)
            putExtra(EXTRA_EPISODE, effectiveEp)
            putExtra(EXTRA_AIRING_TIME, anime.getFormattedTime())
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            anime.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule via AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            }
            AppLogger.d("AnimeNotificationHelper", "Scheduled reminder for ${anime.displayTitle} at $targetMillis")
        } catch (e: SecurityException) {
            AppLogger.w("AnimeNotificationHelper", "Exact alarm permission denied, fallback to set: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        }
    }

    fun cancelAnimeReminder(context: Context, animeId: Int) {
        // Cancel WorkManager task
        try {
            AnimeEpisodeReminderWorker.cancelReminderWithWorkManager(context, animeId)
        } catch (e: Exception) {
            AppLogger.w("AnimeNotificationHelper", "WorkManager cancel exception: ${e.message}")
        }

        // Cancel AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AnimeReminderReceiver::class.java).apply {
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            animeId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showImmediateNotification(
        context: Context,
        animeTitle: String,
        episode: Int,
        airingTime: String,
        minutesBefore: Int = 15
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                AppLogger.w("AnimeNotificationHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val s = AppLocalization.stringsFor(context)
        val notificationTitle = if (minutesBefore <= 0) {
            s.notifTitleAiringNow(animeTitle)
        } else {
            s.notifTitleUpcoming(animeTitle)
        }

        val notificationText = if (minutesBefore <= 0) {
            s.notifBodyAiringNow(episode, animeTitle, airingTime)
        } else {
            s.notifBodyUpcoming(episode, airingTime, minutesBefore)
        }

        val prefs = context.getSharedPreferences("anivexa_prefs", Context.MODE_PRIVATE)
        val quietHours = prefs.getBoolean("quiet_hours", false)
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isQuietTime = quietHours && (currentHour >= 23 || currentHour < 7)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSilent(isQuietTime)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(animeTitle.hashCode(), notification)
        } catch (e: SecurityException) {
            AppLogger.e("AnimeNotificationHelper", "Cannot show notification: ${e.message}")
        }
    }
}
