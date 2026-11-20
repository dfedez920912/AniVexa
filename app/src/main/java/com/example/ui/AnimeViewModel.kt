package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnimeRepository
import com.example.data.local.WatchlistEntity
import com.example.data.local.WatchlistStatus
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

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
    val deviceSecurityReport: DeviceSecurityReport = DeviceSecurityReport()
) {
    val activeZoneId: ZoneId
        get() = if (isJapanTimeZone) ZoneId.of("Asia/Tokyo") else ZoneId.systemDefault()
}

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnimeRepository(application.applicationContext)
    private val prefs = application.getSharedPreferences("anivexa_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        AnimeUiState(
            appThemeMode = loadSavedTheme(),
            isJapanTimeZone = prefs.getBoolean("jst_zone", false),
            notificationsEnabled = prefs.getBoolean("notif_enabled", true),
            defaultReminderMinutes = prefs.getInt("default_reminder_min", 15),
            quietHoursEnabled = prefs.getBoolean("quiet_hours", false),
            globalLanguage = if (prefs.getString("synopsis_lang", "es") == "en") {
                SynopsisLanguage.ENGLISH
            } else {
                SynopsisLanguage.SPANISH
            }
        )
    )
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var seeAllSeasonJob: Job? = null
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
                val warningMsg = when {
                    notifyResult && report.isRooted ->
                        "Aviso: dispositivo con acceso root detectado. Los datos locales pueden quedar expuestos a otras apps con privilegios."
                    notifyResult ->
                        "Verificación de seguridad completada: entorno sin root (${report.playIntegrityStatus.label})."
                    showRootWarning && report.isRooted ->
                        "Aviso de seguridad: dispositivo rooteado detectado. La app sigue operativa, pero protege tus datos locales."
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
        if (preferredLang == SynopsisLanguage.SPANISH && decorated.synopsisEs.isBlank() && decorated.synopsisEn.isNotBlank()) {
            translateDetailSynopsisInBackground(decorated.id, decorated.synopsisEn)
        }
        detailJob = viewModelScope.launch {
            val bundle = repository.getAnimeDetailBundle(anime.id)
            if (_uiState.value.selectedAnimeForDetail?.id != anime.id) return@launch
            if (bundle != null) {
                val mergedAnime = decorateItem(
                    bundle.anime.copy(
                        episode = if (anime.airingAtSeconds > 0L) anime.episode else bundle.anime.episode,
                        airingAtSeconds = if (anime.airingAtSeconds > 0L) anime.airingAtSeconds else bundle.anime.airingAtSeconds,
                        synopsisEs = bundle.anime.synopsisEs.ifBlank { anime.synopsisEs },
                        activeLanguage = _uiState.value.globalLanguage
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
                if (_uiState.value.globalLanguage == SynopsisLanguage.SPANISH &&
                    mergedAnime.synopsisEs.isBlank() &&
                    mergedAnime.synopsisEn.isNotBlank()
                ) {
                    translateDetailSynopsisInBackground(mergedAnime.id, mergedAnime.synopsisEn)
                }
            } else {
                _uiState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    private fun translateDetailSynopsisInBackground(animeId: Int, englishSynopsis: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                val current = state.selectedAnimeForDetail
                if (current?.id == animeId) {
                    state.copy(selectedAnimeForDetail = current.copy(isTranslating = true))
                } else state
            }
            val translated = repository.translateSynopsis(englishSynopsis, SynopsisLanguage.SPANISH)
            _uiState.update { state ->
                val latest = state.selectedAnimeForDetail
                if (latest?.id == animeId) {
                    state.copy(
                        selectedAnimeForDetail = latest.copy(
                            synopsisEs = translated,
                            activeLanguage = SynopsisLanguage.SPANISH,
                            isTranslating = false
                        )
                    )
                } else state
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
            _uiState.update {
                it.copy(
                    notificationMessage = if (added) {
                        "Añadido '${anime.displayTitle}' a Mi Lista"
                    } else {
                        "Eliminado '${anime.displayTitle}' de Mi Lista"
                    }
                )
            }
        }
    }

    fun updateWatchlistStatus(animeId: Int, status: WatchlistStatus) {
        viewModelScope.launch {
            repository.updateWatchlistStatus(animeId, status)
            _uiState.update { it.copy(notificationMessage = "Estado actualizado: ${status.displayName}") }
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
            _uiState.update { it.copy(notificationMessage = "Anime eliminado de Mi Lista") }
        }
    }

    fun toggleWatchedEpisode(anime: AnimeRelease) {
        viewModelScope.launch {
            val ep = anime.effectiveEpisodeNumber()
            val nowWatched = repository.toggleWatchedEpisode(anime.id, ep)
            _uiState.update {
                it.copy(
                    notificationMessage = if (nowWatched) {
                        "Episodio $ep marcado como visto"
                    } else {
                        "Episodio $ep marcado como no visto"
                    }
                )
            }
        }
    }

    fun toggleWatchedEpisodeNum(animeId: Int, episodeNumber: Int) {
        viewModelScope.launch {
            repository.toggleWatchedEpisode(animeId, episodeNumber)
        }
    }

    fun toggleFavoriteReminder(anime: AnimeRelease) {
        viewModelScope.launch {
            val defaultMins = _uiState.value.defaultReminderMinutes
            val targetAnime = anime.copy(reminderMinutesBefore = defaultMins)
            val added = repository.toggleFavorite(targetAnime)
            _uiState.update {
                it.copy(
                    notificationMessage = if (added) {
                        "Recordatorio activado para '${anime.displayTitle}' ($defaultMins min antes)"
                    } else {
                        "Recordatorio desactivado para '${anime.displayTitle}'"
                    }
                )
            }
        }
    }

    fun updateReminder(anime: AnimeRelease, enabled: Boolean, minutesBefore: Int) {
        viewModelScope.launch {
            repository.updateReminder(anime, enabled, minutesBefore)
            _uiState.update {
                it.copy(
                    notificationMessage = if (enabled) {
                        "Alerta programada $minutesBefore min antes para '${anime.displayTitle}'"
                    } else {
                        "Alerta desactivada"
                    }
                )
            }
        }
    }

    fun testEpisodeNotification(anime: AnimeRelease) {
        val context = getApplication<Application>().applicationContext
        repository.sendTestNotification(anime, _uiState.value.defaultReminderMinutes)
        AnimeEpisodeReminderWorker.scheduleImmediateTest(context, anime)
        _uiState.update { it.copy(notificationMessage = "Notificación de prueba enviada para '${anime.displayTitle}'") }
    }

    fun setUserRating(animeId: Int, rating: Int) {
        viewModelScope.launch {
            repository.setUserRating(animeId, rating)
            _uiState.update {
                it.copy(
                    notificationMessage = if (rating > 0) "Calificación de $rating ★ guardada" else "Calificación eliminada"
                )
            }
        }
    }

    fun setAnimeSynopsisLanguage(animeId: Int, language: SynopsisLanguage) {
        val currentDetail = _uiState.value.selectedAnimeForDetail ?: return
        if (currentDetail.id != animeId) return

        if (language == SynopsisLanguage.SPANISH && currentDetail.synopsisEs.isBlank() && currentDetail.synopsisEn.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(selectedAnimeForDetail = currentDetail.copy(isTranslating = true))
                }
                val translated = repository.translateSynopsis(currentDetail.synopsisEn, SynopsisLanguage.SPANISH)
                _uiState.update { state ->
                    val latest = state.selectedAnimeForDetail
                    if (latest?.id == animeId) {
                        state.copy(
                            selectedAnimeForDetail = latest.copy(
                                synopsisEs = translated,
                                activeLanguage = SynopsisLanguage.SPANISH,
                                isTranslating = false
                            )
                        )
                    } else state
                }
            }
        } else {
            _uiState.update {
                it.copy(selectedAnimeForDetail = currentDetail.copy(activeLanguage = language))
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
        com.example.widget.WidgetDataHelper.clearTodaySnapshot(getApplication())
        com.example.widget.WidgetDataHelper.notifyAllWidgets(getApplication())
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
        _uiState.update { it.copy(globalLanguage = lang) }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            repository.clearOfflineCache()
            _uiState.update { it.copy(notificationMessage = "Caché local limpiada correctamente") }
        }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(notificationMessage = null) }
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

    private fun decorateItem(item: AnimeRelease, state: AnimeUiState): AnimeRelease {
        val ep = item.effectiveEpisodeNumber()
        val isFav = state.favoriteIds.contains(item.id)
        val isWatched = state.watchedEpisodeKeys.contains("${item.id}_$ep")
        val isInWatchlist = state.watchlistIds.contains(item.id)
        val rating = state.userRatings[item.id] ?: item.userRating
        return item.copy(
            isFavorite = isFav,
            isWatched = isWatched,
            isInWatchlist = isInWatchlist,
            hasReminder = isFav,
            userRating = rating
        )
    }

    private fun recomputeDerivedLists(state: AnimeUiState): AnimeUiState {
        val zone = state.activeZoneId
        val today = LocalDate.now(zone)

        val decoratedSchedule = state.scheduleReleases.map { decorateItem(it, state) }
        val decoratedSeasonal = state.seasonalAnime.map { decorateItem(it, state) }
        val decoratedSeeAllSeasonal = state.seeAllSeasonAnime.map { decorateItem(it, state) }
        val decoratedTodayReleases = state.todayReleases.map { decorateItem(it, state) }
        val decoratedPopular = state.popularAnime.map { decorateItem(it, state) }
        val decoratedSearch = state.searchResults.map { decorateItem(it, state) }
        val decoratedMonth = state.monthReleases.map { decorateItem(it, state) }

        val tabs = (0..6).map { offset ->
            val date = state.weekStartDate.plusDays(offset.toLong())
            val isToday = date == today
            val isTomorrow = date == today.plusDays(1)
            val shortDay = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))
                .replace(".", "")
                .replaceFirstChar { it.uppercase() }
                .take(3)
            val fullDay = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
                .replaceFirstChar { it.uppercase() }
            val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale("es"))
                .replaceFirstChar { it.uppercase() }

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

        val updatedDetail = state.selectedAnimeForDetail?.let { decorateItem(it, state) }

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
