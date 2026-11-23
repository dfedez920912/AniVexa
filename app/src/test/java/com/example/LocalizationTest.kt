package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupManager
import com.example.data.AnimeRepository
import com.example.data.local.AppDatabase
import com.example.i18n.AppLocalization
import com.example.model.AnimeRelease
import com.example.model.SynopsisLanguage
import com.example.ui.AnimeUiState
import com.example.ui.MainNavScreen
import com.example.ui.OnboardingScreen
import com.example.ui.SettingsScreen
import com.example.ui.components.AniVexaBottomBar
import com.example.ui.theme.AniVexaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all four languages provide complete distinct translations for core helpers`() {
        val sampleAnime = AnimeRelease(
            id = 42,
            airingId = 42,
            titleRomaji = "Test Anime",
            titleEnglish = "Test Anime",
            coverImageUrl = "https://example.com/a.jpg",
            bannerImageUrl = null,
            synopsis = "English synopsis",
            synopsisEs = "Sinopsis en español",
            synopsisFr = "Synopsis en français",
            synopsisJa = "日本語のあらすじ",
            episode = 3,
            totalEpisodes = 12,
            airingAtSeconds = (System.currentTimeMillis() / 1000L) + 5400L,
            genres = listOf("Acción", "Fantasía"),
            score = 8.4,
            studio = "Madhouse"
        )

        val expectedNavHomes = mutableSetOf<String>()
        val expectedSettingsTitles = mutableSetOf<String>()

        SynopsisLanguage.entries.forEach { lang ->
            val s = AppLocalization.stringsFor(lang)
            assertEquals(lang, SynopsisLanguage.fromCode(lang.code))
            assertTrue(s.navHome.isNotBlank())
            assertTrue(s.settingsTitle.isNotBlank())
            assertTrue(s.onboardingTitle.isNotBlank())
            assertTrue(s.onboardingFeature3Title.isNotBlank())
            assertTrue(s.onboardingNextBtn.isNotBlank())
            assertTrue(s.onboardingSkipBtn.isNotBlank())
            assertTrue(s.splashBackupTitle.isNotBlank())
            assertTrue(s.vmDriveAuthDevError.isNotBlank())
            assertTrue(s.vmDriveAuthCode8Error.isNotBlank())
            assertTrue(s.calendarEmptyDayTitle(s.calendarThisDay).isNotBlank())
            assertTrue(AppLocalization.genreLabel("Acción", s).isNotBlank())
            assertTrue(AppLocalization.countdown(sampleAnime, s).text.isNotBlank())
            assertTrue(AppLocalization.relationLabel("SEQUEL", "TV", s).isNotBlank())

            expectedNavHomes.add(s.navHome)
            expectedSettingsTitles.add(s.settingsTitle)

            val localizedAnime = sampleAnime.copy(activeLanguage = lang)
            assertTrue(localizedAnime.getActiveSynopsis().isNotBlank())
        }

        assertEquals(4, expectedNavHomes.size)
        assertEquals(4, expectedSettingsTitles.size)
    }

    @Test
    fun `BackupManager persists and restores French and Japanese language preferences`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = AnimeRepository(context = context, database = db)
        val prefs = context.getSharedPreferences("anivexa_lang_backup_prefs", Context.MODE_PRIVATE)

        try {
            listOf(SynopsisLanguage.FRENCH, SynopsisLanguage.JAPANESE).forEach { lang ->
                prefs.edit().putString("synopsis_lang", lang.code).commit()
                val manager = BackupManager(context, repository, prefs)
                val (encrypted, _) = manager.exportEncryptedBackup()

                prefs.edit().clear().commit()
                assertEquals("es", prefs.getString("synopsis_lang", "es"))

                manager.restoreFromEncryptedBytes(encrypted)
                assertEquals(lang.code, prefs.getString("synopsis_lang", "es"))
            }
        } finally {
            prefs.edit().clear().commit()
            db.close()
        }
    }

    @Test
    fun `selecting a language from Settings dropdown updates entire UI immediately across ES EN FR JA`() {
        var currentLanguage by mutableStateOf(SynopsisLanguage.SPANISH)

        composeRule.setContent {
            AniVexaTheme(appLanguage = currentLanguage) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        uiState = AnimeUiState(globalLanguage = currentLanguage),
                        onToggleTimeZone = {},
                        onSetNotificationsEnabled = {},
                        onSetDefaultReminderMinutes = {},
                        onToggleQuietHours = {},
                        onSetGlobalLanguage = { currentLanguage = it },
                        onSetThemeMode = {},
                        onSyncNow = {},
                        onClearCache = {},
                        modifier = Modifier.weight(1f)
                    )
                    AniVexaBottomBar(
                        currentScreen = MainNavScreen.SETTINGS,
                        watchlistCount = 2,
                        onSelectScreen = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Ajustes").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()

        // Switch to English via dropdown
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_dropdown"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_language_dropdown").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_option_en").performClick()
        composeRule.waitForIdle()
        assertEquals(SynopsisLanguage.ENGLISH, currentLanguage)
        composeRule.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertIsDisplayed()

        // Switch to French via dropdown
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_dropdown"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_language_dropdown").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_option_fr").performClick()
        composeRule.waitForIdle()
        assertEquals(SynopsisLanguage.FRENCH, currentLanguage)
        composeRule.onAllNodesWithText("Paramètres").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Accueil").assertIsDisplayed()

        // Switch to Japanese via dropdown
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_dropdown"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_language_dropdown").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_option_ja").performClick()
        composeRule.waitForIdle()
        assertEquals(SynopsisLanguage.JAPANESE, currentLanguage)
        composeRule.onAllNodesWithText("設定").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("ホーム").assertIsDisplayed()

        // Switch back to Spanish via dropdown
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_dropdown"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_language_dropdown").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_option_es").performClick()
        composeRule.waitForIdle()
        assertEquals(SynopsisLanguage.SPANISH, currentLanguage)
        composeRule.onAllNodesWithText("Ajustes").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }

    @Test
    fun `AnimePosterCard renders localized synopsis preview matching active global language`() {
        var currentLanguage by mutableStateOf(SynopsisLanguage.SPANISH)
        val baseAnime = AnimeRelease(
            id = 42,
            airingId = 42,
            titleRomaji = "Test Anime",
            titleEnglish = "Test Anime",
            coverImageUrl = "https://example.com/a.jpg",
            bannerImageUrl = null,
            synopsis = "English synopsis preview",
            synopsisEs = "Sinopsis en español",
            synopsisFr = "Synopsis en français",
            synopsisJa = "日本語のあらすじ",
            episode = 3,
            totalEpisodes = 12,
            airingAtSeconds = (System.currentTimeMillis() / 1000L) + 5400L,
            genres = listOf("Acción", "Fantasía"),
            score = 8.4,
            studio = "Madhouse"
        )

        composeRule.setContent {
            AniVexaTheme(appLanguage = currentLanguage) {
                com.example.ui.components.AnimePosterCard(
                    anime = baseAnime.copy(activeLanguage = currentLanguage),
                    onClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("poster_card_synopsis_42").assertIsDisplayed()
        composeRule.onNodeWithText("Sinopsis en español").assertIsDisplayed()

        currentLanguage = SynopsisLanguage.ENGLISH
        composeRule.waitForIdle()
        composeRule.onNodeWithText("English synopsis preview").assertIsDisplayed()

        currentLanguage = SynopsisLanguage.FRENCH
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Synopsis en français").assertIsDisplayed()

        currentLanguage = SynopsisLanguage.JAPANESE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("日本語のあらすじ").assertIsDisplayed()
    }

    @Test
    fun `OnboardingScreen backup card reflects active global language on final slide`() {
        var currentLanguage by mutableStateOf(SynopsisLanguage.JAPANESE)

        composeRule.setContent {
            AniVexaTheme(appLanguage = currentLanguage) {
                OnboardingScreen(onOnboardingFinished = {})
            }
        }

        composeRule.waitForIdle()
        repeat(3) {
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText("暗号化バックアップ (.avb)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("復元").performScrollTo().assertIsDisplayed()

        currentLanguage = SynopsisLanguage.FRENCH
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sauvegardes chiffrées (.avb)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Restaurer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `MainActivity launches cleanly without crashing across Splash and Onboarding`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
        controller.pause().stop().destroy()
    }
}
