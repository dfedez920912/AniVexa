package com.example.i18n

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.local.WatchlistStatus
import com.example.model.AnimeRelease
import com.example.model.AppThemeMode
import com.example.model.MediaFormatFilter
import com.example.model.MediaSeason
import com.example.model.MediaStatusFilter
import com.example.model.SynopsisLanguage
import com.example.model.WatchlistSortOrder
import com.example.security.PlayIntegrityStatus
import com.example.ui.CalendarViewMode
import com.example.ui.MainNavScreen
import com.example.ui.MyListFilterTab
import com.example.ui.SeeAllMode

data class AppStrings(
    // App & Nav
    val appName: String,
    val navHome: String,
    val navCalendar: String,
    val navMyList: String,
    val navSearch: String,
    val navSettings: String,
    val backContentDesc: String,

    // Splash
    val splashSubtitle: String,
    val splashNewBadge: String,
    val splashBackupTitle: String,
    val splashBackupDesc: String,
    val splashRestoreBtn: String,
    val splashContinueBtn: String,

    // Home & Catalog
    val offlineBadge: String,
    val offlineContentDesc: String,
    val refreshContentDesc: String,
    val homeThisSeason: String,
    val homePopularRanking: String,
    val homeAiringToday: String,
    val homeSeeAll: String,
    val homeGoCalendar: String,

    // Seasons & Statuses & Formats
    val seasonWinter: String,
    val seasonSpring: String,
    val seasonSummer: String,
    val seasonFall: String,
    val statusAll: String,
    val statusReleasing: String,
    val statusFinished: String,
    val statusNotYetReleased: String,
    val statusCancelled: String,
    val statusHiatus: String,
    val formatAll: String,
    val formatTv: String,
    val formatOna: String,
    val formatOva: String,
    val formatMovie: String,
    val formatSpecial: String,
    val badgeAiring: String,
    val badgeSoon: String,
    val badgePremiere: String,
    val badgeOngoing: String,
    val episodeShortPrefix: String,
    val episodesUnitShort: String,

    // Countdown & Dates
    val countdownNoSchedule: String,
    val countdownAired: String,
    val countdownAiringNow: String,
    val countdownInMinutes: (Long) -> String,
    val countdownInHoursMins: (Long, Long) -> String,
    val countdownInDaysHours: (Long, Long) -> String,
    val periodTba: String,
    val noDateText: String,

    // Calendar
    val calendarTitle: String,
    val calendarTodayBtnDesc: String,
    val calendarFilterBtnDesc: String,
    val calendarMyListBtnDesc: String,
    val calendarModeWeek: String,
    val calendarModeMonth: String,
    val calendarModeList: String,
    val calendarGenreChipDefault: String,
    val calendarAllGenres: String,
    val calendarStudioChipDefault: String,
    val calendarAllStudios: String,
    val calendarPrevWeekDesc: String,
    val calendarNextWeekDesc: String,
    val calendarPrevMonthDesc: String,
    val calendarNextMonthDesc: String,
    val calendarReleasesForDay: (String) -> String,
    val calendarTodayTag: String,
    val calendarThisDay: String,
    val calendarThisWeek: String,
    val calendarEmptyDayTitle: (String) -> String,
    val calendarEmptyDaySubtitle: String,
    val calendarReminderDesc: String,
    val calendarMarkWatchedDesc: String,

    // My List
    val myListTitle: String,
    val myListSortDesc: String,
    val myListRemoveDesc: String,
    val myListTabAll: String,
    val myListTabWatched: String,
    val myListTabUnwatched: String,
    val myListTabWatching: String,
    val myListTabPlanToWatch: String,
    val sortRecent: String,
    val sortTitleAz: String,
    val sortScore: String,
    val sortProgress: String,
    val statusPlanToWatch: String,
    val statusWatching: String,
    val statusCompleted: String,
    val statusDropped: String,
    val statusPillPaused: String,
    val statusPillWatched: String,
    val statusPillWatching: String,
    val statusPillUnwatched: String,
    val myListMarkAsWatched: String,
    val myListPlusOneEp: String,
    val myListEmptyTitle: String,
    val myListEmptyCategoryTitle: String,
    val myListEmptyBody: String,
    val myListEmptyCategoryBody: String,
    val myListExploreBtn: String,

    // Search
    val searchPlaceholder: String,
    val searchIconDesc: String,
    val searchClearDesc: String,
    val searchShowFilters: String,
    val searchHideFilters: String,
    val searchDiscoveryFilters: String,
    val searchClearAll: String,
    val searchLabelGenre: String,
    val searchLabelSeasonYear: String,
    val searchLabelStatusFormat: String,
    val searchLabelStudio: String,
    val searchAnyChip: String,
    val searchAllChip: String,
    val searchNoResultsTitle: String,
    val searchNoResultsSubtitle: String,

    // Detail Screen
    val detailAlertDesc: String,
    val detailShareDesc: String,
    val detailShareChooser: String,
    val detailShareTemplate: (String, Int, String) -> String,
    val detailFansSuffix: (String) -> String,
    val detailSynopsisTitle: String,
    val detailShowLess: String,
    val detailReadMore: String,
    val detailInMyListRemove: String,
    val detailAddToMyList: String,
    val detailReviewsBtn: String,
    val detailCharactersBtn: String,
    val detailBasicInfoTitle: String,
    val detailTypeLabel: String,
    val detailEpisodesLabel: String,
    val detailEpisodesUnknown: String,
    val detailBroadcastPeriodLabel: String,
    val detailStudioPrefix: (String) -> String,
    val detailEpisodeLabel: (Int) -> String,
    val detailNoUpcomingSchedule: String,
    val detailTestAlertBtn: String,
    val detailEpisodeTrackerTitle: String,
    val detailSwipeEpisodesHint: (Int) -> String,
    val detailYourRatingTitle: String,
    val detailRateStarsDesc: (Int) -> String,
    val detailRelatedTitle: String,
    val detailReviewsModalTitle: String,
    val detailNoReviews: String,
    val detailCharactersModalTitle: String,
    val detailNoCharacters: String,
    val detailRoleMain: String,
    val detailRoleSupporting: String,
    val relationPrequel: String,
    val relationSequel: String,
    val relationParent: String,
    val relationSideStory: String,
    val relationSpinOff: String,
    val relationAlternative: String,
    val relationSummary: String,
    val relationCharacter: String,
    val relationOther: String,

    // Genres
    val genreAction: String,
    val genreAdventure: String,
    val genreComedy: String,
    val genreDrama: String,
    val genreFantasy: String,
    val genreMystery: String,
    val genrePsychological: String,
    val genreRomance: String,
    val genreSciFi: String,
    val genreSliceOfLife: String,
    val genreSupernatural: String,
    val genreSports: String,
    val genreThriller: String,
    val genreHorror: String,
    val genreMecha: String,
    val genreMusic: String,

    // Settings
    val settingsTitle: String,
    val settingsNotifSection: String,
    val settingsAlertsTitle: String,
    val settingsAlertsSubtitle: String,
    val settingsDefaultLeadTime: String,
    val settingsMinUnit: (Int) -> String,
    val settingsHourUnit: String,
    val settingsOnAiring: String,
    val settingsOnAiringShort: String,
    val settingsQuietHoursTitle: String,
    val settingsQuietHoursSubtitle: String,
    val settingsRegionLangSection: String,
    val settingsJstTitle: String,
    val settingsJstSubtitleOn: String,
    val settingsJstSubtitleOff: String,
    val settingsLanguageTitle: String,
    val settingsLanguageSubtitle: String,
    val settingsAppearanceSection: String,
    val themeDarkLabel: String,
    val themeDarkDesc: String,
    val themeOledLabel: String,
    val themeOledDesc: String,
    val themeLightLabel: String,
    val themeLightDesc: String,
    val themeGlassLabel: String,
    val themeGlassDesc: String,
    val themeSystemLabel: String,
    val themeSystemDesc: String,
    val settingsOfflineSection: String,
    val settingsCachedSeriesCount: (Int) -> String,
    val settingsSyncNow: String,
    val settingsClearCache: String,
    val settingsBackupSection: String,
    val settingsBackupDesc: (String, String) -> String,
    val settingsBackupBtn: String,
    val settingsRestoreBtn: String,
    val settingsSecuritySection: String,
    val settingsRootDetected: (String) -> String,
    val settingsNoRootDetected: String,
    val settingsPlayIntegrityLine: (String, String) -> String,
    val settingsHardeningInfo: String,
    val settingsVerifySecurityBtn: String,
    val settingsAboutSection: String,
    val settingsAboutTagline: String,
    val settingsAboutDesc: String,

    // Security statuses
    val integrityCheckingLabel: String,
    val integrityCheckingDetail: String,
    val integrityVerifiedLabel: String,
    val integrityVerifiedDetail: String,
    val integrityAvailableLabel: String,
    val integrityAvailableDetail: String,
    val integrityUnavailableLabel: String,
    val integrityUnavailableDetail: String,
    val integrityErrorLabel: String,
    val integrityErrorDetail: String,

    // Backup & Restore Dialog
    val backupDoneTitle: String,
    val restoreDoneTitle: String,
    val backupChooseDestTitle: String,
    val restoreChooseDestTitle: String,
    val backupLocalPermTitle: String,
    val backupEncryptTitle: String,
    val restorePickFileTitle: String,
    val restoreDecryptTitle: String,
    val backupBusyCreating: String,
    val backupBusyRestoring: String,
    val backupChooseDestPrompt: String,
    val restoreChooseDestPrompt: String,
    val backupLocalTitle: String,
    val backupLocalSubtitle: (String) -> String,
    val backupDriveTitle: String,
    val backupDriveSubtitle: String,
    val backupLocalPermBody: (String) -> String,
    val backupGrantFolderBtn: String,
    val restorePickManualBtn: String,
    val backupTargetDrivePath: String,
    val backupTargetLocalPath: (String) -> String,
    val backupEnterPassPrompt: (String) -> String,
    val backupPassLabel: String,
    val backupConfirmPassLabel: String,
    val restoreFolderHintDrive: String,
    val restoreFolderHintLocal: (String) -> String,
    val restoreRefreshListDesc: String,
    val restoreNoFilesFound: String,
    val restoreBrowseOtherBtn: String,
    val restoreWarningBody: (String) -> String,
    val restorePassLabel: String,
    val backupEnterAppBtn: String,
    val backupDoneBtn: String,
    val backupCreateActionBtn: String,
    val restoreActionBtn: String,
    val dialogBackBtn: String,
    val dialogCloseBtn: String,
    val passHideDesc: String,
    val passShowDesc: String,
    val backupMetaFallback: String,
    val backupMetaEncrypted: String,
    val backupErrorMinLength: String,
    val backupErrorMismatch: String,
    val backupErrorEnterPassRestore: String,

    // ViewModel Messages
    val vmSecurityWarningNotifyRoot: String,
    val vmSecurityCleanNotify: (String) -> String,
    val vmSecurityWarningStartupRoot: String,
    val vmWatchlistAdded: (String) -> String,
    val vmWatchlistRemoved: (String) -> String,
    val vmStatusUpdated: (String) -> String,
    val vmEpisodeWatched: (Int) -> String,
    val vmEpisodeUnwatched: (Int) -> String,
    val vmReminderEnabled: (String, Int) -> String,
    val vmReminderDisabled: (String) -> String,
    val vmAlertScheduled: (Int, String) -> String,
    val vmAlertDisabled: String,
    val vmTestNotifSent: (String) -> String,
    val vmRatingSaved: (Int) -> String,
    val vmRatingRemoved: String,
    val vmCacheCleared: String,
    val vmLocalPermCancelled: String,
    val vmLocalFolderPrepFailed: (String) -> String,
    val vmLocalPermRequired: (String) -> String,
    val vmLocalBackupSuccess: (String, String, Int, Int, Int) -> String,
    val vmLocalBackupToast: String,
    val vmLocalBackupError: String,
    val vmLocalListError: String,
    val vmRestoreSuccess: (Int, Int, Int, Int) -> String,
    val vmRestoreToast: String,
    val vmRestoreError: String,
    val vmDriveAuthRequired: String,
    val vmDriveBackupSuccess: (String, String, Int, Int) -> String,
    val vmDriveBackupToast: String,
    val vmDriveBackupError: String,
    val vmDriveListError: String,
    val vmDriveRestoreSuccess: (Int, Int, Int) -> String,
    val vmDriveRestoreToast: String,
    val vmDriveRestoreError: String,

    // Notifications & Widgets
    val notifChannelName: String,
    val notifChannelDesc: String,
    val notifTitleAiringNow: (String) -> String,
    val notifTitleUpcoming: (String) -> String,
    val notifBodyAiringNow: (Int, String, String) -> String,
    val notifBodyUpcoming: (Int, String, Int) -> String,
    val widgetCountLabel: String,
    val widgetListHeader: String,
    val widgetListEmpty: String
)

val LocalAppStrings = staticCompositionLocalOf { StringsEs }
val LocalAppLanguage = staticCompositionLocalOf { SynopsisLanguage.SPANISH }

object AppLocalization {

    private const val PREFS_NAME = "anivexa_prefs"
    private const val KEY_LANG = "synopsis_lang"

    fun stringsFor(language: SynopsisLanguage): AppStrings = when (language) {
        SynopsisLanguage.SPANISH -> StringsEs
        SynopsisLanguage.ENGLISH -> StringsEn
        SynopsisLanguage.FRENCH -> StringsFr
        SynopsisLanguage.JAPANESE -> StringsJa
    }

    fun languageFromContext(context: Context): SynopsisLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SynopsisLanguage.fromCode(prefs.getString(KEY_LANG, "es"))
    }

    fun stringsFor(context: Context): AppStrings = stringsFor(languageFromContext(context))

    fun navTitle(screen: MainNavScreen, s: AppStrings): String = when (screen) {
        MainNavScreen.HOME -> s.navHome
        MainNavScreen.CALENDAR -> s.navCalendar
        MainNavScreen.MY_LIST -> s.navMyList
        MainNavScreen.SEARCH -> s.navSearch
        MainNavScreen.SETTINGS -> s.navSettings
    }

    fun seeAllTitle(mode: SeeAllMode, s: AppStrings): String = when (mode) {
        SeeAllMode.NONE -> ""
        SeeAllMode.THIS_SEASON -> s.homeThisSeason
        SeeAllMode.POPULAR_RANKING -> s.homePopularRanking
    }

    fun calendarModeLabel(mode: CalendarViewMode, s: AppStrings): String = when (mode) {
        CalendarViewMode.WEEK -> s.calendarModeWeek
        CalendarViewMode.MONTH -> s.calendarModeMonth
        CalendarViewMode.LIST -> s.calendarModeList
    }

    fun myListTabLabel(tab: MyListFilterTab, s: AppStrings): String = when (tab) {
        MyListFilterTab.ALL -> s.myListTabAll
        MyListFilterTab.WATCHED -> s.myListTabWatched
        MyListFilterTab.UNWATCHED -> s.myListTabUnwatched
        MyListFilterTab.WATCHING -> s.myListTabWatching
        MyListFilterTab.PLAN_TO_WATCH -> s.myListTabPlanToWatch
    }

    fun seasonLabel(season: MediaSeason, s: AppStrings): String = when (season) {
        MediaSeason.WINTER -> s.seasonWinter
        MediaSeason.SPRING -> s.seasonSpring
        MediaSeason.SUMMER -> s.seasonSummer
        MediaSeason.FALL -> s.seasonFall
    }

    fun statusFilterLabel(status: MediaStatusFilter, s: AppStrings): String = when (status) {
        MediaStatusFilter.ALL -> s.statusAll
        MediaStatusFilter.RELEASING -> s.statusReleasing
        MediaStatusFilter.FINISHED -> s.statusFinished
        MediaStatusFilter.NOT_YET_RELEASED -> s.statusNotYetReleased
    }

    fun formatFilterLabel(format: MediaFormatFilter, s: AppStrings): String = when (format) {
        MediaFormatFilter.ALL -> s.formatAll
        MediaFormatFilter.TV -> s.formatTv
        MediaFormatFilter.ONA -> s.formatOna
        MediaFormatFilter.OVA -> s.formatOva
        MediaFormatFilter.MOVIE -> s.formatMovie
        MediaFormatFilter.SPECIAL -> s.formatSpecial
    }

    fun formatLabel(rawFormat: String, s: AppStrings): String = when (rawFormat.trim().uppercase()) {
        "TV" -> s.formatTv
        "ONA" -> s.formatOna
        "OVA" -> s.formatOva
        "MOVIE", "PELÍCULA" -> s.formatMovie
        "SPECIAL", "ESPECIAL" -> s.formatSpecial
        else -> rawFormat
    }

    fun watchlistSortLabel(order: WatchlistSortOrder, s: AppStrings): String = when (order) {
        WatchlistSortOrder.RECENT -> s.sortRecent
        WatchlistSortOrder.TITLE_AZ -> s.sortTitleAz
        WatchlistSortOrder.SCORE -> s.sortScore
        WatchlistSortOrder.PROGRESS -> s.sortProgress
    }

    fun watchlistStatusDisplay(status: WatchlistStatus, s: AppStrings): String = when (status) {
        WatchlistStatus.PLAN_TO_WATCH -> s.statusPlanToWatch
        WatchlistStatus.WATCHING -> s.statusWatching
        WatchlistStatus.COMPLETED -> s.statusCompleted
        WatchlistStatus.DROPPED -> s.statusDropped
    }

    fun themeModeLabel(mode: AppThemeMode, s: AppStrings): String = when (mode) {
        AppThemeMode.ANIVEXA_DARK -> s.themeDarkLabel
        AppThemeMode.OLED -> s.themeOledLabel
        AppThemeMode.LIGHT -> s.themeLightLabel
        AppThemeMode.LIQUID_GLASS -> s.themeGlassLabel
        AppThemeMode.SYSTEM -> s.themeSystemLabel
    }

    fun themeModeDescription(mode: AppThemeMode, s: AppStrings): String = when (mode) {
        AppThemeMode.ANIVEXA_DARK -> s.themeDarkDesc
        AppThemeMode.OLED -> s.themeOledDesc
        AppThemeMode.LIGHT -> s.themeLightDesc
        AppThemeMode.LIQUID_GLASS -> s.themeGlassDesc
        AppThemeMode.SYSTEM -> s.themeSystemDesc
    }

    fun playIntegrityLabel(status: PlayIntegrityStatus, s: AppStrings): String = when (status) {
        PlayIntegrityStatus.CHECKING -> s.integrityCheckingLabel
        PlayIntegrityStatus.VERIFIED -> s.integrityVerifiedLabel
        PlayIntegrityStatus.AVAILABLE -> s.integrityAvailableLabel
        PlayIntegrityStatus.UNAVAILABLE -> s.integrityUnavailableLabel
        PlayIntegrityStatus.ERROR -> s.integrityErrorLabel
    }

    fun playIntegrityDetail(status: PlayIntegrityStatus, s: AppStrings): String = when (status) {
        PlayIntegrityStatus.CHECKING -> s.integrityCheckingDetail
        PlayIntegrityStatus.VERIFIED -> s.integrityVerifiedDetail
        PlayIntegrityStatus.AVAILABLE -> s.integrityAvailableDetail
        PlayIntegrityStatus.UNAVAILABLE -> s.integrityUnavailableDetail
        PlayIntegrityStatus.ERROR -> s.integrityErrorDetail
    }

    fun animeStatusLabel(status: String, s: AppStrings): String = when (status.uppercase()) {
        "RELEASING" -> s.statusReleasing
        "FINISHED" -> s.statusFinished
        "NOT_YET_RELEASED" -> s.statusNotYetReleased
        "CANCELLED" -> s.statusCancelled
        "HIATUS" -> s.statusHiatus
        else -> s.statusReleasing
    }

    fun relationLabel(relationType: String, format: String, s: AppStrings): String {
        val upperType = relationType.uppercase()
        val upperFormat = format.uppercase()
        return when (upperType) {
            "PREQUEL" -> s.relationPrequel
            "SEQUEL" -> s.relationSequel
            "PARENT" -> s.relationParent
            "SIDE_STORY" -> when (upperFormat) {
                "OVA" -> s.formatOva
                "MOVIE" -> s.formatMovie
                "SPECIAL" -> s.formatSpecial
                else -> s.relationSideStory
            }
            "SPIN_OFF" -> s.relationSpinOff
            "ALTERNATIVE" -> s.relationAlternative
            "SUMMARY" -> s.relationSummary
            "CHARACTER" -> s.relationCharacter
            else -> when (upperFormat) {
                "OVA" -> s.formatOva
                "ONA" -> "ONA"
                "MOVIE" -> s.formatMovie
                "SPECIAL" -> s.formatSpecial
                else -> s.relationOther
            }
        }
    }

    fun genreLabel(rawGenre: String, s: AppStrings): String {
        return when (rawGenre.trim().lowercase()) {
            "acción", "action" -> s.genreAction
            "aventura", "adventure" -> s.genreAdventure
            "comedia", "comedy" -> s.genreComedy
            "drama" -> s.genreDrama
            "fantasía", "fantasy" -> s.genreFantasy
            "misterio", "mystery" -> s.genreMystery
            "psicológico", "psychological" -> s.genrePsychological
            "romance" -> s.genreRomance
            "sci-fi", "ciencia ficción" -> s.genreSciFi
            "slice of life", "vida cotidiana" -> s.genreSliceOfLife
            "sobrenatural", "supernatural" -> s.genreSupernatural
            "deportes", "sports" -> s.genreSports
            "thriller", "suspense", "suspenso" -> s.genreThriller
            "terror", "horror" -> s.genreHorror
            "mecha" -> s.genreMecha
            "música", "music" -> s.genreMusic
            else -> rawGenre.trim().replaceFirstChar { it.uppercase() }
        }
    }

    enum class CountdownKind { AIRED, AIRING_NOW, UPCOMING }

    data class LocalizedCountdown(val kind: CountdownKind, val text: String)

    fun countdown(
        anime: AnimeRelease,
        s: AppStrings,
        currentTimeSeconds: Long = System.currentTimeMillis() / 1000
    ): LocalizedCountdown {
        val ts = anime.effectiveAiringSeconds()
        if (ts <= 0L) return LocalizedCountdown(CountdownKind.AIRED, s.countdownNoSchedule)
        val diff = ts - currentTimeSeconds
        return when {
            diff < -7200 -> LocalizedCountdown(CountdownKind.AIRED, s.countdownAired)
            diff <= 0 -> LocalizedCountdown(CountdownKind.AIRING_NOW, s.countdownAiringNow)
            diff < 3600 -> {
                val mins = (diff / 60).coerceAtLeast(1)
                LocalizedCountdown(CountdownKind.UPCOMING, s.countdownInMinutes(mins))
            }
            diff < 86400 -> {
                val hours = diff / 3600
                val mins = (diff % 3600) / 60
                LocalizedCountdown(CountdownKind.UPCOMING, s.countdownInHoursMins(hours, mins))
            }
            else -> {
                val days = diff / 86400
                val hours = (diff % 86400) / 3600
                LocalizedCountdown(CountdownKind.UPCOMING, s.countdownInDaysHours(days, hours))
            }
        }
    }

    fun broadcastPeriod(anime: AnimeRelease, s: AppStrings): String {
        val start = anime.startDateText?.takeIf { it.isNotBlank() } ?: return s.periodTba
        val end = anime.endDateText?.takeIf { it.isNotBlank() } ?: "?"
        return "$start - $end"
    }
}
