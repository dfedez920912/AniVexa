package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupEntry
import com.example.backup.BackupManager
import com.example.backup.DriveAuthResult
import com.example.backup.DriveResolutionOutcome
import com.example.backup.GoogleDriveBackupClient
import com.example.backup.LocalBackupStorage
import com.example.data.AnimeRepository
import com.example.data.local.WatchlistEntity
import com.example.data.local.WatchlistStatus
import com.example.i18n.AppLocalization
import com.example.i18n.AppStrings
import com.example.model.AnimeCharacter
import com.example.model.AnimeRelease
import com.example.model.AnimeReview
import com.example.model.AppThemeMode
import com.example.model.DayScheduleTab
import com.example.model.GenreHelper
import com.example.model.MediaFormatFilter
import com.example.model.MediaSeason
import com.example.model.MediaStatusFilter
import com.example.model.RelatedAnimeItem
import com.example.model.SynopsisLanguage
import com.example.model.WatchlistSortOrder
import com.example.notifications.AnimeEpisodeReminderWorker
import com.example.notifications.AnimeNotificationHelper
import com.example.security.DeviceSecurityManager
import com.example.security.DeviceSecurityReport
import com.example.widget.WidgetDataHelper
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap

enum class MainNavScreen(val title: String) {
    HOME("Inicio"),
    CALENDAR("Calendario"),
    MY_LIST("Mi Lista"),
    SEARCH("Buscar"),
    SETTINGS("Ajustes")
}

enum class CalendarViewMode(val label: String) {
    WEEK("Semana"),
    MONTH("Mes"),
    LIST("Lista")
}

enum class SeeAllMode(val title: String) {
    NONE(""),
    THIS_SEASON("Esta Temporada"),
    POPULAR_RANKING("Ranking Popular")
}

enum class MyListFilterTab(val label: String) {
    ALL("Todos"),
    WATCHED("Vistos"),
    UNWATCHED("Sin ver"),
    WATCHING("En proceso"),
    PLAN_TO_WATCH("Por ver")
}

data class AnimeUiState(
    val currentNavScreen: MainNavScreen = MainNavScreen.HOME,
    val seeAllMode: SeeAllMode = SeeAllMode.NONE,
    val isOfflineMode: Boolean = false,
    val isLoadingHome: Boolean = true,
    val isLoadingSchedule: Boolean = true,
    val isLoadingSearch: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val notificationMessage: String? = null,

    // Home Data (always locked to current real season/year & today's real releases)
    val currentSeason: MediaSeason = MediaSeason.fromDate(LocalDate.now()),
    val currentSeasonYear: Int = LocalDate.now().year,
    val seasonalAnime: List<AnimeRelease> = emptyList(),
    val popularAnime: List<AnimeRelease> = emptyList(),
    val todayReleases: List<AnimeRelease> = emptyList(),

    // SeeAll Catalog Browser (isolated from Home's current season)
    val seeAllSeason: MediaSeason = MediaSeason.fromDate(LocalDate.now()),
    val seeAllYear: Int = LocalDate.now().year,
    val seeAllSeasonAnime: List<AnimeRelease> = emptyList(),
    val isLoadingSeeAll: Boolean = false,

    // Calendar & Schedule Data
    val calendarViewMode: CalendarViewMode = CalendarViewMode.WEEK,
    val weekStartDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val dayTabs: List<DayScheduleTab> = emptyList(),
    val scheduleReleases: List<AnimeRelease> = emptyList(),
    val filteredDayReleases: List<AnimeRelease> = emptyList(),
    val monthReleases: List<AnimeRelease> = emptyList(),
    val calendarFormatFilter: MediaFormatFilter = MediaFormatFilter.ALL,
    val calendarGenreFilter: String? = null,
    val calendarStudioFilter: String? = null,

    // Search & Discover Data
    val searchQuery: String = "",
    val showSearchFilters: Boolean = false,
    val searchSelectedGenres: Set<String> = emptySet(),
    val searchSeason: MediaSeason? = null,
    val searchYear: Int? = null,
    val searchStatus: MediaStatusFilter = MediaStatusFilter.ALL,
    val searchFormat: MediaFormatFilter = MediaFormatFilter.ALL,
    val searchStudio: String? = null,
    val searchResults: List<AnimeRelease> = emptyList(),

    // My List (Watchlist + Watched tracking)
    val myListFilterTab: MyListFilterTab = MyListFilterTab.ALL,
    val myListSortOrder: WatchlistSortOrder = WatchlistSortOrder.RECENT,
    val watchlistItems: List<WatchlistEntity> = emptyList(),
    val filteredWatchlistItems: List<WatchlistEntity> = emptyList(),
    val watchlistIds: Set<Int> = emptySet(),
    val favoriteIds: Set<Int> = emptySet(),
    val watchedEpisodeKeys: Set<String> = emptySet(),
    val userRatings: Map<Int, Int> = emptyMap(),

    // Detail View
    val selectedAnimeForDetail: AnimeRelease? = null,
    val detailReviews: List<AnimeReview> = emptyList(),
    val detailCharacters: List<AnimeCharacter> = emptyList(),
    val detailRelations: List<RelatedAnimeItem> = emptyList(),

    // Settings & Preferences
    val appThemeMode: AppThemeMode = AppThemeMode.ANIVEXA_DARK,
    val isJapanTimeZone: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15,
    val quietHoursEnabled: Boolean = false,
    val globalLanguage: SynopsisLanguage = SynopsisLanguage.SPANISH,
    val cachedCount: Int = 0,
    val deviceSecurityReport: DeviceSecurityReport = DeviceSecurityReport(),

    // Encrypted Backup & Restore (.avb) & Onboarding
    val hasCompletedOnboarding: Boolean = false,
    val isBackupBusy: Boolean = false,
    val backupStatusMessage: String? = null,
    val backupErrorMessage: String? = null,
    val availableBackups: List<BackupEntry> = emptyList(),
    val hasLocalBackupFolder: Boolean = false
) {
    val activeZoneId: ZoneId
        get() = if (isJapanTimeZone) ZoneId.of("Asia/Tokyo") else ZoneId.systemDefault()
}

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnimeRepository(application.applicationContext)
    private val prefs = application.getSharedPreferences("anivexa_prefs", Context.MODE_PRIVATE)
    private val backupManager = BackupManager(application.applicationContext, repository, prefs)
    private val driveBackupClient = GoogleDriveBackupClient()
    private var transientDriveToken: String? = null

    private val _uiState = MutableStateFlow(
        AnimeUiState(
            appThemeMode = loadSavedTheme(),
            isJapanTimeZone = prefs.getBoolean("jst_zone", false),
            notificationsEnabled = prefs.getBoolean("notif_enabled", true),
            defaultReminderMinutes = prefs.getInt("default_reminder_min", 15),
            quietHoursEnabled = prefs.getBoolean("quiet_hours", false),
            globalLanguage = SynopsisLanguage.fromCode(prefs.getString("synopsis_lang", "es")),
            hasCompletedOnboarding = prefs.getBoolean("onboarding_completed", false),
            hasLocalBackupFolder = LocalBackupStorage.getSavedTreeUri(application.applicationContext) != null
        )
    )
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var seeAllSeasonJob: Job? = null
    private var cardTranslationJob: Job? = null
    private val cardTranslationCache = ConcurrentHashMap<String, String>()
    private val translationSemaphore = Semaphore(4)
    private val detailBackStack = mutableListOf<AnimeRelease>()

    init {
        AnimeNotificationHelper.createNotificationChannel(application.applicationContext)
        observeRoomStreams()
        loadInitialData()
        evaluateDeviceSecurity(showRootWarning = true)
    }

    private fun loadSavedTheme(): AppThemeMode {
        val raw = prefs.getString("theme_mode", AppThemeMode.ANIVEXA_DARK.name)
        return try {
            AppThemeMode.valueOf(raw ?: AppThemeMode.ANIVEXA_DARK.name)
        } catch (_: Exception) {
            AppThemeMode.ANIVEXA_DARK
        }
    }

    private fun observeRoomStreams() {
        viewModelScope.launch {
            repository.favoriteIdsFlow.collect { favIds ->
                _uiState.update { state ->
                    val updated = state.copy(favoriteIds = favIds)
                    recomputeDerivedLists(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.watchedEpisodeKeysFlow.collect { keys ->
                _uiState.update { state ->
                    val updated = state.copy(watchedEpisodeKeys = keys)
                    recomputeDerivedLists(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.allWatchlistFlow.collect { items ->
                _uiState.update { state ->
                    val updated = state.copy(watchlistItems = items)
                    recomputeDerivedLists(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.watchlistIdsFlow.collect { ids ->
                _uiState.update { state ->
                    val updated = state.copy(watchlistIds = ids)
                    recomputeDerivedLists(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.userRatingsFlow.collect { ratings ->
                _uiState.update { state ->
                    val updated = state.copy(userRatings = ratings)
                    recomputeDerivedLists(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.cachedCountFlow.collect { count ->
                _uiState.update { it.copy(cachedCount = count) }
            }
        }
    }

    fun loadInitialData() {
        loadHomeData()
        loadWeekSchedule(_uiState.value.weekStartDate)
        executeSearch()
    }

    private fun strings(): AppStrings = AppLocalization.stringsFor(_uiState.value.globalLanguage)

    fun refreshDeviceSecurity() {
        evaluateDeviceSecurity(showRootWarning = false, notifyResult = true)
    }

    private fun evaluateDeviceSecurity(
        showRootWarning: Boolean = false,
        notifyResult: Boolean = false
    ) {
        viewModelScope.launch {
            val report = DeviceSecurityManager.evaluateDeviceSecurity(getApplication())
            _uiState.update { state ->
                val s = AppLocalization.stringsFor(state.globalLanguage)
                val integrityLabel = AppLocalization.playIntegrityLabel(report.playIntegrityStatus, s)
                val warningMsg = when {
                    notifyResult && report.isRooted -> s.vmSecurityWarningNotifyRoot
                    notifyResult -> s.vmSecurityCleanNotify(integrityLabel)
                    showRootWarning && report.isRooted -> s.vmSecurityWarningStartupRoot
                    else -> state.notificationMessage
                }
                state.copy(
                    deviceSecurityReport = report,
                    notificationMessage = warningMsg
                )
            }
        }
    }

    fun refreshAll() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            loadHomeDataInternal()
            val weekStart = _uiState.value.weekStartDate
            loadWeekScheduleInternal(weekStart)
            val today = LocalDate.now(_uiState.value.activeZoneId)
            val weekEnd = weekStart.plusDays(6)
            if (today.isBefore(weekStart) || today.isAfter(weekEnd)) {
                loadTodayRealScheduleInternal()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHome = true) }
            loadHomeDataInternal()
            _uiState.update { it.copy(isLoadingHome = false) }
        }
    }

    private suspend fun loadHomeDataInternal() {
        val state = _uiState.value
        val zone = state.activeZoneId
        val realToday = LocalDate.now(zone)
        val realSeason = MediaSeason.fromDate(realToday)
        val realYear = realToday.year

        val (seasonal, seasonOffline) = repository.getSeasonalAnime(
            season = realSeason,
            year = realYear,
            page = 1,
            perPage = 30,
            cacheForHome = true
        )
        val (popular, popOffline) = repository.getPopularRanking(page = 1, perPage = 30)

        _uiState.update { s ->
            val updated = s.copy(
                currentSeason = realSeason,
                currentSeasonYear = realYear,
                seasonalAnime = seasonal,
                popularAnime = popular,
                isOfflineMode = seasonOffline && popOffline
            )
            recomputeDerivedLists(updated)
        }
        scheduleCardTranslations(_uiState.value.globalLanguage)
    }

    fun selectSeeAllSeason(season: MediaSeason, year: Int) {
        seeAllSeasonJob?.cancel()
        _uiState.update {
            it.copy(
                seeAllSeason = season,
                seeAllYear = year,
                isLoadingSeeAll = true
            )
        }
        seeAllSeasonJob = viewModelScope.launch {
            val (seasonal, _) = repository.getSeasonalAnime(
                season = season,
                year = year,
                page = 1,
                perPage = 35,
                cacheForHome = false
            )
            _uiState.update { s ->
                val updated = s.copy(
                    seeAllSeasonAnime = seasonal,
                    isLoadingSeeAll = false
                )
                recomputeDerivedLists(updated)
            }
            scheduleCardTranslations(_uiState.value.globalLanguage)
        }
    }

    fun loadWeekSchedule(weekStart: LocalDate = _uiState.value.weekStartDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSchedule = true, weekStartDate = weekStart) }
            loadWeekScheduleInternal(weekStart)
            _uiState.update { it.copy(isLoadingSchedule = false) }
        }
    }

    private suspend fun loadWeekScheduleInternal(weekStart: LocalDate) {
        val zone = _uiState.value.activeZoneId
        val todayInZone = LocalDate.now(zone)
        val weekEnd = weekStart.plusDays(6)
        val includesToday = !todayInZone.isBefore(weekStart) && !todayInZone.isAfter(weekEnd)

        val result = repository.getSchedule(startDate = weekStart, daysCount = 7, zoneId = zone)
        result.onSuccess { releases ->
            _uiState.update { s ->
                val nextToday = if (includesToday) {
                    releases.filter {
                        it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zone) == todayInZone
                    }.sortedBy { it.effectiveAiringSeconds() }
                } else {
                    s.todayReleases
                }
                val updated = s.copy(
                    scheduleReleases = releases,
                    todayReleases = nextToday,
                    errorMessage = null
                )
                recomputeDerivedLists(updated)
            }
            scheduleRemindersForFavorites(releases, _uiState.value.favoriteIds)
            scheduleCardTranslations(_uiState.value.globalLanguage)
        }.onFailure { err ->
            _uiState.update { it.copy(errorMessage = err.message) }
        }
    }

    private fun loadTodayRealSchedule() {
        viewModelScope.launch {
            loadTodayRealScheduleInternal()
        }
    }

    private suspend fun loadTodayRealScheduleInternal() {
        val zone = _uiState.value.activeZoneId
        val realToday = LocalDate.now(zone)
        val result = repository.getSchedule(startDate = realToday, daysCount = 1, zoneId = zone)
        result.onSuccess { releases ->
            val todayOnly = releases.filter {
                it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zone) == realToday
            }.sortedBy { it.effectiveAiringSeconds() }
            _uiState.update { s ->
                recomputeDerivedLists(s.copy(todayReleases = todayOnly))
            }
            scheduleCardTranslations(_uiState.value.globalLanguage)
        }
    }

    fun shiftWeek(deltaWeeks: Long) {
        val newStart = _uiState.value.weekStartDate.plusWeeks(deltaWeeks)
        val newSelected = if (deltaWeeks == 0L) LocalDate.now(_uiState.value.activeZoneId) else newStart
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(weekStartDate = newStart, selectedDate = newSelected))
        }
        loadWeekSchedule(newStart)
    }

    fun jumpToToday() {
        val today = LocalDate.now(_uiState.value.activeZoneId)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        _uiState.update { state ->
            recomputeDerivedLists(
                state.copy(
                    weekStartDate = weekStart,
                    selectedDate = today,
                    selectedMonth = YearMonth.from(today)
                )
            )
        }
        loadWeekSchedule(weekStart)
    }

    fun selectCalendarDate(date: LocalDate) {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekChanged = weekStart != _uiState.value.weekStartDate
        _uiState.update { state ->
            val updated = state.copy(selectedDate = date, weekStartDate = weekStart)
            recomputeDerivedLists(updated)
        }
        if (weekChanged) {
            loadWeekSchedule(weekStart)
        }
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        _uiState.update { it.copy(calendarViewMode = mode) }
        if (mode == CalendarViewMode.MONTH && _uiState.value.monthReleases.isEmpty()) {
            loadMonthSchedule(_uiState.value.selectedMonth)
        }
    }

    fun shiftMonth(deltaMonths: Long) {
        val newMonth = _uiState.value.selectedMonth.plusMonths(deltaMonths)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        loadMonthSchedule(newMonth)
    }

    private fun loadMonthSchedule(month: YearMonth) {
        viewModelScope.launch {
            val start = month.atDay(1)
            val days = month.lengthOfMonth()
            val result = repository.getSchedule(startDate = start, daysCount = days, zoneId = _uiState.value.activeZoneId)
            result.onSuccess { list ->
                _uiState.update { state ->
                    val updated = state.copy(monthReleases = list)
                    recomputeDerivedLists(updated)
                }
                scheduleCardTranslations(_uiState.value.globalLanguage)
            }
        }
    }

    fun setCalendarFormatFilter(format: MediaFormatFilter) {
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(calendarFormatFilter = format))
        }
    }

    fun setCalendarGenreFilter(genre: String?) {
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(calendarGenreFilter = genre))
        }
    }

    fun setCalendarStudioFilter(studio: String?) {
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(calendarStudioFilter = studio))
        }
    }

    fun setNavScreen(screen: MainNavScreen) {
        seeAllSeasonJob?.cancel()
        _uiState.update { it.copy(currentNavScreen = screen, seeAllMode = SeeAllMode.NONE, isLoadingSeeAll = false) }
    }

    fun openSeeAll(mode: SeeAllMode) {
        seeAllSeasonJob?.cancel()
        _uiState.update { state ->
            val realToday = LocalDate.now(state.activeZoneId)
            val realSeason = MediaSeason.fromDate(realToday)
            val realYear = realToday.year
            if (mode == SeeAllMode.THIS_SEASON) {
                state.copy(
                    seeAllMode = mode,
                    seeAllSeason = realSeason,
                    seeAllYear = realYear,
                    seeAllSeasonAnime = state.seasonalAnime,
                    isLoadingSeeAll = false
                )
            } else {
                state.copy(seeAllMode = mode, isLoadingSeeAll = false)
            }
        }
    }

    fun closeSeeAll() {
        seeAllSeasonJob?.cancel()
        _uiState.update { it.copy(seeAllMode = SeeAllMode.NONE, isLoadingSeeAll = false) }
    }

    // Search & Discover
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        triggerDebouncedSearch()
    }

    fun toggleShowSearchFilters() {
        _uiState.update { it.copy(showSearchFilters = !it.showSearchFilters) }
    }

    fun toggleSearchGenre(genre: String) {
        _uiState.update { state ->
            val current = state.searchSelectedGenres.toMutableSet()
            if (current.contains(genre)) current.remove(genre) else current.add(genre)
            state.copy(searchSelectedGenres = current)
        }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun setSearchSeason(season: MediaSeason?) {
        _uiState.update { state ->
            val year = if (season != null && state.searchYear == null) LocalDate.now().year else state.searchYear
            state.copy(searchSeason = season, searchYear = year)
        }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun setSearchYear(year: Int?) {
        _uiState.update { it.copy(searchYear = year) }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun setSearchStatus(status: MediaStatusFilter) {
        _uiState.update { it.copy(searchStatus = status) }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun setSearchFormat(format: MediaFormatFilter) {
        _uiState.update { it.copy(searchFormat = format) }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun setSearchStudio(studio: String?) {
        _uiState.update { it.copy(searchStudio = studio) }
        triggerDebouncedSearch(delayMs = 150)
    }

    fun clearSearchFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchSelectedGenres = emptySet(),
                searchSeason = null,
                searchYear = null,
                searchStatus = MediaStatusFilter.ALL,
                searchFormat = MediaFormatFilter.ALL,
                searchStudio = null
            )
        }
        triggerDebouncedSearch(delayMs = 50)
    }

    private fun triggerDebouncedSearch(delayMs: Long = 380) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(delayMs)
            executeSearch()
        }
    }

    fun executeSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSearch = true) }
            val s = _uiState.value
            val (results, offline) = repository.searchAnime(
                query = s.searchQuery,
                selectedGenres = s.searchSelectedGenres,
                season = s.searchSeason,
                year = s.searchYear,
                statusApi = s.searchStatus.apiValue,
                formatApi = s.searchFormat.apiValue,
                studioFilter = s.searchStudio
            )
            _uiState.update { state ->
                val updated = state.copy(
                    searchResults = results,
                    isLoadingSearch = false,
                    isOfflineMode = offline
                )
                recomputeDerivedLists(updated)
            }
            scheduleCardTranslations(_uiState.value.globalLanguage)
        }
    }

    // Detail Screen
    fun openDetail(anime: AnimeRelease) {
        detailBackStack.clear()
        detailBackStack.add(anime)
        loadDetailFor(anime)
    }

    fun openRelatedDetail(related: RelatedAnimeItem) {
        val current = _uiState.value.selectedAnimeForDetail
        if (current != null && current.id == related.id) return
        val anime = related.toAnimeRelease()
        if (detailBackStack.lastOrNull()?.id != anime.id) {
            detailBackStack.add(anime)
        }
        loadDetailFor(anime)
    }

    private fun needsTranslation(anime: AnimeRelease, lang: SynopsisLanguage): Boolean {
        val source = anime.synopsisEn.ifBlank { anime.synopsis.ifBlank { anime.synopsisEs } }
        if (source.isBlank()) return false
        return when (lang) {
            SynopsisLanguage.ENGLISH -> anime.synopsisEn.isBlank()
            SynopsisLanguage.SPANISH -> anime.synopsisEs.isBlank()
            SynopsisLanguage.FRENCH -> anime.synopsisFr.isBlank()
            SynopsisLanguage.JAPANESE -> anime.synopsisJa.isBlank()
        }
    }

    private fun translationSourceText(anime: AnimeRelease): String {
        return anime.synopsisEn.ifBlank { anime.synopsis.ifBlank { anime.synopsisEs } }
    }

    private fun loadDetailFor(anime: AnimeRelease) {
        detailJob?.cancel()
        val preferredLang = _uiState.value.globalLanguage
        val decorated = decorateItem(anime, _uiState.value).copy(activeLanguage = preferredLang)
        _uiState.update {
            it.copy(
                selectedAnimeForDetail = decorated,
                detailReviews = emptyList(),
                detailCharacters = emptyList(),
                detailRelations = emptyList(),
                isLoadingDetail = true
            )
        }
        if (needsTranslation(decorated, preferredLang)) {
            translateDetailSynopsisInBackground(decorated.id, translationSourceText(decorated), preferredLang)
        }
        detailJob = viewModelScope.launch {
            val bundle = repository.getAnimeDetailBundle(anime.id)
            if (_uiState.value.selectedAnimeForDetail?.id != anime.id) return@launch
            if (bundle != null) {
                val activeLang = _uiState.value.globalLanguage
                val mergedAnime = decorateItem(
                    bundle.anime.copy(
                        episode = if (anime.airingAtSeconds > 0L) anime.episode else bundle.anime.episode,
                        airingAtSeconds = if (anime.airingAtSeconds > 0L) anime.airingAtSeconds else bundle.anime.airingAtSeconds,
                        synopsisEs = bundle.anime.synopsisEs.ifBlank { anime.synopsisEs },
                        synopsisFr = bundle.anime.synopsisFr.ifBlank { anime.synopsisFr },
                        synopsisJa = bundle.anime.synopsisJa.ifBlank { anime.synopsisJa },
                        activeLanguage = activeLang
                    ),
                    _uiState.value
                )
                _uiState.update {
                    it.copy(
                        selectedAnimeForDetail = mergedAnime,
                        detailReviews = bundle.reviews,
                        detailCharacters = bundle.characters,
                        detailRelations = bundle.relations,
                        isLoadingDetail = false
                    )
                }
                if (needsTranslation(mergedAnime, activeLang)) {
                    translateDetailSynopsisInBackground(mergedAnime.id, translationSourceText(mergedAnime), activeLang)
                }
            } else {
                _uiState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    private fun translateDetailSynopsisInBackground(
        animeId: Int,
        sourceSynopsis: String,
        targetLang: SynopsisLanguage
    ) {
        if (sourceSynopsis.isBlank()) return
        viewModelScope.launch {
            _uiState.update { state ->
                val current = state.selectedAnimeForDetail
                if (current?.id == animeId) {
                    state.copy(selectedAnimeForDetail = current.copy(isTranslating = true))
                } else state
            }
            val translated = repository.translateSynopsis(sourceSynopsis, targetLang)
            if (translated.isNotBlank()) {
                cardTranslationCache["${animeId}_${targetLang.code}"] = translated
            }
            _uiState.update { state ->
                val latest = state.selectedAnimeForDetail
                val refreshed = recomputeDerivedLists(state)
                if (latest?.id == animeId) {
                    val updatedDetail = when (targetLang) {
                        SynopsisLanguage.SPANISH -> latest.copy(synopsisEs = translated, activeLanguage = targetLang, isTranslating = false)
                        SynopsisLanguage.FRENCH -> latest.copy(synopsisFr = translated, activeLanguage = targetLang, isTranslating = false)
                        SynopsisLanguage.JAPANESE -> latest.copy(synopsisJa = translated, activeLanguage = targetLang, isTranslating = false)
                        SynopsisLanguage.ENGLISH -> latest.copy(synopsisEn = translated, activeLanguage = targetLang, isTranslating = false)
                    }
                    refreshed.copy(selectedAnimeForDetail = updatedDetail)
                } else {
                    refreshed
                }
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        if (detailBackStack.isNotEmpty()) {
            detailBackStack.removeAt(detailBackStack.lastIndex)
        }
        val previous = detailBackStack.lastOrNull()
        if (previous != null) {
            loadDetailFor(previous)
        } else {
            _uiState.update {
                it.copy(
                    selectedAnimeForDetail = null,
                    detailReviews = emptyList(),
                    detailCharacters = emptyList(),
                    detailRelations = emptyList(),
                    isLoadingDetail = false
                )
            }
        }
    }

    fun navigateHomeFromDetail() {
        detailJob?.cancel()
        seeAllSeasonJob?.cancel()
        detailBackStack.clear()
        _uiState.update {
            it.copy(
                selectedAnimeForDetail = null,
                detailReviews = emptyList(),
                detailCharacters = emptyList(),
                detailRelations = emptyList(),
                isLoadingDetail = false,
                currentNavScreen = MainNavScreen.HOME,
                seeAllMode = SeeAllMode.NONE,
                isLoadingSeeAll = false
            )
        }
    }

    // Watchlist & Watched Actions
    fun setMyListFilterTab(tab: MyListFilterTab) {
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(myListFilterTab = tab))
        }
    }

    fun setMyListSortOrder(order: WatchlistSortOrder) {
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(myListSortOrder = order))
        }
    }

    fun toggleWatchlist(anime: AnimeRelease, status: WatchlistStatus = WatchlistStatus.PLAN_TO_WATCH) {
        viewModelScope.launch {
            val added = repository.toggleWatchlist(anime, status)
            val s = strings()
            _uiState.update {
                it.copy(
                    notificationMessage = if (added) {
                        s.vmWatchlistAdded(anime.displayTitle)
                    } else {
                        s.vmWatchlistRemoved(anime.displayTitle)
                    }
                )
            }
        }
    }

    fun updateWatchlistStatus(animeId: Int, status: WatchlistStatus) {
        viewModelScope.launch {
            repository.updateWatchlistStatus(animeId, status)
            val s = strings()
            val statusLabel = AppLocalization.watchlistStatusDisplay(status, s)
            _uiState.update { it.copy(notificationMessage = s.vmStatusUpdated(statusLabel)) }
        }
    }

    fun updateWatchlistEpisodes(animeId: Int, episodes: Int) {
        viewModelScope.launch {
            val item = _uiState.value.watchlistItems.firstOrNull { it.animeId == animeId } ?: return@launch
            val clamped = item.totalEpisodes?.let { max -> episodes.coerceIn(0, max) } ?: episodes.coerceAtLeast(0)
            if (clamped > 0) {
                repository.markEpisodeWatched(animeId, clamped)
            } else {
                repository.clearWatchedEpisodesForAnime(animeId)
            }
        }
    }

    fun toggleWatchlistItemWatched(item: WatchlistEntity) {
        viewModelScope.launch {
            val watchedCount = maxOf(
                _uiState.value.watchedEpisodeKeys.count { it.startsWith("${item.animeId}_") },
                item.watchedEpisodes
            )
            val isFullyWatched = item.statusEnum == WatchlistStatus.COMPLETED ||
                (item.totalEpisodes != null && item.totalEpisodes > 0 && watchedCount >= item.totalEpisodes)
            if (isFullyWatched) {
                repository.clearWatchedEpisodesForAnime(item.animeId)
            } else {
                repository.markAllEpisodesWatched(item.animeId, item.totalEpisodes)
            }
        }
    }

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(animeId)
            _uiState.update { it.copy(notificationMessage = strings().vmWatchlistRemoved("")) }
        }
    }

    fun toggleWatchedEpisode(anime: AnimeRelease) {
        viewModelScope.launch {
            val ep = anime.effectiveEpisodeNumber()
            val nowWatched = repository.toggleWatchedEpisode(anime.id, ep, anime = anime)
            val s = strings()
            _uiState.update {
                it.copy(
                    notificationMessage = if (nowWatched) {
                        s.vmEpisodeWatched(ep)
                    } else {
                        s.vmEpisodeUnwatched(ep)
                    }
                )
            }
        }
    }

    fun toggleWatchedEpisodeNum(animeId: Int, episodeNumber: Int, anime: AnimeRelease? = null) {
        viewModelScope.launch {
            val resolvedAnime = anime ?: _uiState.value.selectedAnimeForDetail?.takeIf { it.id == animeId }
            repository.toggleEpisodeWithPriorWatched(animeId, episodeNumber, anime = resolvedAnime)
        }
    }

    fun setAllEpisodesWatched(
        animeId: Int,
        markAll: Boolean,
        totalEpisodes: Int,
        anime: AnimeRelease? = null
    ) {
        viewModelScope.launch {
            if (markAll) {
                val resolvedAnime = anime ?: _uiState.value.selectedAnimeForDetail?.takeIf { it.id == animeId }
                repository.markAllEpisodesWatched(animeId, totalEpisodes, anime = resolvedAnime)
            } else {
                repository.clearWatchedEpisodesForAnime(animeId)
            }
        }
    }

    fun toggleFavoriteReminder(anime: AnimeRelease) {
        viewModelScope.launch {
            val defaultMins = _uiState.value.defaultReminderMinutes
            val targetAnime = anime.copy(reminderMinutesBefore = defaultMins)
            val added = repository.toggleFavorite(targetAnime)
            val s = strings()
            _uiState.update {
                it.copy(
                    notificationMessage = if (added) {
                        s.vmReminderEnabled(anime.displayTitle, defaultMins)
                    } else {
                        s.vmReminderDisabled(anime.displayTitle)
                    }
                )
            }
        }
    }

    fun updateReminder(anime: AnimeRelease, enabled: Boolean, minutesBefore: Int) {
        viewModelScope.launch {
            repository.updateReminder(anime, enabled, minutesBefore)
            val s = strings()
            _uiState.update {
                it.copy(
                    notificationMessage = if (enabled) {
                        s.vmAlertScheduled(minutesBefore, anime.displayTitle)
                    } else {
                        s.vmAlertDisabled
                    }
                )
            }
        }
    }

    fun testEpisodeNotification(anime: AnimeRelease) {
        val context = getApplication<Application>().applicationContext
        repository.sendTestNotification(anime, _uiState.value.defaultReminderMinutes)
        AnimeEpisodeReminderWorker.scheduleImmediateTest(context, anime)
        _uiState.update { it.copy(notificationMessage = strings().vmTestNotifSent(anime.displayTitle)) }
    }

    fun setUserRating(animeId: Int, rating: Int) {
        viewModelScope.launch {
            repository.setUserRating(animeId, rating)
            val s = strings()
            _uiState.update {
                it.copy(
                    notificationMessage = if (rating > 0) s.vmRatingSaved(rating) else s.vmRatingRemoved
                )
            }
        }
    }

    // Settings Actions
    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.update { it.copy(appThemeMode = mode) }
    }

    fun toggleJapanTimeZone() {
        val next = !_uiState.value.isJapanTimeZone
        prefs.edit().putBoolean("jst_zone", next).apply()
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(isJapanTimeZone = next))
        }
        WidgetDataHelper.clearTodaySnapshot(getApplication())
        WidgetDataHelper.notifyAllWidgets(getApplication())
        loadTodayRealSchedule()
        loadWeekSchedule(_uiState.value.weekStartDate)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notif_enabled", enabled).apply()
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        prefs.edit().putInt("default_reminder_min", minutes).apply()
        _uiState.update { it.copy(defaultReminderMinutes = minutes) }
    }

    fun toggleQuietHours() {
        val next = !_uiState.value.quietHoursEnabled
        prefs.edit().putBoolean("quiet_hours", next).apply()
        _uiState.update { it.copy(quietHoursEnabled = next) }
    }

    fun setGlobalLanguage(lang: SynopsisLanguage) {
        prefs.edit().putString("synopsis_lang", lang.code).apply()
        _uiState.update { state ->
            recomputeDerivedLists(state.copy(globalLanguage = lang), forceGlobalLanguageOnDetail = true)
        }
        WidgetDataHelper.notifyAllWidgets(getApplication())
        _uiState.value.selectedAnimeForDetail?.let { detail ->
            if (needsTranslation(detail, lang)) {
                translateDetailSynopsisInBackground(detail.id, translationSourceText(detail), lang)
            }
        }
        scheduleCardTranslations(lang)
    }

    private fun scheduleCardTranslations(targetLang: SynopsisLanguage) {
        cardTranslationJob?.cancel()
        val state = _uiState.value
        val prioritized = (
            state.seasonalAnime.take(15) +
                state.popularAnime.take(15) +
                state.todayReleases +
                state.filteredDayReleases +
                state.searchResults.take(20) +
                state.seeAllSeasonAnime.take(20) +
                state.monthReleases.take(20)
            ).distinctBy { it.id }

        val pending = prioritized.filter { item ->
            val decorated = decorateItem(item, state)
            needsTranslation(decorated, targetLang) &&
                !cardTranslationCache.containsKey("${decorated.id}_${targetLang.code}")
        }
        if (pending.isEmpty()) return

        cardTranslationJob = viewModelScope.launch {
            pending.chunked(8).forEach { batch ->
                if (_uiState.value.globalLanguage != targetLang) return@launch
                coroutineScope {
                    batch.map { anime ->
                        async {
                            val decorated = decorateItem(anime, _uiState.value)
                            val source = translationSourceText(decorated)
                            if (source.isBlank()) return@async
                            val translated = translationSemaphore.withPermit {
                                runCatching { repository.translateSynopsis(source, targetLang) }
                                    .getOrNull()
                            }
                            if (!translated.isNullOrBlank()) {
                                cardTranslationCache["${anime.id}_${targetLang.code}"] = translated
                            }
                        }
                    }.awaitAll()
                }
                if (_uiState.value.globalLanguage == targetLang) {
                    _uiState.update { latest -> recomputeDerivedLists(latest) }
                }
            }
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            repository.clearOfflineCache()
            _uiState.update { it.copy(notificationMessage = strings().vmCacheCleared) }
        }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _uiState.update { it.copy(hasCompletedOnboarding = true) }
    }

    // Encrypted Backup & Restore (.avb)
    fun clearBackupMessages() {
        _uiState.update {
            it.copy(
                backupStatusMessage = null,
                backupErrorMessage = null
            )
        }
    }

    fun refreshLocalBackupAccess() {
        val ctx = getApplication<Application>().applicationContext
        val hasAccess = LocalBackupStorage.getSavedTreeUri(ctx) != null
        _uiState.update { it.copy(hasLocalBackupFolder = hasAccess) }
    }

    fun onLocalBackupTreeGranted(treeUri: Uri?, onReady: () -> Unit = {}) {
        if (treeUri == null) {
            _uiState.update {
                it.copy(backupErrorMessage = strings().vmLocalPermCancelled)
            }
            return
        }
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            runCatching {
                LocalBackupStorage.persistTreeUri(ctx, treeUri)
                LocalBackupStorage.ensureBackupFolderUri(ctx, treeUri)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        hasLocalBackupFolder = true,
                        backupErrorMessage = null
                    )
                }
                onReady()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        backupErrorMessage = err.message
                            ?: strings().vmLocalFolderPrepFailed(LocalBackupStorage.BACKUP_FOLDER_NAME)
                    )
                }
            }
        }
    }

    fun createLocalBackup(onSuccess: (String) -> Unit = {}) {
        val ctx = getApplication<Application>().applicationContext
        val treeUri = LocalBackupStorage.getSavedTreeUri(ctx)
        if (treeUri == null) {
            _uiState.update {
                it.copy(
                    hasLocalBackupFolder = false,
                    backupErrorMessage = strings().vmLocalPermRequired(LocalBackupStorage.BACKUP_FOLDER_NAME)
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null, backupStatusMessage = null)
            }
            runCatching {
                val (encryptedBytes, stats) = backupManager.exportEncryptedBackup()
                val fileName = backupManager.generateBackupFileName()
                val entry = LocalBackupStorage.writeBackup(ctx, treeUri, fileName, encryptedBytes)
                entry to stats
            }.onSuccess { (entry, stats) ->
                val s = strings()
                val summary = s.vmLocalBackupSuccess(
                    LocalBackupStorage.BACKUP_FOLDER_NAME,
                    entry.fileName,
                    stats.watchlistCount,
                    stats.favoritesCount,
                    stats.watchedEpisodesCount
                )
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupStatusMessage = summary,
                        notificationMessage = s.vmLocalBackupToast
                    )
                }
                onSuccess(summary)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmLocalBackupError
                    )
                }
            }
        }
    }

    fun loadLocalBackups() {
        val ctx = getApplication<Application>().applicationContext
        val treeUri = LocalBackupStorage.getSavedTreeUri(ctx)
        if (treeUri == null) {
            _uiState.update { it.copy(hasLocalBackupFolder = false, availableBackups = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null)
            }
            runCatching {
                LocalBackupStorage.listBackups(ctx, treeUri)
            }.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        hasLocalBackupFolder = true,
                        availableBackups = list
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmLocalListError
                    )
                }
            }
        }
    }

    fun restoreLocalBackup(documentUri: Uri, onSuccess: (String) -> Unit = {}) {
        val ctx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null, backupStatusMessage = null)
            }
            runCatching {
                val bytes = LocalBackupStorage.readBytes(ctx, documentUri)
                backupManager.restoreFromEncryptedBytes(bytes)
            }.onSuccess { stats ->
                reloadPreferencesAfterRestore()
                val s = strings()
                val msg = s.vmRestoreSuccess(
                    stats.watchlistCount,
                    stats.favoritesCount,
                    stats.watchedEpisodesCount,
                    stats.ratingsCount
                )
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupStatusMessage = msg,
                        notificationMessage = s.vmRestoreToast
                    )
                }
                onSuccess(msg)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmRestoreError
                    )
                }
            }
        }
    }

    fun requestDriveAccess(
        hostContext: Context,
        onAuthorized: () -> Unit,
        onNeedsResolution: (IntentSender) -> Unit,
        onNeedsActivityResolution: (Intent) -> Unit = {}
    ) {
        val existingToken = transientDriveToken
        if (!existingToken.isNullOrBlank()) {
            onAuthorized()
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null, backupStatusMessage = null)
            }
            when (val auth = driveBackupClient.authorizeDriveAccess(hostContext)) {
                is DriveAuthResult.Authorized -> {
                    transientDriveToken = auth.accessToken
                    _uiState.update { it.copy(isBackupBusy = false) }
                    onAuthorized()
                }
                is DriveAuthResult.NeedsResolution -> {
                    _uiState.update { it.copy(isBackupBusy = false) }
                    onNeedsResolution(auth.intentSender)
                }
                is DriveAuthResult.NeedsActivityResolution -> {
                    _uiState.update { it.copy(isBackupBusy = false) }
                    onNeedsActivityResolution(auth.intent)
                }
                is DriveAuthResult.Error -> {
                    val localizedMsg = formatDriveAuthErrorMessage(auth.statusCode, auth.message)
                    _uiState.update {
                        it.copy(isBackupBusy = false, backupErrorMessage = localizedMsg)
                    }
                }
            }
        }
    }

    fun onDriveResolutionResult(
        hostContext: Context,
        resultCode: Int,
        data: Intent?,
        onAuthorized: () -> Unit,
        onNeedsResolution: (IntentSender) -> Unit = {},
        onNeedsActivityResolution: (Intent) -> Unit = {}
    ) {
        when (val outcome = driveBackupClient.extractAuthorizationOutcome(hostContext, resultCode, data)) {
            is DriveResolutionOutcome.Authorized -> {
                transientDriveToken = outcome.accessToken
                _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                onAuthorized()
            }
            is DriveResolutionOutcome.NeedsResolution -> {
                _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                onNeedsResolution(outcome.intentSender)
            }
            is DriveResolutionOutcome.NeedsActivityResolution -> {
                _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                onNeedsActivityResolution(outcome.intent)
            }
            is DriveResolutionOutcome.Cancelled -> {
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = strings().vmDriveAuthCancelled
                    )
                }
            }
            is DriveResolutionOutcome.Error -> {
                if (outcome.statusCode == CommonStatusCodes.INTERNAL_ERROR) {
                    // Play Services GIS threw INTERNAL_ERROR (code 8) after consent UI;
                    // recover automatically using GoogleSignIn + GoogleAuthUtil.
                    viewModelScope.launch {
                        _uiState.update { it.copy(isBackupBusy = true, backupErrorMessage = null) }
                        when (val fallback = driveBackupClient.authorizeViaLegacySignIn(hostContext)) {
                            is DriveAuthResult.Authorized -> {
                                transientDriveToken = fallback.accessToken
                                _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                                onAuthorized()
                            }
                            is DriveAuthResult.NeedsActivityResolution -> {
                                _uiState.update { it.copy(isBackupBusy = false) }
                                onNeedsActivityResolution(fallback.intent)
                            }
                            is DriveAuthResult.NeedsResolution -> {
                                _uiState.update { it.copy(isBackupBusy = false) }
                                onNeedsResolution(fallback.intentSender)
                            }
                            is DriveAuthResult.Error -> {
                                _uiState.update {
                                    it.copy(
                                        isBackupBusy = false,
                                        backupErrorMessage = formatDriveAuthErrorMessage(
                                            fallback.statusCode ?: CommonStatusCodes.INTERNAL_ERROR,
                                            fallback.message
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (resultCode == Activity.RESULT_OK && outcome.statusCode == null) {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isBackupBusy = true, backupErrorMessage = null) }
                        when (val retry = driveBackupClient.authorizeDriveAccess(hostContext)) {
                            is DriveAuthResult.Authorized -> {
                                transientDriveToken = retry.accessToken
                                _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                                onAuthorized()
                            }
                            is DriveAuthResult.NeedsResolution -> {
                                _uiState.update { it.copy(isBackupBusy = false) }
                                onNeedsResolution(retry.intentSender)
                            }
                            is DriveAuthResult.NeedsActivityResolution -> {
                                _uiState.update { it.copy(isBackupBusy = false) }
                                onNeedsActivityResolution(retry.intent)
                            }
                            is DriveAuthResult.Error -> {
                                _uiState.update {
                                    it.copy(
                                        isBackupBusy = false,
                                        backupErrorMessage = formatDriveAuthErrorMessage(retry.statusCode, retry.message)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            backupErrorMessage = formatDriveAuthErrorMessage(outcome.statusCode, outcome.detail)
                        )
                    }
                }
            }
        }
    }

    fun onDriveActivityResult(
        hostContext: Context,
        resultCode: Int,
        data: Intent?,
        onAuthorized: () -> Unit,
        onNeedsActivityResolution: (Intent) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, backupErrorMessage = null) }
            when (val outcome = driveBackupClient.extractLegacySignInOutcome(hostContext, resultCode, data)) {
                is DriveResolutionOutcome.Authorized -> {
                    transientDriveToken = outcome.accessToken
                    _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                    onAuthorized()
                }
                is DriveResolutionOutcome.NeedsActivityResolution -> {
                    _uiState.update { it.copy(isBackupBusy = false, backupErrorMessage = null) }
                    onNeedsActivityResolution(outcome.intent)
                }
                is DriveResolutionOutcome.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            backupErrorMessage = strings().vmDriveAuthCancelled
                        )
                    }
                }
                is DriveResolutionOutcome.NeedsResolution -> {
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            backupErrorMessage = strings().vmDriveAuthRequired
                        )
                    }
                }
                is DriveResolutionOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            backupErrorMessage = formatDriveAuthErrorMessage(outcome.statusCode, outcome.detail)
                        )
                    }
                }
            }
        }
    }

    private fun formatDriveAuthErrorMessage(statusCode: Int?, fallback: String?): String {
        val s = strings()
        return when (statusCode) {
            CommonStatusCodes.INTERNAL_ERROR -> s.vmDriveAuthCode8Error
            10 -> s.vmDriveAuthDevError
            16, 12501 -> s.vmDriveAuthCancelled
            null -> fallback?.takeIf { it.isNotBlank() } ?: s.vmDriveAuthRequired
            else -> s.vmDriveAuthCodeError(statusCode)
        }
    }

    fun createDriveBackup(onSuccess: (String) -> Unit = {}) {
        val token = transientDriveToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(backupErrorMessage = strings().vmDriveAuthRequired)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null, backupStatusMessage = null)
            }
            runCatching {
                val (encryptedBytes, stats) = backupManager.exportEncryptedBackup()
                val fileName = backupManager.generateBackupFileName()
                val entry = driveBackupClient.uploadEncryptedBackup(token, fileName, encryptedBytes)
                entry to stats
            }.onSuccess { (entry, stats) ->
                val s = strings()
                val summary = s.vmDriveBackupSuccess(
                    GoogleDriveBackupClient.DRIVE_FOLDER_NAME,
                    entry.fileName,
                    stats.watchlistCount,
                    stats.favoritesCount
                )
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupStatusMessage = summary,
                        notificationMessage = s.vmDriveBackupToast
                    )
                }
                onSuccess(summary)
            }.onFailure { err ->
                if (err.message?.contains("(401)") == true) {
                    transientDriveToken = null
                }
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmDriveBackupError
                    )
                }
            }
        }
    }

    fun loadDriveBackups() {
        val token = transientDriveToken
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(availableBackups = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null)
            }
            runCatching {
                driveBackupClient.listBackupFiles(token)
            }.onSuccess { files ->
                _uiState.update {
                    it.copy(isBackupBusy = false, availableBackups = files)
                }
            }.onFailure { err ->
                if (err.message?.contains("(401)") == true) {
                    transientDriveToken = null
                }
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmDriveListError
                    )
                }
            }
        }
    }

    fun restoreDriveBackup(fileId: String, onSuccess: (String) -> Unit = {}) {
        val token = transientDriveToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(backupErrorMessage = strings().vmDriveAuthRequired)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBackupBusy = true, backupErrorMessage = null, backupStatusMessage = null)
            }
            runCatching {
                val bytes = driveBackupClient.downloadBackupBytes(token, fileId)
                backupManager.restoreFromEncryptedBytes(bytes)
            }.onSuccess { stats ->
                reloadPreferencesAfterRestore()
                val s = strings()
                val msg = s.vmDriveRestoreSuccess(
                    stats.watchlistCount,
                    stats.favoritesCount,
                    stats.watchedEpisodesCount
                )
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupStatusMessage = msg,
                        notificationMessage = s.vmDriveRestoreToast
                    )
                }
                onSuccess(msg)
            }.onFailure { err ->
                if (err.message?.contains("(401)") == true) {
                    transientDriveToken = null
                }
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupErrorMessage = err.message ?: strings().vmDriveRestoreError
                    )
                }
            }
        }
    }

    private fun reloadPreferencesAfterRestore() {
        val restoredLang = SynopsisLanguage.fromCode(prefs.getString("synopsis_lang", "es"))
        _uiState.update { state ->
            recomputeDerivedLists(
                state.copy(
                    appThemeMode = loadSavedTheme(),
                    isJapanTimeZone = prefs.getBoolean("jst_zone", false),
                    notificationsEnabled = prefs.getBoolean("notif_enabled", true),
                    defaultReminderMinutes = prefs.getInt("default_reminder_min", 15),
                    quietHoursEnabled = prefs.getBoolean("quiet_hours", false),
                    globalLanguage = restoredLang
                ),
                forceGlobalLanguageOnDetail = true
            )
        }
        scheduleCardTranslations(restoredLang)
        WidgetDataHelper.notifyAllWidgets(getApplication())
        loadTodayRealSchedule()
        loadWeekSchedule(_uiState.value.weekStartDate)
    }

    private fun scheduleRemindersForFavorites(releases: List<AnimeRelease>, favIds: Set<Int>) {
        if (!_uiState.value.notificationsEnabled || favIds.isEmpty() || releases.isEmpty()) return
        val context = getApplication<Application>().applicationContext
        val nowMillis = System.currentTimeMillis()
        releases.filter { favIds.contains(it.id) }.forEach { anime ->
            if (anime.effectiveAiringSeconds() * 1000L > nowMillis - (15 * 60 * 1000L)) {
                AnimeNotificationHelper.scheduleAnimeReminder(context, anime, _uiState.value.defaultReminderMinutes)
            }
        }
    }

    private fun decorateItem(
        item: AnimeRelease,
        state: AnimeUiState,
        preserveActiveLanguage: Boolean = false
    ): AnimeRelease {
        val ep = item.effectiveEpisodeNumber()
        val isFav = state.favoriteIds.contains(item.id)
        val isWatched = state.watchedEpisodeKeys.contains("${item.id}_$ep")
        val isInWatchlist = state.watchlistIds.contains(item.id)
        val rating = state.userRatings[item.id] ?: item.userRating
        val cachedEs = cardTranslationCache["${item.id}_es"]
        val cachedEn = cardTranslationCache["${item.id}_en"]
        val cachedFr = cardTranslationCache["${item.id}_fr"]
        val cachedJa = cardTranslationCache["${item.id}_ja"]
        return item.copy(
            synopsisEs = item.synopsisEs.ifBlank { cachedEs.orEmpty() },
            synopsisEn = item.synopsisEn.ifBlank { cachedEn.orEmpty() },
            synopsisFr = item.synopsisFr.ifBlank { cachedFr.orEmpty() },
            synopsisJa = item.synopsisJa.ifBlank { cachedJa.orEmpty() },
            activeLanguage = if (preserveActiveLanguage) item.activeLanguage else state.globalLanguage,
            isFavorite = isFav,
            isWatched = isWatched,
            isInWatchlist = isInWatchlist,
            hasReminder = isFav,
            userRating = rating
        )
    }

    private fun recomputeDerivedLists(
        state: AnimeUiState,
        forceGlobalLanguageOnDetail: Boolean = false
    ): AnimeUiState {
        val zone = state.activeZoneId
        val today = LocalDate.now(zone)

        val decoratedSchedule = state.scheduleReleases.map { decorateItem(it, state) }
        val decoratedSeasonal = state.seasonalAnime.map { decorateItem(it, state) }
        val decoratedSeeAllSeasonal = state.seeAllSeasonAnime.map { decorateItem(it, state) }
        val decoratedTodayReleases = state.todayReleases.map { decorateItem(it, state) }
        val decoratedPopular = state.popularAnime.map { decorateItem(it, state) }
        val decoratedSearch = state.searchResults.map { decorateItem(it, state) }
        val decoratedMonth = state.monthReleases.map { decorateItem(it, state) }

        val locale = state.globalLanguage.toLocale()
        val tabs = (0..6).map { offset ->
            val date = state.weekStartDate.plusDays(offset.toLong())
            val isToday = date == today
            val isTomorrow = date == today.plusDays(1)
            val shortDay = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                .replace(".", "")
                .replaceFirstChar { it.uppercase(locale) }
                .take(3)
            val fullDay = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase(locale) }
            val monthName = date.month.getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.uppercase(locale) }

            val dayItems = decoratedSchedule.filter { it.effectiveAiringSeconds() > 0L && it.getAiringLocalDate(zone) == date }
            DayScheduleTab(
                date = date,
                label = shortDay,
                dayOfMonth = date.dayOfMonth,
                monthName = monthName,
                fullDayName = fullDay,
                isToday = isToday,
                isTomorrow = isTomorrow,
                episodeCount = dayItems.size,
                premieresCount = dayItems.count { it.isSeriesPremiere },
                ongoingCount = dayItems.count { it.isOngoingEpisode }
            )
        }

        val filteredDay = decoratedSchedule.filter { item ->
            val matchesDate = item.effectiveAiringSeconds() > 0L && item.getAiringLocalDate(zone) == state.selectedDate
            val matchesFormat = state.calendarFormatFilter.apiValue == null ||
                item.format.equals(state.calendarFormatFilter.apiValue, ignoreCase = true)
            val matchesGenre = state.calendarGenreFilter == null ||
                GenreHelper.matchesAnyGenre(item.genres, setOf(state.calendarGenreFilter))
            val matchesStudio = state.calendarStudioFilter == null ||
                item.studio?.contains(state.calendarStudioFilter, ignoreCase = true) == true
            matchesDate && matchesFormat && matchesGenre && matchesStudio
        }.sortedBy { it.effectiveAiringSeconds() }

        val filteredWatchlist = state.watchlistItems.filter { item ->
            val watchedCount = maxOf(
                state.watchedEpisodeKeys.count { it.startsWith("${item.animeId}_") },
                item.watchedEpisodes
            )
            val isFullyWatched = item.statusEnum == WatchlistStatus.COMPLETED ||
                (item.totalEpisodes != null && item.totalEpisodes > 0 && watchedCount >= item.totalEpisodes)
            val isInProgress = !isFullyWatched && (item.statusEnum == WatchlistStatus.WATCHING || watchedCount > 0)
            when (state.myListFilterTab) {
                MyListFilterTab.ALL -> true
                MyListFilterTab.WATCHED -> isFullyWatched
                MyListFilterTab.UNWATCHED -> !isFullyWatched && !isInProgress
                MyListFilterTab.WATCHING -> isInProgress
                MyListFilterTab.PLAN_TO_WATCH -> !isFullyWatched && !isInProgress && item.statusEnum == WatchlistStatus.PLAN_TO_WATCH
            }
        }.let { list ->
            when (state.myListSortOrder) {
                WatchlistSortOrder.RECENT -> list.sortedByDescending { it.addedAt }
                WatchlistSortOrder.TITLE_AZ -> list.sortedBy { it.displayTitle.lowercase() }
                WatchlistSortOrder.SCORE -> list.sortedByDescending { it.score ?: 0.0 }
                WatchlistSortOrder.PROGRESS -> list.sortedByDescending { it.watchedEpisodes }
            }
        }

        val updatedDetail = state.selectedAnimeForDetail?.let {
            decorateItem(it, state, preserveActiveLanguage = !forceGlobalLanguageOnDetail)
        }

        return state.copy(
            scheduleReleases = decoratedSchedule,
            seasonalAnime = decoratedSeasonal,
            seeAllSeasonAnime = decoratedSeeAllSeasonal,
            todayReleases = decoratedTodayReleases,
            popularAnime = decoratedPopular,
            searchResults = decoratedSearch,
            monthReleases = decoratedMonth,
            dayTabs = tabs,
            filteredDayReleases = filteredDay,
            filteredWatchlistItems = filteredWatchlist,
            selectedAnimeForDetail = updatedDetail
        )
    }
}
