package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupCrypto
import com.example.backup.BackupCryptoException
import com.example.backup.BackupDestination
import com.example.backup.BackupFormatException
import com.example.backup.BackupJsonSerializer
import com.example.backup.BackupManager
import com.example.backup.GoogleDriveBackupClient
import com.example.data.AnimeRepository
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.UserRatingEntity
import com.example.data.local.WatchedEpisodeEntity
import com.example.data.local.WatchlistEntity
import com.example.data.local.WatchlistStatus
import android.app.Activity
import com.example.backup.DriveResolutionOutcome
import com.example.model.AppThemeMode
import com.example.ui.OnboardingScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.AniVexaTheme
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EncryptedBackupTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AnimeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnimeRepository(
            context = context,
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `BackupCrypto encrypts into AVB1 envelope and rejects wrong password or tampered data`() {
        val secretPayload = """{"appName":"AniVexa","test":123}""".toByteArray()
        val encryptedA = BackupCrypto.encrypt(secretPayload, "MiClaveSegura99")
        val encryptedB = BackupCrypto.encrypt(secretPayload, "MiClaveSegura99")

        assertTrue("Encrypted payload should start with AVB1 header", BackupCrypto.hasAvbMagicHeader(encryptedA))
        assertFalse("Ciphertext must not expose raw JSON", String(encryptedA).contains("AniVexa"))
        assertFalse("Each encryption must use a fresh random salt & IV", encryptedA.contentEquals(encryptedB))

        val decrypted = BackupCrypto.decrypt(encryptedA, "MiClaveSegura99")
        assertArrayEquals(secretPayload, decrypted)

        try {
            BackupCrypto.decrypt(encryptedA, "ClaveIncorrecta")
            fail("Expected BackupCryptoException for wrong password")
        } catch (_: BackupCryptoException) {
            // Expected
        }

        val tampered = encryptedA.copyOf()
        tampered[tampered.lastIndex] = ((tampered[tampered.lastIndex].toInt() + 1) and 0xFF).toByte()
        try {
            BackupCrypto.decrypt(tampered, "MiClaveSegura99")
            fail("Expected BackupCryptoException for tampered ciphertext")
        } catch (_: BackupCryptoException) {
            // Expected
        }
    }

    @Test
    fun `BackupJsonSerializer rejects invalid or incomplete JSON payloads`() {
        val invalidCandidates = listOf(
            """{"invalid":true}""",
            """{"appName":"OtherApp","schemaVersion":1}""",
            """{"appName":"AniVexa","schemaVersion":1,"favorites":[]}"""
        )
        invalidCandidates.forEach { badJson ->
            try {
                BackupJsonSerializer.deserialize(badJson)
                fail("Should reject malformed backup JSON: $badJson")
            } catch (_: BackupFormatException) {
                // Expected
            }
        }
    }

    @Test
    fun `BackupManager exports encrypted avb and restores all Room tables and preferences`(): Unit = runBlocking {
        val prefs = context.getSharedPreferences("anivexa_backup_test_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_mode", AppThemeMode.LIQUID_GLASS.name)
            .putBoolean("jst_zone", true)
            .putBoolean("notif_enabled", false)
            .putInt("default_reminder_min", 30)
            .putBoolean("quiet_hours", true)
            .putString("synopsis_lang", "en")
            .commit()

        db.favoriteAnimeDao().insertFavorite(
            FavoriteAnimeEntity(
                animeId = 101,
                titleRomaji = "Frieren",
                titleEnglish = "Frieren: Beyond Journey's End",
                coverImageUrl = "https://example.com/frieren.jpg",
                bannerImageUrl = null,
                synopsis = "Mage journey",
                genres = "Adventure, Drama, Fantasy",
                studio = "Madhouse",
                score = 9.1,
                format = "TV",
                hasReminder = true,
                reminderMinutesBefore = 30
            )
        )
        db.watchedEpisodeDao().markAllWatched(
            listOf(
                WatchedEpisodeEntity("101_1", 101, 1),
                WatchedEpisodeEntity("101_2", 101, 2)
            )
        )
        db.watchlistDao().insertWatchlist(
            WatchlistEntity(
                animeId = 101,
                titleRomaji = "Frieren",
                titleEnglish = "Frieren: Beyond Journey's End",
                coverImageUrl = "https://example.com/frieren.jpg",
                bannerImageUrl = null,
                synopsis = "Mage journey",
                genres = "Adventure, Drama, Fantasy",
                studio = "Madhouse",
                score = 9.1,
                format = "TV",
                totalEpisodes = 28,
                watchedEpisodes = 2,
                status = WatchlistStatus.WATCHING.name,
                notes = "Masterpiece"
            )
        )
        db.userRatingDao().insertOrUpdateRating(UserRatingEntity(animeId = 101, rating = 5))

        val manager = BackupManager(context, repository, prefs)
        val fileName = manager.generateBackupFileName(1_760_000_000_000L)
        assertTrue(fileName.startsWith("anivexa_backup_"))
        assertTrue(fileName.endsWith(".avb"))

        val (encryptedBytes, exportStats) = manager.exportEncryptedBackup("AniVexa2026!")
        assertTrue(BackupCrypto.hasAvbMagicHeader(encryptedBytes))
        assertEquals(1, exportStats.favoritesCount)
        assertEquals(1, exportStats.watchlistCount)
        assertEquals(2, exportStats.watchedEpisodesCount)
        assertEquals(1, exportStats.ratingsCount)

        // Wipe DB and prefs completely before restoring
        db.favoriteAnimeDao().clearAll()
        db.watchedEpisodeDao().clearAll()
        db.watchlistDao().clearWatchlist()
        db.userRatingDao().clearAll()
        prefs.edit().clear().commit()

        assertTrue(db.favoriteAnimeDao().getAllFavoritesList().isEmpty())
        assertTrue(db.watchlistDao().getAllWatchlistList().isEmpty())

        val restoredStats = manager.restoreFromEncryptedBytes(encryptedBytes, "AniVexa2026!")
        assertEquals(1, restoredStats.favoritesCount)
        assertEquals(1, restoredStats.watchlistCount)
        assertEquals(2, restoredStats.watchedEpisodesCount)
        assertEquals(1, restoredStats.ratingsCount)

        assertTrue(db.favoriteAnimeDao().isFavoriteDirect(101))
        val restoredWatchlist = db.watchlistDao().getWatchlistItemById(101)
        assertNotNull(restoredWatchlist)
        assertEquals(2, restoredWatchlist!!.watchedEpisodes)
        assertEquals("Masterpiece", restoredWatchlist.notes)
        assertEquals(listOf(1, 2), db.watchedEpisodeDao().getWatchedEpisodeNumbersForAnime(101).sorted())
        assertEquals(5, db.userRatingDao().getRatingDirect(101))

        assertEquals(AppThemeMode.LIQUID_GLASS.name, prefs.getString("theme_mode", null))
        assertTrue(prefs.getBoolean("jst_zone", false))
        assertFalse(prefs.getBoolean("notif_enabled", true))
        assertEquals(30, prefs.getInt("default_reminder_min", 15))
        assertTrue(prefs.getBoolean("quiet_hours", false))
        assertEquals("en", prefs.getString("synopsis_lang", "es"))
    }

    @Test
    fun `GoogleDriveBackupClient creates AniVexa folder uploads lists and downloads avb files`() = runBlocking {
        var folderCreated = false
        var uploadedBytes = ByteArray(0)
        val mockFileContent = BackupCrypto.encrypt("""{"appName":"AniVexa"}""".toByteArray(), "TestPass")

        val mockHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                assertEquals("Bearer test-token-123", req.header("Authorization"))

                val q = req.url.queryParameter("q").orEmpty()
                val (code, bodyBytes, contentType) = when {
                    req.method == "GET" && url.contains("/drive/v3/files?") && q.contains("application/vnd.google-apps.folder") -> {
                        val json = if (folderCreated) {
                            """{"files":[{"id":"folder_anivexa_1","name":"AniVexa"}]}"""
                        } else {
                            """{"files":[]}"""
                        }
                        Triple(200, json.toByteArray(), "application/json")
                    }
                    req.method == "POST" && url.endsWith("/drive/v3/files") -> {
                        folderCreated = true
                        Triple(200, """{"id":"folder_anivexa_1","name":"AniVexa"}""".toByteArray(), "application/json")
                    }
                    req.method == "POST" && url.contains("/upload/drive/v3/files") -> {
                        val buffer = Buffer()
                        req.body?.writeTo(buffer)
                        uploadedBytes = buffer.readByteArray()
                        Triple(
                            200,
                            """{"id":"file_avb_1","name":"anivexa_backup_2026.avb","size":"${mockFileContent.size}","modifiedTime":"2026-11-21T10:00:00Z"}""".toByteArray(),
                            "application/json"
                        )
                    }
                    req.method == "GET" && q.contains("in parents") -> {
                        Triple(
                            200,
                            """{"files":[{"id":"file_avb_1","name":"anivexa_backup_2026.avb","size":"${mockFileContent.size}","modifiedTime":"2026-11-21T10:00:00Z"},{"id":"ignored_txt","name":"notes.txt","size":"12"}]}""".toByteArray(),
                            "application/json"
                        )
                    }
                    req.method == "GET" && url.contains("/drive/v3/files/file_avb_1") && url.contains("alt=media") -> {
                        Triple(200, mockFileContent, "application/octet-stream")
                    }
                    else -> Triple(404, """{"error":{"message":"Unexpected URL $url"}}""".toByteArray(), "application/json")
                }

                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("OK")
                    .body(bodyBytes.toResponseBody(contentType.toMediaType()))
                    .build()
            }
            .build()

        val client = GoogleDriveBackupClient(mockHttp)
        val entry = client.uploadEncryptedBackup("test-token-123", "anivexa_backup_2026.avb", mockFileContent)
        assertTrue(folderCreated)
        assertEquals("file_avb_1", entry.id)
        assertEquals(BackupDestination.GOOGLE_DRIVE, entry.source)
        assertTrue(String(uploadedBytes).contains("anivexa_backup_2026.avb"))

        val listed = client.listBackupFiles("test-token-123")
        assertEquals(1, listed.size)
        assertEquals("anivexa_backup_2026.avb", listed.first().fileName)

        val downloaded = client.downloadBackupBytes("test-token-123", "file_avb_1")
        assertArrayEquals(mockFileContent, downloaded)
    }

    @Test
    fun `SplashScreen stays clean like main and auto advances while OnboardingScreen offers Restore`() {
        var splashFinished by mutableStateOf(false)
        var onboardingFinished by mutableStateOf(false)

        composeRule.setContent {
            AniVexaTheme {
                if (!splashFinished) {
                    SplashScreen(onSplashFinished = { splashFinished = true })
                } else {
                    OnboardingScreen(onOnboardingFinished = { onboardingFinished = true })
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasTestTag("splash_backup_card")).fetchSemanticsNodes().size)

        composeRule.mainClock.advanceTimeBy(1000L)
        composeRule.waitForIdle()
        assertTrue(splashFinished)

        composeRule.onNodeWithTag("onboarding_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_backup_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_restore_backup_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_continue_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_restore_backup_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("backup_dest_local").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_dest_drive").assertIsDisplayed()

        composeRule.onNodeWithText("Cerrar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("onboarding_continue_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(onboardingFinished)
    }

    @Test
    fun `GoogleDriveBackupClient extractAuthorizationOutcome distinguishes Cancelled vs MissingToken`() {
        val client = GoogleDriveBackupClient(OkHttpClient())
        val cancelled = client.extractAuthorizationOutcome(context, Activity.RESULT_CANCELED, null)
        assertEquals(DriveResolutionOutcome.Cancelled, cancelled)

        val missingToken = client.extractAuthorizationOutcome(context, Activity.RESULT_OK, null)
        assertTrue(missingToken is DriveResolutionOutcome.Error)
    }

    @Test
    fun `GoogleDriveBackupClient rejects path traversal fileIds and BackupJsonSerializer strips non-HTTPS URLs`() = runBlocking {
        val client = GoogleDriveBackupClient(OkHttpClient())
        try {
            client.downloadBackupBytes("test-token", "../../etc/passwd")
            fail("Expected IOException for malicious fileId")
        } catch (_: java.io.IOException) {
            // Expected
        }

        val payloadJson = """
            {
              "schemaVersion": 1,
              "appName": "AniVexa",
              "appVersion": "0.1.7-beta.1",
              "exportedAtMillis": 1760000000000,
              "favorites": [
                {
                  "animeId": 77,
                  "titleRomaji": "Test",
                  "coverImageUrl": "javascript:alert(1)",
                  "bannerImageUrl": "file:///data/local/tmp/secret.png"
                }
              ],
              "watchedEpisodes": [],
              "watchlist": [],
              "userRatings": []
            }
        """.trimIndent()
        val parsed = BackupJsonSerializer.deserialize(payloadJson)
        assertEquals("", parsed.favorites.first().coverImageUrl)
        assertEquals(null, parsed.favorites.first().bannerImageUrl)
    }
}
